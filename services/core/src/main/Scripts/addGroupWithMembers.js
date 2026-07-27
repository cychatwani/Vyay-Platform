// addGroupWithMembers.js
// Provision a group owned by a given seeded user and fill it to a target member count
// via the group's PRIMARY link, topping up the user pool if short.
//
// PREREQ: server up with app.auth.skip-email-verification=true.
//
// Required:  --userNumber <n>   --memberSize <m>
// Optional:  --name "..."   --description "..."   --type TRIP|HOME|COUPLE|OTHER
//
// Examples:
//   node addGroupWithMembers.js --userNumber 1 --memberSize 10
//   node addGroupWithMembers.js --userNumber 3 --memberSize 5 --name "Goa Trip" --description "weekend" --type HOME

const api = require('./lib/api');
const { ensureUsers } = require('./lib/users');

function parseArgs(argv) {
  const opts = {};
  for (let i = 0; i < argv.length; i++) {
    const raw = argv[i];
    if (!raw.startsWith('--')) continue;
    const eq = raw.indexOf('=');
    if (eq !== -1) {
      opts[raw.slice(2, eq).toLowerCase()] = raw.slice(eq + 1);
    } else {
      opts[raw.slice(2).toLowerCase()] = argv[i + 1];
      i++; // consume the value
    }
  }
  return opts;
}

function usageAndExit() {
  console.error('Required: --userNumber <n> --memberSize <m> (both positive integers).');
  console.error('Optional: --name "..." --description "..." --type TRIP|HOME|COUPLE|OTHER');
  process.exit(1);
}

async function main() {
  const opts = parseArgs(process.argv.slice(2));
  const userNumber = Number(opts.usernumber);
  const memberSize = Number(opts.membersize);
  const name = opts.name;
  const description = opts.description;
  const type = (opts.type || 'TRIP').toUpperCase();

  if (!Number.isInteger(userNumber) || userNumber < 1 ||
      !Number.isInteger(memberSize) || memberSize < 1) {
    usageAndExit();
  }

  // Need the admin (#userNumber) plus enough distinct joiners to reach memberSize.
  const users = await ensureUsers(Math.max(userNumber, memberSize));
  const admin = users.find((u) => u.userNumber === userNumber);
  if (!admin) {
    console.error(`user #${userNumber} not found even after top-up`);
    process.exit(1);
  }

  // 1. Admin logs in and creates the group.
  const adminSession = await api.login(admin.email, admin.password);
  const adminToken = adminSession.accessToken;

  const groupRes = await api.post('/groups', {
    name: name || `Group of ${admin.email}`,
    description: description || `seeded with ${memberSize} members`,
    type,
    defaultCurrencyCode: 'INR',
  }, adminToken);
  if (groupRes.status !== 201) {
    throw new Error(`create group failed: ${groupRes.status} ${JSON.stringify(groupRes.body)}`);
  }
  const groupId = groupRes.body.data.groupId;

  // 2. Mint a PRIMARY invite for a usable join token (create response doesn't expose one).
  const inviteRes = await api.post(`/groups/${groupId}/invites`, { type: 'PRIMARY' }, adminToken);
  if (inviteRes.status !== 201) {
    throw new Error(`create primary invite failed: ${inviteRes.status} ${JSON.stringify(inviteRes.body)}`);
  }
  const primaryToken = inviteRes.body.data.token;

  // 3. Everyone except the admin joins via the primary link (admin is already member #1).
  const joiners = users.filter((u) => u.userNumber !== userNumber).slice(0, memberSize - 1);
  let joined = 0;

  for (const u of joiners) {
    const session = await api.login(u.email, u.password);
    const res = await api.post('/invites/join', { token: primaryToken }, session.accessToken);
    if (res.status === 200) {
      joined++;
      process.stdout.write(`\rjoined ${joined}/${joiners.length}   `);
    } else {
      console.error(`\n#${u.userNumber} (${u.email}) failed: ${res.status} ${res.body && res.body.errorCode}`);
    }
  }

  console.log(`\nDone. Created group ${groupId} ("${name || `Group of ${admin.email}`}") with ${joined + 1} members (admin #${userNumber} + ${joined} joined).`);
}

main();
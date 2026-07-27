// addNewUser.js
// Dev seeder: registers N test users via the HTTP API and records them in testDevUser.csv.
//
// PREREQ: server running with app.auth.skip-email-verification=true, otherwise users
// register but stay unverified and can't log in.
//
// Usage:
//   node addNewUser.js --numberOfUsers 100
//   node addNewUser.js                       (defaults to 1)
//   BASE=http://localhost:8080/api node addNewUser.js --numberOfUsers 50

const fs = require('fs');
const path = require('path');

const BASE = process.env.BASE || 'http://localhost:8080/api';
const DOMAIN = 'splitesy.test';
const PASSWORD_PREFIX = 'PassForTestUser@';

const SCRIPT_DIR = __dirname;
const RESOURCES_DIR = path.join(SCRIPT_DIR, 'resources');
const CSV_PATH = path.join(SCRIPT_DIR, 'testDevUser.csv');
const CSV_HEADER = 'userNumber,firstName,lastName,email,password';

function parseCount(argv) {
  let count = 1;
  for (let i = 0; i < argv.length; i++) {
    const a = argv[i].toLowerCase();
    if (a === '--numberofusers') { count = parseInt(argv[i + 1], 10); break; }
    if (a.startsWith('--numberofusers=')) { count = parseInt(a.split('=')[1], 10); break; }
  }
  if (!Number.isInteger(count) || count < 1) {
    console.error('--numberOfUsers must be a positive integer; defaulting to 1.');
    return 1;
  }
  return count;
}

function loadNames(file) {
  const full = path.join(RESOURCES_DIR, file);
  const names = fs.readFileSync(full, 'utf8').split('\n').map(s => s.trim()).filter(Boolean);
  if (names.length === 0) throw new Error(`No names found in ${full}`);
  return names;
}

function stripBom(s) {
  return s.charCodeAt(0) === 0xFEFF ? s.slice(1) : s;
}

function nextIndexFromCsv() {
  if (!fs.existsSync(CSV_PATH)) {
    fs.writeFileSync(CSV_PATH, CSV_HEADER + '\n');
    return 1;
  }
  const raw = stripBom(fs.readFileSync(CSV_PATH, 'utf8'));
  const lines = raw.split('\n').map(s => s.trim()).filter(Boolean);
  if (lines.length === 0) return 1;

  // Detect header explicitly — survives a missing or extra header line.
  const firstCol = (lines[0].split(',')[0] || '').trim().toLowerCase();
  const dataStart = firstCol === 'usernumber' ? 1 : 0;

  let max = 0;
  for (let i = dataStart; i < lines.length; i++) {
    const n = parseInt(lines[i].split(',')[0], 10);
    if (Number.isInteger(n) && n > max) max = n;
  }
  return max + 1;
}

const pick = arr => arr[Math.floor(Math.random() * arr.length)];

async function main() {
  const count = parseCount(process.argv.slice(2));
  const firstNames = loadNames('firstNames.txt');
  const lastNames = loadNames('lastNames.txt');
  const start = nextIndexFromCsv();

  console.log(`Registering ${count} user(s) starting at #${start} against ${BASE}`);

  let created = 0, failed = 0;

  for (let i = 0; i < count; i++) {
    const n = start + i;
    const firstName = pick(firstNames);
    const lastName = pick(lastNames);
    const email = `${firstName}.${lastName}.${n}@${DOMAIN}`.toLowerCase();
    const password = `${PASSWORD_PREFIX}${n}`;

    let res;
    try {
      res = await fetch(`${BASE}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ firstName, lastName, email, password }),
      });
    } catch (err) {
      console.error(`\nNetwork error reaching ${BASE} - is the server running? (${err.message})`);
      break;
    }

    if (res.status === 200 || res.status === 201) {
      fs.appendFileSync(CSV_PATH, [n, firstName, lastName, email, password].join(',') + '\n');
      created++;
      process.stdout.write(`\r#${n} ok  (${created} created)        `);
    } else {
      failed++;
      let msg = '';
      try { msg = (await res.json()).message || ''; } catch (e) {}
      console.error(`\n#${n} FAILED (${res.status}) ${email} ${msg}`);
    }
  }

  console.log(`\nDone. created=${created} failed=${failed}`);
  console.log(`CSV: ${CSV_PATH}`);
}

main();
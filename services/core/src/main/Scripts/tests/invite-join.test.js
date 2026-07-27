// tests/invite-join.test.js
// Run:  node --test src/main/Scripts/tests/
// Requires the server running with app.auth.skip-email-verification=true.

const { test, describe } = require('node:test');
const assert = require('node:assert/strict');
const { makeUser, makeGroup, makeInvite, join, leave } = require('../lib/factory');

describe('POST /invites/join', () => {

  test('happy path: open invite lets a new user join', async () => {
    const admin = await makeUser();
    const group = await makeGroup   (admin.token);
    const invite = await makeInvite(admin.token, group.id, { maxUses: 5 });
    const joiner = await makeUser();

    const res = await join(joiner.token, invite.token);

    assert.equal(res.status, 200);
    assert.equal(res.body.errorCode, null);
    const memberIds = res.body.data.members.map((m) => m.userId);
    assert.ok(memberIds.includes(joiner.userId), 'joiner should appear in members');
    assert.equal(res.body.data.memberCount, 2);
  });

  test('already a member: re-joining returns 409', async () => {
    const admin = await makeUser();
    const group = await makeGroup(admin.token);
    const invite = await makeInvite(admin.token, group.id, { maxUses: 5 });
    const joiner = await makeUser();

    const first = await join(joiner.token, invite.token);
    assert.equal(first.status, 200);

    const second = await join(joiner.token, invite.token);
    assert.equal(second.status, 409);
    assert.equal(second.body.errorCode, 'ERR_ALREADY_A_MEMBER');
  });

  test('exhausted: a second joiner on a maxUses:1 link returns 409', async () => {
    const admin = await makeUser();
    const group = await makeGroup(admin.token);
    const invite = await makeInvite(admin.token, group.id, { maxUses: 1 });
    const first = await makeUser();
    const second = await makeUser();

    const ok = await join(first.token, invite.token);
    assert.equal(ok.status, 200);

    const res = await join(second.token, invite.token);
    assert.equal(res.status, 409);
    assert.equal(res.body.errorCode, 'ERR_INVITE_LINK_EXHAUSTED');
  });

  test('allowlist: outsider gets 403, invited user gets in', async () => {
    const admin = await makeUser();
    const group = await makeGroup(admin.token);
    const invited = await makeUser();
    const outsider = await makeUser();
    const invite = await makeInvite(admin.token, group.id, {
      maxUses: 5,
      invitedUsers: [invited.userId],
    });

    const blocked = await join(outsider.token, invite.token);
    assert.equal(blocked.status, 403);
    assert.equal(blocked.body.errorCode, 'ERR_USER_NOT_INVITED');

    const allowed = await join(invited.token, invite.token);
    assert.equal(allowed.status, 200);
  });

  test('inactive: an old PRIMARY token rejected after rotation returns 409', async () => {
    const admin = await makeUser();
    const group = await makeGroup(admin.token);
    const oldPrimary = await makeInvite(admin.token, group.id, { type: 'PRIMARY' });
    await makeInvite(admin.token, group.id, { type: 'PRIMARY' }); // rotates -> oldPrimary deactivated
    const joiner = await makeUser();

    const res = await join(joiner.token, oldPrimary.token);
    assert.equal(res.status, 409);
    assert.equal(res.body.errorCode, 'ERR_INVITE_LINK_INACTIVE');
  });

  test('rejoin: a LEFT member can rejoin and the row flips back to ACTIVE', async () => {
    const admin = await makeUser();
    const group = await makeGroup(admin.token);
    const invite = await makeInvite(admin.token, group.id, { maxUses: 5 });
    const member = await makeUser();

    const joined = await join(member.token, invite.token);
    assert.equal(joined.status, 200);

    const left = await leave(member.token, group.id);
    assert.equal(left.status, 200);

    const rejoined = await join(member.token, invite.token);
    assert.equal(rejoined.status, 200);
    const memberIds = rejoined.body.data.members.map((m) => m.userId);
    assert.ok(memberIds.includes(member.userId), 'rejoined member should be back in members');
    assert.equal(rejoined.body.data.memberCount, 2);
  });
});
// lib/factory.js - populate-on-demand helpers. Each test provisions exactly what it needs.

const api = require('./api');

const RUN = Date.now().toString(36); // run-scoped suffix so re-runs never collide
let seq = 0;

async function makeUser() {
  seq += 1;
  const email = `t-${RUN}-${seq}@vyay.test`;
  const password = 'TestPass@12345';

  const reg = await api.post('/auth/register', {
    firstName: 'Test',
    lastName: 'User',
    email,
    password,
  });
  if (reg.status !== 201) {
    throw new Error(`register failed: ${reg.status} ${JSON.stringify(reg.body)}`);
  }

  const session = await api.login(email, password);
  return {
    email,
    password,
    token: session.accessToken,
    userId: session.userDetails.userId,
  };
}

async function makeGroup(token, overrides = {}) {
  seq += 1;
  const res = await api.post('/groups', {
    name: overrides.name || `Group-${RUN}-${seq}`,
    description: overrides.description || 'test group',
    type: overrides.type || 'TRIP',
    defaultCurrencyCode: overrides.defaultCurrencyCode || 'INR',
  }, token);
  if (res.status !== 201) {
    throw new Error(`createGroup failed: ${res.status} ${JSON.stringify(res.body)}`);
  }
  // API now exposes groupId (semantic name); aliasing as .id keeps tests terse.
  const data = res.body.data;
  return { ...data, id: data.groupId };
}

async function makeInvite(token, groupId, opts = {}) {
  const res = await api.post(`/groups/${groupId}/invites`, { type: 'TEMPORARY', ...opts }, token);
  if (res.status !== 201) {
    throw new Error(`createInvite failed: ${res.status} ${JSON.stringify(res.body)}`);
  }
  return res.body.data;
}

// Actions under test - return raw { status, body } so tests can assert.
function join(token, inviteToken) {
  return api.post('/invites/join', { token: inviteToken }, token);
}

function leave(token, groupId) {
  return api.post(`/groups/${groupId}/leave`, undefined, token);
}

module.exports = { makeUser, makeGroup, makeInvite, join, leave, RUN };
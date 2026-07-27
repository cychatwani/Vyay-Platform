// lib/api.js - thin HTTP client for the vyay API.
// Override the base with:  BASE=http://host:port/api node --test ...

const BASE = process.env.BASE || 'http://localhost:8080/api';

async function request(method, path, { token, body } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const text = await res.text();
  let parsed = null;
  if (text) {
    try { parsed = JSON.parse(text); } catch { parsed = text; }
  }
  return { status: res.status, body: parsed };
}

const get = (path, token) => request('GET', path, { token });
const post = (path, body, token) => request('POST', path, { token, body });

async function login(email, password) {
  const res = await post('/auth/login', { email, password });
  if (res.status !== 200) {
    throw new Error(`login failed for ${email}: ${res.status} ${JSON.stringify(res.body)}`);
  }
  return res.body.data; // { accessToken, refreshToken, userDetails: { id, ... } }
}

module.exports = { BASE, request, get, post, login };

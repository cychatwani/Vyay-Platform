// lib/users.js - the seeded user pool: load from testDevUser.csv and top up as needed.

const fs = require('fs');
const path = require('path');
const api = require('./api');

const SCRIPTS_DIR = path.join(__dirname, '..');
const CSV_PATH = path.join(SCRIPTS_DIR, 'testDevUser.csv');
const RESOURCES_DIR = path.join(SCRIPTS_DIR, 'resources');
const CSV_HEADER = 'userNumber,firstName,lastName,email,password';
const DOMAIN = 'splitesy.test';
const PASSWORD_PREFIX = 'PassForTestUser@';

function readNames(file) {
  return fs.readFileSync(path.join(RESOURCES_DIR, file), 'utf8')
    .split('\n').map((s) => s.trim()).filter(Boolean);
}

// Strips a leading UTF-8 BOM if present. Excel / Notepad on Windows like to add one.
function stripBom(s) {
  return s.charCodeAt(0) === 0xFEFF ? s.slice(1) : s;
}

function loadUsers() {
  if (!fs.existsSync(CSV_PATH)) {
    fs.writeFileSync(CSV_PATH, CSV_HEADER + '\n');
    return [];
  }
  const raw = stripBom(fs.readFileSync(CSV_PATH, 'utf8'));
  const lines = raw.split('\n').map((s) => s.trim()).filter(Boolean);
  if (lines.length === 0) return [];

  // Detect header explicitly instead of blindly slicing — survives a missing or extra header.
  const firstCol = (lines[0].split(',')[0] || '').trim().toLowerCase();
  const dataLines = firstCol === 'usernumber' ? lines.slice(1) : lines;

  return dataLines.map((line) => {
    const [userNumber, firstName, lastName, email, password] = line.split(',');
    return { userNumber: Number(userNumber), firstName, lastName, email, password };
  });
}

const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];

// Ensures the pool has at least `target` users (numbered 1..target). Registers + appends any shortfall.
async function ensureUsers(target) {
  const users = loadUsers();
  if (users.length >= target) return users;

  const firstNames = readNames('firstNames.txt');
  const lastNames = readNames('lastNames.txt');
  const max = users.reduce((m, u) => Math.max(m, u.userNumber), 0);

  for (let n = max + 1; n <= target; n++) {
    const firstName = pick(firstNames);
    const lastName = pick(lastNames);
    const email = `${firstName}.${lastName}.${n}@${DOMAIN}`.toLowerCase();
    const password = `${PASSWORD_PREFIX}${n}`;

    const res = await api.post('/auth/register', { firstName, lastName, email, password });
    if (res.status !== 201) {
      throw new Error(`top-up register failed for #${n}: ${res.status} ${JSON.stringify(res.body)}`);
    }
    fs.appendFileSync(CSV_PATH, [n, firstName, lastName, email, password].join(',') + '\n');
    users.push({ userNumber: n, firstName, lastName, email, password });
    process.stdout.write(`\rtopped up user #${n}   `);
  }
  if (target > max) process.stdout.write('\n');
  return users;
}

module.exports = { loadUsers, ensureUsers };
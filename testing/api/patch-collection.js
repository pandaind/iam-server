/**
 * Patches iam-server.postman_collection.json to be idempotent:
 *  1. Adds a re-login request at the start of the Cleanup folder
 *  2. Adds pre-request scripts to skip DELETE if ID variable is empty
 *  3. Changes cleanup assertions to accept 204 OR 404 (resources may already be gone)
 *  4. Handles 409 Conflict on create by looking up the existing ID via sendRequest
 */

const fs = require('fs');
const path = require('path');

const collectionPath = path.join(__dirname, 'iam-server.postman_collection.json');
const c = JSON.parse(fs.readFileSync(collectionPath, 'utf8'));

// ─── 1. Cleanup folder fixes ────────────────────────────────────────────────

const cleanupFolder = c.item.find(f => f.name.includes('Cleanup'));
if (!cleanupFolder) throw new Error('Cleanup folder not found');

// Add re-login request at the start of cleanup
const already = cleanupFolder.item.some(r => r.name.includes('Re-login before cleanup'));
if (!already) {
  const freshLoginReq = {
    name: 'Re-login before cleanup',
    event: [
      {
        listen: 'test',
        script: {
          type: 'text/javascript',
          exec: [
            "pm.test('Status 200', () => pm.response.to.have.status(200));",
            "const j = pm.response.json();",
            "if (j.accessToken) pm.collectionVariables.set('accessToken', j.accessToken);",
            "if (j.refreshToken) pm.collectionVariables.set('refreshToken', j.refreshToken);"
          ]
        }
      }
    ],
    request: {
      method: 'POST',
      header: [{ key: 'Content-Type', value: 'application/json' }],
      body: {
        mode: 'raw',
        raw: '{"username":"{{adminUsername}}","password":"{{adminPassword}}"}'
      },
      url: '{{baseUrl}}/auth/login'
    }
  };
  cleanupFolder.item.unshift(freshLoginReq);
}

// Fix each cleanup DELETE/GET request
cleanupFolder.item.forEach(req => {
  if (req.name.includes('Re-login')) return;

  const urlStr = typeof req.request.url === 'string'
    ? req.request.url
    : JSON.stringify(req.request.url || '');

  let idVar = null;
  if (urlStr.includes('testPermId')) idVar = 'testPermId';
  else if (urlStr.includes('testRoleId')) idVar = 'testRoleId';
  else if (urlStr.includes('testUserId')) idVar = 'testUserId';

  if (!idVar) return;

  if (!req.event) req.event = [];

  // Add pre-request script to skip if ID is empty (avoid 500 from empty URL segment)
  const hasPrereq = req.event.some(ev => ev.listen === 'prerequest');
  if (!hasPrereq) {
    req.event.push({
      listen: 'prerequest',
      script: {
        type: 'text/javascript',
        exec: [
          `const id = pm.collectionVariables.get('${idVar}');`,
          `if (!id || id.trim() === '') {`,
          `  console.log('Skipping cleanup: ${idVar} is not set');`,
          `  pm.execution.skipRequest();`,
          `}`
        ]
      }
    });
  }

  // Update test assertions to accept 204 OR 404 (idempotent)
  req.event.forEach(ev => {
    if (ev.listen !== 'test') return;
    ev.script.exec = ev.script.exec.map(line => {
      if (line.includes('status(204)') || line.includes("status code 204")) {
        return line
          .replace('pm.response.to.have.status(204)', 'pm.expect([204, 404]).to.include(pm.response.code)')
          .replace("expected response to have status code 204", 'Expected 204 or 404');
      }
      if (line.includes('status(404)') || line.includes("status code 404")) {
        return line
          .replace('pm.response.to.have.status(404)', 'pm.expect([404, 204]).to.include(pm.response.code)')
          .replace("expected response to have status code 404", 'Expected 404 or 204');
      }
      return line;
    });
  });
});

// ─── 2. Handle 409 on CREATE requests ───────────────────────────────────────

function addConflictHandler(folderNamePattern, requestNamePattern, idVarName, lookupUrl, matchFn) {
  const folder = c.item.find(f => f.name.match(folderNamePattern));
  if (!folder) { console.warn('Folder not found:', folderNamePattern); return; }
  const req = folder.item.find(r => r.name.match(requestNamePattern));
  if (!req) { console.warn('Request not found:', requestNamePattern); return; }

  req.event = req.event || [];
  const testEv = req.event.find(ev => ev.listen === 'test');
  if (!testEv) return;

  const already = testEv.script.exec.some(l => l.includes('409'));
  if (already) return; // already patched

  testEv.script.exec.push(
    '',
    '// Handle 409 Conflict: resource exists from a previous run — look up its ID',
    'if (pm.response.code === 409) {',
    `  pm.sendRequest({`,
    `    url: pm.collectionVariables.get('baseUrl') + '${lookupUrl}',`,
    `    method: 'GET',`,
    `    header: [{ key: 'Authorization', value: 'Bearer ' + pm.collectionVariables.get('accessToken') }]`,
    `  }, function(err, res) {`,
    `    if (err || res.code !== 200) { console.error('Lookup failed:', err); return; }`,
    `    const data = res.json();`,
    `    const items = Array.isArray(data) ? data : (data.content || []);`,
    `    const found = items.find(${matchFn});`,
    `    if (found) {`,
    `      pm.collectionVariables.set('${idVarName}', found.id.toString());`,
    `      console.log('409: found existing ${idVarName} =', found.id);`,
    `    }`,
    `  });`,
    '}'
  );
}

addConflictHandler(
  /03 - Users/,
  /201 create/,
  'testUserId',
  '/users?page=0&size=200',
  "x => x.username === 'testuser_newman'"
);

addConflictHandler(
  /04 - Roles/,
  /201 create/,
  'testRoleId',
  '/roles',
  "x => x.name === 'ROLE_NEWMAN_TEST'"
);

addConflictHandler(
  /05 - Permissions/,
  /201 create/,
  'testPermId',
  '/permissions',
  "x => x.name === 'NEWMAN_TEST_PERM'"
);

// ─── 3. Write patched collection ──────────────────────────────────────────────

fs.writeFileSync(collectionPath, JSON.stringify(c, null, 2));
console.log('Collection patched successfully.');

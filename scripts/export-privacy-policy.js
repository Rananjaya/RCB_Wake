#!/usr/bin/env node
/**
 * Writes PRIVACY_POLICY.md from src/legal/privacyPolicy.ts (the in-app source of truth).
 * Usage: node scripts/export-privacy-policy.js
 */
const fs = require('fs');
const path = require('path');
const ts = require('typescript');

const root = path.join(__dirname, '..');
const source = fs.readFileSync(path.join(root, 'src/legal/privacyPolicy.ts'), 'utf8');
const { outputText } = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2019 },
});
const mod = { exports: {} };
// Trusted local source file, transpiled above.
// eslint-disable-next-line no-new-func
new Function('module', 'exports', outputText)(mod, mod.exports);
fs.writeFileSync(path.join(root, 'PRIVACY_POLICY.md'), mod.exports.privacyPolicyMarkdown());
console.log('Wrote PRIVACY_POLICY.md');

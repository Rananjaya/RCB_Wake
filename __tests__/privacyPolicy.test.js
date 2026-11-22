const fs = require('fs');
const path = require('path');
const { privacyPolicyMarkdown } = require('../src/legal/privacyPolicy');

describe('privacy policy', () => {
  it('PRIVACY_POLICY.md matches the in-app policy', () => {
    const file = fs.readFileSync(
      path.join(__dirname, '..', 'PRIVACY_POLICY.md'),
      'utf8',
    );
    // Run `node scripts/export-privacy-policy.js` after editing src/legal/privacyPolicy.ts.
    expect(file).toBe(privacyPolicyMarkdown());
  });
});

import { accountLabel } from '../src/features/settings/CalendarSettings';

describe('widget calendar', () => {
  it('labels calendar accounts by source', () => {
    expect(
      accountLabel({ accountName: 'me@gmail.com', accountType: 'com.google' }),
    ).toBe('Google · me@gmail.com');
    expect(
      accountLabel({ accountName: 'RCB Calendar', accountType: 'LOCAL' }),
    ).toBe('On this device · RCB Calendar');
    expect(
      accountLabel({
        accountName: 'Work',
        accountType: 'com.example.exchange',
      }),
    ).toBe('Work');
  });
});

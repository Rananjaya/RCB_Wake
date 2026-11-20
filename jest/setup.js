/* eslint-env jest */
jest.mock('../src/native/NativeRcbWake', () => require('./fakeEngine'));
jest.mock('react-native-reanimated', () =>
  require('react-native-reanimated/mock'),
);
jest.mock('react-native-worklets', () => ({}), { virtual: true });
require('react-native-gesture-handler/jestSetup');

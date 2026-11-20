module.exports = {
  presets: ['module:@react-native/babel-preset'],
  // Must be last: compiles Reanimated worklets.
  plugins: ['react-native-worklets/plugin'],
};

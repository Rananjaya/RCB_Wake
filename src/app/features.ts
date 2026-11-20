/**
 * Feature flags for work that is built but not shipped.
 *
 * nfcChallenge: NFC tag dismiss challenge. On hold at the owner's request; the native code is
 * kept, but the UI is hidden and the NFC permission is not declared in the manifest.
 */
export const FEATURES = {
  nfcChallenge: false,
} as const;

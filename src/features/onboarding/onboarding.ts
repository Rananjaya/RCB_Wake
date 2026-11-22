import { create } from 'zustand';
import { engine } from '../../services/engine';

/**
 * Current privacy agreement version. Increase it when the agreement changes materially; users are
 * then asked to accept again on next launch.
 */
export const PRIVACY_AGREEMENT_VERSION = 2;

const KEY_PRIVACY = 'privacy_ack_version';
const KEY_TUTORIAL = 'tutorial_done';

type OnboardingState = {
  loaded: boolean;
  acceptedVersion: number;
  tutorialDone: boolean;
  load: () => Promise<void>;
  accept: () => Promise<void>;
  finishTutorial: () => Promise<void>;
};

export const useOnboardingStore = create<OnboardingState>(set => ({
  loaded: false,
  acceptedVersion: 0,
  tutorialDone: false,
  load: async () => {
    const [ack, tutorial] = await Promise.all([
      engine.getValue(KEY_PRIVACY),
      engine.getValue(KEY_TUTORIAL),
    ]);
    set({
      loaded: true,
      acceptedVersion: Number(ack ?? 0) || 0,
      tutorialDone: tutorial === '1',
    });
  },
  accept: async () => {
    await engine.setValue(KEY_PRIVACY, String(PRIVACY_AGREEMENT_VERSION));
    set({ acceptedVersion: PRIVACY_AGREEMENT_VERSION });
  },
  finishTutorial: async () => {
    await engine.setValue(KEY_TUTORIAL, '1');
    set({ tutorialDone: true });
  },
}));

export type OnboardingStep = 'loading' | 'privacy' | 'tutorial' | 'done';

/** Pure gate rule: agreement (versioned) → tutorial (first launch only) → app. */
export function onboardingStep(
  s: Pick<OnboardingState, 'loaded' | 'acceptedVersion' | 'tutorialDone'>,
): OnboardingStep {
  if (!s.loaded) return 'loading';
  if (s.acceptedVersion < PRIVACY_AGREEMENT_VERSION) return 'privacy';
  if (!s.tutorialDone) return 'tutorial';
  return 'done';
}

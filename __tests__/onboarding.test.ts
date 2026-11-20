import {
  onboardingStep,
  PRIVACY_AGREEMENT_VERSION,
} from '../src/features/onboarding/onboarding';

describe('onboarding gate', () => {
  it('shows the agreement, then the tutorial, then the app', () => {
    expect(
      onboardingStep({
        loaded: false,
        acceptedVersion: 0,
        tutorialDone: false,
      }),
    ).toBe('loading');
    expect(
      onboardingStep({ loaded: true, acceptedVersion: 0, tutorialDone: false }),
    ).toBe('privacy');
    expect(
      onboardingStep({
        loaded: true,
        acceptedVersion: PRIVACY_AGREEMENT_VERSION,
        tutorialDone: false,
      }),
    ).toBe('tutorial');
    expect(
      onboardingStep({
        loaded: true,
        acceptedVersion: PRIVACY_AGREEMENT_VERSION,
        tutorialDone: true,
      }),
    ).toBe('done');
  });
  it('asks again when the agreement version increases', () => {
    expect(
      onboardingStep({
        loaded: true,
        acceptedVersion: PRIVACY_AGREEMENT_VERSION - 1,
        tutorialDone: true,
      }),
    ).toBe('privacy');
  });
});

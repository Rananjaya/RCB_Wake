import type {AlarmConfig, AlarmRecord, Settings} from '../../services/engine';

export const SNOOZE_OPTIONS = [5, 10, 15, 20, 30];
export const MIN_QUESTIONS = 1;
export const MAX_QUESTIONS = 10;

/** A new alarm draft: next whole hour plus the user's defaults. */
export function newAlarmDraft(settings: Settings, now = new Date()): AlarmConfig {
  return {
    id: 0,
    hour: (now.getHours() + 1) % 24,
    minute: 0,
    enabled: true,
    repeatDays: 0,
    label: '',
    soundUri: settings.defaultSoundUri,
    soundName: settings.defaultSoundName,
    vibrationEnabled: settings.defaultVibration,
    gradualVolumeEnabled: settings.defaultGradualVolume,
    snoozeMinutes: settings.defaultSnoozeMinutes,
    challengeEnabled: settings.defaultChallengeEnabled,
    challengeDifficulty: settings.defaultChallengeDifficulty,
    challengeQuestionCount: settings.defaultChallengeQuestionCount,
  };
}

export function draftFromRecord(record: AlarmRecord): AlarmConfig {
  return {
    id: record.id,
    hour: record.hour,
    minute: record.minute,
    // Saving an edited alarm turns it on, as users expect after changing its time.
    enabled: true,
    repeatDays: record.repeatDays,
    label: record.label,
    soundUri: record.soundUri,
    soundName: record.soundName,
    vibrationEnabled: record.vibrationEnabled,
    gradualVolumeEnabled: record.gradualVolumeEnabled,
    snoozeMinutes: record.snoozeMinutes,
    challengeEnabled: record.challengeEnabled,
    challengeDifficulty: record.challengeDifficulty,
    challengeQuestionCount: record.challengeQuestionCount,
  };
}

export function clampQuestions(n: number): number {
  return Math.min(MAX_QUESTIONS, Math.max(MIN_QUESTIONS, Math.round(n)));
}

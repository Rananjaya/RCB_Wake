import React, {useEffect, useRef, useState} from 'react';
import {Pressable, StyleSheet, Text, Vibration, View} from 'react-native';
import Animated, {useAnimatedStyle, useSharedValue, withSequence, withTiming} from 'react-native-reanimated';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Button} from '../../components/ui';
import {useT} from '../../i18n';
import {engine} from '../../services/engine';
import {useAlarmStore} from '../../storage/alarmStore';
import {useTheme} from '../../theme/useTheme';
import {radius, spacing} from '../../theme/themes';
import {generateQuestion, parseProgress, toDifficulty, type ChallengeProgress} from './mathChallenge';

const PROGRESS_KEY = 'challenge_progress';
const EMERGENCY_HOLD_MILLIS = 3000;

/**
 * React Native challenge UI. The alarm keeps ringing natively the whole time; this screen only
 * asks the engine to dismiss once every question is solved (or on emergency dismissal).
 */
export function ChallengeScreen() {
  const theme = useTheme();
  const t = useT();
  const session = useAlarmStore(s => s.session);
  const [progress, setProgress] = useState<ChallengeProgress | null>(null);
  const [feedback, setFeedback] = useState<'correct' | 'incorrect' | null>(null);
  const finishing = useRef(false);
  const shake = useSharedValue(0);
  const shakeStyle = useAnimatedStyle(() => ({transform: [{translateX: shake.value}]}));

  const difficulty = toDifficulty(session?.challengeDifficulty ?? 'easy');
  const total = Math.max(1, session?.challengeQuestionCount ?? 1);

  // Restore persisted progress for this session, or start fresh.
  useEffect(() => {
    if (!session) return;
    engine.getValue(PROGRESS_KEY).then(raw => {
      const restored = parseProgress(raw, session.startedAt);
      setProgress(
        restored ?? {sessionStartedAt: session.startedAt, solved: 0, current: generateQuestion(difficulty)},
      );
    });
  }, [session, difficulty]);

  const persist = (p: ChallengeProgress) => {
    setProgress(p);
    engine.setValue(PROGRESS_KEY, JSON.stringify(p)).catch(() => undefined);
  };

  const answer = (choice: number) => {
    if (!progress || finishing.current) return;
    if (choice === progress.current.answer) {
      const solved = progress.solved + 1;
      setFeedback('correct');
      if (solved >= total) {
        finishing.current = true;
        persist({...progress, solved});
        engine.dismissSession('challenge');
        return;
      }
      persist({...progress, solved, current: generateQuestion(difficulty)});
    } else {
      setFeedback('incorrect');
      Vibration.vibrate(120);
      shake.value = withSequence(
        withTiming(-12, {duration: 50}),
        withTiming(12, {duration: 50}),
        withTiming(-8, {duration: 50}),
        withTiming(0, {duration: 50}),
      );
      // A new question prevents guessing by elimination.
      persist({...progress, current: generateQuestion(difficulty)});
    }
  };

  if (!session) {
    return <SafeAreaView style={[styles.screen, {backgroundColor: theme.colors.background}]} />;
  }

  return (
    <SafeAreaView style={[styles.screen, {backgroundColor: theme.colors.background}]}>
      <View style={styles.top}>
        <Text style={[styles.title, {color: theme.colors.text}]} accessibilityRole="header">
          {t('challengeTitle')}
        </Text>
        {session.labels.filter(Boolean).length > 0 && (
          <Text style={[styles.label, {color: theme.colors.textSecondary}]}>{session.labels.join(' · ')}</Text>
        )}
        <Text style={[styles.progress, {color: theme.colors.secondary}]}>
          {t('questionProgress', {n: Math.min((progress?.solved ?? 0) + 1, total), total})}
        </Text>
        <View style={styles.dots}>
          {Array.from({length: total}, (_, i) => (
            <View
              key={i}
              style={[
                styles.dot,
                {backgroundColor: i < (progress?.solved ?? 0) ? theme.colors.primary : theme.colors.divider},
              ]}
            />
          ))}
        </View>
      </View>

      {progress && (
        <Animated.View style={[styles.questionBox, shakeStyle]}>
          <Text style={[styles.question, {color: theme.colors.text}]} accessibilityLiveRegion="polite">
            {progress.current.prompt} = ?
          </Text>
          <Text
            style={[
              styles.feedback,
              {color: feedback === 'correct' ? theme.colors.success : theme.colors.danger},
            ]}
            accessibilityLiveRegion="assertive">
            {feedback === 'correct' ? t('correct') : feedback === 'incorrect' ? t('incorrect') : ' '}
          </Text>
          <View style={styles.options}>
            {progress.current.options.map((option, i) => (
              <Pressable
                key={`${progress.current.prompt}-${i}`}
                testID={`challenge-option-${i}`}
                onPress={() => answer(option)}
                accessibilityRole="button"
                style={({pressed}) => [
                  styles.option,
                  {backgroundColor: pressed ? theme.colors.selected : theme.colors.surface},
                ]}>
                <Text style={[styles.optionText, {color: theme.colors.text}]}>{option}</Text>
              </Pressable>
            ))}
          </View>
        </Animated.View>
      )}

      <View style={styles.bottom}>
        <Button label={t('snooze')} variant="secondary" onPress={() => engine.snoozeSession()} />
        <Pressable
          testID="emergency-dismiss"
          delayLongPress={EMERGENCY_HOLD_MILLIS}
          onLongPress={() => engine.dismissSession('emergency')}
          accessibilityRole="button"
          accessibilityHint={t('emergencyHint')}
          style={styles.emergency}>
          <Text style={[styles.emergencyText, {color: theme.colors.danger}]}>{t('emergencyDismiss')}</Text>
          <Text style={[styles.emergencyHint, {color: theme.colors.textSecondary}]}>{t('emergencyHint')}</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1, paddingHorizontal: spacing.xl},
  top: {alignItems: 'center', paddingTop: spacing.xxl, gap: spacing.sm},
  title: {fontSize: 24, fontWeight: '600'},
  label: {fontSize: 16},
  progress: {fontSize: 15, fontWeight: '600', marginTop: spacing.sm},
  dots: {flexDirection: 'row', gap: 6},
  dot: {width: 10, height: 10, borderRadius: 5},
  questionBox: {flex: 1, justifyContent: 'center'},
  question: {fontSize: 52, fontWeight: '300', textAlign: 'center'},
  feedback: {fontSize: 16, fontWeight: '600', textAlign: 'center', marginVertical: spacing.lg, minHeight: 22},
  options: {flexDirection: 'row', flexWrap: 'wrap', gap: spacing.md, justifyContent: 'center'},
  option: {width: '46%', height: 76, borderRadius: radius.lg, alignItems: 'center', justifyContent: 'center'},
  optionText: {fontSize: 30, fontWeight: '500'},
  bottom: {paddingBottom: spacing.xl, gap: spacing.lg},
  emergency: {alignItems: 'center', paddingVertical: spacing.sm},
  emergencyText: {fontSize: 15, fontWeight: '700'},
  emergencyHint: {fontSize: 12, marginTop: 2},
});

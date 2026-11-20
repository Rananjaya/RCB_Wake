import React, {useEffect, useState} from 'react';
import {FlatList, StyleSheet, Text, View} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Toolbar} from '../../components/Toolbar';
import {Button, Divider} from '../../components/ui';
import {useT} from '../../i18n';
import type {RootStackParamList} from '../../navigation/types';
import {engine} from '../../services/engine';
import {useTheme} from '../../theme/useTheme';
import {spacing} from '../../theme/themes';
import {formatStopwatch} from '../../utils/time';
import * as sw from './stopwatch';

const STORAGE_KEY = 'stopwatch';

function useStopwatch() {
  const [state, setState] = useState<sw.StopwatchState>(() => sw.initialStopwatch(engine.getBootCount()));
  const [now, setNow] = useState(() => engine.elapsedRealtime());

  useEffect(() => {
    engine.getValue(STORAGE_KEY).then(raw => {
      if (!raw) return;
      try {
        setState(sw.restore(JSON.parse(raw), engine.getBootCount()));
      } catch {
        // Corrupt state is discarded; the stopwatch starts fresh.
      }
    });
  }, []);

  useEffect(() => {
    if (state.status !== 'running') return;
    const id = setInterval(() => setNow(engine.elapsedRealtime()), 33);
    return () => clearInterval(id);
  }, [state.status]);

  const apply = (fn: (s: sw.StopwatchState, now: number) => sw.StopwatchState) => {
    const t = engine.elapsedRealtime();
    setNow(t);
    setState(prev => {
      const next = fn(prev, t);
      engine.setValue(STORAGE_KEY, JSON.stringify(next)).catch(() => undefined);
      return next;
    });
  };

  return {state, now: state.status === 'running' ? now : 0, apply};
}

export function StopwatchScreen() {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const theme = useTheme();
  const t = useT();
  const {state, now, apply} = useStopwatch();
  const rows = sw.lapRows(state.splits);
  const {fastest, slowest} = sw.lapExtremes(rows);
  const currentLap = sw.elapsed(state, now) - (state.splits[state.splits.length - 1] ?? 0);

  return (
    <SafeAreaView edges={['top']} style={[styles.screen, {backgroundColor: theme.colors.background}]}>
      <Toolbar title={t('tabStopwatch')} onSettings={() => navigation.navigate('Settings')} />
      <View style={styles.dial}>
        <Text
          testID="stopwatch-elapsed"
          style={[styles.elapsed, {color: theme.colors.text}]}
          accessibilityRole="timer">
          {formatStopwatch(sw.elapsed(state, now))}
        </Text>
        {state.splits.length > 0 && (
          <Text style={[styles.currentLap, {color: theme.colors.textSecondary}]}>
            {t('lapNumber', {n: state.splits.length + 1})} · {formatStopwatch(currentLap)}
          </Text>
        )}
      </View>

      <View style={styles.controls}>
        {state.status === 'idle' && (
          <Button testID="sw-start" label={t('start')} icon="play" onPress={() => apply(sw.start)} style={styles.wide} />
        )}
        {state.status === 'running' && (
          <>
            <Button testID="sw-lap" label={t('lap')} icon="flag" variant="secondary" onPress={() => apply(sw.lap)} style={styles.half} />
            <Button testID="sw-pause" label={t('pause')} icon="pause" onPress={() => apply(sw.pause)} style={styles.half} />
          </>
        )}
        {state.status === 'paused' && (
          <>
            <Button testID="sw-reset" label={t('reset')} icon="reset" variant="secondary" onPress={() => apply(sw.reset)} style={styles.half} />
            <Button testID="sw-resume" label={t('resume')} icon="play" onPress={() => apply(sw.start)} style={styles.half} />
          </>
        )}
      </View>

      <FlatList
        data={[...rows].reverse()}
        keyExtractor={r => String(r.index)}
        contentContainerStyle={styles.laps}
        ItemSeparatorComponent={Divider}
        renderItem={({item}) => {
          const tag = item.index === fastest ? t('fastest') : item.index === slowest ? t('slowest') : null;
          const color =
            item.index === fastest ? theme.colors.success : item.index === slowest ? theme.colors.warning : theme.colors.text;
          return (
            <View style={styles.lapRow}>
              <Text style={[styles.lapName, {color: theme.colors.textSecondary}]}>{t('lapNumber', {n: item.index})}</Text>
              <Text style={[styles.lapTag, {color}]}>{tag ?? ''}</Text>
              <Text style={[styles.lapTime, {color}]}>{formatStopwatch(item.lapTime)}</Text>
              <Text style={[styles.splitTime, {color: theme.colors.textSecondary}]}>{formatStopwatch(item.splitTime)}</Text>
            </View>
          );
        }}
        ListFooterComponent={
          rows.length > 0 && state.status !== 'running' ? (
            <Button label={t('clearLaps')} variant="secondary" onPress={() => apply(sw.clearLaps)} style={styles.clear} />
          ) : undefined
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1},
  dial: {alignItems: 'center', paddingVertical: spacing.xxl},
  elapsed: {fontSize: 64, fontWeight: '200', fontVariant: ['tabular-nums']},
  currentLap: {fontSize: 16, marginTop: spacing.sm, fontVariant: ['tabular-nums']},
  controls: {flexDirection: 'row', gap: spacing.md, paddingHorizontal: spacing.xl},
  wide: {flex: 1},
  half: {flex: 1},
  laps: {paddingHorizontal: spacing.xl, paddingTop: spacing.lg, paddingBottom: 48},
  lapRow: {flexDirection: 'row', alignItems: 'center', height: 52},
  lapName: {width: 72, fontSize: 15},
  lapTag: {flex: 1, fontSize: 13, fontWeight: '600'},
  lapTime: {fontSize: 17, fontVariant: ['tabular-nums'], marginRight: spacing.lg},
  splitTime: {fontSize: 14, fontVariant: ['tabular-nums']},
  clear: {marginTop: spacing.xl},
});

import React from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import {Button} from '../../components/ui';
import {useT} from '../../i18n';
import type {RootStackParamList} from '../../navigation/types';
import {engine} from '../../services/engine';
import {useAlarmStore} from '../../storage/alarmStore';
import {useTheme} from '../../theme/useTheme';
import {radius, spacing} from '../../theme/themes';

/**
 * Fallback controls for a ringing alarm when the native full-screen UI was not shown
 * (for example, notifications are blocked).
 */
export function RingingBanner() {
  const session = useAlarmStore(s => s.session);
  const theme = useTheme();
  const t = useT();
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  if (!session) return null;
  const label = session.labels.filter(Boolean).join(' · ');
  return (
    <View style={[styles.box, {backgroundColor: theme.colors.selected}]}>
      <Text style={[styles.title, {color: theme.colors.onSelected}]}>{t('alarmRinging')}</Text>
      {label ? <Text style={{color: theme.colors.onSelected}}>{label}</Text> : null}
      <View style={styles.actions}>
        <Button label={t('snooze')} variant="secondary" onPress={() => engine.snoozeSession()} style={styles.action} />
        <Button
          label={session.challengeRequired ? t('solveToDismiss') : t('dismiss')}
          onPress={() =>
            session.challengeRequired ? navigation.navigate('Challenge') : engine.dismissSession('normal')
          }
          style={styles.action}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  box: {borderRadius: radius.md, padding: spacing.lg, marginBottom: spacing.md, gap: spacing.xs},
  title: {fontSize: 17, fontWeight: '700'},
  actions: {flexDirection: 'row', gap: spacing.md, marginTop: spacing.sm},
  action: {flex: 1},
});

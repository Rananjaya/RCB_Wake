import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { FEATURES } from '../../app/features';
import { Icon } from '../../components/Icon';
import { Button, Card, Divider, SectionTitle } from '../../components/ui';
import { useT } from '../../i18n';
import { engine, type NfcStatus, type NfcTagInfo } from '../../services/engine';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

/** Register NFC tags (e.g. one stuck in the bathroom) that dismiss alarms using the NFC challenge. */
export function NfcTagSettings() {
  const theme = useTheme();
  const t = useT();
  const [status, setStatus] = useState<NfcStatus | null>(null);
  const [tags, setTags] = useState<NfcTagInfo[]>([]);
  const [scanning, setScanning] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setStatus(await engine.getNfcStatus());
    setTags(await engine.getNfcTags());
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  if (!FEATURES.nfcChallenge || !status?.supported) return null;

  const scan = async () => {
    setMessage(null);
    setScanning(true);
    try {
      const tag = await engine.registerNfcTag(`Tag ${tags.length + 1}`);
      setMessage(tag ? t('nfcTagAdded') : t('nfcScanTimeout'));
    } catch {
      setMessage(t('nfcOff'));
    } finally {
      setScanning(false);
      reload();
    }
  };

  return (
    <>
      <SectionTitle>{t('nfcTags')}</SectionTitle>
      <Card style={styles.card}>
        <Text style={[styles.hint, { color: theme.colors.textSecondary }]}>
          {t('nfcTagsHint')}
        </Text>
        {tags.map(tag => (
          <View key={tag.id}>
            <Divider />
            <View style={styles.row}>
              <Text style={[styles.name, { color: theme.colors.text }]}>
                {tag.name}
              </Text>
              <Pressable
                onPress={() => engine.removeNfcTag(tag.id).then(reload)}
                accessibilityRole="button"
                accessibilityLabel={t('removeCity', { city: tag.name })}
                hitSlop={10}
              >
                <Icon
                  name="trash"
                  color={theme.colors.textSecondary}
                  size={20}
                />
              </Pressable>
            </View>
          </View>
        ))}
        <Divider />
        {!status.enabled ? (
          <Button
            label={t('nfcTurnOn')}
            variant="secondary"
            onPress={() => engine.openNfcSettings()}
            style={styles.button}
          />
        ) : scanning ? (
          <View style={styles.scanning}>
            <ActivityIndicator color={theme.colors.primary} />
            <Text style={[styles.hint, { color: theme.colors.text }]}>
              {t('nfcHoldNear')}
            </Text>
          </View>
        ) : (
          <Button
            label={t('nfcAddTag')}
            icon="plus"
            variant="secondary"
            onPress={scan}
            style={styles.button}
          />
        )}
        {message && (
          <Text style={[styles.hint, { color: theme.colors.secondary }]}>
            {message}
          </Text>
        )}
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  card: { paddingVertical: spacing.sm, gap: spacing.xs },
  hint: { fontSize: 14, paddingVertical: spacing.xs },
  row: { flexDirection: 'row', alignItems: 'center', minHeight: 48 },
  name: { flex: 1, fontSize: 16 },
  button: { marginVertical: spacing.sm },
  scanning: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    minHeight: 56,
  },
});

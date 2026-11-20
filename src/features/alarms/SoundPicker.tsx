import React, { useEffect, useState } from 'react';
import {
  FlatList,
  Modal,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { Icon } from '../../components/Icon';
import { useT } from '../../i18n';
import { engine, type Sound } from '../../services/engine';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';
import { soundDisplayName } from './soundNames';

type Props = {
  visible: boolean;
  selectedUri: string;
  onSelect: (sound: Sound) => void;
  onClose: () => void;
};

export function SoundPicker({
  visible,
  selectedUri,
  onSelect,
  onClose,
}: Props) {
  const theme = useTheme();
  const t = useT();
  const [sounds, setSounds] = useState<Sound[]>([]);
  const [previewing, setPreviewing] = useState<string | null>(null);

  useEffect(() => {
    if (visible) engine.getBuiltInSounds().then(setSounds);
  }, [visible]);

  const close = () => {
    engine.stopPreview();
    setPreviewing(null);
    onClose();
  };

  const togglePreview = (uri: string) => {
    if (previewing === uri) {
      engine.stopPreview();
      setPreviewing(null);
    } else {
      engine.previewSound(uri);
      setPreviewing(uri);
    }
  };

  const pickFile = async () => {
    engine.stopPreview();
    setPreviewing(null);
    const picked = await engine.pickCustomSound().catch(() => null);
    if (picked) {
      onSelect(picked);
      onClose();
    }
  };

  const custom = selectedUri.startsWith('content://');

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={close}
    >
      <Pressable
        style={styles.scrim}
        onPress={close}
        accessibilityLabel={t('cancel')}
      />
      <View style={[styles.sheet, { backgroundColor: theme.colors.elevated }]}>
        <Text style={[styles.title, { color: theme.colors.text }]}>
          {t('chooseSound')}
        </Text>
        <FlatList
          data={sounds}
          keyExtractor={s => s.uri}
          renderItem={({ item }) => {
            const selected = item.uri === selectedUri;
            return (
              <View style={styles.row}>
                <Pressable
                  style={styles.rowMain}
                  onPress={() => {
                    onSelect(item);
                    close();
                  }}
                  accessibilityRole="radio"
                  accessibilityState={{ checked: selected }}
                >
                  <View
                    style={[
                      styles.radio,
                      {
                        borderColor: selected
                          ? theme.colors.primary
                          : theme.colors.divider,
                      },
                    ]}
                  >
                    {selected && (
                      <View
                        style={[
                          styles.radioDot,
                          { backgroundColor: theme.colors.primary },
                        ]}
                      />
                    )}
                  </View>
                  <Text style={[styles.name, { color: theme.colors.text }]}>
                    {soundDisplayName(item.uri, item.name, t)}
                  </Text>
                </Pressable>
                <Pressable
                  onPress={() => togglePreview(item.uri)}
                  accessibilityRole="button"
                  accessibilityLabel={
                    previewing === item.uri ? t('stopPreview') : t('preview')
                  }
                  hitSlop={8}
                  style={styles.preview}
                >
                  <Icon
                    name={previewing === item.uri ? 'pause' : 'play'}
                    color={theme.colors.primary}
                    size={20}
                  />
                </Pressable>
              </View>
            );
          }}
          ListFooterComponent={
            <Pressable
              style={styles.row}
              onPress={pickFile}
              accessibilityRole="button"
            >
              <View style={styles.rowMain}>
                <Icon
                  name="music"
                  color={
                    custom ? theme.colors.primary : theme.colors.textSecondary
                  }
                  size={22}
                />
                <Text style={[styles.name, { color: theme.colors.text }]}>
                  {t('chooseFromFiles')}
                </Text>
              </View>
            </Pressable>
          }
        />
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  scrim: { flex: 1, backgroundColor: 'rgba(0,0,0,0.45)' },
  sheet: {
    borderTopLeftRadius: radius.lg,
    borderTopRightRadius: radius.lg,
    paddingHorizontal: spacing.xl,
    paddingTop: spacing.xl,
    paddingBottom: spacing.xxl,
    maxHeight: '70%',
  },
  title: { fontSize: 20, fontWeight: '600', marginBottom: spacing.md },
  row: { flexDirection: 'row', alignItems: 'center', minHeight: 56 },
  rowMain: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.lg,
    minHeight: 56,
  },
  radio: {
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioDot: { width: 10, height: 10, borderRadius: 5 },
  name: { fontSize: 16 },
  preview: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
  },
});

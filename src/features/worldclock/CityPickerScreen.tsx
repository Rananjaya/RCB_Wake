import React, { useEffect, useMemo, useState } from 'react';
import {
  FlatList,
  Pressable,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import type { NativeStackScreenProps } from '@react-navigation/native-stack';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Icon } from '../../components/Icon';
import { Toolbar } from '../../components/Toolbar';
import { Divider } from '../../components/ui';
import { useT } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import { useSettingsStore } from '../../storage/settingsStore';
import { useWorldClockStore } from '../../storage/worldClockStore';
import { useTheme } from '../../theme/useTheme';
import { radius, spacing } from '../../theme/themes';
import { searchPlaces, type Place } from '../../services/placeSearch';
import { searchCities } from './cities';

type Props = NativeStackScreenProps<RootStackParamList, 'CityPicker'>;

export function CityPickerScreen({ navigation, route }: Props) {
  const weatherMode = route.params?.mode === 'weather';
  const updateSettings = useSettingsStore(s => s.update);
  const theme = useTheme();
  const t = useT();
  const [query, setQuery] = useState('');
  const add = useWorldClockStore(s => s.add);
  const language = useSettingsStore(s => s.settings.language);
  const [online, setOnline] = useState<Place[]>([]);
  const [searching, setSearching] = useState(false);

  // Weather mode: also search villages and suburbs online (debounced, cancelled on new input).
  useEffect(() => {
    if (!weatherMode || query.trim().length < 2) {
      setOnline([]);
      return;
    }
    const controller = new AbortController();
    const id = setTimeout(() => {
      setSearching(true);
      searchPlaces(query, language, controller.signal)
        .then(setOnline)
        .catch(() => setOnline([]))
        .finally(() => setSearching(false));
    }, 400);
    return () => {
      clearTimeout(id);
      controller.abort();
    };
  }, [weatherMode, query, language]);

  const results = useMemo(() => {
    const local = searchCities(query).map(c => ({
      key: `${c.name}|${c.zoneId}`,
      name: c.name,
      detail: c.country,
      lat: c.lat,
      lon: c.lon,
      zoneId: c.zoneId,
    }));
    if (!weatherMode) return local;
    const extra = online.map(p => ({
      key: `${p.name}|${p.lat},${p.lon}`,
      name: p.name,
      detail: p.detail,
      lat: p.lat,
      lon: p.lon,
      zoneId: '',
    }));
    return query.trim().length >= 2 ? [...extra, ...local] : local;
  }, [query, online, weatherMode]);

  return (
    <SafeAreaView
      style={[styles.screen, { backgroundColor: theme.colors.background }]}
    >
      <Toolbar
        title={weatherMode ? t('chooseCity') : t('addCity')}
        onBack={() => navigation.goBack()}
      />
      <View style={[styles.search, { backgroundColor: theme.colors.surface }]}>
        <Icon name="search" color={theme.colors.textSecondary} size={20} />
        <TextInput
          testID="city-search"
          autoFocus
          value={query}
          onChangeText={setQuery}
          placeholder={t('searchCity')}
          placeholderTextColor={theme.colors.textSecondary}
          style={[styles.input, { color: theme.colors.text }]}
          accessibilityLabel={t('searchCity')}
        />
      </View>
      {weatherMode && (
        <Text style={[styles.hint, { color: theme.colors.textSecondary }]}>
          {t('placeSearchHint')}
        </Text>
      )}
      <FlatList
        data={results}
        keyboardShouldPersistTaps="handled"
        keyExtractor={c => c.key}
        ItemSeparatorComponent={Divider}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <Text style={[styles.empty, { color: theme.colors.textSecondary }]}>
            {searching ? t('placeSearching') : t('noCityResults')}
          </Text>
        }
        renderItem={({ item }) => (
          <Pressable
            onPress={async () => {
              if (weatherMode) {
                await updateSettings({
                  weatherEnabled: true,
                  weatherCity: item.name,
                  weatherLatitude: item.lat,
                  weatherLongitude: item.lon,
                });
              } else {
                await add(item.name, item.zoneId);
              }
              navigation.goBack();
            }}
            accessibilityRole="button"
            style={styles.row}
          >
            <Text style={[styles.city, { color: theme.colors.text }]}>
              {item.name}
            </Text>
            <Text
              style={[styles.country, { color: theme.colors.textSecondary }]}
            >
              {item.detail}
            </Text>
          </Pressable>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  search: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginHorizontal: spacing.lg,
    paddingHorizontal: spacing.lg,
    borderRadius: radius.pill,
    height: 52,
  },
  input: { flex: 1, fontSize: 16 },
  list: { paddingHorizontal: spacing.xl, paddingVertical: spacing.md },
  row: { paddingVertical: spacing.md, minHeight: 56, justifyContent: 'center' },
  city: { fontSize: 17 },
  country: { fontSize: 13, marginTop: 2 },
  hint: {
    fontSize: 12,
    marginHorizontal: spacing.xl,
    marginTop: spacing.sm,
  },
  empty: { textAlign: 'center', marginTop: spacing.xl },
});

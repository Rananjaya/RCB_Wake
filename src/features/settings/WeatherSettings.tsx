import React, { useEffect, useState } from 'react';
import {
  PermissionsAndroid,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  Button,
  Card,
  Chip,
  Divider,
  NavRow,
  SectionTitle,
  ToggleRow,
} from '../../components/ui';
import { useT } from '../../i18n';
import type { RootStackParamList } from '../../navigation/types';
import { engine, type WeatherInfo } from '../../services/engine';
import { useSettingsStore } from '../../storage/settingsStore';
import { useTheme } from '../../theme/useTheme';
import { spacing } from '../../theme/themes';

function formatTemp(celsius: number, unit: string): string {
  const value = unit === 'f' ? (celsius * 9) / 5 + 32 : celsius;
  return `${Math.round(value)}°${unit === 'f' ? 'F' : 'C'}`;
}

/** Opt-in weather for the home-screen widget. Off by default; see the privacy statement. */
export function WeatherSettings() {
  const theme = useTheme();
  const t = useT();
  const navigation =
    useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const settings = useSettingsStore(s => s.settings);
  const update = useSettingsStore(s => s.update);
  const [weather, setWeather] = useState<WeatherInfo | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [apiKey, setApiKey] = useState(settings.openWeatherMapApiKey);
  // Choosing OpenWeatherMap only opens the key field; it is saved together with a valid key.
  const [owmDraft, setOwmDraft] = useState(false);
  const owm = settings.weatherProvider === 'openweathermap' || owmDraft;
  const keyValid = /^[A-Za-z0-9]{16,64}$/.test(apiKey.trim());

  useEffect(() => {
    if (!settings.weatherEnabled) {
      setWeather(null);
      return;
    }
    engine.getWeather().then(cached => setWeather(cached));
    // Pick up the background fetch that starts when weather is first enabled.
    const id = setTimeout(() => engine.getWeather().then(setWeather), 4000);
    return () => clearTimeout(id);
  }, [
    settings.weatherEnabled,
    settings.weatherCity,
    settings.weatherUseLocation,
    settings.weatherProvider,
    settings.openWeatherMapApiKey,
  ]);

  const chooseProvider = (provider: string) => {
    if (provider === 'openweathermap' && !settings.openWeatherMapApiKey) {
      setOwmDraft(true);
      return;
    }
    setOwmDraft(false);
    update({ weatherProvider: provider }).catch(() => undefined);
  };

  const saveKey = () =>
    update({
      openWeatherMapApiKey: apiKey.trim(),
      weatherProvider: 'openweathermap',
    })
      .then(() => setOwmDraft(false))
      .catch(() => undefined);

  /** Coarse location only; asked at the moment the user opts in, never in the background. */
  const toggleLocation = async (on: boolean) => {
    if (!on) {
      update({ weatherUseLocation: false }).catch(() => undefined);
      return;
    }
    const result = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION,
    );
    if (result !== PermissionsAndroid.RESULTS.GRANTED) return;
    await update({ weatherUseLocation: true }).catch(() => undefined);
    await engine.updateLocation();
    setWeather(await engine.getWeather());
  };

  const toggle = (on: boolean) => {
    if (on && !settings.weatherCity && !settings.weatherUseLocation) {
      navigation.navigate('CityPicker', { mode: 'weather' });
      return;
    }
    update({ weatherEnabled: on }).catch(() => undefined);
  };

  return (
    <>
      <SectionTitle>{t('widgetWeather')}</SectionTitle>
      <Card style={styles.card}>
        <ToggleRow
          label={t('weatherToggle')}
          hint={t('weatherHint')}
          value={settings.weatherEnabled}
          onValueChange={toggle}
        />
        {settings.weatherEnabled && (
          <>
            <Divider />
            <ToggleRow
              label={t('weatherUseLocation')}
              hint={t('weatherUseLocationHint')}
              value={settings.weatherUseLocation}
              onValueChange={toggleLocation}
            />
            <Divider />
            <NavRow
              label={
                settings.weatherUseLocation
                  ? t('weatherFallbackCity')
                  : t('weatherCity')
              }
              value={settings.weatherCity || t('chooseCity')}
              onPress={() =>
                navigation.navigate('CityPicker', { mode: 'weather' })
              }
            />
            <Divider />
            <View style={styles.unitRow}>
              <Text style={[styles.label, { color: theme.colors.text }]}>
                {t('temperatureUnit')}
              </Text>
              <Chip
                label={t('celsius')}
                selected={settings.temperatureUnit === 'c'}
                onPress={() => update({ temperatureUnit: 'c' })}
              />
              <Chip
                label={t('fahrenheit')}
                selected={settings.temperatureUnit === 'f'}
                onPress={() => update({ temperatureUnit: 'f' })}
              />
            </View>
            <Divider />
            <View style={styles.unitRow}>
              <Text style={[styles.label, { color: theme.colors.text }]}>
                {t('weatherRefresh')}
              </Text>
            </View>
            <View style={styles.chips}>
              {[15, 30, 60, 180].map(m => (
                <Chip
                  key={m}
                  label={
                    m < 60
                      ? t('snoozeMinutes', { m })
                      : t('hoursShort', { h: m / 60 })
                  }
                  selected={settings.weatherRefreshMinutes === m}
                  onPress={() => update({ weatherRefreshMinutes: m })}
                />
              ))}
            </View>
            <Divider />
            <View style={styles.unitRow}>
              <Text style={[styles.label, { color: theme.colors.text }]}>
                {t('weatherProvider')}
              </Text>
            </View>
            <View style={styles.chips}>
              <Chip
                label={t('providerOpenMeteo')}
                selected={!owm}
                onPress={() => chooseProvider('openmeteo')}
              />
              <Chip
                label={t('providerOpenWeatherMap')}
                selected={owm}
                onPress={() => chooseProvider('openweathermap')}
              />
            </View>
            {owm && (
              <View style={styles.keyBox}>
                <Text style={[styles.label, { color: theme.colors.text }]}>
                  {t('owmApiKey')}
                </Text>
                <Text
                  style={[styles.hint, { color: theme.colors.textSecondary }]}
                >
                  {t('owmApiKeyHint')}
                </Text>
                <TextInput
                  value={apiKey}
                  onChangeText={setApiKey}
                  secureTextEntry
                  autoCapitalize="none"
                  autoCorrect={false}
                  maxLength={64}
                  placeholder="••••••••••••••••"
                  placeholderTextColor={theme.colors.textSecondary}
                  accessibilityLabel={t('owmApiKey')}
                  style={[
                    styles.input,
                    {
                      backgroundColor: theme.colors.elevated,
                      color: theme.colors.text,
                    },
                  ]}
                />
                {apiKey.length > 0 && !keyValid && (
                  <Text style={[styles.hint, { color: theme.colors.danger }]}>
                    {t('owmApiKeyInvalid')}
                  </Text>
                )}
                <Button
                  label={t('owmSave')}
                  disabled={
                    !keyValid || apiKey.trim() === settings.openWeatherMapApiKey
                  }
                  onPress={saveKey}
                />
              </View>
            )}
            <Text
              style={[styles.status, { color: theme.colors.textSecondary }]}
            >
              {weather
                ? t('weatherNow', {
                    temp: formatTemp(
                      weather.temperatureCelsius,
                      settings.temperatureUnit,
                    ),
                    city: weather.city || t('currentLocation'),
                  })
                : owm
                ? t('owmNoWeather')
                : t('weatherNotYet')}
            </Text>
            {weather && weather.latitude !== undefined && (
              <Text
                style={[styles.details, { color: theme.colors.textSecondary }]}
              >
                {[
                  t('weatherSent', {
                    lat: weather.latitude.toFixed(2),
                    lon: weather.longitude.toFixed(2),
                  }),
                  weather.place
                    ? t('weatherMatched', {
                        provider: t('providerOpenWeatherMap'),
                        place: weather.place,
                      })
                    : null,
                  t('weatherUpdated', {
                    time: new Date(weather.fetchedAt).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    }),
                  }),
                ]
                  .filter(Boolean)
                  .join('\n')}
              </Text>
            )}
            <Button
              label={t('weatherRefreshNow')}
              variant="secondary"
              disabled={refreshing}
              onPress={async () => {
                setRefreshing(true);
                try {
                  setWeather((await engine.refreshWeather()) ?? weather);
                } finally {
                  setRefreshing(false);
                }
              }}
            />
            <Text
              style={[
                styles.attribution,
                { color: theme.colors.textSecondary },
              ]}
            >
              {owm ? t('owmAttribution') : t('weatherAttribution')}
            </Text>
          </>
        )}
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  card: { paddingVertical: spacing.xs },
  unitRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    minHeight: 56,
  },
  label: { flex: 1, fontSize: 16, fontWeight: '500' },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
    paddingBottom: spacing.md,
  },
  keyBox: { gap: spacing.sm, paddingBottom: spacing.md },
  hint: { fontSize: 13 },
  input: { borderRadius: 12, paddingHorizontal: spacing.lg, height: 48 },
  status: { fontSize: 14, paddingTop: spacing.sm },
  details: { fontSize: 13, paddingBottom: spacing.sm },
  attribution: { fontSize: 12, paddingVertical: spacing.sm },
});

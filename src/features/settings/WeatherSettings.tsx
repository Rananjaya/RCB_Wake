import React, {useEffect, useState} from 'react';
import {StyleSheet, Text, View} from 'react-native';
import {useNavigation} from '@react-navigation/native';
import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import {Card, Chip, Divider, NavRow, SectionTitle, ToggleRow} from '../../components/ui';
import {useT} from '../../i18n';
import type {RootStackParamList} from '../../navigation/types';
import {engine, type WeatherInfo} from '../../services/engine';
import {useSettingsStore} from '../../storage/settingsStore';
import {useTheme} from '../../theme/useTheme';
import {spacing} from '../../theme/themes';

function formatTemp(celsius: number, unit: string): string {
  const value = unit === 'f' ? (celsius * 9) / 5 + 32 : celsius;
  return `${Math.round(value)}°${unit === 'f' ? 'F' : 'C'}`;
}

/** Opt-in weather for the home-screen widget. Off by default; see the privacy statement. */
export function WeatherSettings() {
  const theme = useTheme();
  const t = useT();
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const settings = useSettingsStore(s => s.settings);
  const update = useSettingsStore(s => s.update);
  const [weather, setWeather] = useState<WeatherInfo | null>(null);

  useEffect(() => {
    if (!settings.weatherEnabled) {
      setWeather(null);
      return;
    }
    engine.getWeather().then(cached => setWeather(cached));
    // Pick up the background fetch that starts when weather is first enabled.
    const id = setTimeout(() => engine.getWeather().then(setWeather), 4000);
    return () => clearTimeout(id);
  }, [settings.weatherEnabled, settings.weatherCity]);

  const toggle = (on: boolean) => {
    if (on && !settings.weatherCity) {
      navigation.navigate('CityPicker', {mode: 'weather'});
      return;
    }
    update({weatherEnabled: on}).catch(() => undefined);
  };

  return (
    <>
      <SectionTitle>{t('widgetWeather')}</SectionTitle>
      <Card style={styles.card}>
        <ToggleRow label={t('weatherToggle')} hint={t('weatherHint')} value={settings.weatherEnabled} onValueChange={toggle} />
        {settings.weatherEnabled && (
          <>
            <Divider />
            <NavRow
              label={t('weatherCity')}
              value={settings.weatherCity || t('chooseCity')}
              onPress={() => navigation.navigate('CityPicker', {mode: 'weather'})}
            />
            <Divider />
            <View style={styles.unitRow}>
              <Text style={[styles.label, {color: theme.colors.text}]}>{t('temperatureUnit')}</Text>
              <Chip label={t('celsius')} selected={settings.temperatureUnit === 'c'} onPress={() => update({temperatureUnit: 'c'})} />
              <Chip label={t('fahrenheit')} selected={settings.temperatureUnit === 'f'} onPress={() => update({temperatureUnit: 'f'})} />
            </View>
            <Text style={[styles.status, {color: theme.colors.textSecondary}]}>
              {weather
                ? t('weatherNow', {temp: formatTemp(weather.temperatureCelsius, settings.temperatureUnit), city: weather.city})
                : t('weatherNotYet')}
            </Text>
            <Text style={[styles.attribution, {color: theme.colors.textSecondary}]}>{t('weatherAttribution')}</Text>
          </>
        )}
      </Card>
    </>
  );
}

const styles = StyleSheet.create({
  card: {paddingVertical: spacing.xs},
  unitRow: {flexDirection: 'row', alignItems: 'center', gap: spacing.sm, minHeight: 56},
  label: {flex: 1, fontSize: 16, fontWeight: '500'},
  status: {fontSize: 14, paddingTop: spacing.sm},
  attribution: {fontSize: 12, paddingVertical: spacing.sm},
});

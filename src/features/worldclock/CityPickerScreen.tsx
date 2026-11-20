import React, {useMemo, useState} from 'react';
import {FlatList, Pressable, StyleSheet, Text, TextInput, View} from 'react-native';
import type {NativeStackScreenProps} from '@react-navigation/native-stack';
import {SafeAreaView} from 'react-native-safe-area-context';
import {Icon} from '../../components/Icon';
import {Toolbar} from '../../components/Toolbar';
import {Divider} from '../../components/ui';
import {useT} from '../../i18n';
import type {RootStackParamList} from '../../navigation/types';
import {useWorldClockStore} from '../../storage/worldClockStore';
import {useTheme} from '../../theme/useTheme';
import {radius, spacing} from '../../theme/themes';
import {searchCities} from './cities';

type Props = NativeStackScreenProps<RootStackParamList, 'CityPicker'>;

export function CityPickerScreen({navigation}: Props) {
  const theme = useTheme();
  const t = useT();
  const [query, setQuery] = useState('');
  const add = useWorldClockStore(s => s.add);
  const results = useMemo(() => searchCities(query), [query]);

  return (
    <SafeAreaView style={[styles.screen, {backgroundColor: theme.colors.background}]}>
      <Toolbar title={t('addCity')} onBack={() => navigation.goBack()} />
      <View style={[styles.search, {backgroundColor: theme.colors.surface}]}>
        <Icon name="search" color={theme.colors.textSecondary} size={20} />
        <TextInput
          testID="city-search"
          autoFocus
          value={query}
          onChangeText={setQuery}
          placeholder={t('searchCity')}
          placeholderTextColor={theme.colors.textSecondary}
          style={[styles.input, {color: theme.colors.text}]}
          accessibilityLabel={t('searchCity')}
        />
      </View>
      <FlatList
        data={results}
        keyboardShouldPersistTaps="handled"
        keyExtractor={c => `${c.name}|${c.zoneId}`}
        ItemSeparatorComponent={Divider}
        contentContainerStyle={styles.list}
        ListEmptyComponent={<Text style={[styles.empty, {color: theme.colors.textSecondary}]}>{t('noCityResults')}</Text>}
        renderItem={({item}) => (
          <Pressable
            onPress={async () => {
              await add(item.name, item.zoneId);
              navigation.goBack();
            }}
            accessibilityRole="button"
            style={styles.row}>
            <Text style={[styles.city, {color: theme.colors.text}]}>{item.name}</Text>
            <Text style={[styles.country, {color: theme.colors.textSecondary}]}>{item.country}</Text>
          </Pressable>
        )}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {flex: 1},
  search: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    marginHorizontal: spacing.lg,
    paddingHorizontal: spacing.lg,
    borderRadius: radius.pill,
    height: 52,
  },
  input: {flex: 1, fontSize: 16},
  list: {paddingHorizontal: spacing.xl, paddingVertical: spacing.md},
  row: {paddingVertical: spacing.md, minHeight: 56, justifyContent: 'center'},
  city: {fontSize: 17},
  country: {fontSize: 13, marginTop: 2},
  empty: {textAlign: 'center', marginTop: spacing.xl},
});

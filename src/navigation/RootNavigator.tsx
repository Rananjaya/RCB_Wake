import React from 'react';
import { StyleSheet, Text } from 'react-native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Icon, type IconName } from '../components/Icon';
import { AlarmEditorScreen } from '../features/alarms/AlarmEditorScreen';
import { AlarmListScreen } from '../features/alarms/AlarmListScreen';
import { ChallengeScreen } from '../features/challenges/ChallengeScreen';
import { AboutScreen } from '../features/settings/AboutScreen';
import { SettingsScreen } from '../features/settings/SettingsScreen';
import { StopwatchScreen } from '../features/stopwatch/StopwatchScreen';
import { TutorialScreen } from '../features/onboarding/TutorialScreen';
import { TimerScreen } from '../features/timer/TimerScreen';
import { CityPickerScreen } from '../features/worldclock/CityPickerScreen';
import { WorldClockScreen } from '../features/worldclock/WorldClockScreen';
import { useT } from '../i18n';
import { useTheme } from '../theme/useTheme';
import type { RootStackParamList, TabParamList } from './types';

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<TabParamList>();

const tabIcons: Record<keyof TabParamList, IconName> = {
  Alarms: 'alarm',
  Timers: 'timer',
  Stopwatch: 'stopwatch',
  WorldClock: 'globe',
};

function tabIcon(name: keyof TabParamList) {
  return function TabIcon({ color }: { color: string }) {
    return <Icon name={tabIcons[name]} color={color} />;
  };
}

/** Long translations (e.g. Japanese "Stopwatch") shrink to fit instead of being cut off. */
function tabLabel({ color, children }: { color: string; children: string }) {
  return (
    <Text
      numberOfLines={1}
      adjustsFontSizeToFit
      minimumFontScale={0.7}
      style={[styles.tabLabel, { color }]}
    >
      {children}
    </Text>
  );
}

function Tabs({ initialTab }: { initialTab?: keyof TabParamList }) {
  const theme = useTheme();
  const t = useT();
  const insets = useSafeAreaInsets();
  return (
    <Tab.Navigator
      initialRouteName={initialTab ?? 'Alarms'}
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.textSecondary,
        tabBarStyle: {
          backgroundColor: theme.colors.surface,
          borderTopColor: theme.colors.divider,
          height: 64 + insets.bottom,
          paddingTop: 8,
          paddingBottom: 8 + insets.bottom,
        },
        tabBarLabelStyle: { fontSize: 12, fontWeight: '600' },
        tabBarIcon: tabIcon(route.name),
        tabBarLabel: tabLabel,
      })}
    >
      <Tab.Screen
        name="Alarms"
        component={AlarmListScreen}
        options={{ title: t('tabAlarm') }}
      />
      <Tab.Screen
        name="Timers"
        component={TimerScreen}
        options={{ title: t('tabTimer') }}
      />
      <Tab.Screen
        name="Stopwatch"
        component={StopwatchScreen}
        options={{ title: t('tabStopwatch') }}
      />
      <Tab.Screen
        name="WorldClock"
        component={WorldClockScreen}
        options={{ title: t('tabWorldClock') }}
      />
    </Tab.Navigator>
  );
}

function TutorialReplay({
  navigation,
}: {
  navigation: { goBack: () => void };
}) {
  return <TutorialScreen replay onFinish={() => navigation.goBack()} />;
}

export function RootNavigator({
  initialTab,
}: {
  initialTab?: keyof TabParamList;
}) {
  const theme = useTheme();
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
        contentStyle: { backgroundColor: theme.colors.background },
        animation: 'fade_from_bottom',
      }}
    >
      <Stack.Screen name="Tabs">
        {() => <Tabs initialTab={initialTab} />}
      </Stack.Screen>
      <Stack.Screen
        name="AlarmEditor"
        component={AlarmEditorScreen}
        options={{ presentation: 'modal' }}
      />
      <Stack.Screen name="Settings" component={SettingsScreen} />
      <Stack.Screen name="About" component={AboutScreen} />
      <Stack.Screen name="CityPicker" component={CityPickerScreen} />
      <Stack.Screen name="Tutorial" component={TutorialReplay} />
      <Stack.Screen
        name="Challenge"
        component={ChallengeScreen}
        options={{
          presentation: 'fullScreenModal',
          gestureEnabled: false,
          animation: 'fade',
        }}
      />
    </Stack.Navigator>
  );
}

const styles = StyleSheet.create({
  tabLabel: { fontSize: 12, fontWeight: '600', paddingHorizontal: 2 },
});

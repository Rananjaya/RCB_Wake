import React, {useEffect, useRef} from 'react';
import {AppState, PermissionsAndroid, Platform, StatusBar} from 'react-native';
import {
  createNavigationContainerRef,
  DarkTheme,
  DefaultTheme,
  NavigationContainer,
} from '@react-navigation/native';
import {GestureHandlerRootView} from 'react-native-gesture-handler';
import {SafeAreaProvider} from 'react-native-safe-area-context';
import type {RootStackParamList, TabParamList} from '../navigation/types';
import {RootNavigator} from '../navigation/RootNavigator';
import {engine, subscribeEngine} from '../services/engine';
import {useAlarmStore} from '../storage/alarmStore';
import {useSettingsStore} from '../storage/settingsStore';
import {useTimerStore} from '../storage/timerStore';
import {useTheme} from '../theme/useTheme';

const navigationRef = createNavigationContainerRef<RootStackParamList>();

const TAB_BY_LAUNCH: Record<string, keyof TabParamList> = {timer: 'Timers'};

async function requestNotificationPermission() {
  if (Platform.OS !== 'android' || Platform.Version < 33) return;
  const permission = PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS;
  if (!(await PermissionsAndroid.check(permission))) {
    await PermissionsAndroid.request(permission);
  }
}

function useEngineBootstrap() {
  useEffect(() => {
    const alarms = useAlarmStore.getState();
    const timers = useTimerStore.getState();
    (async () => {
      await useSettingsStore.getState().load();
      const status = await engine.initializeEngine();
      useAlarmStore.setState({status});
      await Promise.all([alarms.load(), timers.load(), alarms.refreshSession()]);
      await requestNotificationPermission();
      await alarms.refreshStatus();
    })().catch(() => {
      // The engine reports per-operation errors to the screens; startup continues with defaults.
    });

    const subscription = subscribeEngine(event => {
      switch (event.type) {
        case 'alarmsChanged':
          useAlarmStore.getState().load();
          useAlarmStore.getState().refreshStatus();
          break;
        case 'timersChanged':
          useTimerStore.getState().load();
          break;
        case 'sessionStarted':
        case 'sessionEnded':
          useAlarmStore.getState().refreshSession();
          break;
      }
    });
    const appState = AppState.addEventListener('change', state => {
      if (state === 'active') {
        useAlarmStore.getState().refreshStatus();
        useAlarmStore.getState().refreshSession();
        useTimerStore.getState().load();
      }
    });
    return () => {
      subscription.remove();
      appState.remove();
    };
  }, []);
}

/** Opens the challenge whenever a ringing session requires one, and closes it when the session ends. */
function useChallengeRouting() {
  const session = useAlarmStore(s => s.session);
  const wasActive = useRef(false);
  useEffect(() => {
    if (!navigationRef.isReady()) return;
    const current = navigationRef.getCurrentRoute()?.name;
    if (session?.challengeRequired) {
      wasActive.current = true;
      if (current !== 'Challenge') navigationRef.navigate('Challenge');
    } else if (wasActive.current && current === 'Challenge') {
      wasActive.current = false;
      navigationRef.goBack();
    }
  }, [session]);
}

export default function App({initialTab}: {initialTab?: string}) {
  const theme = useTheme();
  useEngineBootstrap();
  useChallengeRouting();

  const base = theme.dark ? DarkTheme : DefaultTheme;
  const navTheme = {
    ...base,
    colors: {
      ...base.colors,
      background: theme.colors.background,
      card: theme.colors.surface,
      text: theme.colors.text,
      primary: theme.colors.primary,
      border: theme.colors.divider,
    },
  };

  return (
    <GestureHandlerRootView style={{flex: 1, backgroundColor: theme.colors.background}}>
      <SafeAreaProvider>
        <StatusBar barStyle={theme.dark ? 'light-content' : 'dark-content'} />
        <NavigationContainer
          ref={navigationRef}
          theme={navTheme}
          onReady={() => useAlarmStore.getState().refreshSession()}>
          <RootNavigator initialTab={initialTab ? TAB_BY_LAUNCH[initialTab] : undefined} />
        </NavigationContainer>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

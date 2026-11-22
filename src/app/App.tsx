import React, { useEffect } from 'react';
import {
  AppState,
  PermissionsAndroid,
  Platform,
  StatusBar,
  StyleSheet,
  View,
} from 'react-native';
import {
  createNavigationContainerRef,
  DarkTheme,
  DefaultTheme,
  NavigationContainer,
} from '@react-navigation/native';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import type { RootStackParamList, TabParamList } from '../navigation/types';
import { RootNavigator } from '../navigation/RootNavigator';
import {
  onboardingStep,
  useOnboardingStore,
} from '../features/onboarding/onboarding';
import { PrivacyAgreementScreen } from '../features/onboarding/PrivacyAgreementScreen';
import { TutorialScreen } from '../features/onboarding/TutorialScreen';
import { engine, subscribeEngine } from '../services/engine';
import { useAlarmStore } from '../storage/alarmStore';
import { usePomodoroStore } from '../storage/pomodoroStore';
import { useSettingsStore } from '../storage/settingsStore';
import { useTimerStore } from '../storage/timerStore';
import { useTheme } from '../theme/useTheme';

const navigationRef = createNavigationContainerRef<RootStackParamList>();

const TAB_BY_LAUNCH: Record<string, keyof TabParamList> = { timer: 'Timers' };

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
      useAlarmStore.setState({ status });
      await Promise.all([
        alarms.load(),
        timers.load(),
        alarms.refreshSession(),
      ]);
      await alarms.refreshStatus();
      // Weather by location: refresh the position (and place name) on launch too.
      if (useSettingsStore.getState().settings.weatherUseLocation) {
        engine.updateLocation().catch(() => undefined);
      }
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
        case 'pomodoroChanged':
          usePomodoroStore.getState().load();
          break;
        case 'sessionStarted':
        case 'sessionEnded':
          useAlarmStore.getState().refreshSession();
          break;
      }
    });
    const appState = AppState.addEventListener('change', state => {
      if (state === 'active') {
        useSettingsStore
          .getState()
          .load()
          .catch(() => undefined);
        if (useSettingsStore.getState().settings.weatherUseLocation) {
          engine.updateLocation().catch(() => undefined);
        }
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

/**
 * Opens the challenge whenever a ringing session requires one, and closes it as soon as no
 * challenge is active — also when the app was opened mid-alarm or the alarm ended while the app
 * was in the background.
 */
function useChallengeRouting() {
  const session = useAlarmStore(s => s.session);
  useEffect(() => {
    if (!navigationRef.isReady()) return;
    const current = navigationRef.getCurrentRoute()?.name;
    if (session?.challengeRequired) {
      if (current !== 'Challenge') navigationRef.navigate('Challenge');
    } else if (current === 'Challenge') {
      closeChallenge();
    }
  }, [session]);
}

export function closeChallenge() {
  if (navigationRef.canGoBack()) navigationRef.goBack();
  else navigationRef.reset({ index: 0, routes: [{ name: 'Tabs' }] });
}

function OnboardingGate({ children }: { children: React.ReactNode }) {
  const theme = useTheme();
  const state = useOnboardingStore();
  useEffect(() => {
    state.load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  const step = onboardingStep(state);
  // Permissions are never requested during onboarding; ask for notifications once it is complete.
  useEffect(() => {
    if (step === 'done')
      requestNotificationPermission().then(() =>
        useAlarmStore.getState().refreshStatus(),
      );
  }, [step]);
  if (step === 'loading')
    return (
      <View
        style={[styles.root, { backgroundColor: theme.colors.background }]}
      />
    );
  if (step === 'privacy')
    return <PrivacyAgreementScreen onAgree={state.accept} />;
  if (step === 'tutorial')
    return <TutorialScreen onFinish={state.finishTutorial} />;
  return <>{children}</>;
}

export default function App({ initialTab }: { initialTab?: string }) {
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
    <GestureHandlerRootView
      style={[styles.root, { backgroundColor: theme.colors.background }]}
    >
      <SafeAreaProvider>
        <StatusBar barStyle={theme.dark ? 'light-content' : 'dark-content'} />
        <OnboardingGate>
          <NavigationContainer
            ref={navigationRef}
            theme={navTheme}
            onReady={() => useAlarmStore.getState().refreshSession()}
          >
            <RootNavigator
              initialTab={initialTab ? TAB_BY_LAUNCH[initialTab] : undefined}
            />
          </NavigationContainer>
        </OnboardingGate>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}

const styles = StyleSheet.create({ root: { flex: 1 } });

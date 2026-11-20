import type {EventSubscription} from 'react-native';
import NativeRcbWake, {type EngineEvent} from '../native/NativeRcbWake';

export type {
  AlarmConfig,
  AlarmRecord,
  EngineStatus,
  SessionInfo,
  Settings,
  Sound,
  TimerRecord,
  WorldClockRecord,
  ZoneOffset,
} from '../native/NativeRcbWake';

/** The Kotlin engine. All persistence and scheduling goes through this object. */
export const engine = NativeRcbWake;

export function subscribeEngine(listener: (event: EngineEvent) => void): EventSubscription {
  return NativeRcbWake.onEngineEvent(listener);
}

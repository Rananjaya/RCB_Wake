# Security Policy

## Supported versions

| Version | Supported |
| --- | --- |
| 1.0.x | Yes |

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Report privately through GitHub's private vulnerability reporting on this repository
(**Security → Report a vulnerability**), or contact the maintainer through
https://github.com/Rananjaya.

Please include the affected version, the Android version and device, steps to reproduce, and the
impact you expect. You can expect an acknowledgement within 7 days.

## Scope

Things we especially want to hear about:

- An alarm failing to ring, or ringing at the wrong time, because of a code defect.
- Any way for another app to trigger, cancel or read RCB Wake alarms, timers or settings.
- Any network traffic other than the opt-in Open-Meteo weather request described in
  [docs/security-privacy.md](docs/security-privacy.md).
- Leaks of alarm labels, custom sound file names or location outside the device.

## Design summary

- No accounts, no analytics, no ads, no tracking SDKs.
- All components except the launcher activity and the system-event receiver are
  `exported="false"`; all `PendingIntent`s are immutable.
- Data lives in app-private, device-protected storage and is excluded from cloud backup and
  device transfer.
- Release signing keys are never stored in this repository.

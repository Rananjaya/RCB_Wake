import React from 'react';
import { Banner } from '../../components/ui';
import { useT } from '../../i18n';
import { engine } from '../../services/engine';
import { useAlarmStore } from '../../storage/alarmStore';

/** Explains, and offers to fix, anything that would stop alarms from ringing reliably. */
export function PermissionBanners() {
  const status = useAlarmStore(s => s.status);
  const t = useT();
  if (!status) return null;
  return (
    <>
      {!status.exactAlarmPermitted && (
        <Banner
          title={t('permExactTitle')}
          body={t('permExactBody')}
          actionLabel={t('allow')}
          onAction={() => engine.requestExactAlarmPermission()}
        />
      )}
      {!status.notificationsPermitted && (
        <Banner
          title={t('permNotifTitle')}
          body={t('permNotifBody')}
          actionLabel={t('allow')}
          onAction={() => engine.openNotificationSettings()}
        />
      )}
      {status.notificationsPermitted && !status.fullScreenIntentPermitted && (
        <Banner
          title={t('permFsiTitle')}
          body={t('permFsiBody')}
          actionLabel={t('allow')}
          onAction={() => engine.requestFullScreenIntentPermission()}
        />
      )}
    </>
  );
}

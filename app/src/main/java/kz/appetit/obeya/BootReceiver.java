package kz.appetit.obeya;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** После перезагрузки телефона пересоздаёт будильники и синхронизацию. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        String a = intent.getAction();
        if (a == null) return;
        if (a.equals(Intent.ACTION_BOOT_COMPLETED)
                || a.equals("android.intent.action.QUICKBOOT_POWERON")
                || a.equals(Intent.ACTION_MY_PACKAGE_REPLACED)){
            AlarmReceiver.ensureChannel(context);
            AlarmScheduler.rescheduleAll(context, TaskStore.getAll(context));
            SyncReceiver.schedulePeriodic(context);
            SyncReceiver.trigger(context);
        }
    }
}

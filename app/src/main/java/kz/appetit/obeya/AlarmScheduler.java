package kz.appetit.obeya;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

/** Точный будильник на каждую задачу. setAlarmClock() не требует спец-разрешений. */
public final class AlarmScheduler {
    private AlarmScheduler() {}

    private static PendingIntent pi(Context c, Task t){
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction("kz.appetit.obeya.FIRE");
        i.putExtra("taskId", t.id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, t.requestCode(), i, flags);
    }

    public static void schedule(Context c, Task t){
        if (t.acknowledged || t.alarmAtMillis <= System.currentTimeMillis()) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.setAlarmClock(new AlarmManager.AlarmClockInfo(t.alarmAtMillis, pi(c, t)), pi(c, t));
    }

    public static void cancel(Context c, Task t){
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(c, t));
    }

    public static void rescheduleAll(Context c, List<Task> tasks){
        long now = System.currentTimeMillis();
        for (Task t : tasks){
            if (t.acknowledged || t.alarmAtMillis <= now) cancel(c, t);
            else schedule(c, t);
        }
    }
}

package kz.appetit.obeya;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Срабатывает в точное время задачи и передаёт звонок фоновой службе. */
public class AlarmReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "obeya_alarm";
    private static final int NOTIF_ID = 4711;

    @Override
    public void onReceive(Context context, Intent intent){
        String taskId = intent.getStringExtra("taskId");
        if (taskId == null) return;
        Task t = TaskStore.get(context, taskId);
        if (t == null || t.acknowledged) return;
        AlarmService.ring(context, taskId);   // служба проиграет сигнал и поднимет экран
    }

    /** Поднять полноэкранный экран задачи (вызывает служба). */
    static void showFullScreen(Context context, Task t){
        ensureChannel(context);
        Intent full = new Intent(context, AlarmActivity.class);
        full.putExtra("taskId", t.id);
        full.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent fullPi = PendingIntent.getActivity(context, t.requestCode(), full, piFlags);

        Notification.Builder b = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Задача филиала")
                .setContentText(t.title)
                .setCategory(Notification.CATEGORY_ALARM)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullPi, true);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, b.build());
        try { context.startActivity(full); } catch (Exception ignored) {}
    }

    static void ensureChannel(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) return;
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Будильник задач", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Срабатывание задач филиала");
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        }
    }

    static void clear(Context context){
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIF_ID);
    }
}

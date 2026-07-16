package kz.appetit.obeya;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Постоянная фоновая служба.
 *  - Держит приложение "живым", чтобы телефон не усыпил его в фоне
 *    (тогда будильник надёжен, как системный).
 *  - По команде RING сама проигрывает громкий сигнал и поднимает экран задачи.
 */
public class AlarmService extends Service {
    public static final String ACTION_RING = "kz.appetit.obeya.RING";
    public static final String ACTION_STOP_RING = "kz.appetit.obeya.STOP_RING";
    public static final String CH_SERVICE = "obeya_service";
    private static final int SERVICE_NOTIF = 7001;

    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        ensureServiceChannel(this);
        startForeground(SERVICE_NOTIF, buildServiceNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ensureServiceChannel(this);
        startForeground(SERVICE_NOTIF, buildServiceNotification());
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_RING.equals(action)) startRinging(intent.getStringExtra("taskId"));
        else if (ACTION_STOP_RING.equals(action)) stopRinging();
        return START_STICKY;
    }

    private Notification buildServiceNotification() {
        Notification.Builder b = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? new Notification.Builder(this, CH_SERVICE)
                : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("Obeya задачи")
                .setContentText("Слежу за задачами филиала")
                .setOngoing(true)
                .build();
    }

    private void startRinging(String taskId) {
        Task t = TaskStore.get(this, taskId);
        if (t == null || t.acknowledged) return;
        acquireWake();
        AlarmReceiver.showFullScreen(this, t);   // поднять экран задачи
        if (player == null) {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            try {
                player = new MediaPlayer();
                player.setDataSource(this, uri);
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
                player.setLooping(true);
                player.prepare();
                player.start();
            } catch (Exception ignored) {}
        }
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pat = {0, 700, 700};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pat, 0));
            else vibrator.vibrate(pat, 0);
        }
    }

    private void stopRinging() {
        try { if (player != null) { player.stop(); player.release(); } } catch (Exception ignored) {}
        player = null;
        if (vibrator != null) vibrator.cancel();
        AlarmReceiver.clear(this);
        releaseWake();
    }

    private void acquireWake() {
        try {
            if (wakeLock == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "obeya:alarm");
            }
            if (!wakeLock.isHeld()) wakeLock.acquire(5 * 60 * 1000L);
        } catch (Exception ignored) {}
    }
    private void releaseWake() {
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) {}
    }

    static void ensureServiceChannel(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && nm.getNotificationChannel(CH_SERVICE) == null) {
                NotificationChannel ch = new NotificationChannel(CH_SERVICE, "Работа приложения", NotificationManager.IMPORTANCE_MIN);
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
        }
    }

    static void ensureRunning(Context c) {
        Intent i = new Intent(c, AlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i);
        else c.startService(i);
    }
    static void ring(Context c, String taskId) {
        Intent i = new Intent(c, AlarmService.class);
        i.setAction(ACTION_RING);
        i.putExtra("taskId", taskId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i);
        else c.startService(i);
    }
    static void stopRing(Context c) {
        Intent i = new Intent(c, AlarmService.class);
        i.setAction(ACTION_STOP_RING);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) c.startForegroundService(i);
        else c.startService(i);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onDestroy() { super.onDestroy(); stopRinging(); }
}

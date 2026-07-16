package kz.appetit.obeya;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Периодически тянет задачи филиала, ставит будильники и отправляет отметки. */
public class SyncReceiver extends BroadcastReceiver {
    private static final long INTERVAL = 15 * 60 * 1000L;
    private static final int REQ = 999;

    @Override
    public void onReceive(Context context, Intent intent){
        final PendingResult pr = goAsync();
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            try { runSync(app); } catch (Exception ignored) {}
            schedulePeriodic(app);
            pr.finish();
        }).start();
    }

    public static void schedulePeriodic(Context c){
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent i = new Intent(c, SyncReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getBroadcast(c, REQ, i, flags);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + INTERVAL, pi);
    }

    public static void trigger(Context c){
        final Context app = c.getApplicationContext();
        new Thread(() -> { try { runSync(app); } catch (Exception ignored) {} }).start();
    }

    public static void runSync(Context c) throws Exception {
        String branch = Config.getBranch(c);
        if (branch == null) return;
        String body = Net.get(Config.tasksUrl(branch));
        List<Task> server = new ArrayList<>();
        JSONArray arr = new JSONArray(body);
        for (int i = 0; i < arr.length(); i++) server.add(Task.fromJson(arr.getJSONObject(i)));
        List<Task> merged = TaskStore.mergeFromServer(c, server);
        AlarmScheduler.rescheduleAll(c, merged);

        for (Task t : TaskStore.pendingAcks(c)){
            JSONObject o = new JSONObject();
            o.put("type", "ack"); o.put("branch", branch); o.put("id", t.id); o.put("acknowledgedAt", t.acknowledgedAtMillis);
            try { Net.postJson(Config.EXEC_URL, o.toString()); TaskStore.markAckSynced(c, t.id); } catch (Exception ignored) {}
        }
    }
}

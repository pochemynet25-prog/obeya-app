package kz.appetit.obeya;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.json.JSONArray;
import java.util.Map;

/** Приём пуша от Google. Google будит нас даже если приложение закрыто. */
public class ObeyaMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        Push.saveAndRegister(this, token);
    }

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        Map<String, String> d = msg.getData();
        String taskId = d.get("taskId");
        if (taskId == null) { SyncReceiver.trigger(this); return; }
        try {
            Task t = new Task();
            t.id = taskId;
            t.title = d.get("title") != null ? d.get("title") : "";
            t.alarmAtMillis = d.get("alarmAt") != null ? Long.parseLong(d.get("alarmAt")) : System.currentTimeMillis();
            String cl = d.get("checklist");
            if (cl != null) {
                JSONArray arr = new JSONArray(cl);
                for (int i = 0; i < arr.length(); i++) t.checklist.add(arr.getString(i));
            }
            TaskStore.upsertOne(this, t);
        } catch (Exception ignored) {}
        AlarmService.ring(this, taskId);
    }
}

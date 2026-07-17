package kz.appetit.obeya;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Локальный кэш задач: чтобы будильники работали офлайн и переживали перезагрузку. */
public final class TaskStore {
    private static final String PREFS = "obeya_tasks";
    private static final String KEY = "tasks";
    private TaskStore() {}

    private static SharedPreferences p(Context c){ return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static synchronized List<Task> getAll(Context c){
        List<Task> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(p(c).getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(Task.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {}
        return out;
    }

    public static synchronized Task get(Context c, String id){
        if (id == null) return null;
        for (Task t : getAll(c)) if (id.equals(t.id)) return t;
        return null;
    }

    public static synchronized void saveAll(Context c, List<Task> tasks){
        JSONArray arr = new JSONArray();
        try { for (Task t : tasks) arr.put(t.toJson()); } catch (JSONException ignored) {}
        p(c).edit().putString(KEY, arr.toString()).apply();
    }

    public static synchronized List<Task> mergeFromServer(Context c, List<Task> server){
        Map<String, Task> local = new LinkedHashMap<>();
        for (Task t : getAll(c)) local.put(t.id, t);
        List<Task> merged = new ArrayList<>();
        for (Task s : server){
            Task l = local.get(s.id);
            if (l != null && l.acknowledged && !l.ackSynced){
                s.acknowledged = true; s.acknowledgedAtMillis = l.acknowledgedAtMillis; s.ackSynced = false;
            }
            merged.add(s);
        }
        for (Task l : local.values()){
            boolean inServer = false;
            for (Task s : server) if (s.id.equals(l.id)){ inServer = true; break; }
            if (!inServer && l.acknowledged && !l.ackSynced) merged.add(l);
        }
        saveAll(c, merged);
        return merged;
    }

    public static synchronized void markAcknowledged(Context c, String id){
        List<Task> all = getAll(c);
        for (Task t : all) if (t.id.equals(id)){ t.acknowledged = true; t.acknowledgedAtMillis = System.currentTimeMillis(); t.ackSynced = false; }
        saveAll(c, all);
    }

    public static synchronized void markAckSynced(Context c, String id){
        List<Task> all = getAll(c);
        for (Task t : all) if (t.id.equals(id)) t.ackSynced = true;
        saveAll(c, all);
    }

    public static synchronized List<Task> pendingAcks(Context c){
        List<Task> out = new ArrayList<>();
        for (Task t : getAll(c)) if (t.acknowledged && !t.ackSynced) out.add(t);
        return out;
    }
public static synchronized void upsertOne(Context c, Task t){
        List<Task> all = getAll(c);
        for (int i = 0; i < all.size(); i++){
            if (all.get(i).id.equals(t.id)){
                if (all.get(i).acknowledged) return;
                all.set(i, t);
                saveAll(c, all);
                return;
            }
        }
        all.add(t);
        saveAll(c, all);
    }
}

package kz.appetit.obeya;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Task {
    public String id;
    public String title;
    public List<String> checklist = new ArrayList<>();
    public long alarmAtMillis;
    public boolean acknowledged;
    public long acknowledgedAtMillis;
    public boolean ackSynced;

    public static Task fromJson(JSONObject o) throws JSONException {
        Task t = new Task();
        t.id = o.getString("id");
        t.title = o.optString("title", "");
        t.alarmAtMillis = o.optLong("alarmAt", 0);
        t.acknowledged = o.optBoolean("acknowledged", false);
        t.acknowledgedAtMillis = o.optLong("acknowledgedAt", 0);
        t.ackSynced = o.optBoolean("ackSynced", false);
        JSONArray arr = o.optJSONArray("checklist");
        if (arr != null) for (int i = 0; i < arr.length(); i++) t.checklist.add(arr.getString(i));
        return t;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("alarmAt", alarmAtMillis);
        o.put("acknowledged", acknowledged);
        o.put("acknowledgedAt", acknowledgedAtMillis);
        o.put("ackSynced", ackSynced);
        JSONArray arr = new JSONArray();
        for (String s : checklist) arr.put(s);
        o.put("checklist", arr);
        return o;
    }

    public int requestCode(){ return id.hashCode(); }
}

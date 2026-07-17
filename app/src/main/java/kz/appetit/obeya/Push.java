package kz.appetit.obeya;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;

public final class Push {
    private static final String PREFS = "obeya_push";
    private static final String KEY_TOKEN = "fcm_token";
    private Push() {}

    private static SharedPreferences p(Context c){ return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    static void fetchAndRegister(Context c){
        try {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) saveAndRegister(c, task.getResult());
            });
        } catch (Exception ignored) {}
    }

    static void saveAndRegister(Context c, String token){
        p(c).edit().putString(KEY_TOKEN, token).apply();
        register(c);
    }

    static void register(Context c){
        final String token = p(c).getString(KEY_TOKEN, null);
        final String branch = Config.getBranch(c);
        if (token == null || branch == null) return;
        new Thread(() -> {
            try {
                JSONObject o = new JSONObject();
                o.put("type", "token");
                o.put("branch", branch);
                o.put("token", token);
                Net.postJson(Config.EXEC_URL, o.toString());
            } catch (Exception ignored) {}
        }).start();
    }
}

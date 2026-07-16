package kz.appetit.obeya;

import android.content.Context;
import android.content.SharedPreferences;
import java.net.URLEncoder;

/** Настройки: адрес твоей панели (Google-скрипт) и выбранный филиал телефона. */
public final class Config {
    // Ссылка /exec твоей панели уже вписана:
    public static final String EXEC_URL =
        "https://script.google.com/macros/s/AKfycbzJLyfqQgyHfr94I1MXnf3UlPwEA3Q3nGs-QXFcVzPLeLee6kjqy7PtWwaZPn2MjDUh/exec";

    private static final String PREFS = "obeya";
    private static final String KEY_ID = "branch_id";
    private static final String KEY_NAME = "branch_name";

    private Config() {}
    private static SharedPreferences p(Context c){ return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }

    public static String getBranch(Context c){ return p(c).getString(KEY_ID, null); }
    public static String getBranchName(Context c){ return p(c).getString(KEY_NAME, ""); }
    public static void setBranch(Context c, String id, String name){ p(c).edit().putString(KEY_ID, id).putString(KEY_NAME, name).apply(); }
    public static void clearBranch(Context c){ p(c).edit().remove(KEY_ID).remove(KEY_NAME).apply(); }

    public static String branchesUrl(){ return EXEC_URL + "?resource=branches"; }
    public static String tasksUrl(String branch){
        try { return EXEC_URL + "?resource=tasks&branch=" + URLEncoder.encode(branch, "UTF-8"); }
        catch (Exception e){ return EXEC_URL + "?resource=tasks&branch=" + branch; }
    }
}

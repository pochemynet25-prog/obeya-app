package kz.appetit.obeya;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;

/** Выбор филиала один раз на телефоне. Список тянется из панели. */
public class BranchPickerActivity extends Activity {
    private LinearLayout container;
    private TextView status;

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_branch_picker);
        container = findViewById(R.id.ll_branches);
        status = findViewById(R.id.tv_status);
        load();
    }

    private void load(){
        status.setText("Загрузка списка филиалов…");
        container.removeAllViews();
        new Thread(() -> {
            String res = null;
            try { res = Net.get(Config.branchesUrl()); } catch (Exception ignored) {}
            final String r = res;
            new Handler(Looper.getMainLooper()).post(() -> render(r));
        }).start();
    }

    private void render(String json){
        container.removeAllViews();
        if (json == null){ status.setText("Нет связи. Проверь интернет."); addBtn("Повторить", v -> load()); return; }
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0){ status.setText("Филиалы ещё не заведены в панели."); addBtn("Повторить", v -> load()); return; }
            status.setText("Выберите свой филиал:");
            for (int i = 0; i < arr.length(); i++){
                JSONObject o = arr.getJSONObject(i);
                final String id = o.getString("id");
                final String name = o.optString("name", id);
                addBtn(name, v -> choose(id, name));
            }
        } catch (Exception e){
            status.setText("Ошибка данных. Повторите.");
            addBtn("Повторить", v -> load());
        }
    }

    private void addBtn(String text, View.OnClickListener cl){
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextSize(18);
        btn.setTextColor(0xFF303030);
        btn.setBackgroundColor(0xFFFFB700);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 12, 0, 12);
        btn.setLayoutParams(lp);
        btn.setPadding(24, 32, 24, 32);
        btn.setOnClickListener(cl);
        container.addView(btn);
    }

    private void choose(String id, String name){
        Config.setBranch(this, id, name);
        AlarmReceiver.ensureChannel(this);
        SyncReceiver.schedulePeriodic(this);
        SyncReceiver.trigger(this);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}

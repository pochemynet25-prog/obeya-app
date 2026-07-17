package kz.appetit.obeya;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Главный экран: филиал, список ближайших задач, кнопки. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle b){
        super.onCreate(b);

        if (Config.getBranch(this) == null){
            startActivity(new Intent(this, BranchPickerActivity.class));
            finish();
            return;
        }

        AlarmReceiver.ensureChannel(this);
        AlarmService.ensureRunning(this);
        SyncReceiver.schedulePeriodic(this);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        setContentView(R.layout.activity_main);
        ((TextView) findViewById(R.id.tv_branch)).setText("Филиал: " + Config.getBranchName(this));

        findViewById(R.id.btn_refresh).setOnClickListener(v -> {
            SyncReceiver.trigger(this);
            Toast.makeText(this, "Обновляю…", Toast.LENGTH_SHORT).show();
            findViewById(R.id.ll_tasks).postDelayed(this::renderList, 2500);
        });
        findViewById(R.id.btn_change).setOnClickListener(v -> {
            Config.clearBranch(this);
            startActivity(new Intent(this, BranchPickerActivity.class));
            finish();
        });
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()))); } catch (Exception ignored) {}
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        AlarmService.ensureRunning(this);
        renderList();
        SyncReceiver.trigger(this);
        Push.fetchAndRegister(this);
        findViewById(R.id.ll_tasks).postDelayed(this::renderList, 2500);
    }

    private void renderList(){
        LinearLayout ll = findViewById(R.id.ll_tasks);
        if (ll == null) return;
        ll.removeAllViews();
        List<Task> all = TaskStore.getAll(this);
        SimpleDateFormat f = new SimpleDateFormat("dd.MM  HH:mm", new Locale("ru"));
        int shown = 0;
        for (Task t : all){
            if (t.acknowledged) continue;
            TextView tv = new TextView(this);
            tv.setText("🔔  " + f.format(new Date(t.alarmAtMillis)) + "   —   " + t.title);
            tv.setTextColor(0xFFFFFFFF);
            tv.setTextSize(16);
            tv.setPadding(0, 20, 0, 20);
            ll.addView(tv);
            shown++;
        }
        if (shown == 0){
            TextView tv = new TextView(this);
            tv.setText("Активных задач нет.");
            tv.setTextColor(0xFF9A9A9A);
            tv.setTextSize(15);
            ll.addView(tv);
        }
    }
}

package kz.appetit.obeya;

import android.app.Activity;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Полноэкранный звонящий будильник. «Готово» доступно только когда отмечены все пункты. */
public class AlarmActivity extends Activity {
    private MediaPlayer player;
    private Vibrator vibrator;
    private Task task;
    private final List<CheckBox> boxes = new ArrayList<>();
    private Button doneBtn;

    @Override
    protected void onCreate(Bundle s){
        super.onCreate(s);
        showOverLockscreen();
        String taskId = getIntent().getStringExtra("taskId");
        task = TaskStore.get(this, taskId);
        if (task == null || task.acknowledged){ finish(); return; }

        setContentView(R.layout.activity_alarm);
        ((TextView) findViewById(R.id.tv_title)).setText(task.title);
        LinearLayout list = findViewById(R.id.ll_checklist);
        doneBtn = findViewById(R.id.btn_done);

        for (String item : task.checklist){
            CheckBox cb = new CheckBox(this);
            cb.setText(item);
            cb.setTextSize(18);
            cb.setTextColor(0xFFFFFFFF);
            cb.setPadding(0, 24, 0, 24);
            cb.setOnCheckedChangeListener((v, c) -> updateDone());
            boxes.add(cb);
            list.addView(cb);
        }
        doneBtn.setEnabled(task.checklist.isEmpty());
        doneBtn.setOnClickListener(v -> acknowledge());
        startAlarm();
    }

    private void showOverLockscreen(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1){ setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void updateDone(){
        boolean all = true;
        for (CheckBox cb : boxes) if (!cb.isChecked()){ all = false; break; }
        doneBtn.setEnabled(all);
    }

    private void startAlarm(){
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
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()){
            long[] pat = {0, 700, 700};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createWaveform(pat, 0));
            else vibrator.vibrate(pat, 0);
        }
    }

    private void acknowledge(){
        stopAlarm();
        TaskStore.markAcknowledged(this, task.id);
        AlarmReceiver.clear(this);
        SyncReceiver.trigger(this);
        finish();
    }

    private void stopAlarm(){
        try { if (player != null){ player.stop(); player.release(); } } catch (Exception ignored) {}
        player = null;
        if (vibrator != null) vibrator.cancel();
    }

    @Override public void onBackPressed(){ /* закрыть без отметки нельзя */ }
    @Override protected void onDestroy(){ super.onDestroy(); stopAlarm(); }
}

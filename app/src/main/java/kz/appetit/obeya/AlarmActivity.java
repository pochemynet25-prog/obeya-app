package kz.appetit.obeya;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

/** Полноэкранный экран задачи. Звук держит служба; «Готово» доступно только когда всё отмечено. */
public class AlarmActivity extends Activity {
    private Task task;
    private final List<CheckBox> boxes = new ArrayList<>();
    private Button doneBtn;

    @Override
    protected void onCreate(Bundle s){
        super.onCreate(s);
        showOverLockscreen();
        String taskId = getIntent().getStringExtra("taskId");
        task = TaskStore.get(this, taskId);
        if (task == null || task.acknowledged){ AlarmService.stopRing(this); finish(); return; }

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

    private void acknowledge(){
        AlarmService.stopRing(this);
        TaskStore.markAcknowledged(this, task.id);
        SyncReceiver.trigger(this);
        finish();
    }

    @Override public void onBackPressed(){ /* закрыть без отметки нельзя */ }
}

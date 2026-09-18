package com.lecong.clock;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(13, 13, 13);
    private static final int CARD = Color.rgb(30, 30, 30);
    private static final int ORANGE = Color.rgb(255, 159, 10);
    private static final int GREEN = Color.rgb(48, 209, 88);
    private static final int GRAY = Color.rgb(142, 142, 147);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FrameLayout content;
    private Runnable activeTicker;
    private SharedPreferences prefs;
    private long stopwatchStartedAt, stopwatchAccumulated;
    private boolean stopwatchRunning;
    private final ArrayList<String> laps = new ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("clock_data", MODE_PRIVATE);
        requestNotificationPermission();
        showShell();
    }

    @Override protected void onDestroy() {
        if (activeTicker != null) handler.removeCallbacks(activeTicker);
        super.onDestroy();
    }

    private void showShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(0, dp(7), 0, dp(8));
        String[] labels = {"Quốc tế", "Báo thức", "Đi ngủ", "Bấm giờ", "Hẹn giờ"};
        String[] icons = {"◎", "●", "☾", "◉", "◷"};
        for (int i = 0; i < labels.length; i++) {
            final int tab = i;
            Button b = new Button(this);
            b.setText(icons[i] + "\n" + labels[i]);
            b.setTextSize(11); b.setTextColor(i == 0 ? ORANGE : GRAY);
            b.setAllCaps(false); b.setGravity(Gravity.CENTER); b.setBackgroundColor(BG);
            b.setPadding(0, 0, 0, 0);
            b.setOnClickListener(v -> { resetNavColors(nav); b.setTextColor(ORANGE); openTab(tab); });
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(72)));
        setContentView(root);
        showWorldClock();
    }

    private void resetNavColors(LinearLayout nav) {
        for (int i = 0; i < nav.getChildCount(); i++) ((Button) nav.getChildAt(i)).setTextColor(GRAY);
    }

    private void openTab(int tab) {
        if (activeTicker != null) handler.removeCallbacks(activeTicker);
        activeTicker = null;
        switch (tab) {
            case 0: showWorldClock(); break;
            case 1: showAlarms(); break;
            case 2: showBedtime(); break;
            case 3: showStopwatch(); break;
            default: showTimer();
        }
    }

    private LinearLayout page(String title) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(BG); p.setPadding(dp(18), dp(20), dp(18), dp(12));
        TextView h = text(title, 32, Color.WHITE); h.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        p.addView(h, new LinearLayout.LayoutParams(-1, dp(58)));
        return p;
    }

    private void setPage(View v) { content.removeAllViews(); content.addView(v, new FrameLayout.LayoutParams(-1, -1)); }

    private void showWorldClock() {
        LinearLayout p = page("Giờ quốc tế");
        TextView sub = text("Hôm nay", 15, GRAY); p.addView(sub);
        String[][] zones = {{"Hà Nội", "Asia/Ho_Chi_Minh"}, {"London", "Europe/London"}, {"Tokyo", "Asia/Tokyo"}, {"New York", "America/New_York"}};
        ArrayList<TextView> clocks = new ArrayList<>();
        for (String[] zone : zones) {
            LinearLayout row = card();
            LinearLayout left = new LinearLayout(this); left.setOrientation(LinearLayout.VERTICAL);
            TextView city = text(zone[0], 20, Color.WHITE); TextView diff = text("", 13, GRAY);
            left.addView(city); left.addView(diff);
            TextView time = text("--:--", 42, Color.WHITE); time.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(time);
            p.addView(row, marginParams(-1, dp(82), 0, dp(6)));
            time.setTag(zone[1]); diff.setTag("diff:" + zone[1]); clocks.add(time); clocks.add(diff);
        }
        activeTicker = new Runnable() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                int localOffset = TimeZone.getDefault().getOffset(now) / 3600000;
                for (TextView tv : clocks) {
                    String tag = String.valueOf(tv.getTag());
                    if (tag.startsWith("diff:")) {
                        TimeZone z = TimeZone.getTimeZone(tag.substring(5));
                        int d = z.getOffset(now) / 3600000 - localOffset;
                        tv.setText((d == 0 ? "Cùng múi giờ" : (d > 0 ? "+" : "") + d + " giờ") + " · Hôm nay");
                    } else {
                        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.getDefault()); f.setTimeZone(TimeZone.getTimeZone(tag)); tv.setText(f.format(new Date(now)));
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        activeTicker.run(); setPage(p);
    }

    private void showAlarms() {
        LinearLayout outer = page("Báo thức");
        Button add = actionButton("＋  Thêm báo thức", ORANGE); outer.addView(add, new LinearLayout.LayoutParams(-1, dp(48)));
        ScrollView scroll = new ScrollView(this); LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list); outer.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        add.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new TimePickerDialog(this, (view, h, m) -> { saveAlarm(h, m, true); scheduleAlarm(h, m, "Báo thức"); showAlarms(); }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
        ArrayList<String> alarms = loadAlarms();
        if (alarms.isEmpty()) list.addView(emptyText("Chưa có báo thức\nNhấn “Thêm báo thức” để bắt đầu"));
        for (String a : alarms) {
            String[] x = a.split(":"); int h = Integer.parseInt(x[0]), m = Integer.parseInt(x[1]); boolean enabled = x.length < 3 || x[2].equals("1");
            LinearLayout row = card();
            LinearLayout info = new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL);
            TextView t = text(String.format(Locale.getDefault(), "%02d:%02d", h, m), 44, enabled ? Color.WHITE : GRAY);
            TextView d = text("Báo thức · Hằng ngày", 14, GRAY); info.addView(t); info.addView(d);
            Switch sw = new Switch(this); sw.setChecked(enabled); sw.setButtonTintList(null);
            sw.setOnCheckedChangeListener((button, checked) -> { saveAlarm(h, m, checked); if (checked) scheduleAlarm(h, m, "Báo thức"); else cancelAlarm(h, m); showAlarms(); });
            row.addView(info, new LinearLayout.LayoutParams(0, -2, 1)); row.addView(sw);
            row.setOnLongClickListener(v -> { deleteAlarm(h, m); cancelAlarm(h, m); showAlarms(); Toast.makeText(this, "Đã xóa báo thức", Toast.LENGTH_SHORT).show(); return true; });
            list.addView(row, marginParams(-1, dp(92), 0, dp(6)));
        }
        TextView hint = text("Giữ một báo thức để xóa", 12, GRAY); hint.setGravity(Gravity.CENTER); outer.addView(hint);
        setPage(outer);
    }

    private void showBedtime() {
        LinearLayout p = page("Giờ đi ngủ");
        TextView moon = text("☾", 80, ORANGE); moon.setGravity(Gravity.CENTER); p.addView(moon, new LinearLayout.LayoutParams(-1, dp(110)));
        TextView desc = text("Đặt lịch ngủ đều đặn để bắt đầu ngày mới tốt hơn.", 16, GRAY); desc.setGravity(Gravity.CENTER); p.addView(desc);
        int bedH = prefs.getInt("bed_h", 22), bedM = prefs.getInt("bed_m", 30), wakeH = prefs.getInt("wake_h", 6), wakeM = prefs.getInt("wake_m", 30);
        Button bed = pickerButton("Giờ đi ngủ", bedH, bedM); Button wake = pickerButton("Giờ thức dậy", wakeH, wakeM);
        p.addView(bed, marginParams(-1, dp(64), 0, dp(18))); p.addView(wake, marginParams(-1, dp(64), 0, dp(10)));
        LinearLayout enabledRow = card(); TextView label = text("Nhắc lịch hằng ngày", 17, Color.WHITE); Switch enabled = new Switch(this); enabled.setChecked(prefs.getBoolean("bed_enabled", false));
        enabledRow.addView(label, new LinearLayout.LayoutParams(0, -2, 1)); enabledRow.addView(enabled); p.addView(enabledRow, marginParams(-1, dp(64), 0, dp(10)));
        bed.setOnClickListener(v -> new TimePickerDialog(this, (a,h,m)->{prefs.edit().putInt("bed_h",h).putInt("bed_m",m).apply(); showBedtime();}, bedH, bedM, true).show());
        wake.setOnClickListener(v -> new TimePickerDialog(this, (a,h,m)->{prefs.edit().putInt("wake_h",h).putInt("wake_m",m).apply(); showBedtime();}, wakeH, wakeM, true).show());
        enabled.setOnCheckedChangeListener((v,on)->{ prefs.edit().putBoolean("bed_enabled",on).apply(); if(on){scheduleAlarm(bedH,bedM,"Đã đến giờ đi ngủ"); scheduleAlarm(wakeH,wakeM,"Chào buổi sáng");} else {cancelAlarm(bedH,bedM);cancelAlarm(wakeH,wakeM);} });
        setPage(p);
    }

    private void showStopwatch() {
        LinearLayout p = page("Bấm giờ");
        TextView clock = text("00:00.00", 58, Color.WHITE); clock.setTypeface(Typeface.create("monospace", Typeface.NORMAL)); clock.setGravity(Gravity.CENTER);
        p.addView(clock, new LinearLayout.LayoutParams(-1, dp(140)));
        LinearLayout actions = new LinearLayout(this); actions.setGravity(Gravity.CENTER);
        Button reset = roundButton(stopwatchRunning ? "Vòng" : "Đặt lại", Color.DKGRAY); Button start = roundButton(stopwatchRunning ? "Dừng" : "Bắt đầu", stopwatchRunning ? Color.rgb(90,25,25) : Color.rgb(15,80,35)); start.setTextColor(stopwatchRunning ? Color.RED : GREEN);
        actions.addView(reset, marginParams(dp(112), dp(64), dp(16), 0)); actions.addView(start, marginParams(dp(112), dp(64), dp(16), 0)); p.addView(actions);
        ScrollView scroll = new ScrollView(this); LinearLayout lapList = new LinearLayout(this); lapList.setOrientation(LinearLayout.VERTICAL); scroll.addView(lapList); p.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        renderLaps(lapList);
        start.setOnClickListener(v->{ if(stopwatchRunning){stopwatchAccumulated += SystemClock.elapsedRealtime()-stopwatchStartedAt; stopwatchRunning=false;} else {stopwatchStartedAt=SystemClock.elapsedRealtime();stopwatchRunning=true;} showStopwatch(); });
        reset.setOnClickListener(v->{ if(stopwatchRunning){laps.add(formatDuration(stopwatchAccumulated+SystemClock.elapsedRealtime()-stopwatchStartedAt));} else {stopwatchAccumulated=0;laps.clear();} showStopwatch(); });
        activeTicker = new Runnable(){@Override public void run(){long ms=stopwatchAccumulated+(stopwatchRunning?SystemClock.elapsedRealtime()-stopwatchStartedAt:0);clock.setText(formatDuration(ms));handler.postDelayed(this,32);}};
        activeTicker.run(); setPage(p);
    }

    private void renderLaps(LinearLayout list) {
        for(int i=laps.size()-1;i>=0;i--){LinearLayout r=card();r.addView(text("Vòng "+(i+1),16,Color.WHITE),new LinearLayout.LayoutParams(0,-2,1));r.addView(text(laps.get(i),16,Color.WHITE));list.addView(r,marginParams(-1,dp(52),0,dp(4)));}
    }

    private void showTimer() {
        LinearLayout p=page("Hẹn giờ");
        LinearLayout picks=new LinearLayout(this);picks.setGravity(Gravity.CENTER);
        NumberPicker ph=picker(0,23), pm=picker(0,59), ps=picker(0,59); picks.addView(labeledPicker(ph,"giờ")); picks.addView(labeledPicker(pm,"phút")); picks.addView(labeledPicker(ps,"giây")); p.addView(picks,new LinearLayout.LayoutParams(-1,0,1));
        Button start=actionButton("Bắt đầu",GREEN);p.addView(start,new LinearLayout.LayoutParams(-1,dp(56)));
        start.setOnClickListener(v->{long seconds=ph.getValue()*3600L+pm.getValue()*60L+ps.getValue();if(seconds<=0){Toast.makeText(this,"Hãy chọn thời gian",Toast.LENGTH_SHORT).show();return;}showTimerRunning(seconds*1000L);});
        setPage(p);
    }

    private void showTimerRunning(long totalMs) {
        if(activeTicker!=null)handler.removeCallbacks(activeTicker);
        LinearLayout p=page("Hẹn giờ"); long end=SystemClock.elapsedRealtime()+totalMs; final boolean[] paused={false}; final long[] remain={totalMs};
        TextView time=text(formatTimer(totalMs),62,Color.WHITE);time.setTypeface(Typeface.create("monospace",Typeface.NORMAL));time.setGravity(Gravity.CENTER);p.addView(time,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout buttons=new LinearLayout(this);Button cancel=roundButton("Hủy",Color.DKGRAY);Button pause=roundButton("Tạm dừng",Color.rgb(90,75,15));pause.setTextColor(ORANGE);LinearLayout.LayoutParams leftButton=marginParams(0,dp(64),dp(16),0);leftButton.weight=1;LinearLayout.LayoutParams rightButton=marginParams(0,dp(64),0,0);rightButton.weight=1;buttons.addView(cancel,leftButton);buttons.addView(pause,rightButton);p.addView(buttons,new LinearLayout.LayoutParams(-1,dp(82)));
        final long[] target={end};
        activeTicker=new Runnable(){@Override public void run(){if(!paused[0])remain[0]=Math.max(0,target[0]-SystemClock.elapsedRealtime());time.setText(formatTimer(remain[0]));if(remain[0]==0){notifyNow("Hẹn giờ");Toast.makeText(MainActivity.this,"Hết giờ!",Toast.LENGTH_LONG).show();showTimer();}else handler.postDelayed(this,200);}};activeTicker.run();
        cancel.setOnClickListener(v->showTimer()); pause.setOnClickListener(v->{paused[0]=!paused[0];if(paused[0]){remain[0]=Math.max(0,target[0]-SystemClock.elapsedRealtime());pause.setText("Tiếp tục");}else{target[0]=SystemClock.elapsedRealtime()+remain[0];pause.setText("Tạm dừng");}});
        setPage(p);
    }

    private ArrayList<String> loadAlarms(){return new ArrayList<>(prefs.getStringSet("alarms",new HashSet<>()));}
    private void saveAlarm(int h,int m,boolean on){Set<String>s=new HashSet<>(prefs.getStringSet("alarms",new HashSet<>()));s.removeIf(x->x.startsWith(h+":"+m+":"));s.add(h+":"+m+":"+(on?1:0));prefs.edit().putStringSet("alarms",s).apply();}
    private void deleteAlarm(int h,int m){Set<String>s=new HashSet<>(prefs.getStringSet("alarms",new HashSet<>()));s.removeIf(x->x.startsWith(h+":"+m+":"));prefs.edit().putStringSet("alarms",s).apply();}
    private int alarmId(int h,int m){return 1000+h*60+m;}
    private void scheduleAlarm(int h,int m,String title){Calendar c=Calendar.getInstance();c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,m);c.set(Calendar.SECOND,0);if(c.getTimeInMillis()<=System.currentTimeMillis())c.add(Calendar.DAY_OF_YEAR,1);Intent i=new Intent(this,AlarmReceiver.class).putExtra("title",title);PendingIntent pi=PendingIntent.getBroadcast(this,alarmId(h,m),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);try{if(am.canScheduleExactAlarms())am.setRepeating(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),AlarmManager.INTERVAL_DAY,pi);else{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));am.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);}}catch(Exception e){am.set(AlarmManager.RTC_WAKEUP,c.getTimeInMillis(),pi);}}
    private void cancelAlarm(int h,int m){Intent i=new Intent(this,AlarmReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,alarmId(h,m),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(pi);}
    private void notifyNow(String title){sendBroadcast(new Intent(this,AlarmReceiver.class).putExtra("title",title));}
    private void requestNotificationPermission(){if(android.os.Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);}

    private LinearLayout card(){LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(16),dp(8),dp(16),dp(8));r.setBackgroundColor(CARD);return r;}
    private TextView text(String value,float size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private TextView emptyText(String s){TextView t=text(s,16,GRAY);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(80),0,dp(20));return t;}
    private Button actionButton(String s,int color){Button b=new Button(this);b.setText(s);b.setTextSize(17);b.setTextColor(color);b.setAllCaps(false);b.setBackgroundColor(CARD);return b;}
    private Button pickerButton(String label,int h,int m){Button b=actionButton(label+"                 "+String.format(Locale.getDefault(),"%02d:%02d",h,m),Color.WHITE);b.setGravity(Gravity.CENTER_VERTICAL);return b;}
    private Button roundButton(String s,int bg){Button b=actionButton(s,Color.WHITE);b.setBackgroundColor(bg);return b;}
    private NumberPicker picker(int min,int max){NumberPicker p=new NumberPicker(this);p.setMinValue(min);p.setMaxValue(max);if(android.os.Build.VERSION.SDK_INT>=29)p.setTextColor(Color.WHITE);return p;}
    private LinearLayout labeledPicker(NumberPicker picker,String label){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.addView(picker,new LinearLayout.LayoutParams(dp(92),dp(180)));TextView t=text(label,14,GRAY);t.setGravity(Gravity.CENTER);box.addView(t,new LinearLayout.LayoutParams(dp(92),dp(36)));return box;}
    private LinearLayout.LayoutParams marginParams(int w,int h,int right,int bottom){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(w,h);lp.setMargins(0,0,right,bottom);return lp;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private String formatDuration(long ms){long cs=(ms/10)%100,s=(ms/1000)%60,m=(ms/60000);return String.format(Locale.getDefault(),"%02d:%02d.%02d",m,s,cs);}
    private String formatTimer(long ms){long total=(ms+999)/1000,h=total/3600,m=(total/60)%60,s=total%60;return String.format(Locale.getDefault(),"%02d:%02d:%02d",h,m,s);}
}

package com.lecong.clock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        if (title == null) title = "Báo thức";
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("clock_alarm", "Báo thức", NotificationManager.IMPORTANCE_HIGH);
        channel.enableVibration(true);
        nm.createNotificationChannel(channel);
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        android.app.Notification n = new android.app.Notification.Builder(context, "clock_alarm")
                .setSmallIcon(com.lecong.clock.R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText("Đã đến giờ")
                .setContentIntent(pi).setAutoCancel(true).setCategory(android.app.Notification.CATEGORY_ALARM).build();
        nm.notify((int) System.currentTimeMillis(), n);
    }
}

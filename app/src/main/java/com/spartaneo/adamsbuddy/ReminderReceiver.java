package com.spartaneo.adamsbuddy;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Calendar;

public final class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL = "buddy_check_in";
    private static final String PREFS = "buddy_reminders";
    private static final String ENABLED = "enabled";

    static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false);
    }

    static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).apply();
        schedule(context);
    }

    static void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(context, 31, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarms.cancel(pending);
        if (!enabled(context)) return;
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 21);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 1);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pending);
    }

    static void notifyNow(Context context) {
        post(context, CHANNEL, "Buddy check-in", "Check in with Buddy",
                "Open your tasks and daily PC report.", 31);
    }

    static void notifyWatchTest(Context context) {
        post(context, "buddy_watch_test", "Buddy watch pings", "Buddy says hello 👋",
                "If enabled in your watch app, this alert should appear on your AM06C.", 33);
    }

    private static void post(Context context, String channel, String channelName,
                             String title, String message, int id) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        NotificationManager notifications = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notifications == null) return;
        notifications.createNotificationChannel(new NotificationChannel(channel, channelName,
                NotificationManager.IMPORTANCE_DEFAULT));
        Intent open = new Intent(context, MainActivity.class);
        open.setAction(Intent.ACTION_VIEW);
        open.putExtra("buddy_path", "/");
        PendingIntent tap = PendingIntent.getActivity(context, 32, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, channel)
                .setSmallIcon(R.drawable.buddy_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(tap)
                .setAutoCancel(true)
                .build();
        notifications.notify(id, notification);
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (enabled(context)) notifyNow(context);
    }
}

package com.spartaneo.adamsbuddy;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.browser.customtabs.CustomTabsIntent;

public final class MainActivity extends Activity {
    private static final int NOTIFICATION_REQUEST = 40;
    private Button reminderButton;
    private boolean pendingReminderEnable;
    private boolean pendingTestNotification;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        drawHome();
        if (state == null) {
            String path = getIntent().getStringExtra("buddy_path");
            openBuddy("/game".equals(path) ? BuddyLinks.GAME : BuddyLinks.HOME);
        }
    }

    private void drawHome() {
        int background = Color.rgb(7, 19, 30);
        int foreground = Color.rgb(232, 243, 247);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(background);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("Adam's Buddy");
        title.setTextSize(29);
        title.setTextColor(foreground);
        layout.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Your assistant and game, one tap away.");
        subtitle.setTextSize(16);
        subtitle.setTextColor(Color.rgb(180, 208, 220));
        layout.addView(subtitle);

        addButton(layout, "Talk to Buddy", () -> openBuddy(BuddyLinks.HOME));
        addButton(layout, "Play game", () -> openBuddy(BuddyLinks.GAME));
        addButton(layout, "Earnings", () -> openBuddy(BuddyLinks.EARNINGS));
        addButton(layout, "Health", () -> openBuddy(BuddyLinks.HEALTH));
        addButton(layout, "AI tools", () -> openBuddy(BuddyLinks.AI_TOOLS));
        addButton(layout, "Studio", () -> openBuddy(BuddyLinks.STUDIO));
        addButton(layout, "How to use", () -> openBuddy(BuddyLinks.HELP));
        reminderButton = addButton(layout, "", this::toggleReminders);
        updateReminderButton();
        addButton(layout, "Test notification", () -> {
            if (requestNotificationPermission()) { pendingTestNotification = true; return; }
            ReminderReceiver.notifyNow(this);
        });
        TextView note = new TextView(this);
        note.setText("Buddy opens securely using your phone's browser sign-in. Daily reminders are generated on this phone and may arrive later than 9 PM. Live PC alerts are still being built.");
        note.setTextColor(Color.rgb(180, 208, 220));
        note.setTextSize(14);
        note.setPadding(0, pad, 0, 0);
        layout.addView(note);
        setContentView(scroll);
    }

    private Button addButton(LinearLayout layout, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = (int) (12 * getResources().getDisplayMetrics().density);
        layout.addView(button, params);
        return button;
    }

    private void openBuddy(String url) {
        try {
            CustomTabsIntent tab = new CustomTabsIntent.Builder()
                    .setToolbarColor(Color.rgb(16, 39, 56))
                    .setShowTitle(true)
                    .build();
            tab.launchUrl(this, Uri.parse(url));
        } catch (ActivityNotFoundException error) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch (ActivityNotFoundException ignored) { Toast.makeText(this, "Install a browser to open Buddy.", Toast.LENGTH_LONG).show(); }
        }
    }

    private boolean requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
            return true;
        }
        return false;
    }

    private void toggleReminders() {
        boolean enable = !ReminderReceiver.enabled(this);
        if (enable && requestNotificationPermission()) { pendingReminderEnable = true; return; }
        ReminderReceiver.setEnabled(this, enable);
        updateReminderButton();
    }

    private void updateReminderButton() {
        reminderButton.setText(ReminderReceiver.enabled(this) ? "Turn off daily reminder" : "Turn on daily reminder");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == NOTIFICATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingReminderEnable) {
                ReminderReceiver.setEnabled(this, true);
                updateReminderButton();
                Toast.makeText(this, "Daily Buddy reminder enabled.", Toast.LENGTH_SHORT).show();
            }
            if (pendingTestNotification) ReminderReceiver.notifyNow(this);
        }
        pendingReminderEnable = false;
        pendingTestNotification = false;
    }
}

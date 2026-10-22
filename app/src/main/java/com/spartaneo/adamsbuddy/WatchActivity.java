package com.spartaneo.adamsbuddy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;

public final class WatchActivity extends Activity {
    private static final int SAVE_FACE = 51;
    private static final int NOTIFICATION_REQUEST = 52;
    private final int ink = Color.rgb(8, 22, 34);
    private final int card = Color.rgb(18, 47, 62);
    private final int mint = Color.rgb(134, 224, 194);
    private Bitmap face;
    private boolean saveArcade;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) saveArcade = state.getBoolean("save_arcade", false);
        getWindow().setStatusBarColor(ink);
        getWindow().setNavigationBarColor(ink);
        face = WatchFaceArt.create(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(ink);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20), dp(18), dp(20), dp(28));
        scroll.addView(body);

        TextView back = label("‹  Back to Buddy", 16, mint, false);
        back.setOnClickListener(view -> finish());
        body.addView(back);
        addSpace(body, 22);
        body.addView(label("BUDDY ON YOUR WATCH", 13, mint, true));
        body.addView(label("A little Buddy, all day.", 27, Color.WHITE, true));
        addSpace(body, 8);
        body.addView(label("For the AM06C shown in your photos. No extra service or AI calls.",
                15, Color.rgb(182, 208, 219), false));
        addSpace(body, 22);

        LinearLayout faceCard = card();
        ImageView preview = new ImageView(this);
        preview.setImageBitmap(face);
        preview.setContentDescription("Preview of the Buddy watch face image");
        faceCard.addView(preview, new LinearLayout.LayoutParams(-1, dp(270)));
        faceCard.addView(label("Buddy face  •  room for your watch's clock", 13,
                Color.rgb(182, 208, 219), false));
        body.addView(faceCard);
        addSpace(body, 16);

        action(body, "Save Buddy watch face", true, () -> saveFace(false));
        action(body, "Save arcade watch face", false, () -> saveFace(true));
        body.addView(label("Then open your watch app: Device → Watch Face Gallery → Custom. Pick the saved image there.",
                14, Color.rgb(182, 208, 219), false));
        addSpace(body, 12);
        action(body, "Open your watch app", false, this::openWatchApp);
        addSpace(body, 24);

        LinearLayout alertCard = card();
        alertCard.addView(label("Make Buddy buzz your wrist", 19, Color.WHITE, true));
        alertCard.addView(label("Buddy can send a phone notification. Your watch app can mirror it when Buddy is enabled in its Notification list.",
                14, Color.rgb(182, 208, 219), false));
        body.addView(alertCard);
        addSpace(body, 12);
        action(body, "Send a test alert", true, this::sendTest);
        action(body, "Buddy notification settings", false, this::openNotificationSettings);
        body.addView(label("In the watch app, open Device → Notification and allow Adam's Buddy. The test confirms only that the phone sent an alert; check the watch to confirm mirroring.",
                14, Color.rgb(182, 208, 219), false));
        addSpace(body, 20);
        body.addView(label("Live screen or video mirroring is not available from the watch controls shown. Buddy does not write unknown Bluetooth commands to the watch.",
                13, Color.rgb(174, 192, 203), false));
        addSpace(body, 14);
        action(body, "Play Buddy Blaster on phone", false,
                () -> startActivity(new Intent(this, ArcadeActivity.class)));
        setContentView(scroll);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(0, dp(3), 0, dp(3));
        if (bold) view.setTypeface(null, 1);
        return view;
    }

    private LinearLayout card() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(14), dp(16), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(card);
        background.setCornerRadius(dp(22));
        background.setStroke(dp(1), Color.rgb(61, 112, 123));
        layout.setBackground(background);
        return layout;
    }

    private void action(LinearLayout parent, String title, boolean primary, Runnable action) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(title);
        button.setTextSize(16);
        button.setTypeface(null, 1);
        button.setTextColor(primary ? ink : Color.WHITE);
        button.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? mint : card);
        background.setCornerRadius(dp(15));
        if (!primary) background.setStroke(dp(1), Color.rgb(73, 137, 149));
        button.setBackground(background);
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(54));
        params.bottomMargin = dp(10);
        parent.addView(button, params);
    }

    private void addSpace(LinearLayout parent, int height) {
        parent.addView(new TextView(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private void saveFace(boolean arcade) {
        saveArcade = arcade;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_TITLE, arcade ? "Buddy-Arcade-AM06C.png" : "Buddy-AM06C-face.png");
        startActivityForResult(intent, SAVE_FACE);
    }

    private void openWatchApp() {
        Intent launch = getPackageManager().getLaunchIntentForPackage("com.sma.smartv3.pro");
        if (launch == null) {
            Toast.makeText(this, "Watch app not found. Open it from your phone's app list.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(launch);
    }

    private void sendTest() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        } else postTest();
    }

    private void postTest() {
        ReminderReceiver.notifyWatchTest(this);
        Toast.makeText(this, "Sent to phone. Check your watch too.", Toast.LENGTH_LONG).show();
    }

    private void openNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        startActivity(intent);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == NOTIFICATION_REQUEST && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) postTest();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAVE_FACE || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Bitmap selected = saveArcade ? WatchFaceArt.createArcade() : face;
        try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
            if (stream == null || !selected.compress(Bitmap.CompressFormat.PNG, 100, stream))
                throw new IOException("Could not save image");
            Toast.makeText(this, "Face saved. Choose it in your watch app's Custom tab.", Toast.LENGTH_LONG).show();
        } catch (IOException error) {
            Toast.makeText(this, "Face could not be saved. Try again.", Toast.LENGTH_LONG).show();
        } finally {
            if (saveArcade) selected.recycle();
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("save_arcade", saveArcade);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (face != null) face.recycle();
    }
}

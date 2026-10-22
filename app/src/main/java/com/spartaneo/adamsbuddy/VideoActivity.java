package com.spartaneo.adamsbuddy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public final class VideoActivity extends Activity {
    private static final int PICK_VIDEO = 61;
    private VideoView player;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        int ink = Color.rgb(8, 22, 34);
        getWindow().setStatusBarColor(ink);
        getWindow().setNavigationBarColor(ink);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(16), dp(18), dp(18));
        root.setBackgroundColor(ink);
        TextView title = new TextView(this);
        title.setText("BUDDY TV");
        title.setTextColor(Color.rgb(139, 229, 197));
        title.setTextSize(25);
        title.setTypeface(null, 1);
        root.addView(title);
        TextView description = new TextView(this);
        description.setText("Pick a video on your phone. Buddy plays it here without uploading it.");
        description.setTextColor(Color.rgb(186, 211, 220));
        description.setTextSize(15);
        description.setPadding(0, dp(6), 0, dp(14));
        root.addView(description);

        player = new VideoView(this);
        player.setBackgroundColor(Color.BLACK);
        MediaController controls = new MediaController(this);
        controls.setAnchorView(player);
        player.setMediaController(controls);
        player.setOnErrorListener((view, what, extra) -> {
            Toast.makeText(this, "This video could not play on this phone.", Toast.LENGTH_LONG).show();
            return true;
        });
        root.addView(player, new LinearLayout.LayoutParams(-1, 0, 1));

        Button pick = new Button(this);
        pick.setText("Pick a video");
        pick.setAllCaps(false);
        pick.setTextSize(17);
        pick.setTypeface(null, 1);
        pick.setTextColor(ink);
        pick.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(139, 229, 197));
        background.setCornerRadius(dp(15));
        pick.setBackground(background);
        pick.setOnClickListener(view -> pickVideo());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(56));
        params.topMargin = dp(14);
        root.addView(pick, params);
        setContentView(root);

        String saved = getPreferences(MODE_PRIVATE).getString("selected_video", null);
        if (saved != null) play(Uri.parse(saved));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, PICK_VIDEO);
    }

    private void play(Uri uri) {
        player.setVideoURI(uri);
        player.setOnPreparedListener(media -> {
            media.setLooping(false);
            player.start();
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_VIDEO || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Playback still works with the current selection if this provider does not persist grants.
        }
        getPreferences(MODE_PRIVATE).edit().putString("selected_video", uri.toString()).apply();
        play(uri);
    }

    @Override protected void onPause() {
        player.pause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        player.stopPlayback();
        super.onDestroy();
    }
}

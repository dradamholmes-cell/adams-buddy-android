package com.spartaneo.adamsbuddy;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class ArcadeActivity extends Activity {
    private GameView game;
    private MediaSession watchRemote;
    private TextView watchRemoteStatus;
    private boolean watchRemoteEnabled;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            game.step();
            game.invalidate();
            handler.postDelayed(this, 65); // about 15 fps, including on modest phones
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8, 22, 34));
        getWindow().setNavigationBarColor(Color.rgb(8, 22, 34));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(12));
        root.setBackgroundColor(Color.rgb(7, 19, 30));
        TextView title = new TextView(this);
        title.setText("BUDDY BLASTER  ·  ARCADE BREAK");
        title.setTextColor(Color.rgb(139, 229, 197));
        title.setTextSize(17);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        TextView note = new TextView(this);
        note.setText("Clear the room. Score is for fun; it is not cash.");
        note.setTextColor(Color.rgb(187, 211, 219));
        note.setTextSize(13);
        root.addView(note);

        watchRemoteStatus = new TextView(this);
        watchRemoteStatus.setText("Watch remote off · game runs on phone");
        watchRemoteStatus.setTextColor(Color.rgb(187, 211, 219));
        watchRemoteStatus.setTextSize(12);
        root.addView(watchRemoteStatus);

        game = new GameView();
        LinearLayout.LayoutParams gameSpace = new LinearLayout.LayoutParams(-1, 0, 1);
        gameSpace.topMargin = dp(12);
        root.addView(game, gameSpace);

        LinearLayout controls = new LinearLayout(this);
        controls.setPadding(0, dp(12), 0, 0);
        control(controls, "↶", 0);
        control(controls, "↑", 1);
        control(controls, "↷", 2);
        control(controls, "FIRE", 3);
        root.addView(controls);

        LinearLayout bottom = new LinearLayout(this);
        controlAction(bottom, "New room", () -> game.reset());
        controlAction(bottom, "Play a video", () -> startActivity(new Intent(this, VideoActivity.class)));
        root.addView(bottom);
        LinearLayout remoteRow = new LinearLayout(this);
        controlAction(remoteRow, "Watch play/pause → FIRE", () -> {
            watchRemoteEnabled = !watchRemoteEnabled;
            updateWatchRemote();
        });
        root.addView(remoteRow);
        setContentView(root);
    }

    private void updateWatchRemote() {
        if (!watchRemoteEnabled) {
            if (watchRemote != null) watchRemote.setActive(false);
            watchRemoteStatus.setText("Watch remote off · game runs on phone");
            return;
        }
        if (watchRemote == null) {
            watchRemote = new MediaSession(this, "Buddy Blaster watch remote");
            watchRemote.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                    | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
            watchRemote.setCallback(new MediaSession.Callback() {
                @Override public boolean onMediaButtonEvent(Intent intent) {
                    KeyEvent key = intent == null ? null
                            : intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                    if (key != null && key.getAction() == KeyEvent.ACTION_DOWN
                            && (key.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                            || key.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY
                            || key.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PAUSE)) {
                        runOnUiThread(() -> game.fire());
                        return true;
                    }
                    return super.onMediaButtonEvent(intent);
                }
                @Override public void onPlay() { runOnUiThread(() -> game.fire()); }
                @Override public void onPause() { runOnUiThread(() -> game.fire()); }
            });
            watchRemote.setPlaybackState(new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                            | PlaybackState.ACTION_PLAY_PAUSE)
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                    .build());
        }
        if (running) watchRemote.setActive(true);
        watchRemoteStatus.setText("Watch remote on · try Play/Pause in the watch Music screen");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private void control(LinearLayout row, String label, int control) {
        Button button = styled(label, control == 3);
        button.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                game.pressed[control] = true;
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                game.pressed[control] = false;
                view.performClick();
                return true;
            }
            return false;
        });
        button.setOnClickListener(view -> {
            if (control == 3) game.fire();
            else game.nudge(control);
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(58), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        row.addView(button, params);
    }

    private void controlAction(LinearLayout row, String label, Runnable action) {
        Button button = styled(label, false);
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1);
        params.setMargins(dp(3), dp(8), dp(3), 0);
        row.addView(button, params);
    }

    private Button styled(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(primary ? Color.rgb(7, 23, 34) : Color.WHITE);
        GradientDrawable background = new GradientDrawable();
        background.setColor(primary ? Color.rgb(241, 183, 97) : Color.rgb(22, 57, 72));
        background.setCornerRadius(dp(12));
        if (!primary) background.setStroke(dp(1), Color.rgb(74, 136, 150));
        button.setBackground(background);
        return button;
    }

    @Override protected void onResume() {
        super.onResume();
        running = true;
        if (watchRemoteEnabled) updateWatchRemote();
        handler.post(tick);
    }

    @Override protected void onPause() {
        running = false;
        handler.removeCallbacks(tick);
        if (watchRemote != null) watchRemote.setActive(false);
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (watchRemote != null) {
            watchRemote.release();
            watchRemote = null;
        }
        super.onDestroy();
    }

    private final class GameView extends View {
        private final String[] map = {
                "##########", "#........#", "#..#.....#", "#..#.....#", "#........#",
                "#......###", "#........#", "#..##....#", "#........#", "##########"
        };
        private final double[][] enemyStarts = {{6.5, 3.5}, {7.4, 7.3}, {2.2, 7.1}};
        private final boolean[] alive = {true, true, true};
        private final boolean[] pressed = new boolean[4];
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private double x = 2.0, y = 2.0, angle = 0;
        private int score;
        private int flash;

        GameView() {
            super(ArcadeActivity.this);
            setBackgroundColor(Color.rgb(6, 16, 26));
        }

        void reset() {
            x = 2.0; y = 2.0; angle = 0; score = 0; flash = 0;
            for (int i = 0; i < alive.length; i++) alive[i] = true;
            invalidate();
        }

        private boolean wall(double px, double py) {
            int mx = (int) px, my = (int) py;
            return my < 0 || my >= map.length || mx < 0 || mx >= map[my].length()
                    || map[my].charAt(mx) == '#';
        }

        void nudge(int control) {
            if (control == 0) angle -= .10;
            if (control == 2) angle += .10;
            if (control == 1) move(.13);
            invalidate();
        }

        void step() {
            if (pressed[0]) angle -= .065;
            if (pressed[2]) angle += .065;
            if (pressed[1]) move(.085);
            if (flash > 0) flash--;
        }

        private void move(double distance) {
            double nx = x + Math.cos(angle) * distance;
            double ny = y + Math.sin(angle) * distance;
            if (!wall(nx + Math.signum(distance) * Math.cos(angle) * .2, y)) x = nx;
            if (!wall(x, ny + Math.signum(distance) * Math.sin(angle) * .2)) y = ny;
        }

        private double wallDistance(double rayAngle) {
            double dx = Math.cos(rayAngle) * .05, dy = Math.sin(rayAngle) * .05;
            double rx = x, ry = y;
            for (int step = 1; step < 190; step++) {
                rx += dx; ry += dy;
                if (wall(rx, ry)) return step * .05;
            }
            return 9.5;
        }

        void fire() {
            flash = 3;
            int target = -1;
            double nearest = 9;
            for (int i = 0; i < alive.length; i++) {
                if (!alive[i]) continue;
                double dx = enemyStarts[i][0] - x, dy = enemyStarts[i][1] - y;
                double distance = Math.hypot(dx, dy);
                double delta = Math.atan2(Math.sin(Math.atan2(dy, dx) - angle),
                        Math.cos(Math.atan2(dy, dx) - angle));
                if (Math.abs(delta) < .15 && distance < nearest
                        && wallDistance(Math.atan2(dy, dx)) > distance - .2) {
                    target = i; nearest = distance;
                }
            }
            if (target >= 0) { alive[target] = false; score++; }
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth(), height = getHeight();
            if (width == 0 || height == 0) return;
            paint.setColor(Color.rgb(19, 48, 69));
            canvas.drawRect(0, 0, width, height / 2f, paint);
            paint.setColor(Color.rgb(31, 39, 46));
            canvas.drawRect(0, height / 2f, width, height, paint);

            final int columns = 80;
            for (int column = 0; column < columns; column++) {
                double rayAngle = angle + (column / (double) columns - .5) * 1.05;
                double distance = Math.max(.1, wallDistance(rayAngle) * Math.cos(rayAngle - angle));
                float wallHeight = (float) Math.min(height * 1.8, height * .72 / distance);
                int shade = (int) Math.max(31, 148 - distance * 18);
                paint.setColor(Color.rgb(shade / 3, shade, Math.min(255, shade + 18)));
                float left = column * width / (float) columns;
                float right = (column + 1) * width / (float) columns + 1;
                canvas.drawRect(left, height / 2f - wallHeight / 2, right,
                        height / 2f + wallHeight / 2, paint);
            }

            for (int i = 0; i < alive.length; i++) {
                if (!alive[i]) continue;
                double dx = enemyStarts[i][0] - x, dy = enemyStarts[i][1] - y;
                double distance = Math.hypot(dx, dy);
                double delta = Math.atan2(Math.sin(Math.atan2(dy, dx) - angle),
                        Math.cos(Math.atan2(dy, dx) - angle));
                if (Math.abs(delta) > .57 || wallDistance(Math.atan2(dy, dx)) < distance - .15) continue;
                float center = (float) (width * (.5 + delta / 1.05));
                float radius = (float) Math.min(height * .35, height * .25 / distance);
                paint.setColor(Color.rgb(249, 107, 95));
                canvas.drawCircle(center, height / 2f, radius, paint);
                paint.setColor(Color.rgb(27, 16, 27));
                canvas.drawCircle(center - radius * .33f, height / 2f - radius * .1f, radius * .12f, paint);
                canvas.drawCircle(center + radius * .33f, height / 2f - radius * .1f, radius * .12f, paint);
            }

            paint.setColor(flash > 0 ? Color.rgb(255, 220, 133) : Color.WHITE);
            paint.setStrokeWidth(3);
            canvas.drawLine(width / 2f - 9, height / 2f, width / 2f + 9, height / 2f, paint);
            canvas.drawLine(width / 2f, height / 2f - 9, width / 2f, height / 2f + 9, paint);
            paint.setColor(Color.rgb(34, 48, 61));
            canvas.drawRoundRect(new RectF(width * .35f, height * .79f,
                    width * .65f, height * 1.04f), 18, 18, paint);
            paint.setColor(Color.rgb(245, 196, 114));
            paint.setTypeface(Typeface.MONOSPACE);
            paint.setTextSize(dp(18));
            canvas.drawText("SCORE  " + score + " / " + alive.length, dp(13), dp(28), paint);
            if (score == alive.length) {
                paint.setColor(Color.rgb(142, 233, 193));
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(dp(24));
                canvas.drawText("ROOM CLEARED!", width / 2f, height * .32f, paint);
                paint.setTextAlign(Paint.Align.LEFT);
            }
        }
    }
}

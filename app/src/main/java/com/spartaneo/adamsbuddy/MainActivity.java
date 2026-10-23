package com.spartaneo.adamsbuddy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int NOTIFICATION_REQUEST = 40;
    private WebView webView;
    private BuddySpeech speech;
    private boolean pendingReminderEnable;
    private boolean pendingTestNotification;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        speech = new BuddySpeech(this);
        setTitle("Adam's Buddy");
        getWindow().setStatusBarColor(Color.rgb(8, 22, 34));
        getWindow().setNavigationBarColor(Color.rgb(8, 22, 34));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 19, 30));

        LinearLayout header = new LinearLayout(this);
        header.setPadding(dp(16), dp(10), dp(16), dp(7));
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.buddy_icon);
        header.addView(logo, new LinearLayout.LayoutParams(dp(43), dp(43)));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText("ADAM'S BUDDY");
        title.setTextSize(18);
        title.setTypeface(null, 1);
        title.setTextColor(Color.WHITE);
        titles.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Your day, your Buddy, your game.");
        subtitle.setTextSize(12);
        subtitle.setTextColor(Color.rgb(170, 210, 220));
        titles.addView(subtitle);
        header.addView(titles);
        root.addView(header);

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(7), dp(2), dp(7), dp(8));
        addButton(bar, "Today", () -> openBuddy(BuddyLinks.TODAY));
        addButton(bar, "Talk", () -> openBuddy(BuddyLinks.HOME));
        addButton(bar, "Game", () -> openBuddy(BuddyLinks.GAME));
        addButton(bar, "Earn", () -> openBuddy(BuddyLinks.EARNINGS));
        addButton(bar, "More", this::showOptions);
        root.addView(bar);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setUserAgentString(settings.getUserAgentString() + " AdamBuddyAndroid/0.13");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("buddy-voice".equalsIgnoreCase(scheme)) {
                    Uri current = Uri.parse(view.getUrl() == null ? "" : view.getUrl());
                    if (request.isForMainFrame() && "speak".equalsIgnoreCase(uri.getHost())
                            && "https".equalsIgnoreCase(current.getScheme())
                            && "adams-buddy-control.l3onidas.chatgpt.site".equalsIgnoreCase(current.getHost())) {
                        speech.speak(uri.getQueryParameter("text"));
                    }
                    return true;
                }
                // The private site's login crosses several HTTPS origins. Opening even one
                // redirect in Chrome loses WebView's sign-in cookies and invalidates the flow.
                if ("https".equalsIgnoreCase(scheme)) return false;
                if ("http".equalsIgnoreCase(scheme)) return true;
                openExternal(uri);
                return true;
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        if (state == null || webView.restoreState(state) == null) {
            String path = getIntent().getStringExtra("buddy_path");
            openBuddy("/game".equals(path) ? BuddyLinks.GAME : "/".equals(path) ? BuddyLinks.HOME : BuddyLinks.TODAY);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private void addButton(LinearLayout row, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setTextColor(Color.rgb(228, 247, 248));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(19, 56, 72));
        background.setCornerRadius(dp(12));
        background.setStroke(dp(1), Color.rgb(76, 141, 154));
        button.setBackground(background);
        button.setOnClickListener(view -> action.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(43), 1);
        params.setMargins(dp(2), 0, dp(2), 0);
        row.addView(button, params);
    }

    private void showOptions() {
        String reminder = ReminderReceiver.enabled(this) ? "Turn off daily reminder" : "Turn on daily reminder";
        new AlertDialog.Builder(this).setTitle("Buddy settings")
                .setItems(new String[]{"Daily checklist", "Connections", "Buddy voice", "Watch", "Buddy Blaster arcade",
                        "Play a video", reminder, "Test notification", "Reload page"}, (dialog, which) -> {
                    if (which == 0) openBuddy(BuddyLinks.BASE + "/daily");
                    else if (which == 1) openBuddy(BuddyLinks.BASE + "/connections");
                    else if (which == 2) speech.showPicker();
                    else if (which == 3) startActivity(new Intent(this, WatchActivity.class));
                    else if (which == 4) startActivity(new Intent(this, ArcadeActivity.class));
                    else if (which == 5) startActivity(new Intent(this, VideoActivity.class));
                    else if (which == 6) toggleReminders();
                    else if (which == 7) {
                        if (requestNotificationPermission()) pendingTestNotification = true;
                        else ReminderReceiver.notifyNow(this);
                    } else webView.reload();
                }).show();
    }

    private void openBuddy(String url) {
        webView.loadUrl(url);
    }

    private void openExternal(Uri uri) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
        catch (ActivityNotFoundException ignored) {
            Toast.makeText(this, "No app can open this link.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (webView != null) webView.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (speech != null) speech.shutdown();
        if (webView != null) webView.destroy();
        super.onDestroy();
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
        Toast.makeText(this, enable ? "Daily reminder on" : "Daily reminder off", Toast.LENGTH_SHORT).show();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == NOTIFICATION_REQUEST && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            if (pendingReminderEnable) {
                ReminderReceiver.setEnabled(this, true);
                Toast.makeText(this, "Daily reminder on", Toast.LENGTH_SHORT).show();
            }
            if (pendingTestNotification) ReminderReceiver.notifyNow(this);
        }
        pendingReminderEnable = false;
        pendingTestNotification = false;
    }
}

package com.spartaneo.adamsbuddy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.widget.LinearLayout;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private static final int NOTIFICATION_REQUEST = 40;
    private static final String BUDDY_HOST = "adams-buddy-control.l3onidas.chatgpt.site";
    private WebView webView;
    private boolean pendingReminderEnable;
    private boolean pendingTestNotification;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Adam's Buddy");
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(7, 19, 30));

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(4), dp(2), dp(4), dp(2));
        addButton(bar, "Buddy", () -> openBuddy(BuddyLinks.HOME));
        addButton(bar, "Game", () -> openBuddy(BuddyLinks.GAME));
        addButton(bar, "Daily", () -> openBuddy(BuddyLinks.BASE + "/daily"));
        addButton(bar, "⋮", this::showOptions);
        root.addView(bar);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("https".equalsIgnoreCase(scheme)) {
                    if (BUDDY_HOST.equalsIgnoreCase(uri.getHost()) || isSignInHost(uri.getHost())) return false;
                    openExternal(uri);
                    return true;
                }
                if ("http".equalsIgnoreCase(scheme)) return true;
                openExternal(uri);
                return true;
            }
        });
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        if (state == null || webView.restoreState(state) == null) {
            String path = getIntent().getStringExtra("buddy_path");
            openBuddy("/game".equals(path) ? BuddyLinks.GAME : BuddyLinks.HOME);
        }
    }

    private static boolean isSignInHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(java.util.Locale.ROOT);
        return h.equals("chatgpt.com") || h.endsWith(".chatgpt.com")
                || h.equals("openai.com") || h.endsWith(".openai.com")
                || h.equals("auth0.com") || h.endsWith(".auth0.com");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private void addButton(LinearLayout row, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(12);
        button.setOnClickListener(view -> action.run());
        row.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
    }

    private void showOptions() {
        String reminder = ReminderReceiver.enabled(this) ? "Turn off daily reminder" : "Turn on daily reminder";
        new AlertDialog.Builder(this).setTitle("Buddy settings")
                .setItems(new String[]{reminder, "Test notification", "Reload page"}, (dialog, which) -> {
                    if (which == 0) toggleReminders();
                    else if (which == 1) {
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

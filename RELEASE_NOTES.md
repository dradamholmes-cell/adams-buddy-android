Adam's Buddy Android in-app test

Install the APK on an Android phone, then tap the Buddy icon. Buddy and the entire HTTPS sign-in redirect chain stay inside the app. This repairs the previous version's session split, which sent part of sign-in to Chrome and caused an authentication error. Use the top right menu to enable or test the optional 9 PM daily check-in notification.

This is a test APK. Private sign-in needs a phone test because Android WebView does not share Chrome's sign-in session. Some sign-in providers refuse embedded login. The app does not yet send live PC or task alerts, and it needs the internet for Buddy content. It uses a temporary debug signing key, so the previous test app may need uninstalling before this one installs. No API keys or PC bridge credentials are packaged in the app.

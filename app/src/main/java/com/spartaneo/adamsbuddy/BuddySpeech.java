package com.spartaneo.adamsbuddy;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class BuddySpeech {
    private final Activity activity;
    private final SharedPreferences preferences;
    private TextToSpeech engine;
    private boolean ready;

    BuddySpeech(Activity activity) {
        this.activity = activity;
        preferences = activity.getSharedPreferences("buddy_voice", Activity.MODE_PRIVATE);
        engine = new TextToSpeech(activity, status -> activity.runOnUiThread(() -> {
            ready = status == TextToSpeech.SUCCESS;
            if (ready) configure();
        }));
    }

    private List<Voice> installedEnglishVoices() {
        List<Voice> voices = new ArrayList<>();
        if (!ready || engine == null || engine.getVoices() == null) return voices;
        for (Voice voice : engine.getVoices()) {
            if ("en".equals(voice.getLocale().getLanguage()) && !voice.isNetworkConnectionRequired()) {
                voices.add(voice);
            }
        }
        voices.sort(Comparator.comparing(Voice::getName));
        return voices;
    }

    private void configure() {
        if (!ready || engine == null) return;
        engine.setSpeechRate(0.88f);
        engine.setPitch(0.78f);
        String selected = preferences.getString("name", "");
        for (Voice voice : installedEnglishVoices()) {
            if (voice.getName().equals(selected)) {
                engine.setVoice(voice);
                return;
            }
        }
        engine.setLanguage(Locale.US);
    }

    void speak(String words) {
        if (!ready || engine == null) {
            Toast.makeText(activity, "Buddy's voice is loading. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (words == null || words.trim().isEmpty()) return;
        engine.speak(words.substring(0, Math.min(words.length(), 1000)), TextToSpeech.QUEUE_FLUSH, null, "buddy_reply");
    }

    void showPicker() {
        if (!ready) {
            Toast.makeText(activity, "Buddy's voice is loading. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Voice> voices = installedEnglishVoices();
        String[] labels = new String[voices.size() + 1];
        labels[0] = "Phone default";
        for (int i = 0; i < voices.size(); i++) labels[i + 1] = voices.get(i).getName();
        new AlertDialog.Builder(activity)
                .setTitle("Buddy voice")
                .setMessage("Tap a voice to hear it. These are the free English voices installed on your phone.")
                .setItems(labels, (dialog, which) -> {
                    preferences.edit().putString("name", which == 0 ? "" : voices.get(which - 1).getName()).apply();
                    configure();
                    speak("Hey Adam. I'm Buddy. Let's make something and get things moving.");
                })
                .setNegativeButton("Close", null)
                .show();
    }

    void shutdown() {
        if (engine != null) {
            engine.stop();
            engine.shutdown();
            engine = null;
        }
    }
}

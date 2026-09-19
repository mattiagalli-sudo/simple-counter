package com.example.simplecounter;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;

final class PinStore {
    static final String DEFAULT_PIN = "0000";

    private static final String LEGACY_DEFAULT_PIN = "1234";
    private static final String PREFS = "simple_counter_prefs";
    private static final String KEY_PIN_SALT = "pin_salt";
    private static final String KEY_PIN_HASH = "pin_hash";
    private static final String KEY_DEFAULT_PIN_MIGRATED = "default_pin_migrated";

    private PinStore() {
    }

    static void ensurePinExists(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.contains(KEY_PIN_SALT) || !prefs.contains(KEY_PIN_HASH)) {
            setPin(context, DEFAULT_PIN);
        } else if (!prefs.getBoolean(KEY_DEFAULT_PIN_MIGRATED, false)) {
            if (pinMatches(prefs, LEGACY_DEFAULT_PIN)) {
                setPin(context, DEFAULT_PIN);
            } else {
                prefs.edit().putBoolean(KEY_DEFAULT_PIN_MIGRATED, true).apply();
            }
        }
    }

    static boolean verifyPin(Context context, String pin) {
        ensurePinExists(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String salt = prefs.getString(KEY_PIN_SALT, "");
        String expectedHash = prefs.getString(KEY_PIN_HASH, "");
        return expectedHash.equals(hashPin(pin, salt));
    }

    static void setPin(Context context, String pin) {
        String salt = newSalt();
        String hash = hashPin(pin, salt);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_PIN_SALT, salt)
                .putString(KEY_PIN_HASH, hash)
                .putBoolean(KEY_DEFAULT_PIN_MIGRATED, true)
                .apply();
    }

    private static boolean pinMatches(SharedPreferences prefs, String pin) {
        String salt = prefs.getString(KEY_PIN_SALT, "");
        String expectedHash = prefs.getString(KEY_PIN_HASH, "");
        return expectedHash.equals(hashPin(pin, salt));
    }

    private static String newSalt() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static String hashPin(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] data = (salt + ":" + pin).getBytes("UTF-8");
            return Base64.encodeToString(digest.digest(data), Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash PIN", e);
        }
    }
}

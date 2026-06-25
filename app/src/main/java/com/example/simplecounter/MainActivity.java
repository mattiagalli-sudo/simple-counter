package com.example.simplecounter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int MULTIPLIER = 7;
    private static final String PREFS = "simple_counter_prefs";
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_DATE_FORMAT = "date_format";
    private static final String KEY_UPDATE_INTERVAL_MS = "update_interval_ms";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_FONT_STYLE = "font_style";
    private static final String KEY_COUNTER_FONT_SIZE = "counter_font_size";
    private static final long INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L;

    private SharedPreferences prefs;
    private Handler handler;
    private View mainRoot;
    private TextView dateTimeText;
    private TextView counterText;
    private TextView derivedText;
    private LinearLayout unlockedControls;
    private int counter;
    private boolean unlocked;
    private boolean passcodeShowing;
    private long updateIntervalMs;

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateTime();
            handler.postDelayed(this, updateIntervalMs);
        }
    };

    private final Runnable inactivityRunnable = new Runnable() {
        @Override
        public void run() {
            if (unlocked) {
                setUnlocked(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        handler = new Handler();
        PinStore.ensurePinExists(this);

        mainRoot = findViewById(R.id.mainRoot);
        dateTimeText = (TextView) findViewById(R.id.dateTimeText);
        counterText = (TextView) findViewById(R.id.counterText);
        derivedText = (TextView) findViewById(R.id.derivedText);
        unlockedControls = (LinearLayout) findViewById(R.id.unlockedControls);
        Button decrementButton = (Button) findViewById(R.id.decrementButton);
        Button incrementButton = (Button) findViewById(R.id.incrementButton);
        Button lockButton = (Button) findViewById(R.id.lockButton);
        Button settingsButton = (Button) findViewById(R.id.settingsButton);

        decrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCounter(-1);
            }
        });
        incrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeCounter(1);
            }
        });
        lockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setUnlocked(false);
            }
        });
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetInactivityTimer();
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        counterText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (unlocked) {
                    showManualCounterDialog();
                }
            }
        });
        mainRoot.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (unlocked) {
                    resetInactivityTimer();
                }
                if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN
                        && event.getPointerCount() >= 3) {
                    showPasscodeDialog();
                    return true;
                }
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadState();
        applyPreferences();
        updateCounterViews();
        updateDateTime();
        handler.removeCallbacks(clockRunnable);
        handler.post(clockRunnable);
        if (unlocked) {
            resetInactivityTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(inactivityRunnable);
    }

    private void loadState() {
        counter = prefs.getInt(KEY_COUNTER, 0);
        updateIntervalMs = prefs.getLong(KEY_UPDATE_INTERVAL_MS, 1000L);
        setUnlocked(unlocked);
    }

    private void setUnlocked(boolean value) {
        unlocked = value;
        unlockedControls.setVisibility(unlocked ? View.VISIBLE : View.GONE);
        counterText.setClickable(unlocked);
        if (unlocked) {
            resetInactivityTimer();
        } else {
            handler.removeCallbacks(inactivityRunnable);
        }
    }

    private void changeCounter(int delta) {
        counter += delta;
        prefs.edit().putInt(KEY_COUNTER, counter).apply();
        updateCounterViews();
        resetInactivityTimer();
    }

    private void updateCounterViews() {
        counterText.setText(String.valueOf(counter));
        derivedText.setText(String.valueOf(counter * MULTIPLIER));
    }

    private void updateDateTime() {
        String pattern = prefs.getString(KEY_DATE_FORMAT, "yyyy-MM-dd HH:mm:ss");
        dateTimeText.setText(new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date()));
    }

    private void applyPreferences() {
        int textColor = prefs.getInt(KEY_TEXT_COLOR, 0xFF222222);
        int backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, 0xFFFFFFFF);
        int fontStyle = prefs.getInt(KEY_FONT_STYLE, 0);
        int counterSize = prefs.getInt(KEY_COUNTER_FONT_SIZE, 72);
        Typeface typeface = typefaceForStyle(fontStyle);

        mainRoot.setBackgroundColor(backgroundColor);
        dateTimeText.setTextColor(textColor);
        counterText.setTextColor(textColor);
        derivedText.setTextColor(textColor);
        dateTimeText.setTypeface(typeface);
        counterText.setTypeface(typeface);
        derivedText.setTypeface(typeface);
        counterText.setTextSize(counterSize);
        derivedText.setTextSize(Math.max(18, counterSize * 0.4f));
    }

    private Typeface typefaceForStyle(int style) {
        if (style == 1) {
            return Typeface.DEFAULT_BOLD;
        } else if (style == 2) {
            return Typeface.MONOSPACE;
        } else if (style == 3) {
            return Typeface.SERIF;
        }
        return Typeface.DEFAULT;
    }

    private void showPasscodeDialog() {
        if (passcodeShowing) {
            return;
        }
        passcodeShowing = true;
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_passcode, null);
        final EditText input = (EditText) view.findViewById(R.id.passcodeInput);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Unlock", null)
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialogInterface) {
                final AlertDialog shownDialog = (AlertDialog) dialogInterface;
                shownDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (PinStore.verifyPin(MainActivity.this, input.getText().toString())) {
                            setUnlocked(true);
                            shownDialog.dismiss();
                        } else {
                            Toast.makeText(MainActivity.this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                passcodeShowing = false;
            }
        });
        dialog.show();
    }

    private void showManualCounterDialog() {
        resetInactivityTimer();
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_manual_counter, null);
        final EditText input = (EditText) view.findViewById(R.id.manualCounterInput);
        input.setText(String.valueOf(counter));
        input.setSelection(input.length());

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Set", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            counter = Integer.parseInt(input.getText().toString());
                            prefs.edit().putInt(KEY_COUNTER, counter).apply();
                            updateCounterViews();
                            resetInactivityTimer();
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "Enter a valid number", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetInactivityTimer() {
        handler.removeCallbacks(inactivityRunnable);
        handler.postDelayed(inactivityRunnable, INACTIVITY_TIMEOUT_MS);
    }
}

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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int DEFAULT_MULTIPLIER = 7;
    private static final String PREFS = "simple_counter_prefs";
    private static final String KEY_COUNTER = "counter";
    private static final String KEY_DATE_FORMAT = "date_format";
    private static final String KEY_UPDATE_INTERVAL_MS = "update_interval_ms";
    private static final String KEY_TEXT_COLOR = "text_color";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_FONT_STYLE = "font_style";
    private static final String KEY_COUNTER_FONT_SIZE = "counter_font_size";
    private static final String KEY_CLOCK_FONT_SIZE = "clock_font_size";
    private static final String KEY_COUNTER_PREFIX = "counter_prefix";
    private static final String KEY_COUNTER_LABEL = "counter_label";
    private static final String KEY_DERIVED_PREFIX = "derived_prefix";
    private static final String KEY_DERIVED_LABEL = "derived_label";
    private static final String KEY_MULTIPLIER = "multiplier";
    private static final String KEY_OPERATIONS = "operations";
    private static final String KEY_HISTORY = "counter_history";
    private static final String KEY_COUNTER_DIRTY = "counter_dirty_since_history";
    private static final String KEY_LAST_HISTORY_HOUR = "last_history_hour";
    private static final long INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L;
    private static final long HOUR_MS = 60 * 60 * 1000L;

    private SharedPreferences prefs;
    private Handler handler;
    private View mainRoot;
    private TextView dateTimeText;
    private TextView counterText;
    private LinearLayout derivedValuesContainer;
    private TextView historyTitle;
    private TextView historyList;
    private HistoryChartView historyChart;
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

    private final Runnable historyRunnable = new Runnable() {
        @Override
        public void run() {
            recordHourlyHistoryIfNeeded();
            scheduleNextHistoryCheck();
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
        derivedValuesContainer = (LinearLayout) findViewById(R.id.derivedValuesContainer);
        historyTitle = (TextView) findViewById(R.id.historyTitle);
        historyList = (TextView) findViewById(R.id.historyList);
        historyChart = (HistoryChartView) findViewById(R.id.historyChart);
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
        updateHistoryViews();
        updateDateTime();
        recordHourlyHistoryIfNeeded();
        handler.removeCallbacks(clockRunnable);
        handler.post(clockRunnable);
        handler.removeCallbacks(historyRunnable);
        scheduleNextHistoryCheck();
        if (unlocked) {
            resetInactivityTimer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(clockRunnable);
        handler.removeCallbacks(historyRunnable);
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
        prefs.edit()
                .putInt(KEY_COUNTER, counter)
                .putBoolean(KEY_COUNTER_DIRTY, true)
                .apply();
        updateCounterViews();
        updateHistoryViews();
        resetInactivityTimer();
    }

    private void updateCounterViews() {
        counterText.setText(formatValue(
                counter,
                prefs.getString(KEY_COUNTER_PREFIX, ""),
                prefs.getString(KEY_COUNTER_LABEL, "")));
        derivedValuesContainer.removeAllViews();
        for (Operation operation : loadOperations()) {
            TextView textView = new TextView(this);
            textView.setGravity(Gravity.CENTER);
            textView.setTextColor(prefs.getInt(KEY_TEXT_COLOR, 0xFF222222));
            textView.setTypeface(typefaceForStyle(prefs.getInt(KEY_FONT_STYLE, 0)));
            textView.setTextSize(Math.max(18, prefs.getInt(KEY_COUNTER_FONT_SIZE, 72) * 0.4f));
            String value = evaluateOperation(operation);
            String label = operation.label.length() == 0 ? "" : operation.label + ": ";
            textView.setText(label + formatValue(
                    value,
                    prefs.getString(KEY_DERIVED_PREFIX, ""),
                    prefs.getString(KEY_DERIVED_LABEL, "")));
            derivedValuesContainer.addView(textView);
        }
    }

    private String formatValue(int value, String prefix, String suffix) {
        return formatValue(String.valueOf(value), prefix, suffix);
    }

    private String formatValue(String value, String prefix, String suffix) {
        String cleanPrefix = prefix == null ? "" : prefix.trim();
        String cleanSuffix = suffix == null ? "" : suffix.trim();
        String result = value;
        if (cleanPrefix.length() > 0) {
            result = cleanPrefix + " " + result;
        }
        if (cleanSuffix.length() > 0) {
            result = result + " " + cleanSuffix;
        }
        return result;
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
        int clockSize = prefs.getInt(KEY_CLOCK_FONT_SIZE, 16);
        Typeface typeface = typefaceForStyle(fontStyle);

        mainRoot.setBackgroundColor(backgroundColor);
        dateTimeText.setTextColor(textColor);
        counterText.setTextColor(textColor);
        historyTitle.setTextColor(textColor);
        historyList.setTextColor(textColor);
        dateTimeText.setTypeface(typeface);
        counterText.setTypeface(typeface);
        historyTitle.setTypeface(typeface);
        historyList.setTypeface(typeface);
        dateTimeText.setTextSize(clockSize);
        counterText.setTextSize(counterSize);
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
                            prefs.edit()
                                    .putInt(KEY_COUNTER, counter)
                                    .putBoolean(KEY_COUNTER_DIRTY, true)
                                    .apply();
                            updateCounterViews();
                            updateHistoryViews();
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

    private List<Operation> loadOperations() {
        String stored = prefs.getString(KEY_OPERATIONS, "");
        if (stored == null || stored.trim().length() == 0) {
            int multiplier = prefs.getInt(KEY_MULTIPLIER, DEFAULT_MULTIPLIER);
            stored = "Multiplier\tmultiply\t" + multiplier;
        }
        List<Operation> operations = new ArrayList<Operation>();
        String[] lines = stored.split("\\n");
        for (String line : lines) {
            String[] parts = line.split("\\t", -1);
            if (parts.length == 3) {
                try {
                    operations.add(new Operation(parts[0], parts[1], Double.parseDouble(parts[2])));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return operations;
    }

    private String evaluateOperation(Operation operation) {
        double result;
        if ("divide".equals(operation.type)) {
            if (operation.operand == 0d) {
                return "Undefined";
            }
            result = counter / operation.operand;
        } else if ("add".equals(operation.type)) {
            result = counter + operation.operand;
        } else if ("subtract".equals(operation.type)) {
            result = counter - operation.operand;
        } else if ("power".equals(operation.type)) {
            result = Math.pow(counter, operation.operand);
        } else if ("root".equals(operation.type)) {
            if (operation.operand == 0d) {
                return "Undefined";
            }
            result = Math.pow(counter, 1d / operation.operand);
        } else {
            result = counter * operation.operand;
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            return "Undefined";
        }
        if (Math.abs(result - Math.rint(result)) < 0.00001d) {
            return String.valueOf((long) Math.rint(result));
        }
        return String.format(Locale.getDefault(), "%.2f", result);
    }

    private void recordHourlyHistoryIfNeeded() {
        long currentHour = System.currentTimeMillis() / HOUR_MS;
        long lastHistoryHour = prefs.getLong(KEY_LAST_HISTORY_HOUR, currentHour);
        if (lastHistoryHour == currentHour) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit().putLong(KEY_LAST_HISTORY_HOUR, currentHour);
        if (prefs.getBoolean(KEY_COUNTER_DIRTY, false)) {
            String entry = currentHour + "\t" + counter;
            String history = prefs.getString(KEY_HISTORY, "");
            editor.putString(KEY_HISTORY, history == null || history.length() == 0 ? entry : history + "\n" + entry);
            editor.putBoolean(KEY_COUNTER_DIRTY, false);
        }
        editor.apply();
        updateHistoryViews();
    }

    private void scheduleNextHistoryCheck() {
        long now = System.currentTimeMillis();
        long delay = HOUR_MS - (now % HOUR_MS);
        handler.postDelayed(historyRunnable, delay);
    }

    private void updateHistoryViews() {
        String history = prefs.getString(KEY_HISTORY, "");
        if (history == null || history.trim().length() == 0) {
            historyTitle.setVisibility(View.GONE);
            historyList.setVisibility(View.GONE);
            historyChart.setVisibility(View.GONE);
            return;
        }
        historyTitle.setVisibility(View.VISIBLE);
        historyList.setVisibility(View.VISIBLE);
        historyChart.setVisibility(View.VISIBLE);

        List<Integer> values = new ArrayList<Integer>();
        StringBuilder list = new StringBuilder();
        String[] lines = history.split("\\n");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault());
        for (int i = lines.length - 1; i >= 0; i--) {
            String[] parts = lines[i].split("\\t");
            if (parts.length != 2) {
                continue;
            }
            try {
                long hour = Long.parseLong(parts[0]);
                int value = Integer.parseInt(parts[1]);
                values.add(0, value);
                list.append(format.format(new Date(hour * HOUR_MS))).append("  ").append(value).append("\n");
            } catch (NumberFormatException ignored) {
            }
        }
        historyList.setText(list.toString().trim());
        historyChart.setValues(values);
    }

    private static class Operation {
        final String label;
        final String type;
        final double operand;

        Operation(String label, String type, double operand) {
            this.label = label == null ? "" : label;
            this.type = type == null ? "multiply" : type;
            this.operand = operand;
        }
    }
}

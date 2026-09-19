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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Collections;
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
    private static final String KEY_CLOCK_POSITION = "clock_position";
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
    private static final int MAX_HISTORY_ENTRIES = 720;

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
    private Button historyToggleButton;
    private int counter;
    private boolean unlocked;
    private boolean passcodeShowing;
    private boolean historyVisible;
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
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (unlocked) {
            resetInactivityTimer();
        }
        if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN
                && event.getPointerCount() >= 3) {
            showPasscodeDialog();
            return true;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FullscreenWindow.apply(this);
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
        historyToggleButton = (Button) findViewById(R.id.historyToggleButton);
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
        historyToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!historyVisible && !hasHistory()) {
                    Toast.makeText(MainActivity.this, "No counter history yet", Toast.LENGTH_SHORT).show();
                    return;
                }
                historyVisible = !historyVisible;
                updateHistoryViews();
                updateHistoryToggleButton();
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
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            FullscreenWindow.apply(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        FullscreenWindow.apply(this);
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
        updateIntervalMs = validInterval(prefs.getLong(KEY_UPDATE_INTERVAL_MS, 1000L));
        setUnlocked(unlocked);
    }

    private long validInterval(long value) {
        return value == 60000L ? 60000L : 1000L;
    }

    private void setUnlocked(boolean value) {
        unlocked = value;
        unlockedControls.setVisibility(unlocked ? View.VISIBLE : View.GONE);
        historyToggleButton.setVisibility(unlocked ? View.GONE : View.VISIBLE);
        counterText.setClickable(unlocked);
        if (unlocked) {
            resetInactivityTimer();
        } else {
            handler.removeCallbacks(inactivityRunnable);
        }
        updateHistoryViews();
        updateHistoryToggleButton();
    }

    private void changeCounter(int delta) {
        if (delta > 0 && counter == Integer.MAX_VALUE) {
            Toast.makeText(this, "Counter is already at the maximum value", Toast.LENGTH_SHORT).show();
            return;
        }
        if (delta < 0 && counter == Integer.MIN_VALUE) {
            Toast.makeText(this, "Counter is already at the minimum value", Toast.LENGTH_SHORT).show();
            return;
        }
        counter += delta;
        saveCounterChange();
        updateCounterViews();
        updateHistoryViews();
        resetInactivityTimer();
    }

    private void saveCounterChange() {
        SharedPreferences.Editor editor = prefs.edit()
                .putInt(KEY_COUNTER, counter)
                .putBoolean(KEY_COUNTER_DIRTY, true);
        if (!prefs.contains(KEY_LAST_HISTORY_HOUR)) {
            editor.putLong(KEY_LAST_HISTORY_HOUR, System.currentTimeMillis() / HOUR_MS);
        }
        editor.apply();
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
            textView.setTypeface(typefaceForStyle(clamp(prefs.getInt(KEY_FONT_STYLE, 0), 0, 3)));
            textView.setTextSize(Math.max(18, clamp(prefs.getInt(KEY_COUNTER_FONT_SIZE, 72), 32, 160) * 0.4f));
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
        try {
            dateTimeText.setText(new SimpleDateFormat(pattern, Locale.getDefault()).format(new Date()));
        } catch (IllegalArgumentException e) {
            dateTimeText.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        }
    }

    private void applyPreferences() {
        int textColor = prefs.getInt(KEY_TEXT_COLOR, 0xFF222222);
        int backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, 0xFFFFFFFF);
        int fontStyle = clamp(prefs.getInt(KEY_FONT_STYLE, 0), 0, 3);
        int counterSize = clamp(prefs.getInt(KEY_COUNTER_FONT_SIZE, 72), 32, 160);
        int clockSize = clamp(prefs.getInt(KEY_CLOCK_FONT_SIZE, 16), 10, 42);
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
        applyClockPosition();
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

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void applyClockPosition() {
        int position = clamp(prefs.getInt(KEY_CLOCK_POSITION, 0), 0, 5);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) dateTimeText.getLayoutParams();
        int horizontalGravity;
        if (position == 1 || position == 4) {
            horizontalGravity = Gravity.CENTER_HORIZONTAL;
        } else if (position == 2 || position == 5) {
            horizontalGravity = Gravity.RIGHT;
        } else {
            horizontalGravity = Gravity.LEFT;
        }
        int verticalGravity = position >= 3 ? Gravity.BOTTOM : Gravity.TOP;
        params.gravity = verticalGravity | horizontalGravity;
        int sideMargin = dp(16);
        int topMargin = verticalGravity == Gravity.TOP ? dp(16) : 0;
        int bottomMargin = verticalGravity == Gravity.BOTTOM ? dp(128) : 0;
        params.setMargins(sideMargin, topMargin, sideMargin, bottomMargin);
        dateTimeText.setLayoutParams(params);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
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
                            saveCounterChange();
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
                    double operand = Double.parseDouble(parts[2]);
                    if (Double.isNaN(operand) || Double.isInfinite(operand)) {
                        continue;
                    }
                    operations.add(new Operation(parts[0], normalizeOperationType(parts[1]), operand));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (operations.size() == 0) {
            operations.add(new Operation("Multiplier", "multiply", prefs.getInt(KEY_MULTIPLIER, DEFAULT_MULTIPLIER)));
        }
        return operations;
    }

    private String normalizeOperationType(String type) {
        if ("divide".equals(type)
                || "add".equals(type)
                || "subtract".equals(type)
                || "power".equals(type)
                || "root".equals(type)) {
            return type;
        }
        return "multiply";
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
            result = root(counter, operation.operand);
        } else {
            result = counter * operation.operand;
        }
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            return "Undefined";
        }
        if (Math.abs(result - Math.rint(result)) < 0.00001d
                && result <= Long.MAX_VALUE
                && result >= Long.MIN_VALUE) {
            return String.valueOf((long) Math.rint(result));
        }
        return String.format(Locale.getDefault(), "%.2f", result);
    }

    private double root(int value, double degree) {
        double roundedDegree = Math.rint(degree);
        if (value < 0 && Math.abs(degree - roundedDegree) < 0.00001d
                && ((long) roundedDegree) % 2 != 0) {
            return -Math.pow(Math.abs((double) value), 1d / roundedDegree);
        }
        return Math.pow(value, 1d / degree);
    }

    private void recordHourlyHistoryIfNeeded() {
        long currentHour = System.currentTimeMillis() / HOUR_MS;
        if (!prefs.contains(KEY_LAST_HISTORY_HOUR)) {
            prefs.edit().putLong(KEY_LAST_HISTORY_HOUR, currentHour).apply();
            return;
        }
        long lastHistoryHour = prefs.getLong(KEY_LAST_HISTORY_HOUR, currentHour);
        if (lastHistoryHour == currentHour) {
            return;
        }
        SharedPreferences.Editor editor = prefs.edit().putLong(KEY_LAST_HISTORY_HOUR, currentHour);
        if (prefs.getBoolean(KEY_COUNTER_DIRTY, false)) {
            String entry = currentHour + "\t" + counter;
            String history = prefs.getString(KEY_HISTORY, "");
            editor.putString(KEY_HISTORY, appendHistoryEntry(history, entry));
            editor.putBoolean(KEY_COUNTER_DIRTY, false);
        }
        editor.apply();
        updateHistoryViews();
    }

    private String appendHistoryEntry(String history, String entry) {
        String combined = history == null || history.length() == 0 ? entry : history + "\n" + entry;
        String[] lines = combined.split("\\n");
        if (lines.length <= MAX_HISTORY_ENTRIES) {
            return combined;
        }
        StringBuilder trimmed = new StringBuilder();
        int start = lines.length - MAX_HISTORY_ENTRIES;
        for (int i = start; i < lines.length; i++) {
            if (trimmed.length() > 0) {
                trimmed.append('\n');
            }
            trimmed.append(lines[i]);
        }
        return trimmed.toString();
    }

    private void scheduleNextHistoryCheck() {
        long now = System.currentTimeMillis();
        long delay = HOUR_MS - (now % HOUR_MS);
        handler.postDelayed(historyRunnable, delay);
    }

    private void updateHistoryViews() {
        String history = prefs.getString(KEY_HISTORY, "");
        if (!shouldShowHistory() || history == null || history.trim().length() == 0) {
            historyTitle.setVisibility(View.GONE);
            historyList.setVisibility(View.GONE);
            historyChart.setVisibility(View.GONE);
            return;
        }
        historyTitle.setVisibility(View.VISIBLE);
        historyList.setVisibility(View.VISIBLE);
        historyChart.setVisibility(View.VISIBLE);

        List<Integer> values = new ArrayList<Integer>();
        List<String> displayRows = new ArrayList<String>();
        String[] lines = history.split("\\n");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:00", Locale.getDefault());
        for (String line : lines) {
            String[] parts = line.split("\\t");
            if (parts.length != 2) {
                continue;
            }
            try {
                long hour = Long.parseLong(parts[0]);
                int value = Integer.parseInt(parts[1]);
                values.add(value);
                displayRows.add(format.format(new Date(hour * HOUR_MS)) + "  " + value);
            } catch (NumberFormatException ignored) {
            }
        }
        if (values.size() == 0) {
            historyTitle.setVisibility(View.GONE);
            historyList.setVisibility(View.GONE);
            historyChart.setVisibility(View.GONE);
            return;
        }
        Collections.reverse(displayRows);
        StringBuilder list = new StringBuilder();
        for (String row : displayRows) {
            if (list.length() > 0) {
                list.append('\n');
            }
            list.append(row);
        }
        historyList.setText(list.toString());
        historyChart.setValues(values);
    }

    private boolean shouldShowHistory() {
        return unlocked || historyVisible;
    }

    private void updateHistoryToggleButton() {
        if (!hasHistory()) {
            historyVisible = false;
        }
        historyToggleButton.setText(historyVisible ? "Hide chart" : "Show chart");
    }

    private boolean hasHistory() {
        String history = prefs.getString(KEY_HISTORY, "");
        return history != null && history.trim().length() > 0;
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

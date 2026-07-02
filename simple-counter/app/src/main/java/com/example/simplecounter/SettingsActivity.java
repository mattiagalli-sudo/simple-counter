package com.example.simplecounter;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private static final String PREFS = "simple_counter_prefs";
    private static final int DEFAULT_MULTIPLIER = 7;
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

    private static final String[] DATE_LABELS = {
            "2026-06-25 14:30:05",
            "25 Jun 2026, 14:30",
            "Thu, Jun 25 2026",
            "Thursday, June 25 2026",
            "June 25, 2026",
            "25/06/2026 14:30",
            "06/25/2026 2:30 PM",
            "14:30:05",
            "2:30 PM"
    };
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "dd MMM yyyy, HH:mm",
            "EEE, MMM dd yyyy",
            "EEEE, MMMM dd yyyy",
            "MMMM dd, yyyy",
            "dd/MM/yyyy HH:mm",
            "MM/dd/yyyy h:mm a",
            "HH:mm:ss",
            "h:mm a"
    };
    private static final String[] INTERVAL_LABELS = {"Every second", "Every minute"};
    private static final long[] INTERVAL_VALUES = {1000L, 60000L};
    private static final String[] FONT_LABELS = {"Default", "Bold", "Monospace", "Serif"};
    private static final String[] OPERATION_LABELS = {"Multiply", "Divide", "Add", "Subtract", "Power", "Root"};
    private static final String[] OPERATION_VALUES = {"multiply", "divide", "add", "subtract", "power", "root"};

    private SharedPreferences prefs;
    private TextView fontSizeLabel;
    private TextView clockSizeLabel;
    private TextView operationsListText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        PinStore.ensurePinExists(this);

        final EditText currentPinInput = (EditText) findViewById(R.id.currentPinInput);
        final EditText newPinInput = (EditText) findViewById(R.id.newPinInput);
        final EditText confirmPinInput = (EditText) findViewById(R.id.confirmPinInput);
        final EditText counterPrefixInput = (EditText) findViewById(R.id.counterPrefixInput);
        final EditText counterLabelInput = (EditText) findViewById(R.id.counterLabelInput);
        final EditText derivedPrefixInput = (EditText) findViewById(R.id.derivedPrefixInput);
        final EditText derivedLabelInput = (EditText) findViewById(R.id.derivedLabelInput);
        final EditText operationLabelInput = (EditText) findViewById(R.id.operationLabelInput);
        final EditText multiplierInput = (EditText) findViewById(R.id.multiplierInput);
        Button savePinButton = (Button) findViewById(R.id.savePinButton);
        Button saveDisplayTextButton = (Button) findViewById(R.id.saveDisplayTextButton);
        Button saveMultiplierButton = (Button) findViewById(R.id.saveMultiplierButton);
        Button clearOperationsButton = (Button) findViewById(R.id.clearOperationsButton);
        Button doneButton = (Button) findViewById(R.id.doneButton);
        Spinner dateFormatSpinner = (Spinner) findViewById(R.id.dateFormatSpinner);
        Spinner updateIntervalSpinner = (Spinner) findViewById(R.id.updateIntervalSpinner);
        Spinner fontStyleSpinner = (Spinner) findViewById(R.id.fontStyleSpinner);
        final Spinner operationTypeSpinner = (Spinner) findViewById(R.id.operationTypeSpinner);
        SeekBar fontSizeSeekBar = (SeekBar) findViewById(R.id.fontSizeSeekBar);
        SeekBar clockSizeSeekBar = (SeekBar) findViewById(R.id.clockSizeSeekBar);
        fontSizeLabel = (TextView) findViewById(R.id.fontSizeLabel);
        clockSizeLabel = (TextView) findViewById(R.id.clockSizeLabel);
        operationsListText = (TextView) findViewById(R.id.operationsListText);

        savePinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePin(currentPinInput, newPinInput, confirmPinInput);
            }
        });
        counterPrefixInput.setText(prefs.getString(KEY_COUNTER_PREFIX, ""));
        counterLabelInput.setText(prefs.getString(KEY_COUNTER_LABEL, ""));
        derivedPrefixInput.setText(prefs.getString(KEY_DERIVED_PREFIX, ""));
        derivedLabelInput.setText(prefs.getString(KEY_DERIVED_LABEL, ""));
        multiplierInput.setText(String.valueOf(prefs.getInt(KEY_MULTIPLIER, DEFAULT_MULTIPLIER)));
        saveDisplayTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDisplayText(counterPrefixInput, counterLabelInput, derivedPrefixInput, derivedLabelInput);
            }
        });
        saveMultiplierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addOperation(operationLabelInput, operationTypeSpinner, multiplierInput);
            }
        });
        clearOperationsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putString(KEY_OPERATIONS, "").apply();
                updateOperationsList();
                toast("Operations cleared");
            }
        });
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        setupDateFormatSpinner(dateFormatSpinner);
        setupIntervalSpinner(updateIntervalSpinner);
        setupFontSpinner(fontStyleSpinner);
        setupOperationSpinner(operationTypeSpinner);
        setupFontSizeSeekBar(fontSizeSeekBar);
        setupClockSizeSeekBar(clockSizeSeekBar);
        setupColorPicker(findViewById(R.id.textColorPicker), KEY_TEXT_COLOR, 0xFF222222);
        setupColorPicker(findViewById(R.id.backgroundColorPicker), KEY_BACKGROUND_COLOR, 0xFFFFFFFF);
        updateOperationsList();
    }

    private void saveDisplayText(EditText counterPrefixInput, EditText counterLabelInput,
                                 EditText derivedPrefixInput, EditText derivedLabelInput) {
        prefs.edit()
                .putString(KEY_COUNTER_PREFIX, counterPrefixInput.getText().toString().trim())
                .putString(KEY_COUNTER_LABEL, counterLabelInput.getText().toString().trim())
                .putString(KEY_DERIVED_PREFIX, derivedPrefixInput.getText().toString().trim())
                .putString(KEY_DERIVED_LABEL, derivedLabelInput.getText().toString().trim())
                .apply();
        toast("Display text saved");
    }

    private void addOperation(EditText operationLabelInput, Spinner operationTypeSpinner, EditText multiplierInput) {
        try {
            double operand = Double.parseDouble(multiplierInput.getText().toString().trim());
            String label = sanitizeOperationPart(operationLabelInput.getText().toString().trim());
            String type = OPERATION_VALUES[operationTypeSpinner.getSelectedItemPosition()];
            String operation = label + "\t" + type + "\t" + operand;
            String existing = editableOperations();
            prefs.edit()
                    .putString(KEY_OPERATIONS, existing == null || existing.length() == 0 ? operation : existing + "\n" + operation)
                    .apply();
            operationLabelInput.setText("");
            updateOperationsList();
            toast("Operation added");
        } catch (NumberFormatException e) {
            toast("Enter a valid operation number");
        }
    }

    private void savePin(EditText currentPinInput, EditText newPinInput, EditText confirmPinInput) {
        String currentPin = currentPinInput.getText().toString();
        String newPin = newPinInput.getText().toString();
        String confirmation = confirmPinInput.getText().toString();

        if (!PinStore.verifyPin(this, currentPin)) {
            toast("Current PIN is incorrect");
            return;
        }
        if (newPin.length() < 4) {
            toast("New PIN must be at least 4 digits");
            return;
        }
        if (!newPin.equals(confirmation)) {
            toast("New PIN values do not match");
            return;
        }

        PinStore.setPin(this, newPin);
        currentPinInput.setText("");
        newPinInput.setText("");
        confirmPinInput.setText("");
        toast("PIN saved");
    }

    private void setupDateFormatSpinner(Spinner spinner) {
        spinner.setAdapter(simpleAdapter(DATE_LABELS));
        String currentPattern = prefs.getString(KEY_DATE_FORMAT, DATE_PATTERNS[0]);
        spinner.setSelection(indexOf(DATE_PATTERNS, currentPattern));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(KEY_DATE_FORMAT, DATE_PATTERNS[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupIntervalSpinner(Spinner spinner) {
        spinner.setAdapter(simpleAdapter(INTERVAL_LABELS));
        long current = prefs.getLong(KEY_UPDATE_INTERVAL_MS, INTERVAL_VALUES[0]);
        spinner.setSelection(indexOf(INTERVAL_VALUES, current));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putLong(KEY_UPDATE_INTERVAL_MS, INTERVAL_VALUES[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupFontSpinner(Spinner spinner) {
        spinner.setAdapter(simpleAdapter(FONT_LABELS));
        spinner.setSelection(prefs.getInt(KEY_FONT_STYLE, 0));
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(KEY_FONT_STYLE, position).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupOperationSpinner(Spinner spinner) {
        spinner.setAdapter(simpleAdapter(OPERATION_LABELS));
    }

    private void setupFontSizeSeekBar(SeekBar seekBar) {
        int size = prefs.getInt(KEY_COUNTER_FONT_SIZE, 72);
        seekBar.setProgress(Math.max(0, Math.min(128, size - 32)));
        updateFontSizeLabel(size);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newSize = 32 + progress;
                prefs.edit().putInt(KEY_COUNTER_FONT_SIZE, newSize).apply();
                updateFontSizeLabel(newSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupClockSizeSeekBar(SeekBar seekBar) {
        int size = prefs.getInt(KEY_CLOCK_FONT_SIZE, 16);
        seekBar.setProgress(Math.max(0, Math.min(32, size - 10)));
        updateClockSizeLabel(size);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newSize = 10 + progress;
                prefs.edit().putInt(KEY_CLOCK_FONT_SIZE, newSize).apply();
                updateClockSizeLabel(newSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupColorPicker(View pickerRoot, final String prefKey, int defaultColor) {
        final TextView preview = (TextView) pickerRoot.findViewById(R.id.colorPreview);
        SeekBar red = (SeekBar) pickerRoot.findViewById(R.id.redSeekBar);
        SeekBar green = (SeekBar) pickerRoot.findViewById(R.id.greenSeekBar);
        SeekBar blue = (SeekBar) pickerRoot.findViewById(R.id.blueSeekBar);
        int color = prefs.getInt(prefKey, defaultColor);

        red.setProgress(Color.red(color));
        green.setProgress(Color.green(color));
        blue.setProgress(Color.blue(color));
        updateColorPreview(preview, color);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                View root = (View) seekBar.getParent();
                int newColor = Color.rgb(
                        ((SeekBar) root.findViewById(R.id.redSeekBar)).getProgress(),
                        ((SeekBar) root.findViewById(R.id.greenSeekBar)).getProgress(),
                        ((SeekBar) root.findViewById(R.id.blueSeekBar)).getProgress());
                prefs.edit().putInt(prefKey, newColor).apply();
                updateColorPreview(preview, newColor);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };

        red.setOnSeekBarChangeListener(listener);
        green.setOnSeekBarChangeListener(listener);
        blue.setOnSeekBarChangeListener(listener);
    }

    private void updateColorPreview(TextView preview, int color) {
        preview.setBackgroundColor(color);
        preview.setText(String.format("#%06X", (0xFFFFFF & color)));
        int brightness = Color.red(color) + Color.green(color) + Color.blue(color);
        preview.setTextColor(brightness > 382 ? Color.BLACK : Color.WHITE);
    }

    private void updateFontSizeLabel(int size) {
        fontSizeLabel.setText("Counter font size: " + size + "sp");
    }

    private void updateClockSizeLabel(int size) {
        clockSizeLabel.setText("Clock font size: " + size + "sp");
    }

    private void updateOperationsList() {
        String stored = editableOperations();
        if (stored == null || stored.trim().length() == 0) {
            operationsListText.setText("Default: Multiply by " + prefs.getInt(KEY_MULTIPLIER, DEFAULT_MULTIPLIER));
            return;
        }
        StringBuilder builder = new StringBuilder();
        String[] lines = stored.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split("\\t", -1);
            if (parts.length == 3) {
                builder.append(i + 1)
                        .append(". ")
                        .append(parts[0].length() == 0 ? OPERATION_LABELS[indexOf(OPERATION_VALUES, parts[1])] : parts[0])
                        .append(" - ")
                        .append(OPERATION_LABELS[indexOf(OPERATION_VALUES, parts[1])])
                        .append(" ")
                        .append(parts[2])
                        .append("\n");
            }
        }
        operationsListText.setText(builder.toString().trim());
    }

    private String editableOperations() {
        String stored = prefs.getString(KEY_OPERATIONS, "");
        if (stored == null || stored.trim().length() == 0) {
            return "Multiplier\tmultiply\t" + prefs.getInt(KEY_MULTIPLIER, DEFAULT_MULTIPLIER);
        }
        return stored;
    }

    private String sanitizeOperationPart(String value) {
        return value.replace('\t', ' ').replace('\n', ' ');
    }

    private ArrayAdapter<String> simpleAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private int indexOf(long[] values, long value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return 0;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}

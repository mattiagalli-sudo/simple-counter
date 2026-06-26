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
    private static final String KEY_COUNTER_LABEL = "counter_label";
    private static final String KEY_DERIVED_LABEL = "derived_label";
    private static final String KEY_MULTIPLIER = "multiplier";

    private static final String[] DATE_LABELS = {
            "2026-06-25 14:30:05",
            "25 Jun 2026, 14:30",
            "Thu, Jun 25 2026",
            "14:30:05"
    };
    private static final String[] DATE_PATTERNS = {
            "yyyy-MM-dd HH:mm:ss",
            "dd MMM yyyy, HH:mm",
            "EEE, MMM dd yyyy",
            "HH:mm:ss"
    };
    private static final String[] INTERVAL_LABELS = {"Every second", "Every minute"};
    private static final long[] INTERVAL_VALUES = {1000L, 60000L};
    private static final String[] FONT_LABELS = {"Default", "Bold", "Monospace", "Serif"};

    private SharedPreferences prefs;
    private TextView fontSizeLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        PinStore.ensurePinExists(this);

        final EditText currentPinInput = (EditText) findViewById(R.id.currentPinInput);
        final EditText newPinInput = (EditText) findViewById(R.id.newPinInput);
        final EditText confirmPinInput = (EditText) findViewById(R.id.confirmPinInput);
        final EditText counterLabelInput = (EditText) findViewById(R.id.counterLabelInput);
        final EditText derivedLabelInput = (EditText) findViewById(R.id.derivedLabelInput);
        final EditText multiplierInput = (EditText) findViewById(R.id.multiplierInput);
        Button savePinButton = (Button) findViewById(R.id.savePinButton);
        Button saveDisplayTextButton = (Button) findViewById(R.id.saveDisplayTextButton);
        Button saveMultiplierButton = (Button) findViewById(R.id.saveMultiplierButton);
        Button doneButton = (Button) findViewById(R.id.doneButton);
        Spinner dateFormatSpinner = (Spinner) findViewById(R.id.dateFormatSpinner);
        Spinner updateIntervalSpinner = (Spinner) findViewById(R.id.updateIntervalSpinner);
        Spinner fontStyleSpinner = (Spinner) findViewById(R.id.fontStyleSpinner);
        SeekBar fontSizeSeekBar = (SeekBar) findViewById(R.id.fontSizeSeekBar);
        fontSizeLabel = (TextView) findViewById(R.id.fontSizeLabel);

        savePinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePin(currentPinInput, newPinInput, confirmPinInput);
            }
        });
        counterLabelInput.setText(prefs.getString(KEY_COUNTER_LABEL, ""));
        derivedLabelInput.setText(prefs.getString(KEY_DERIVED_LABEL, ""));
        multiplierInput.setText(String.valueOf(prefs.getInt(KEY_MULTIPLIER, DEFAULT_MULTIPLIER)));
        saveDisplayTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDisplayText(counterLabelInput, derivedLabelInput);
            }
        });
        saveMultiplierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMultiplier(multiplierInput);
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
        setupFontSizeSeekBar(fontSizeSeekBar);
        setupColorPicker(findViewById(R.id.textColorPicker), KEY_TEXT_COLOR, 0xFF222222);
        setupColorPicker(findViewById(R.id.backgroundColorPicker), KEY_BACKGROUND_COLOR, 0xFFFFFFFF);
    }

    private void saveDisplayText(EditText counterLabelInput, EditText derivedLabelInput) {
        prefs.edit()
                .putString(KEY_COUNTER_LABEL, counterLabelInput.getText().toString().trim())
                .putString(KEY_DERIVED_LABEL, derivedLabelInput.getText().toString().trim())
                .apply();
        toast("Display text saved");
    }

    private void saveMultiplier(EditText multiplierInput) {
        try {
            int multiplier = Integer.parseInt(multiplierInput.getText().toString().trim());
            prefs.edit().putInt(KEY_MULTIPLIER, multiplier).apply();
            toast("Multiplier saved");
        } catch (NumberFormatException e) {
            toast("Enter a valid multiplier");
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

    private void setupFontSizeSeekBar(SeekBar seekBar) {
        int size = prefs.getInt(KEY_COUNTER_FONT_SIZE, 72);
        seekBar.setProgress(Math.max(0, Math.min(48, size - 48)));
        updateFontSizeLabel(size);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int newSize = 48 + progress;
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

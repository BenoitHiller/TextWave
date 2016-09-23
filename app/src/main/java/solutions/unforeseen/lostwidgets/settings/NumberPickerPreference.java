package solutions.unforeseen.lostwidgets.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.benoithiller.textwave.R;

/**
 * Number Picker Preference class
 */
public class NumberPickerPreference extends DialogPreference {

    private int min;
    private int max;
    private int value;
    private String units;

    private NumberPicker numberPicker;

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray numberPickerType = context.obtainStyledAttributes(attrs,
                R.styleable.NumberPickerPreference, 0, 0);

        max = numberPickerType.getInt(R.styleable.NumberPickerPreference_max, 100);
        min = numberPickerType.getInt(R.styleable.NumberPickerPreference_min, 0);
        units = numberPickerType.getString(R.styleable.NumberPickerPreference_units);

        numberPickerType.recycle();
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            value = getPersistedInt(min);
        } else {
            value = (Integer) defaultValue;
            persistInt(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray typedArray, int index) {
        return typedArray.getInteger(index, min);
    }

    @Override
    protected View onCreateDialogView() {

        View view = View.inflate(getContext(), R.layout.number_picker_preference, null);

        numberPicker = (NumberPicker) view.findViewById(R.id.number_picker);

        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        if (units != null && !units.isEmpty()) {
            TextView unitsView = (TextView) view.findViewById(R.id.units_field);
            unitsView.setText(units);
            unitsView.setPadding(10, 0, 0, 0);
        }

        return view;
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        numberPicker.setMaxValue(max);
        numberPicker.setMinValue(min);
        numberPicker.setValue(value);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            value = numberPicker.getValue();
            persistInt(value);
        }
    }
}

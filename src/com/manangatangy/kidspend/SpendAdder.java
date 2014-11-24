package com.manangatangy.kidspend;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 24/11/14
 * Time: 3:00 PM
 * To change this template use File | Settings | File Templates.
 */
public class SpendAdder extends Activity implements View.OnClickListener {

    //static final String TAG = "Spend";

    private EditText dateDisplay;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private int mYear;
    private int mMonth;
    private int mDay;
    static final int DIALOG_DATE_ID = 0;
    static final int DIALOG_NEW_SPEND_TYPE_ID = 1;

    private SpinnerWithPrefs spinnerWithPrefs;
    private EditableSpinner periodSpinner;		// Only non-null when doRepeat is true;
    private EditText amountInput;
    private String currentAccount = "";
    private boolean doRepeat = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.new_spend);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_new_spend);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentAccount = extras.getString("account");
            if ("yes".equalsIgnoreCase(extras.getString("doRepeat")))
                doRepeat = true;
        }

        ((TextView)findViewById(R.id.titleNewSpendText)).setText(currentAccount + (doRepeat ? ": New Repeat" : ": New Spend"));
        ((TextView)findViewById(R.id.newSpendDateLabel)).setText(doRepeat ? "Next Date:" : "Date:");
        ((TextView)findViewById(R.id.periodLabel)).setVisibility(doRepeat ? View.VISIBLE : View.INVISIBLE);
        ((Spinner)findViewById(R.id.periodSpinner)).setVisibility(doRepeat ? View.VISIBLE : View.INVISIBLE);

        // Setup the date picking, which comprises a button and an edit field.
        // Clicking on the dateDisplay button causes the DATE_DIALOG_ID (a DatePicker)
        // to popup. When this dialog sets a date, it calls back the onDateSet method
        // which updates the date display field with the picked date value.
        dateDisplay = (EditText)findViewById(R.id.dateDisplay);
        findViewById(R.id.pickDateButton).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showDialog(DIALOG_DATE_ID);
            }
        });
        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            // Called back when the DatePickerDialog sets a date.
            // Uses the values to update the dateDisplay in this class.
            public void onDateSet(DatePicker view, int year, int month, int day) {
                updateDateDisplay(year, month, day);
            }
        };

        if (doRepeat) {
            String[] periodsArray = getResources().getStringArray(R.array.periods_array);
            List<String> list = Arrays.asList(periodsArray);
            periodSpinner = new EditableSpinner(this, R.id.periodSpinner, list, null);
        }

        // Setup the expenditure type spinner.
        spinnerWithPrefs = new SpinnerWithPrefs(this, currentAccount);

        amountInput = (EditText)findViewById(R.id.amountInput);

        findViewById(R.id.okButton).setOnClickListener(this);
        findViewById(R.id.cancelButton).setOnClickListener(this);
    }

    /**
     * The user is returning to the activity so ensure the date field is up-to-date.
     */
    @Override
    public void onResume() {
        super.onResume();
        // Display the current date.
        Calendar c = Calendar.getInstance();
        updateDateDisplay(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * The activity is about to be shut down so save in the prefs file and extra types and type pos.
     * Attempt to write the state to the preferences file. If this fails, notify the user.
     * http://developer.android.com/guide/topics/fundamentals/activities.html#Lifecycle
     * http://developer.android.com/guide/topics/fundamentals/activities.html#SavingActivityState
     @Override
     public void onDestroy() {
     super.onDestroy();
     }
     */

    public static final String monthNames[] = {
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
            "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
    };

    // Assigns new values for the member variables, and uses them to update the date in the TextView.
    // The values are stored in the class because they are used (by onCreateDialog) to initialize the date picker.
    private void updateDateDisplay(int year, int month, int day) {
        mYear = year;
        mMonth = month;
        mDay = day;
        String dateString;
        if (!doRepeat) {
            dateString = new StringBuilder().append(mDay).append(" ").append(monthNames[mMonth]).append(" ").append(mYear).toString();
        } else {
            dateString = String.format("%4d-%02d-%02d", mYear, mMonth + 1, mDay);
        }
        dateDisplay.setText(dateString);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DATE_ID:
                return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);

            case DIALOG_NEW_SPEND_TYPE_ID:		// Called in here from SpinnerWithPrefs.
                return spinnerWithPrefs.createAddTypeDialog();

        }
        return null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.okButton:
                // Ensure all field values are non empty.
                String date = dateDisplay.getText().toString();
                String spendType = spinnerWithPrefs.getSelectedValue();

                Integer amount = TextUtils.isEmpty(amountInput.getText()) ? null : Integer.valueOf(amountInput.getText().toString());
                if (TextUtils.isEmpty(date)) {
                    showAlertDialog("value for DATE is required");
                } else if (TextUtils.isEmpty(spendType)) {
                    showAlertDialog("value for TYPE is required");
                } else if (amount == null) {
                    showAlertDialog("value for AMOUNT is required");
                } else {
                    // Add a new spend or repeat record to the content provider.
                    ContentValues values = new ContentValues();
                    Uri addUri;
                    String message;
                    if (doRepeat) {
                        String period = periodSpinner.getSelected();
                        values.put(SpendProviderMetaData.SpendsTableMetaData.REPEAT_PERIOD, period);
                        values.put(SpendProviderMetaData.SpendsTableMetaData.REPEAT_NEXTDATE, date);
                        values.put(SpendProviderMetaData.SpendsTableMetaData.REPEAT_TYPE, spendType);
                        values.put(SpendProviderMetaData.SpendsTableMetaData.REPEAT_AMOUNT, amount);
                        values.put(SpendProviderMetaData.SpendsTableMetaData.REPEAT_ACCOUNT, currentAccount);
                        addUri = SpendProviderMetaData.SpendsTableMetaData.REPEAT_CONTENT_URI;
                        message = "saved: " + period + ":" + spendType + ":" + amount;
                    } else {
                        values.put(SpendProviderMetaData.SpendsTableMetaData.SPEND_DATE, date);
                        values.put(SpendProviderMetaData.SpendsTableMetaData.SPEND_TYPE, spendType);
                        values.put(SpendProviderMetaData.SpendsTableMetaData.SPEND_AMOUNT, amount);
                        values.put(SpendProviderMetaData.SpendsTableMetaData.SPEND_ACCOUNT, currentAccount);
                        addUri = SpendProviderMetaData.SpendsTableMetaData.SPEND_CONTENT_URI;
                        message = "saved: " + spendType + ":" + amount;
                    }
                    Uri uri = getContentResolver().insert(addUri, values);
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    // Return to previous screen (maybe the manager list?)
                    finish();
                }
                break;
            case R.id.cancelButton:
                finish();
                break;
        }
    }

    private void showAlertDialog(String alertMessage) {
        new AlertDialog.Builder(this).setMessage(alertMessage)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

}

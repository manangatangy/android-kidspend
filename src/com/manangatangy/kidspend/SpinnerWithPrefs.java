package com.manangatangy.kidspend;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 24/11/14
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import android.util.Log;

import java.util.ArrayList;

/**
 * Manages a Spinner and its Adapter.  The Spinner is loaded with from the string
 * resources and also from the preferences.
 * The value currently selected is available at getCurrentSelection().
 * The extra types held in the preferences may be cleared or added to.
 * Calling addType will add a new type to the preferences and to the spinner (also selecting it).
 *
 */
public class SpinnerWithPrefs {

    static final String TAG = "Expenditure";

    static final String PREFERENCES_FILE = "ExpenditurePrefs";
    static final String TYPE_EXTRAS_KEY = "ExtraTypes";
    static final String TYPE_POSITION_KEY = "TypePosition";

    private final SpendAdder spendAdder;						// Used for prefs and resources.
    private EditText newTypeInput;						// View holding the candidate new type value.

    // The extraTypes string is a comma-separated list of expenditure types.
    // It is only referenced by loadExtraTypesFromPrefs and saveNewType.
    private String extraTypes = "";		// maybe should be ArrayList<String> ???

    // Only types prefixed with this value will be loaded into the spinner.
    private String currentPrefix = "";

    private EditableSpinner editableSpinner;

    /**
     * A SpinnerWithPrefs creates a Spinner and ArrayAdapter to back it, which is filled with values from
     * the string resources and user preferences. The item selected from the spinners list will be available
     * at getSelectedValue().  Short clicking on the Spinner widget pops up the selection list and long
     * clicking on the widget pops up a dialog (invoked by the spendAdder.onCreateDialog(DIALOG_NEW_SPEND_TYPE_ID))
     * that allows a new spend type to be created and added/stored to the prefs (if it is not a duplicate of
     * an existing spend type).  When the new spend type dialog appears, then long clicking on the add button
     * will prompt for the clearing of all the spend type preferences.
     */
    public SpinnerWithPrefs(final SpendAdder spendAdder, String prefix) {
        this.spendAdder = spendAdder;
        ArrayList<String> arrayList = getFromResourceAndPrefs(prefix);
        editableSpinner = new EditableSpinner(spendAdder, R.id.typeSpinner, arrayList, new View.OnLongClickListener() {
            // Long clicking the spinner widget pops up the new type dialog, first clearing previous value, error.
            @Override
            public boolean onLongClick(View v) {
                if (newTypeInput != null) {		// Will only be set after onCreateDialog first called.
                    newTypeInput.setText("");
                    newTypeInput.setError(null);
                }
                spendAdder.showDialog(SpendAdder.DIALOG_NEW_SPEND_TYPE_ID);
                return true;
            }
        });
    }

    public String getSelectedValue() {
        return editableSpinner.getSelected();
    }

    /**
     * Create a new ArrayAdapter and load it with values from the string resources and from the prefs.
     * Only load values with the specified prefix (stripping it from the loaded value).
     * If the newPrefix _ non null, then use tit to set a new currentPrefix
     */
    public void reset(String newPrefix) {
        ArrayList<String> arrayList = getFromResourceAndPrefs(newPrefix);
        editableSpinner.resetAdapter(spendAdder, arrayList);
    }

    /**
     * Add a new type to the current set of preferences and save it (also adding it to the local field).
     * The new type is also added into the adapter and set as the selection in the spinner.
     * Returns false if the newType failed validation.
     */
    private boolean addType(String newType) {
        if (editableSpinner.addOption(newType) == false)
            return false;

        // Save it to the prefs, (with the current prefix) first storing locally in extraTypes.
        if (extraTypes.length() > 0)
            extraTypes = extraTypes + ",";
        extraTypes = extraTypes + currentPrefix + newType;
        savePref(extraTypes);
        return true;
    }

    /**
     * Assign a new value for the currentPrefix.
     * Create a new ArrayList and fill it with the values from the resource and the prefs.
     * Only values prefixed with the currentPriefix will be loaded (after stripping the prefix).
     * @return
     */
    private ArrayList<String> getFromResourceAndPrefs(String newPrefix) {

        if (!TextUtils.isEmpty(newPrefix)) {
            this.currentPrefix = newPrefix + ":";
        }
        // The spinner is loaded with a bunch of statically
        // defined values from the string resource, and then any user-defined values from the
        // prefs file.  In addition, the spinner initial position may also be held as a user
        // pref, from a previous session, so we try and load that too.
        // The preferences are stored in a SharedPreferences file, which is a "file"
        // containing a hashmap and is shared by all instances of the application.
        // Note: use boolean SharedPreferences.contains("some_key") to test if pref exists.
        // R.array.types_array
        String[] typesArray = spendAdder.getResources().getStringArray(R.array.types_array);
        ArrayList<String> arrayList = new ArrayList<String>();		// Arrays.asList(spendTypesArray)
        for (String spendType : typesArray) {
            String stripped = hasPrefix(spendType, currentPrefix);
            if (!TextUtils.isEmpty(stripped)) {
                arrayList.add(stripped);
            }
        }

        // http://developer.android.com/guide/topics/fundamentals/activities.html#SavingActivityState
        SharedPreferences prefs = spendAdder.getSharedPreferences(PREFERENCES_FILE, Activity.MODE_WORLD_READABLE);
        extraTypes = prefs.getString(TYPE_EXTRAS_KEY, "");
        //Log.v(TAG, "loadExtraTypesFromPrefs: extraTypes=" + extraTypes);

        for (String extraType : extraTypes.split(",")) {
            if (!TextUtils.isEmpty(extraType)) {
                String stripped = hasPrefix(extraType, currentPrefix);
                if (!TextUtils.isEmpty(stripped)) {
                    //Log.d(TAG, "adding: " + extraType);
                    arrayList.add(stripped);
                }
            }
        }

        return arrayList;
    }

    /**
     * If the text has the specified prefix, return the text (minus the prefix) else return null.
     */
    private String hasPrefix(String text, String prefix) {
        if (text.startsWith(prefix)) {
            return text.substring(prefix.length());
        } else
            return null;
    }

    public AlertDialog createAddTypeDialog() {
        // Ref: http://developer.android.com/guide/topics/ui/dialogs.html#CustomDialog
        //Context mContext = getApplicationContext();
        LayoutInflater inflater = (LayoutInflater)spendAdder.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.new_type, (ViewGroup)spendAdder.findViewById(R.id.new_type_root));

        newTypeInput = (EditText)layout.findViewById(R.id.newTypeInput);

        // Note that the Context passed to AlertDialog.Builder() must be an Activity
        // ref: http://stackoverflow.com/questions/1561803/android-progressdialog-show-crashes-with-getapplicationcontext
        final AlertDialog newTypeDialog = new AlertDialog.Builder(spendAdder)
                .setView(layout)
                .setTitle(R.string.new_type_dialog_title)
                .setPositiveButton(R.string.new_type_create_label, null)
                .setNegativeButton(R.string.new_type_cancel_label, null)
                .create();

        // The buttons default onClickListeners always dismiss the dialog.  We want to keep
        // it hanging around if there is an input eror, so override them.  Must do it after
        // onShow() is called otherwise getButton() returns null.
        // ref: http://stackoverflow.com/questions/2620444/android-how-to-prevent-dialog-closed-or-remain-dialog-when-button-is-clicked
        newTypeDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button positiveButton = newTypeDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Log.v(TAG, "mSpendList: onItemLongclick,onClick: " + deleteId);
                        String newType = newTypeInput.getText().toString();
                        if (!addType(newType))
                            newTypeInput.setError("duplicate/empty type");
                        else {
                            Toast.makeText(spendAdder.getApplicationContext(), "added new-type  " + currentPrefix + newType, Toast.LENGTH_SHORT).show();
                            //Dismiss once everything is OK.
                            newTypeDialog.dismiss();
                        }
                    }
                });
                // Long clicking the add button on the new spend type dialog, prompts to delete all extra types.
                positiveButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(spendAdder)
                                .setMessage("Delete all extra type prefs?")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        savePref(extraTypes = "");		// Empty the local prefs and save it.
                                        reset(null);		// No change to currentPrefix
                                        Toast.makeText(spendAdder.getApplicationContext(), "Deleted all spend type prefs", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        return false;
                    }
                });
            }
        });
        return newTypeDialog;
    }

    private void savePref(String typePref) {
        SharedPreferences prefs = spendAdder.getSharedPreferences(PREFERENCES_FILE, Activity.MODE_WORLD_READABLE);
        SharedPreferences.Editor editor = prefs.edit();
        //e.putInt(TYPE_POSITION_KEY, typePosition);
        editor.putString(TYPE_EXTRAS_KEY, typePref);
        // The commit to persistent storage may fail
        if (!editor.commit()) {
            Toast.makeText(spendAdder, "Expenditure: failed to write state", Toast.LENGTH_LONG).show();
        } else {
            Log.v(TAG, "savePref: typePref=" + typePref);
        }
    }


}
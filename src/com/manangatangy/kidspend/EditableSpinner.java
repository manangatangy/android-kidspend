package com.manangatangy.kidspend;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 24/11/14
 * Time: 2:56 PM
 * To change this template use File | Settings | File Templates.
 */

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

/**
 * Wraps a Spinner view to provide item clickability, long-clickability, and loadability.
 */
public class EditableSpinner {

    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private String selected;

    /**
     * May call resetAdapter in order to re-load the spinner.
     * @param activity
     * @param spinnerId
     * @param list
     * @param longClickListener is the callback for long clicks on a list item, it may be null
     */
    public EditableSpinner(Activity activity, int spinnerId, List<String> list, View.OnLongClickListener longClickListener) {
        spinner = (Spinner)activity.findViewById(spinnerId);
        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                        selected = parent.getItemAtPosition(pos).toString();
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                }
        );

        if (longClickListener != null)
            spinner.setOnLongClickListener(longClickListener);

        resetAdapter(activity, list);
    }

    /**
     * Create a new ArrayAdapter<String>, backed by the arrayList, and set it to the Spinner.
     * @param list
     * @param context
     */
    public void resetAdapter(Context context, List<String> list) {
        arrayAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, list);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
    }

    /**
     * Add a new option into the spinner and make it the selected item.
     * If the option already exists in the spinner then just return false.
     * @param option
     * @return
     */
    public boolean addOption(String option) {
        if (TextUtils.isEmpty(option))
            return false;
        for (int i = 0; i < arrayAdapter.getCount(); i++) {
            if (option.equalsIgnoreCase(arrayAdapter.getItem(i)))
                return false;
        }
        arrayAdapter.add(option);
        int typeCount = spinner.getCount();
        spinner.setSelection(typeCount - 1);
        return true;
    }

    /**
     * @return
     */
    public String getSelected() {
        return selected;
    }

}
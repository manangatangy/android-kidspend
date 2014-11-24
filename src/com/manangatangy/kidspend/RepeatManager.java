package com.manangatangy.kidspend;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 24/11/14
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.manangatangy.kidspend.SpendProviderMetaData.SpendsTableMetaData;

/** Modelled from SpendManager. */
public class RepeatManager extends Activity {

    public static final String TAG = "RepeatManager";

    private Button mAddRepeatButton;
    private ListView mRepeatList;

    private Cursor cursor;
    private SimpleCursorAdapter adapter;
    private long deleteId;

    private String currentAccount = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.repeat_manager);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_repeat);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            currentAccount = extras.getString("account");

        setActivityTitle();

        // Obtain handles to UI objects
        mAddRepeatButton = (Button) findViewById(R.id.addRepeatButton);
        mRepeatList = (ListView) findViewById(R.id.repeatList);

        // Register long-click handler for deletion of list items
        mRepeatList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                deleteId = id;
                String deleteCandidate = getChildText((ViewGroup)view, 0) + "/" + getChildText((ViewGroup)view, 1)
                        + "/" + getChildText((ViewGroup)view, 2) + "/" + getChildText((ViewGroup)view, 3);
                AlertDialog.Builder builder = new AlertDialog.Builder(RepeatManager.this);
                builder.setMessage("Delete '" + deleteCandidate + "' ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int idx) {
                                // Delete the specified spend record.
                                Uri uri = Uri.withAppendedPath(SpendProviderMetaData.SpendsTableMetaData.REPEAT_CONTENT_URI, Long.toString(deleteId));
                                int count = getContentResolver().delete(uri, null, null);	// Remove from the database.
                                cursor.requery();											// Refresh the cursor, causing adapter to change
                                adapter.notifyDataSetChanged();								// Update any registered observers (views).
                                //Log.v(TAG, "mSpendList: onItemLongclick,onClick: " + deleteId);
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
                return true;	// Indicate that long click was consumed.
            }
        });

        // Register handler for UI elements
        mAddRepeatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(RepeatManager.this, SpendAdder.class);
                intent.putExtra("account", currentAccount);
                intent.putExtra("doRepeat", "yes");
                startActivity(intent);
            }
        });

        // Populate the repeats list
        Uri uri = SpendProviderMetaData.SpendsTableMetaData.REPEAT_CONTENT_URI;
        cursor = managedQuery(uri, projection, SpendsTableMetaData.REPEAT_ACCOUNT + " ='" + currentAccount + "'", null, null);
        adapter = new SimpleCursorAdapter(this, R.layout.repeat_entry, cursor, fields, itemIds );
        mRepeatList.setAdapter(adapter);
    }

    private void setActivityTitle() {
        TextView titleView = (TextView)findViewById(R.id.titleRepeatListText);
        titleView.setText(currentAccount + ":Repeats");
    }

    public static final String[] projection = new String[] {
            SpendsTableMetaData._ID,
            SpendsTableMetaData.REPEAT_NEXTDATE,
            SpendsTableMetaData.REPEAT_TYPE,
            SpendsTableMetaData.REPEAT_AMOUNT,
            SpendsTableMetaData.REPEAT_PERIOD,
            SpendsTableMetaData.REPEAT_ACCOUNT
    };
    static final String[] fields = new String[] {
            SpendsTableMetaData.REPEAT_PERIOD,
            SpendsTableMetaData.REPEAT_NEXTDATE,
            SpendsTableMetaData.REPEAT_TYPE,
            SpendsTableMetaData.REPEAT_AMOUNT
    };
    static final int[] itemIds = new int[] {
            R.id.repeatEntryPeriodText,
            R.id.repeatEntryNextDateText,
            R.id.repeatEntryTypeText,
            R.id.repeatEntryAmountText
    };

    private String getChildText(ViewGroup item, int childIndex) {
        if (item == null)
            return null;
        TextView child = (TextView)item.getChildAt(childIndex);
        return child.getText().toString();
    }

}

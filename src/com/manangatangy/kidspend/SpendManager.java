package com.manangatangy.kidspend;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.manangatangy.kidspend.SpendProviderMetaData.SpendsTableMetaData;

public class SpendManager extends Activity {

    public static final String TAG = "SpendManager";

    private Button mAddSpendButton;
    private Button mRepeatButton;
    private Button mTotalsButton;
    private Button mIoButton;
    private ListView mSpendList;

    private Cursor cursor;
    private SimpleCursorAdapter adapter;
    private long deleteId;
    private int currentAccountIndex = 0;
    private String[] accountArray = {};

    /**
     * Called when the activity is first created. Responsible for initializing the UI.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Log.v(TAG, "Activity State: onCreate()");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.spend_manager);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_manager);

        // Must load the accountArray before setting the activity title.
        accountArray = getResources().getStringArray(R.array.accounts_array);

        // Obtain handles to UI objects
        mTotalsButton = (Button) findViewById(R.id.totalsButton);
        mAddSpendButton = (Button) findViewById(R.id.addSpendButton);
        mRepeatButton = (Button) findViewById(R.id.repeatButton);
        mSpendList = (ListView) findViewById(R.id.spendList);
        mIoButton = (Button) findViewById(R.id.ioButton);

        // Register long-click handler for list items
        // Ref: http://stackoverflow.com/questions/6386430/difficult-question-delete-list-items-using-onlongpress-update-adapter
        mSpendList.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                deleteId = id;
                String deleteCandidate = getChildText((ViewGroup)view, 0) + "/" + getChildText((ViewGroup)view, 1) + "/" + getChildText((ViewGroup)view, 2);
                // Ref: http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(SpendManager.this);
                builder.setMessage("Delete '" + deleteCandidate + "' ?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int idx) {
                                // Delete the specified spend record.
                                Uri uri = Uri.withAppendedPath(SpendProviderMetaData.SpendsTableMetaData.SPEND_CONTENT_URI, Long.toString(deleteId));
                                int count = getContentResolver().delete(uri, null, null);	// Remove from the database.
                                cursor.requery();											// Refresh the cursor, causing adapter to change
                                adapter.notifyDataSetChanged();								// Update any registered observers (views).
                                // TODO: Note that requery is deprecated because it could take a long time, and as it's
                                // processing in the apps UI thread, it could lead to an ANR (app not responding exception).
                                // ref: http://developer.android.com/reference/android/database/Cursor.html#requery%28%29
                                // Another approach is from
                                // ref: http://stackoverflow.com/questions/6386430/difficult-question-delete-list-items-using-onlongpress-update-adapter
                                // and is to just manually delete the item from the adapter using item=parent.getItemAtPosition(position) and
                                // then adapter.remove(item)  but I don't know if this will work.  Another approach is to use LoaderManager/CursorLoader
                                // ref: http://developer.android.com/guide/topics/fundamentals/loaders.html
                                // ref: http://stackoverflow.com/questions/5572733/alternative-to-requery-on-android-honeycomb
                                // ref: http://mobile.tutsplus.com/tutorials/android/android-sdk_loading-data_cursorloader/

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

        mAddSpendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(SpendManager.this, SpendAdder.class);
                intent.putExtra("account", accountArray[currentAccountIndex]);
                startActivity(intent);
            }
        });

        mRepeatButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(SpendManager.this, RepeatManager.class);
                intent.putExtra("account", accountArray[currentAccountIndex]);
                startActivity(intent);
            }
        });

        mTotalsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // ref: http://stackoverflow.com/questions/2091465/how-do-i-pass-data-between-activities-in-android
                Intent intent = new Intent(SpendManager.this, SpendTotals.class);
                intent.putExtra("account", accountArray[currentAccountIndex]);
                startActivity(intent);
            }
        });

        mIoButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(SpendManager.this);
                builder.setMessage("Which type of I/O ?")
                        .setPositiveButton("Import (!!!)", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int idx) {
                                importExport(true);
                            }
                        })
                        .setNegativeButton("Export", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                importExport(false);
                            }
                        })
                        .setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });

        // Register for long click on the title - changes the account.
        findViewById(R.id.titleManagerText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SpendManager.this)
                        .setTitle("Select the Account")
                        .setItems(accountArray, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                // Selected was items[item]
                                setActivityTitleAndRefreshList(item);
                            }
                        }).show();
            }
        });

        setActivityTitleAndRefreshList(0);
    }

    ProgressDialog progress;

    private void importExportBackground(final boolean doImport) {
        Thread thread =  new Thread(null, new Runnable() {
            @Override
            public void run() {
                importExport(doImport);
                //runOnUiThread(loadAdapter);
            }
        }, "background-import-export");
        progress = ProgressDialog.show(this, "Please wait...", (doImport ? "Importing..." : "Exporting..."), true);
        thread.start();
    }

    private void importExport(final boolean doImport) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(getBaseContext(), "ExternalStorageMedia not available (!MEDIA_MOUNTED)", Toast.LENGTH_LONG).show();
            return;
        }
        final File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        path.mkdirs();		// Ensure existence.
        final String path2 = path + "/expend-*";
        AlertDialog.Builder builder = new AlertDialog.Builder(SpendManager.this);
        builder.setMessage((doImport ? "Import (delete existing) from: " : "Export to: ") + path2 + " ?")
                .setPositiveButton((doImport ? "Import" : "Export"), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int idx) {
                        doBackgroundExportImport(path, doImport);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                }).show();
    }

    static final String[] ioProjSpends = new String[] {
            SpendsTableMetaData.SPEND_ACCOUNT,
            SpendsTableMetaData.SPEND_DATE,
            SpendsTableMetaData.SPEND_TYPE,
            SpendsTableMetaData.SPEND_AMOUNT
    };
    static final String[] ioProjRepeat = new String[] {
            SpendsTableMetaData.REPEAT_AMOUNT,
            SpendsTableMetaData.REPEAT_NEXTDATE,
            SpendsTableMetaData.REPEAT_TYPE,
            SpendsTableMetaData.REPEAT_ACCOUNT,
            SpendsTableMetaData.REPEAT_PERIOD
    };

    private String backgroundMessage;

    private void doBackgroundExportImport(final File path, final boolean doImport) {
        progress = ProgressDialog.show(this, "Please wait...", (doImport ? "Importing..." : "Exporting..."), true);
        new Thread(null, new Runnable() {
            @Override
            public void run() {
                try {
                    if (doImport) {
                        int c = 0;
                        c += doImport(SpendsTableMetaData.SPEND_CONTENT_URI, ioProjSpends, new File(path, "/expend-spends"));
                        c += doImport(SpendsTableMetaData.REPEAT_CONTENT_URI, ioProjRepeat, new File(path, "/expend-repeat"));
                        backgroundMessage = "Imported " + c + " records from " + path + "/expends";
                    } else {
                        int c = 0;
                        c += doExport(SpendsTableMetaData.SPEND_CONTENT_URI, ioProjSpends, new File(path, "/expend-spends"));
                        c += doExport(SpendsTableMetaData.REPEAT_CONTENT_URI, ioProjRepeat, new File(path, "/expend-repeat"));
                        backgroundMessage = "Exported " + c + " records to " + path + "/expends";
                    }
                } catch (Exception e) {
                    String error = "Error during " + (doImport ? "import" : "export");
                    Log.w("ExternalStorage", error, e);
                    backgroundMessage = error + e;
                }
                progress.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getBaseContext(), backgroundMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "background-" + (doImport ? "import" : "export")).start();
    }

    private int doImport(Uri uri, String[] proj, File file) throws IOException, JSONException {
        // First delete all rows.
        getContentResolver().delete(uri, null, null);

        BufferedReader reader = new BufferedReader(new FileReader(file));
        int c = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            c++;
            ContentValues values = new ContentValues();
            JSONObject json = new JSONObject(line);
            for (String name : proj) {
                String value = json.getString(name);
                values.put(name, value);
            }
            getContentResolver().insert(uri, values);
        }
        reader.close();
        return c;
    }

    private int doExport(Uri uri, String[] proj, File file) throws IOException, JSONException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        int c = 0;
        Cursor exportCursor = managedQuery(uri, proj, null, null, null);
        while (exportCursor.moveToNext()) {
            c++;
            JSONObject json = new JSONObject();
            for (String name : proj) {
                String value = getField(exportCursor, name);
                json.put(name, value);
            }
            writer.write(json.toString());
            writer.newLine();
        }
        writer.close();
        exportCursor.close();
        return c;
    }

    private String getField(Cursor cursor, String sourceName) {
        int sourceIndex = cursor.getColumnIndex(sourceName);
        String value = cursor.getString(sourceIndex);
        return value;
    }

    private void getField(Cursor cursor, String sourceName, ContentValues values, String targetName) {
        String value = getField(cursor, sourceName);
        values.put(targetName, value);
        Log.v(TAG, "sourceName=" + sourceName + ", targetName=" + targetName + ", value=" + value);
    }

    public int processRepeats() {
        int count = 0;
        Uri uri = SpendProviderMetaData.SpendsTableMetaData.REPEAT_CONTENT_URI;
        Cursor cursor = managedQuery(uri, RepeatManager.projection,
                "strftime('%s', " + SpendsTableMetaData.REPEAT_NEXTDATE + ") < strftime('%s', 'now')", null, null);
        while (cursor.moveToNext()) {
            count++;
            // With each candidate repeat, first add a new spend record and then update the existing repeat record.
            Log.v(TAG, "inserting spend count=" + count);
            ContentValues spend = new ContentValues();
            // Spend records added automatically this way will have differently formatted dates. Helps to distinguish them.
            getField(cursor, SpendProviderMetaData.SpendsTableMetaData.REPEAT_NEXTDATE, spend, SpendProviderMetaData.SpendsTableMetaData.SPEND_DATE);
            getField(cursor, SpendProviderMetaData.SpendsTableMetaData.REPEAT_ACCOUNT, spend, SpendProviderMetaData.SpendsTableMetaData.SPEND_ACCOUNT);
            getField(cursor, SpendProviderMetaData.SpendsTableMetaData.REPEAT_TYPE, spend, SpendProviderMetaData.SpendsTableMetaData.SPEND_TYPE);
            getField(cursor, SpendProviderMetaData.SpendsTableMetaData.REPEAT_AMOUNT, spend, SpendProviderMetaData.SpendsTableMetaData.SPEND_AMOUNT);
            getContentResolver().insert(SpendProviderMetaData.SpendsTableMetaData.SPEND_CONTENT_URI, spend);

            // Now update the repeat record.
            // Process to the next date using the period.
            // The contentValues are not used since they don't support expressions.
            // Instead use the "modifyNextDate" uri and specify the _ID in the where clause.
            String updateId = getField(cursor, SpendProviderMetaData.SpendsTableMetaData._ID);
            String period = getField(cursor, SpendProviderMetaData.SpendsTableMetaData.REPEAT_PERIOD);
            String where = SpendProviderMetaData.SpendsTableMetaData._ID + "=" + updateId;

            Log.v(TAG, "updating repeat (" + period + ") for " + where);

            Uri modifyDateUri = SpendProviderMetaData.SpendsTableMetaData.REPEAT_CONTENT_URI;
            modifyDateUri = Uri.withAppendedPath(modifyDateUri, "modifyNextDate");
            modifyDateUri = Uri.withAppendedPath(modifyDateUri, period);

            getContentResolver().update(modifyDateUri, null, where, null);

        }
        Toast.makeText(this, "processed " + count + " repeats", Toast.LENGTH_SHORT).show();
        return count;
    }

    static final String[] projection = new String[] {
            SpendsTableMetaData._ID,
            SpendsTableMetaData.SPEND_DATE,
            SpendsTableMetaData.SPEND_TYPE,
            SpendsTableMetaData.SPEND_AMOUNT
    };
    static final String[] fields = new String[] {
            SpendsTableMetaData.SPEND_DATE,
            SpendsTableMetaData.SPEND_TYPE,
            SpendsTableMetaData.SPEND_AMOUNT
    };
    static final int[] itemIds = new int[] {
            R.id.spendEntryDateText,
            R.id.spendEntryTypeText,
            R.id.spendEntryAmountText
    };

    /**
     * Populate the spends list, including processing any repeats (for all accounts) that have fallen due.
     * @param newCurrentAccountIndex
     */
    private void setActivityTitleAndRefreshList(int newCurrentAccountIndex) {
        processRepeats();

        this.currentAccountIndex = newCurrentAccountIndex;
        TextView titleView = (TextView)findViewById(R.id.titleManagerText);
        titleView.setText(accountArray[currentAccountIndex] + ":Spends");

        // Populate the spends list
        Uri spendsUri = SpendProviderMetaData.SpendsTableMetaData.SPEND_CONTENT_URI;
        cursor = managedQuery(spendsUri, projection, SpendsTableMetaData.SPEND_ACCOUNT + " ='" + accountArray[currentAccountIndex] + "'", null, null);
        // Should this cursor be closed on destroy?
        adapter = new SimpleCursorAdapter(this, R.layout.spend_entry, cursor, fields, itemIds );
        mSpendList.setAdapter(adapter);
    }

    private String getChildText(ViewGroup item, int childIndex) {
        if (item == null)
            return null;
        TextView child = (TextView)item.getChildAt(childIndex);
        return child.getText().toString();
    }

}

package com.manangatangy.kidspend;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 24/11/14
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;

import com.manangatangy.kidspend.SpendProviderMetaData.SpendsTableMetaData;

/**
 * Modelled on ref: http://developer.android.com/guide/topics/fundamentals/loaders.html
 *
 *
 */
public class SpendTotals extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = "SpendTotals";

    private static final float DAYS_PER_DAY = (float) 1.0;
    private static final float DAYS_PER_WEEK = (float) 7.0;
    private static final float DAYS_PER_MONTH = (float) (365.24/12.0);

    private static final String KEY_DAY = "day";
    private static final String KEY_WEEK = "week";
    private static final String KEY_MONTH = "month";
    private static final String KEY_TOTAL = "total";

    // The fields and itemIds are used in the adapter to map from the cursor to the view.
    private static final String[] fields = new String[] {
            SpendsTableMetaData.SPEND_TYPE,
            SpendsTableMetaData.SPEND_AMOUNT
    };
    private static final int[] itemIds = new int[] {
            R.id.totalEntryTypeText,
            R.id.totalEntryAmountText
    };

    class Formatter {
        private String[] map;
        private String fullTitle;
        private String summaryItemTitle;

        private int totalPrecision;
        private float periodCount;

        public String toString() {
            return "{Formatter summaryItemTitle:" + summaryItemTitle + ", periodCount:" + periodCount + ", totalPrecision:" + totalPrecision + "}";
        }

        /**
         * Generates various strings based on the period and dayCount.
         * @param title is the main title (the first part of the full title)
         * @param periodName is used to format the second part of the full title.
         * @param periodDuration in days
         * @param periodCountPrecision is the number of decimal places in the second part of the full title.
         * @param dayCount for the entire duration
         * @param totalPrecision is the number of decimal places in the amount column.
         */
        public Formatter(String title, String periodName, float periodDuration, String periodCountPrecision,
                         float dayCount, int totalPrecision) {
            // Note that periodDuration will be zero for the "Totals" if there are no spend records (ie dayCount is zero)
            periodCount = dayCount / (periodDuration == 0 ? 1 : periodDuration);
            map = new String[] {
                    SpendsTableMetaData._ID,
                    SpendsTableMetaData.SPEND_TYPE,
                    String.format("ROUND(TOTAL(%s)/%f,%d) as %s",
                            SpendsTableMetaData.SPEND_AMOUNT, periodCount, totalPrecision, SpendsTableMetaData.SPEND_AMOUNT)
            };

            summaryItemTitle = "TOTAL per " + periodName;

            // For the total counts (entire period), show number of days, not number of entire periods (which would be 1)
            float pCount = periodCount;
            if ("Totals".equalsIgnoreCase(title)) {
                pCount = dayCount;
                summaryItemTitle = "TOTAL";
            }
            fullTitle = String.format("%s (%." + periodCountPrecision + "f %ss)", title, pCount, periodName);

            this.totalPrecision = totalPrecision;
        }
        public final String[] getMap() {
            return map;
        }
        public String getFullTitle(String accountName) {
            return accountName + ":" + fullTitle;
        }
        public String getTotalAmountPerPeriod() {
            // Note that periodCount (below) will be 0 when there are no spend records (ie dayCount is zero)
            return String.format("%." + totalPrecision + "f", totalAmount/(periodCount == 0 ? 1 : periodCount));
        }
        public String getSummaryItemTitle() {
            return summaryItemTitle;
        }
    }

    // This is the Adapter being used to display the list's data.
    private SimpleCursorAdapter mAdapter;
    // This specifies which averaging period to use.
    private String formatsIndex = KEY_DAY;
    private String currentAccount = "";
    private float totalAmount = (float) 0.0;

    private String[] accountArray = {};
    private HashMap<String,Formatter> formats = new HashMap<String,Formatter>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Set a custom title view
        // ref: http://stackoverflow.com/questions/10189037/android-custom-title-how-to-follow-the-system-theme
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.totals_manager);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_totals);

        accountArray = getResources().getStringArray(R.array.accounts_array);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
            currentAccount = extras.getString("account");

        // Can't calculate these two until the current account is set.
        long dayCount = getDayCount();
        totalAmount = getTotalAmount(this, currentAccount);

        formats.put(KEY_DAY, new Formatter("Daily", "day", DAYS_PER_DAY, "0", dayCount, 1));
        formats.put(KEY_WEEK, new Formatter("Weekly", "week", DAYS_PER_WEEK, "1", dayCount, 1));
        formats.put(KEY_MONTH, new Formatter("Monthly", "month", DAYS_PER_MONTH, "1", dayCount, 0));
        formats.put(KEY_TOTAL, new Formatter("Totals", "day", dayCount, "0", dayCount, 0));

        // Buttons to change the average period.
        findViewById(R.id.totalByDayButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                formatsIndex = KEY_DAY;
                setActivityTitle();
                getSupportLoaderManager().restartLoader(0, null, SpendTotals.this);
            }
        });
        findViewById(R.id.totalByWeekButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                formatsIndex = KEY_WEEK;
                setActivityTitle();
                getSupportLoaderManager().restartLoader(0, null, SpendTotals.this);
            }
        });
        findViewById(R.id.totalByMonthButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                formatsIndex = KEY_MONTH;
                setActivityTitle();
                getSupportLoaderManager().restartLoader(0, null, SpendTotals.this);
            }
        });
        findViewById(R.id.totalCompleteButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                formatsIndex = KEY_TOTAL;
                setActivityTitle();
                getSupportLoaderManager().restartLoader(0, null, SpendTotals.this);
            }
        });

        ListView mSpendList = (ListView) findViewById(R.id.totalsList);
        mAdapter = new SimpleCursorAdapter(this, R.layout.totals_entry, null, fields, itemIds, 0);
        mSpendList.setAdapter(mAdapter);

        formatsIndex = KEY_TOTAL;
        setActivityTitle();

        getSupportLoaderManager().initLoader(0, null, this);		// Assigns loader callbacks here.

        // Register for long click on the title - changes the account.
        findViewById(R.id.titleTotalTypeText).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(SpendTotals.this)
                        .setTitle("Select the Account")
                        .setItems(getAccountNamesWithTotals(SpendTotals.this, accountArray), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int item) {
                                // Selected was items[item]
                                currentAccount = accountArray[item];
                                totalAmount = getTotalAmount(SpendTotals.this, currentAccount);
                                setActivityTitle();
                                getSupportLoaderManager().restartLoader(0, null, SpendTotals.this);
                            }
                        }).show();
            }
        });


    }

    private long getDayCount() {
        // Determine how many days are in the complete period.
        // Assume that the items in the list are ordered chronologically, take first and last.
        String date1 = null;
        String date2 = null;
        Uri spendsUri = SpendProviderMetaData.SpendsTableMetaData.SPEND_CONTENT_URI;

        String where = "WHERE " + SpendsTableMetaData.SPEND_ACCOUNT + " ='" + currentAccount + "'";

        Cursor cursor = managedQuery(spendsUri,
                new String[] {SpendProviderMetaData.SpendsTableMetaData.SPEND_DATE},
                " _id in (select min(_id) from spends " + where + " union select max(_id) from spends " + where + " )",
                null, null);
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            int index = cursor.getColumnIndex(SpendProviderMetaData.SpendsTableMetaData.SPEND_DATE);
            String value = cursor.getString(index);
            Log.v(TAG, "value=" + value);
            if (TextUtils.isEmpty(date1))
                date1 = value;
            else if (TextUtils.isEmpty(date2))
                date2 = value;
        }
        long dayCount = 0;
        if (!TextUtils.isEmpty(date1) && !TextUtils.isEmpty(date2))
            dayCount =  Math.abs(getTimeInMillis(date1) - getTimeInMillis(date2)) / (24 * 60 * 60 * 1000) + 1;
        Log.v(TAG, "dayCount:" + dayCount);
        return dayCount;
    }

    public static String[] getAccountNamesWithTotals(Activity activity, String[] accountNames) {
        String[] tempAccount = new String[accountNames.length];
        float[] amounts = new float[accountNames.length];
        float maxAmount = 0;
        float totalSpend = 0;
        for (int i = 0; i < accountNames.length; i++) {
            String account = accountNames[i];
            if ("DAVID".equals(account)) {
                tempAccount[i] = account;
            } else {
                amounts[i] = getTotalAmount(activity, account);
                totalSpend += amounts[i];
                tempAccount[i] = String.format("%s: %.0f", account, amounts[i]);
                if (maxAmount < amounts[i]) {
                    maxAmount = amounts[i];
                }
            }
        }
        for (int i = 0; i < accountNames.length; i++) {
            String account = accountNames[i];
            if ("DAVID".equals(account)) {
                float pendingSpend = (accountNames.length - 1)*maxAmount - totalSpend;
                tempAccount[i] = String.format("%s ++%.0f", tempAccount[i], pendingSpend);
            } else {
                tempAccount[i] = String.format("%s +%.0f", tempAccount[i], maxAmount - amounts[i]);
            }
        }
        return tempAccount;
    }

    public static float getTotalAmount(Activity activity, String accountName) {
        // Query for the total amount.
        Uri spendsUri = SpendProviderMetaData.SpendsTableMetaData.SPEND_CONTENT_URI;
        Cursor cursor = activity.managedQuery(spendsUri, new String[]{String.format("TOTAL(%s) as total_amount", SpendsTableMetaData.SPEND_AMOUNT)},
                SpendsTableMetaData.SPEND_ACCOUNT + " ='" + accountName + "'", null, null);
        float totalAmount = 0;
        if (cursor.moveToFirst()) {
            int index = cursor.getColumnIndex("total_amount");
            String value = cursor.getString(index);
            totalAmount = Float.parseFloat(value);
        }
        return totalAmount;
    }

    private void setActivityTitle() {
        // formatsIndex is the period type; total, month, week, day
        TextView typeView = (TextView)findViewById(R.id.titleTotalTypeText);
        TextView amountView = (TextView)findViewById(R.id.titleTotalAmountText);

        Log.v(TAG, "formatsIndex:" + formatsIndex + " --> " + formats.get(formatsIndex));

        typeView.setText(formats.get(formatsIndex).getFullTitle(currentAccount));
        amountView.setText(formats.get(formatsIndex).getTotalAmountPerPeriod());
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.
        Uri uri = SpendProviderMetaData.SpendsTableMetaData.SPEND_CONTENT_URI;
        uri = Uri.withAppendedPath(uri, "groupBy");
        uri = Uri.withAppendedPath(uri, SpendsTableMetaData.SPEND_TYPE);
        return new CursorLoader(this, uri, formats.get(formatsIndex).getMap(),
                SpendsTableMetaData.SPEND_ACCOUNT + " ='" + currentAccount + "'",
                null, SpendProviderMetaData.SpendsTableMetaData.SPEND_MAX_SORT_ORDER);
        // ref: http://developer.android.com/reference/android/content/ContentResolver.html#query%28android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String%29
        // The above explains the meanings of the parameters.
    }


    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in.  (The framework will take care of closing the
        // old cursor once we return.)

        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    static Calendar cal = Calendar.getInstance();

    // dateText is formatted like "10 MAY 2012" or like "2012-05-25"
    public static long getTimeInMillis(String dateText) {
        if (dateText == null)
            return 0;

        int month;	// jan = 0.
        String[] parts;
        if (dateText.contains("-")) {
            parts = dateText.split("-");
            // Swap year and month parts;
            String temp = parts[0];
            parts[0] = parts[2];
            parts[2] = temp;
            month = Integer.parseInt(parts[1]) - 1;
        } else {
            parts = dateText.split(" ");
            for (month = 0; month < 12; month++) {
                if (SpendAdder.monthNames[month].equalsIgnoreCase(parts[1]))
                    break;
            }
        }
        int day = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[2]);
        cal.set(year, month, day);
        return cal.getTimeInMillis();
    }

}

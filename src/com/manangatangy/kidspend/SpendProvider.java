package com.manangatangy.kidspend;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 24/11/14
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.manangatangy.kidspend.SpendProviderMetaData.SpendsTableMetaData;

public class SpendProvider extends ContentProvider {

    public static final String TAG = "SpendProvider";

    private static HashMap<String, String> spendProjectionMap;
    static {
        spendProjectionMap = new HashMap<String, String>();
        spendProjectionMap.put(SpendsTableMetaData._ID, SpendsTableMetaData._ID);
        spendProjectionMap.put(SpendsTableMetaData.SPEND_DATE, SpendsTableMetaData.SPEND_DATE);
        spendProjectionMap.put(SpendsTableMetaData.SPEND_TYPE, SpendsTableMetaData.SPEND_TYPE);
        spendProjectionMap.put(SpendsTableMetaData.SPEND_AMOUNT, SpendsTableMetaData.SPEND_AMOUNT);
        spendProjectionMap.put(SpendsTableMetaData.SPEND_ACCOUNT, SpendsTableMetaData.SPEND_ACCOUNT);
    }
    private static HashMap<String, String> repeatProjectionMap;
    static {
        repeatProjectionMap = new HashMap<String, String>();
        repeatProjectionMap.put(SpendsTableMetaData._ID, SpendsTableMetaData._ID);
        repeatProjectionMap.put(SpendsTableMetaData.REPEAT_NEXTDATE, SpendsTableMetaData.REPEAT_NEXTDATE);
        repeatProjectionMap.put(SpendsTableMetaData.REPEAT_TYPE, SpendsTableMetaData.REPEAT_TYPE);
        repeatProjectionMap.put(SpendsTableMetaData.REPEAT_AMOUNT, SpendsTableMetaData.REPEAT_AMOUNT);
        repeatProjectionMap.put(SpendsTableMetaData.REPEAT_ACCOUNT, SpendsTableMetaData.REPEAT_ACCOUNT);
        repeatProjectionMap.put(SpendsTableMetaData.REPEAT_PERIOD, SpendsTableMetaData.REPEAT_PERIOD);
    }

    private static final UriMatcher uriMatcher;
    private static final int SPEND_URI_INDICATOR_COLLECTION = 1;
    private static final int SPEND_URI_INDICATOR_SINGLE = 2;
    private static final int SPEND_URI_INDICATOR_COLLECTION_GROUP_BY = 3;
    private static final int REPEAT_URI_INDICATOR_COLLECTION = 4;
    private static final int REPEAT_URI_INDICATOR_SINGLE = 5;
    private static final int REPEAT_URI_INDICATOR_MODIFY_NEXT_DATE = 6;
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(SpendProviderMetaData.AUTHORITY, "spends", SPEND_URI_INDICATOR_COLLECTION);
        uriMatcher.addURI(SpendProviderMetaData.AUTHORITY, "spends/#", SPEND_URI_INDICATOR_SINGLE);
        uriMatcher.addURI(SpendProviderMetaData.AUTHORITY, "spends/groupBy/*", SPEND_URI_INDICATOR_COLLECTION_GROUP_BY);		// * = group by column
        uriMatcher.addURI(SpendProviderMetaData.AUTHORITY, "repeats", REPEAT_URI_INDICATOR_COLLECTION);
        uriMatcher.addURI(SpendProviderMetaData.AUTHORITY, "repeats/#", REPEAT_URI_INDICATOR_SINGLE);
        // For the modify_next_date uri, it is expected that the _ID is specified in the where/whereArgs parameter
        // and the uri looks something like "repeats/modifyNextDate/{DAY,WEEK,F-NIGHT,MONTH}"
        uriMatcher.addURI(SpendProviderMetaData.AUTHORITY, "repeats/modifyNextDate/*", REPEAT_URI_INDICATOR_MODIFY_NEXT_DATE);
    }

    @Override
    public String getType(Uri uri) {
        switch(uriMatcher.match(uri)) {
            case SPEND_URI_INDICATOR_COLLECTION:
                return SpendsTableMetaData.SPEND_CONTENT_TYPE;
            case SPEND_URI_INDICATOR_SINGLE:
                return SpendsTableMetaData.SPEND_CONTENT_ITEM_TYPE;
            case SPEND_URI_INDICATOR_COLLECTION_GROUP_BY:
                return SpendsTableMetaData.SPEND_CONTENT_TYPE;
            case REPEAT_URI_INDICATOR_COLLECTION:
                return SpendsTableMetaData.REPEAT_CONTENT_TYPE;
            case REPEAT_URI_INDICATOR_SINGLE:
                return SpendsTableMetaData.REPEAT_CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, SpendProviderMetaData.DATABASE_NAME, null, SpendProviderMetaData.DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + SpendsTableMetaData.SPEND_TABLE_NAME + " ("
                    + SpendsTableMetaData._ID + " INTEGER PRIMARY KEY,"
                    + SpendsTableMetaData.SPEND_DATE + " TEXT,"
                    + SpendsTableMetaData.SPEND_TYPE + " TEXT,"
                    + SpendsTableMetaData.SPEND_ACCOUNT + " TEXT,"
                    + SpendsTableMetaData.SPEND_AMOUNT + " INTEGER" + ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + SpendsTableMetaData.REPEAT_TABLE_NAME + " ("
                    + SpendsTableMetaData._ID + " INTEGER PRIMARY KEY,"
                    + SpendsTableMetaData.REPEAT_NEXTDATE + " TEXT,"
                    + SpendsTableMetaData.REPEAT_TYPE + " TEXT,"
                    + SpendsTableMetaData.REPEAT_ACCOUNT + " TEXT,"
                    + SpendsTableMetaData.REPEAT_PERIOD + " TEXT,"
                    + SpendsTableMetaData.REPEAT_AMOUNT + " INTEGER" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "upgrading database from version " + oldVersion + " to " + newVersion);
            if (oldVersion == 1 && newVersion == 2) {
                db.execSQL("ALTER TABLE " + SpendsTableMetaData.SPEND_TABLE_NAME + " ADD COLUMN " + SpendsTableMetaData.SPEND_ACCOUNT + " TEXT");
                db.execSQL("UPDATE " + SpendsTableMetaData.SPEND_TABLE_NAME + " SET " + SpendsTableMetaData.SPEND_ACCOUNT + " = 'PERSONAL'");
            }
            //db.execSQL("DROP TABLE IF EXISTS " + SpendsTableMetaData.TABLE_NAME);
            onCreate(db);
        }
    }

    private DatabaseHelper openHelper;

    @Override
    public boolean onCreate() {
        openHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        String groupBy = null;
        switch(uriMatcher.match(uri)) {
            case SPEND_URI_INDICATOR_SINGLE:
                qb.setTables(SpendsTableMetaData.SPEND_TABLE_NAME);
                qb.setProjectionMap(spendProjectionMap);
                qb.appendWhere(SpendsTableMetaData._ID + "=" + uri.getPathSegments().get(1));
                break;
            case SPEND_URI_INDICATOR_COLLECTION:
                qb.setTables(SpendsTableMetaData.SPEND_TABLE_NAME);
                qb.setProjectionMap(spendProjectionMap);
                break;
            case SPEND_URI_INDICATOR_COLLECTION_GROUP_BY:
                qb.setTables(SpendsTableMetaData.SPEND_TABLE_NAME);
                qb.setProjectionMap(spendProjectionMap);
                groupBy = uri.getPathSegments().get(2);
                break;
            case REPEAT_URI_INDICATOR_SINGLE:
                qb.setTables(SpendsTableMetaData.REPEAT_TABLE_NAME);
                qb.setProjectionMap(repeatProjectionMap);
                qb.appendWhere(SpendsTableMetaData._ID + "=" + uri.getPathSegments().get(1));
                break;
            case REPEAT_URI_INDICATOR_COLLECTION:
                qb.setTables(SpendsTableMetaData.REPEAT_TABLE_NAME);
                qb.setProjectionMap(repeatProjectionMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (TextUtils.isEmpty(sortOrder))
            sortOrder = SpendsTableMetaData.DEFAULT_SORT_ORDER;
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, sortOrder);
        int i = c.getCount();
        Log.w(TAG, "selection:" + selection + ", count=" + i);
        // Tell cursor which uri to watch, so it knows when its source data changes.
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int match = uriMatcher.match(uri);
        String tableName;
        Uri tableUri;

        if (match == SPEND_URI_INDICATOR_COLLECTION) {
            // Ensure that all the fields are set.
            checkFieldIsPresent(values, SpendsTableMetaData.SPEND_DATE, uri);
            checkFieldIsPresent(values, SpendsTableMetaData.SPEND_TYPE, uri);
            checkFieldIsPresent(values, SpendsTableMetaData.SPEND_AMOUNT, uri);
            checkFieldIsPresent(values, SpendsTableMetaData.SPEND_ACCOUNT, uri);
            tableName = SpendsTableMetaData.SPEND_TABLE_NAME;
            tableUri = SpendsTableMetaData.SPEND_CONTENT_URI;
        } else if (match == REPEAT_URI_INDICATOR_COLLECTION) {
            checkFieldIsPresent(values, SpendsTableMetaData.REPEAT_NEXTDATE, uri);
            checkFieldIsPresent(values, SpendsTableMetaData.REPEAT_TYPE, uri);
            checkFieldIsPresent(values, SpendsTableMetaData.REPEAT_AMOUNT, uri);
            checkFieldIsPresent(values, SpendsTableMetaData.REPEAT_ACCOUNT, uri);
            checkFieldIsPresent(values, SpendsTableMetaData.REPEAT_PERIOD, uri);
            tableName = SpendsTableMetaData.REPEAT_TABLE_NAME;
            tableUri = SpendsTableMetaData.REPEAT_CONTENT_URI;
        } else
            throw new IllegalArgumentException("Unknown URI " + uri);

        SQLiteDatabase db = openHelper.getWritableDatabase();
        long rowId = db.insert(tableName, null, values);
        if (rowId <= 0)
            throw new SQLException("Failed to insert row into " + uri);

        //Log.v(TAG, "inserted: _ID=" + rowId);
        Uri insertedUri = ContentUris.withAppendedId(tableUri, rowId);
        getContext().getContentResolver().notifyChange(insertedUri, null);
        return insertedUri;
    }

    private void checkFieldIsPresent(ContentValues values, String field, Uri uri) {
        if (values.containsKey(field) == false)
            throw new SQLException("Failed to insert row because " + field + " date is needed" + uri);
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count;
        switch(uriMatcher.match(uri)) {
            case SPEND_URI_INDICATOR_COLLECTION:
                count = db.update(SpendsTableMetaData.SPEND_TABLE_NAME, values, where, whereArgs);
                //Log.v(TAG, "updated: count=" + count);
                break;
            case SPEND_URI_INDICATOR_SINGLE:
                String rowId = uri.getPathSegments().get(1);
                count = db.update(SpendsTableMetaData.SPEND_TABLE_NAME, values, whereClause(rowId, where), whereArgs);
                //Log.v(TAG, "updated: _ID=" + rowId);
                break;
            case REPEAT_URI_INDICATOR_SINGLE:
                String repeatId = uri.getPathSegments().get(1);
                count = db.update(SpendsTableMetaData.REPEAT_TABLE_NAME, values, whereClause(repeatId, where), whereArgs);
                //Log.v(TAG, "updated: _ID=" + rowId);
                break;
            case REPEAT_URI_INDICATOR_MODIFY_NEXT_DATE:
                // uri looks something like "repeats/modifyNextDate/{DAY,WEEK,F-NIGHT,MONTH}"
                // The reason this update is handled differently to the others is because db.update(.. ContentValues ..)
                // can't perform expression evaluation for the updated value.  Have to use db.execSQL instead.
                String period = uri.getPathSegments().get(2);
                String modifier = "";
                if (period.equalsIgnoreCase("DAY"))
                    modifier = "+1 days";
                else if (period.equalsIgnoreCase("WEEK"))
                    modifier = "+7 days";
                else if (period.equalsIgnoreCase("FNIGHT"))
                    modifier = "+14 days";
                else if (period.equalsIgnoreCase("MONTH"))
                    modifier = "+1 months";
                else
                    throw new IllegalArgumentException("Unknown URI(modifier) " + uri);
                String sql = String.format("UPDATE %s SET %s = DATE(%s, '%s') WHERE %s",
                        SpendsTableMetaData.REPEAT_TABLE_NAME,
                        SpendsTableMetaData.REPEAT_NEXTDATE,
                        SpendsTableMetaData.REPEAT_NEXTDATE,
                        modifier, where);
                Log.v(TAG, "updating: " + sql);
                db.execSQL(sql);
                count = 1;
                break;
            default:
                return 0;		// temp
            //throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        int count;
        switch(uriMatcher.match(uri)) {
            case SPEND_URI_INDICATOR_COLLECTION:
                count = db.delete(SpendsTableMetaData.SPEND_TABLE_NAME, where, whereArgs);
                //Log.v(TAG, "deleted: count=" + count);
                break;
            case SPEND_URI_INDICATOR_SINGLE:
                String rowId1 = uri.getPathSegments().get(1);
                count = db.delete(SpendsTableMetaData.SPEND_TABLE_NAME, whereClause(rowId1, where), whereArgs);
                //Log.v(TAG, "deleted: _ID=" + rowId1);
                break;
            case REPEAT_URI_INDICATOR_COLLECTION:
                count = db.delete(SpendsTableMetaData.REPEAT_TABLE_NAME, where, whereArgs);
                //Log.v(TAG, "deleted: count=" + count);
                break;
            case REPEAT_URI_INDICATOR_SINGLE:
                String rowId2 = uri.getPathSegments().get(1);
                count = db.delete(SpendsTableMetaData.REPEAT_TABLE_NAME, whereClause(rowId2, where), whereArgs);
                //Log.v(TAG, "deleted: _ID=" + rowId2);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    private String whereClause(String rowId, String where) {
        return SpendsTableMetaData._ID + "=" + rowId + (TextUtils.isEmpty(where) ? "" : " AND (" + where + ")");
    }
}

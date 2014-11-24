package com.manangatangy.kidspend;

/**
 * Created with IntelliJ IDEA.
 * User: david
 * Date: 24/11/14
 * Time: 3:05 PM
 * To change this template use File | Settings | File Templates.
 */
import android.net.Uri;

import android.provider.BaseColumns;

public class SpendProviderMetaData {

    public static final String AUTHORITY = "com.manangatangy.kidspend.SpendProvider";
    public static final String DATABASE_NAME = "spend.db";
    public static final int DATABASE_VERSION = 3;
    //public static final String SPENDS_TABLE_NAME = "spends";

    private SpendProviderMetaData() {}

    /**
     * Provides content relating to spend records and also to repeated records
     */
    public static final class SpendsTableMetaData implements BaseColumns {
        private SpendsTableMetaData() {}

        public static final String SPEND_TABLE_NAME = "spends";
        public static final Uri SPEND_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + SPEND_TABLE_NAME);
        public static final String SPEND_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.expenditure.spend";
        public static final String SPEND_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.expenditure.spend";
        public static final String DEFAULT_SORT_ORDER = "_ID ASC";
        public static final String SPEND_MAX_SORT_ORDER = "amount DESC";
        public static final String SPEND_ACCOUNT = "account";
        public static final String SPEND_DATE = "created";
        public static final String SPEND_TYPE = "type";
        public static final String SPEND_AMOUNT = "amount";

        public static final String REPEAT_TABLE_NAME = "repeats";
        public static final Uri REPEAT_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + REPEAT_TABLE_NAME);
        public static final String REPEAT_CONTENT_TYPE = "vnd.android.cursor.dir/vnd.expenditure.repeat";
        public static final String REPEAT_CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.expenditure.repeat";
        public static final String REPEAT_ACCOUNT = "account";
        public static final String REPEAT_NEXTDATE = "next_date";
        public static final String REPEAT_TYPE = "type";
        public static final String REPEAT_AMOUNT = "amount";
        public static final String REPEAT_PERIOD = "period";
    }
}
package ir.paadino.scheduling.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by r.kiani on 05/14/2015.
 */
public class DbHelper extends SQLiteOpenHelper {
    public DbHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    private static DbHelper mInstance = null;
    // database version
    private static final int database_VERSION = 8;
    // database name
    private static final String database_NAME = "data";

    private SQLiteDatabase db;

    public static final String table_sources = "sources";
    public static final String sources_id = "_id";
    public static final String sources_plate = "plate_no";
    public static final String sources_date = "date";
    public static final String sources_slump = "slump";
    public static final String sources_volume = "volume";
    public static final String sources_password = "password";
    public static final String sources_trnas_code = "trans_code";
    public static final String sources_sensor = "sensor";
    public static final String sources_start_time = "start_time";
    private static final String[] col_sources = {
            sources_id,
            sources_plate,
            sources_date,
            sources_slump,
            sources_volume,
            sources_password,
            sources_trnas_code,
            sources_sensor,
            sources_start_time
    };

    public static final String table_destinations = "destinations";
    public static final String destinations_id = "_id";
    public static final String destinations_plate = "plate_no";
    public static final String destinations_date = "date";
    public static final String destinations_dest_date = "dest_date";
    public static final String destinations_slump = "slump";
    public static final String destinations_volume = "volume";
    public static final String destinations_total_rounds = "total_rounds";
    public static final String destinations_report = "report";
    public static final String destinations_trnas_code = "trans_code";

    private static final String[] col_destinations = {
            destinations_id,
            destinations_plate,
            destinations_date,
            destinations_dest_date,
            destinations_slump,
            destinations_volume,
            destinations_total_rounds,
            destinations_report,
            destinations_trnas_code};


    public static DbHelper getInstance(Context context)
    {
        if(mInstance == null){
            mInstance = new DbHelper(context);
        }
        return mInstance;
    }

    public DbHelper(Context context) {
        super(context, database_NAME, null, database_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        String create_sources_table = "CREATE TABLE " + table_sources + "(" +
                sources_id + " INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                sources_plate  + " TEXT,"+
                sources_date  + " TEXT,"+
                sources_start_time  + " TEXT,"+
                sources_slump  + " INTEGER," +
                sources_volume  + " INTEGER," +
                sources_password  + " TEXT," +
                sources_sensor  + " INTEGER," +
                sources_trnas_code + " TEXT)";

        String create_destinations_table = "CREATE TABLE " + table_destinations + "(" +
                destinations_id + " INTEGER  PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                destinations_plate  + " TEXT,"+
                destinations_date  + " TEXT,"+
                destinations_dest_date  + " TEXT,"+
                destinations_slump  + " INTEGER, " +
                destinations_volume  + " INTEGER, " +
                destinations_total_rounds  + " INTEGER, " +
                destinations_report  + " TEXT, " +
                destinations_trnas_code  + " TEXT )";

        db.execSQL(create_sources_table);
        db.execSQL(create_destinations_table);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i2) {
        db.execSQL("DROP TABLE IF EXISTS " + table_sources);
        db.execSQL("DROP TABLE IF EXISTS " + table_destinations);
        this.onCreate(db);
    }

}

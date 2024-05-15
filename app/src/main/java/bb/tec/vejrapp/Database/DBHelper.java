package bb.tec.vejrapp.Database;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import bb.tec.vejrapp.R;

public class DBHelper extends SQLiteOpenHelper {
    private Resources mResources;
    private static final String DATABASE_NAME = "city.db";
    private static final int DATABASE_VERSION = 1;
    SQLiteDatabase db;


    public DBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        mResources = context.getResources();

        db = this.getWritableDatabase();

        onCreate(db);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {

        // drops table
        final String SQL_DROP_CITIES_TABLE = "DROP TABLE IF EXISTS " + DBContract.MenuEntry.TABLE_NAME + ";";

        // creates table
        final String SQL_CREATE_CITIES_TABLE = "CREATE TABLE IF NOT EXISTS " + DBContract.MenuEntry.TABLE_NAME + " (" +
                DBContract.MenuEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                DBContract.MenuEntry.CITY + " TEXT, " +
                DBContract.MenuEntry.LAT + " NUMERIC, " +
                DBContract.MenuEntry.LNG + " NUMERIC, " +
                DBContract.MenuEntry.COUNTRY + " TEXT, " +
                DBContract.MenuEntry.ISO2 + " TEXT, " +
                DBContract.MenuEntry.ADMIN_NAME + " TEXT, " +
                DBContract.MenuEntry.CAPITAL + " TEXT, " +
                DBContract.MenuEntry.POPULATION + " INTEGER, " +
                DBContract.MenuEntry.POPULATION_PROPER + " INTEGER" + " );";

        // execute queries
        db.execSQL(SQL_DROP_CITIES_TABLE);
        db.execSQL(SQL_CREATE_CITIES_TABLE);

        try {
            readDataToDb(db);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void readDataToDb(SQLiteDatabase db) throws IOException, JSONException{

        String jsonDataString = readJsonDataFromFile();
        JSONArray menuItemsJsonArray = new JSONArray(jsonDataString);

        // puts JSON into db
        try {
            for(int i = 0; i < menuItemsJsonArray.length(); ++i){
                JSONObject menuItemObject = menuItemsJsonArray.getJSONObject(i);

                ContentValues menuValues = new ContentValues();

                menuValues.put(DBContract.MenuEntry.CITY, menuItemObject.getString("city"));
                menuValues.put(DBContract.MenuEntry.LAT, menuItemObject.getString("lat"));
                menuValues.put(DBContract.MenuEntry.LNG, menuItemObject.getString("lng"));
                menuValues.put(DBContract.MenuEntry.COUNTRY, menuItemObject.getString("country"));
                menuValues.put(DBContract.MenuEntry.ISO2, menuItemObject.getString("iso2"));
                menuValues.put(DBContract.MenuEntry.ADMIN_NAME, menuItemObject.getString("admin_name"));
                menuValues.put(DBContract.MenuEntry.CAPITAL, menuItemObject.getString("capital"));
                menuValues.put(DBContract.MenuEntry.POPULATION, menuItemObject.getString("population"));
                menuValues.put(DBContract.MenuEntry.POPULATION_PROPER, menuItemObject.getString("population_proper"));

                db.insert(DBContract.MenuEntry.TABLE_NAME, null, menuValues);
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    private String readJsonDataFromFile() throws IOException{
        InputStream inputStream = null;
        StringBuilder builder = new StringBuilder();

        // gets document with JSON data
        try {
            String jsonDataString = null;
            inputStream = mResources.openRawResource(R.raw.dk);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"));
            while((jsonDataString = bufferedReader.readLine()) != null){
                builder.append(jsonDataString);
            }

        } finally {
            if(inputStream != null){
                inputStream.close();
            }
        }
        return new String(builder);
    }

}

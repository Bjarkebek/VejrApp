package bb.tec.vejrapp.Database;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class DBContract {

    public static final class MenuEntry implements BaseColumns{

        public static final String TABLE_NAME = "cities";
        public static final String CITY = "city";
        public static final String LAT = "lat";
        public static final String LNG = "lng";
        public static final String COUNTRY = "country";
        public static final String ISO2 = "iso2";
        public static final String ADMIN_NAME = "admin_name";
        public static final String CAPITAL = "capital";
        public static final String POPULATION = "population";
        public static final String POPULATION_PROPER = "population_proper";
    }
}

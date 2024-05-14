package bb.tec.vejrapp;

import android.annotation.SuppressLint;
import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConnectionHelper {
    Connection con;
    private static String
            server = "LAPTOP-GNCUVPL0\\MSSQLSERVER2022",
    ip = "192.168.0.222",
    port ="1433",
            db = "VejrAppDB",
            username = "user",
            password = "Passw0rd";

    @SuppressLint("NewApi")
    public static Connection connectToDb() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Connection connection=null;

        try {
            // SET CONNECTIONSTRING
            Class.forName("net.sourceforge.jtds.jdbc.Driver").newInstance();
            String conString = "jdbc:jtds:sqlserver://10.131.203.78:1433" + "/" + db + ";user=" + username + ";password=" + password + ";";
//            String conString = "jdbc:jtds:sqlserver://" + server + "/" + db + ";user=" + username + ";password=" + password + ";";
            connection = DriverManager.getConnection(conString);
        } catch (Exception e) {
            Log.e("Connection error: ", e.getMessage());
        }
        return connection;
    }


}

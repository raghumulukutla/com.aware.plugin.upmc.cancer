package com.aware.plugin.upmc.dash.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.aware.plugin.upmc.dash.utils.Constants;
import com.aware.plugin.upmc.dash.utils.DBUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.aware.plugin.upmc.dash.utils.Constants.DB_URL;
import static com.aware.plugin.upmc.dash.utils.Constants.PASS;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_INTERVENTIONS;
import static com.aware.plugin.upmc.dash.utils.Constants.TABLE_SENSOR_DATA;
import static com.aware.plugin.upmc.dash.utils.Constants.USER;

public class LocalDBWorker extends Worker {

    public LocalDBWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(Constants.TAG, "LocalDBWorker: Starting work...");
        Connection conn = null;
        Statement stmt = null;
        try {
            //Register JDBC driver
            Class.forName("com.mysql.jdbc.Driver");
            // 1. Sync Step Count
            //Open a connection
            int counter = 0;
            Log.d(Constants.TAG, "LocalDBWorker: Connecting to database [SensorData]");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM ");
            sql.append(TABLE_SENSOR_DATA);
            ResultSet rs = stmt.executeQuery(sql.toString());
            while (rs.next()) {
                double timeStamp = rs.getDouble("timestamp");
                int type = rs.getInt("type");
                int data = rs.getInt("data");
                String sessionId = rs.getString("session_id");
                DBUtils.saveSensor(getApplicationContext(), timeStamp, type, data, sessionId);
                counter++;
            }
            Log.d(Constants.TAG, "LocalDBWorker: Synced " + counter + " records [SensorData]");
            // 2. drop step count table
            //After syncing all the data records, clear the table
            String command = "DROP TABLE SensorData";
            stmt.executeUpdate(command);
            Log.d(Constants.TAG, "LocalDBWorker: Dropped table [SensorData]");
            command =
                    "CREATE TABLE SensorData " + "(timestamp double not NULL, " + " session_id " + "varchar(255) NULL, " + " type int(11) not NULL, " + " data int(11) " + "not NULL)";
            stmt.executeUpdate(command);
            Log.d(Constants.TAG, "LocalDBWorker:Reset SensorData table");
            // 3. Sync Interventions Count
            Log.d(Constants.TAG, "LocalDBWorker: Connecting to database [interventions_watch]");
            sql = new StringBuilder();
            sql.append("SELECT * FROM ");
            sql.append(TABLE_INTERVENTIONS);
            rs = stmt.executeQuery(sql.toString());
            counter = 0;
            while (rs.next()) {
                double timeStamp = rs.getDouble("timestamp");
                int type = rs.getInt("notif_type");
                int snooze_shown = Constants.SNOOZE_NOT_SHOWN;
                String session_id = rs.getString("session_id");
                if (type == 0) {
                    type = Constants.NOTIF_TYPE_APPRAISAL;
                } else if (type == 1) {
                    type = Constants.NOTIF_TYPE_INACTIVITY;
                    snooze_shown = Constants.SNOOZE_SHOWN;
                } else if (type == 2) {
                    type = Constants.NOTIF_TYPE_INACTIVITY;
                } else if (type == 3) {
                    type = Constants.NOTIF_TYPE_BATT_LOW;
                } else {
                    Log.d(Constants.TAG, "LocalDBWorker: Corrupt  table [interventions_watch]");
                    return Result.failure();
                }
                DBUtils.saveWIntervention(getApplicationContext(), timeStamp, session_id, type,
                        Constants.NOTIF_DEVICE_WATCH, snooze_shown);
                counter++;
            }
            Log.d(Constants.TAG,
                    "LocalDBWorker: Synced " + counter + " records [interventions_watch]");
            // 4. drop interventions table
            command = "DROP TABLE interventions_watch";
            Log.d(Constants.TAG, "LocalDBWorker: Dropping table [interventions_watch]");
            stmt.executeUpdate(command);
            command =
                    "CREATE TABLE interventions_watch " + "(id int(11) not NULL AUTO_INCREMENT, " + " timestamp double not NULL, " + " session_id varchar(255) NULL, " + " notif_type int NOT NULL, " + " PRIMARY KEY ( id ))";
            stmt.executeUpdate(command);
            // 5. Sync responses
            Log.d(Constants.TAG, "LocalDBWorker: Connecting to database [responses_watch]");
            command = "SELECT * FROM responses_watch";
            rs = stmt.executeQuery(command);
            counter = 0;
            while (rs.next()) {
                int busy = rs.getInt("busy");
                int pain = rs.getInt("pain");
                int nausea = rs.getInt("nausea");
                int tired = rs.getInt("tired");
                int other = rs.getInt("other");
                int ok = rs.getInt("ok");
                int no = rs.getInt("no");
                int snooze = rs.getInt("snooze");
                double timeStamp = rs.getDouble("timestamp");
                String sessionId = rs.getString("session_id");
                DBUtils.saveWResponseWatch(getApplicationContext(), timeStamp, sessionId, busy, pain,
                        nausea, tired, other, ok, no, snooze);
                counter++;
            }
            Log.d(Constants.TAG, "LocalDBWorker: Synced " + counter + " records [responses_watch]");
            // 6. Drop responses table
            Log.d(Constants.TAG, "LocalDBWorker:Dropping table [responses_watch]");
            command = "DROP TABLE responses_watch";
            stmt.executeUpdate(command);
            command =
                    "CREATE TABLE responses_watch " + "(id int(11) not NULL AUTO_INCREMENT, " +
                            " timestamp double not NULL, " + " session_id varchar(255) NULL, " +
                            " ok int NULL, " + " no int NULL, " + " snooze int NULL, " + " busy " + "int NULL, " + " pain int NULL, " + " nausea int NULL, " + " tired " + "int NULL, " + " other int NULL, " + " PRIMARY KEY ( id ))";
            stmt.executeUpdate(command);
            //Clean-up environment
            rs.close();
            stmt.close();
            conn.close();
        } catch (ClassNotFoundException e) {
            Log.d(Constants.TAG, "LocalDBWorker:class not found.. retrying..");
            e.printStackTrace();
            return Result.retry();
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.retry();
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException se2) {
                se2.printStackTrace();
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        return Result.success();
    }
}

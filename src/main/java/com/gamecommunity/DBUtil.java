package com.gamecommunity;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {

    private static final String URL =
            "jdbc:oracle:thin:@(description=(retry_count=20)(retry_delay=3)(address=(protocol=tcps)(port=1522)(host=adb.ap-tokyo-1.oraclecloud.com))(connect_data=(service_name=g9587fb0aea9019_gamecommunity_tp.adb.oraclecloud.com))(security=(ssl_server_dn_match=yes)))";

    private static final String USER =
            "GAMECOMMUNITY";

    private static final String PASSWORD =
            "Classkosa2188";

    public static Connection getConnection() throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
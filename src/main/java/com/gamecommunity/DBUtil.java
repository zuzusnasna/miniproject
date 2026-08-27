package com.gamecommunity;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBUtil {

    private static final String URL =
            "jdbc:oracle:thin:@//localhost:1521/freepdb1";

    private static final String USER =
            "gamecommunity";

    private static final String PASSWORD =
            "1234";

    public static Connection getConnection() throws Exception {
        Class.forName("oracle.jdbc.OracleDriver");
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
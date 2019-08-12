package com.gareeva.cloudStorage.server;

import java.sql.*;

class JdbcClass {
    static {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:server/dbCloudStorage.db");
            String SQLcreateTable = "CREATE TABLE IF NOT EXISTS 'users'" +
                    " ('id'	INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                    "'login'	TEXT NOT NULL UNIQUE,'password'	TEXT NOT NULL)";
            Statement statement = connection.createStatement();
            statement.executeUpdate(SQLcreateTable);

            String SQLCreateIndex = "CREATE INDEX IF NOT EXISTS 'user_pass_idx' ON 'users' ('login', 'password' )";
            statement.executeUpdate(SQLCreateIndex);

            connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static JdbcClass jdbc = new JdbcClass();
    private Connection connection;

    static JdbcClass getInstance() {
        return jdbc;
    }


    private JdbcClass() {
        try {
            this.connection = DriverManager.getConnection("jdbc:sqlite:server/dbCloudStorage.db");
            System.out.println("Connection with database established!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void createUser(String login, String password) throws SQLException {

        PreparedStatement insertUser = connection.prepareStatement("INSERT INTO users (login, password) VALUES (?, ?)");
        insertUser.setString(1, login);
        insertUser.setString(2, password);
        if (insertUser.executeUpdate() == 1) {
            //хорошо бы это залогировать
            //System.out.println("В БД добавлен новый пользователь : " + login);
        }
    }

    boolean authUser(String login, String password) throws SQLException {
        //Statement statement = connection.createStatement();
        PreparedStatement authStatement = connection.prepareStatement("SELECT * FROM users WHERE login = ? AND password = ?");
        authStatement.setString(1, login);
        authStatement.setString(2, password);
        ResultSet resultSet = authStatement.executeQuery();
        boolean authResult = resultSet.next();
        return authResult;
    }

    void disconnect() {
        try {
            if (!connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}

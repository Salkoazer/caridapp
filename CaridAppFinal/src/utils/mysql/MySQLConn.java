/*
 * MySQL JDBC Connection
 *
 */

package utils.mysql;

import java.sql.*;
import java.sql.Connection;
import java.util.List;

public class MySQLConn {

    private Connection connection;
    private static MySQLConn instance;

    public MySQLConn() {

    }

    /**
     * Returns the shared singleton instance.
     */
    public static MySQLConn getSharedInstance() {
        if (instance == null) {
            instance = new MySQLConn();
        }
        return instance;
    }

    /**
     * Initializes the database connection
     *
     * @param hostname The database hostname
     * @param port The database port
     * @param username The username
     * @param password The password
     * @param database The schema database
     */
    public void init(String hostname, String port, String username, String password, String database) {
        try {
            String url = "jdbc:mysql://" + hostname + ":" + port + "/" + database;
            this.connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        this.connection = null;
    }

    /**
     * Executes an INSERT, UPDATE or DELETE sql statement.
     *
     * @param sql the sql statement
     * @return either the id or 0 for statements that
     *         do not return anything
     */
    public int executeUpdate(String sql) {
        try {
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = stmt.getGeneratedKeys();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * Executes a given sql statement.
     *
     * @param sql the sql statement
     * @return DBRowList with the results
     */
    public DBRowList executeQuery(String sql) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            return new DBRowList(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Executes a prepared statement with a given query and a list of values
     *
     * @param query The query
     * @param values Array of values
     * @return DBRowList with the results
     */
    public DBRowList executeStatement(String query, List<Object> values) {
        try {
            PreparedStatement ps = this.connection.prepareStatement(query);
            for (int i = 1; i <= values.size(); i++) {
                ps.setObject(i, values.get(i - 1));
            }
            ps.execute();
            ResultSet rs = ps.getResultSet();
            return rs == null ? null : new DBRowList(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

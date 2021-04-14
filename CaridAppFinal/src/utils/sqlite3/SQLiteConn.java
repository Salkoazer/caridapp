/*
 * SQLite JDBC Connection
 *
 */

package utils.sqlite3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class SQLiteConn {

    private String filename;
    private Connection connection;
    private static SQLiteConn instance;

    public SQLiteConn() {

    }

    /**
     * Returns the shared singleton instance.
     */
    public static SQLiteConn getSharedInstance() {
        if (instance == null) {
            instance = new SQLiteConn();
        }
        return instance;
    }

    /**
     * Initializes the database connection
     *
     * @param filename: the database filename
     */
    public void init(String filename) {
        this.filename = filename;
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + filename);
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
    }

    /**
     * Deletes the database file
     */
    public void delete() {
        File file = new File(this.filename);
        file.delete();
    }

    /**
     * Recreates the database from empty state
     *
     * @param filename Script file to execute
     */
    public void recreate(String filename) {
        close();
        delete();
        init(this.filename);
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));
            executeUpdate(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            stmt.executeUpdate(sql);
            return stmt.getGeneratedKeys().getInt(1);
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
}

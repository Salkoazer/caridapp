/*
 * Represents a list of database rows
 *
 */

package utils.sqlite3;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DBRowList extends ArrayList<DBRow> {

    public DBRowList(ResultSet rs) {
        try {
            while (rs.next()) {
                DBRow row = new DBRow(rs);
                this.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Returns first element of list or none
    public DBRow first() {
        return (this.size() > 0) ? this.get(0) : null;
    }

    // Returns first element of list instantiating a class
    public <T> T first(Class<T> clazz) {
        DBRow first = first();
        if (first == null) {
            return null;
        } else {
            try {
                return (T) clazz.getDeclaredConstructor(DBRow.class).newInstance(first);
            } catch (Exception e) {
                return null;
            }
        }
    }
}

/*
 * Represents a database row
 *
 */

package utils.sqlite3;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class DBRow extends HashMap<String, Object> {

    public DBRow(ResultSet rs) {
        try {
            int columns = rs.getMetaData().getColumnCount();
            for (int i = 0; i < columns; i++) {
                String key = rs.getMetaData().getColumnName(i + 1);
                Object value = rs.getObject(i + 1);
                this.put(key, value);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

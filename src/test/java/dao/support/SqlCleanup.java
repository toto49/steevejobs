package dao.support;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlCleanup {

    void run() throws SQLException;
}

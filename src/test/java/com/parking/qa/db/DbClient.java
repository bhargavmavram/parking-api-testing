package com.parking.qa.db;

import com.parking.qa.config.TestConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DbClient {
    public boolean userExists(String username) {
        String sql = "select count(*) from users where username = ?";
        return count(sql, username) > 0;
    }

    public boolean userRoleExists(String username, String role) {
        String sql = """
                select count(*)
                from users u
                join user_roles ur on ur.user_id = u.id
                where u.username = ? and ur.role = ?
                """;
        return count(sql, username, role) > 0;
    }

    private int count(String sql, String... parameters) {
        try (Connection connection = DriverManager.getConnection(
                TestConfig.dbUrl(),
                TestConfig.dbUsername(),
                TestConfig.dbPassword());
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (int index = 0; index < parameters.length; index++) {
                statement.setString(index + 1, parameters[index]);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Database verification failed. Active environment is '" + TestConfig.environment()
                            + "' and DB URL is '" + TestConfig.dbUrl()
                            + "'. If this is aws-tunnel, start scripts/start-db-tunnel.ps1 before running @db tests.",
                    ex
            );
        }
    }
}

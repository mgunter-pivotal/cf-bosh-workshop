package org.cloudfoundry.community.servicebroker.postgresql.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PostgreSQLDatabase {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLDatabase.class);

    private static JdbcTemplate jdbcTemplate;

    private static String databaseHost;

    private static int databasePort;

    @Autowired
    public PostgreSQLDatabase(JdbcTemplate jdbcTemplate) {
        PostgreSQLDatabase.jdbcTemplate = jdbcTemplate;

        try {
            String jdbcUrl = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
            // Remove "jdbc:" prefix from the connection JDBC URL to create an URI out of it.
            String cleanJdbcUrl = jdbcUrl.replace("jdbc:", "");

            URI uri = new URI(cleanJdbcUrl);
            PostgreSQLDatabase.databaseHost = uri.getHost();
            PostgreSQLDatabase.databasePort = uri.getPort() == -1 ? 5432 : uri.getPort();
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to get DatabaseMetadata from Connection", e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to parse JDBC URI for DatabaseAdmin Connection", e);
        }
    }


    public static void executeUpdate(String query) throws SQLException {
            jdbcTemplate.update(query);
    }

    public static Map<String, Object> executeSelect(String query) throws SQLException {

            return jdbcTemplate.queryForMap(query);

    }

    public static void executePreparedUpdate(String query, Map<Integer, String> parameterMap) throws SQLException {
        if(parameterMap == null) {
            throw new IllegalStateException("parameterMap cannot be null");
        }
        jdbcTemplate.update(query,parameterMap.values());
    }

    public static List<Map<String, Object>> executePreparedSelect(String query, Object[] args) {
        return jdbcTemplate.queryForList(query, args);
    }

    public static String getDatabaseHost() {
        return databaseHost;
    }

    public static int getDatabasePort() {
        return databasePort;
    }

    private static Map<String, String> getResultMapFromResultSet(ResultSet result) throws SQLException {
        ResultSetMetaData resultMetaData = result.getMetaData();
        int columns = resultMetaData.getColumnCount();

        Map<String, String> resultMap = new HashMap<String, String>(columns);

        if(result.next()) {
            for(int i = 1; i <= columns; i++) {
                resultMap.put(resultMetaData.getColumnName(i), result.getString(i));
            }
        }

        return resultMap;
    }
}

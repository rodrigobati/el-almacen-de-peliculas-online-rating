package unrn.rating.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeycloakUserService {

    // Environment variables used by Docker Compose to locate Keycloak DB
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public KeycloakUserService() {
        this.jdbcUrl = System.getenv().getOrDefault("KEYCLOAK_DB_JDBC_URL",
                "jdbc:postgresql://keycloak-postgres:5432/keycloak");
        this.username = System.getenv().getOrDefault("KEYCLOAK_DB_USERNAME", "keycloak");
        this.password = System.getenv().getOrDefault("KEYCLOAK_DB_PASSWORD", "keycloak");
    }

    public Map<String, String> findUsernamesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty())
            return Collections.emptyMap();
        var placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        var sql = "SELECT id, username, email, first_name, last_name FROM user_entity WHERE id IN (" + placeholders
                + ")";
        var result = new HashMap<String, String>();

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
                PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            for (String id : ids)
                ps.setObject(i++, id);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var id = rs.getString("id");
                    var uname = rs.getString("username");
                    if (uname == null || uname.isBlank()) {
                        var fn = rs.getString("first_name");
                        var ln = rs.getString("last_name");
                        uname = (fn == null ? "" : fn) + (ln == null ? "" : " " + ln);
                        if (uname.isBlank())
                            uname = rs.getString("email");
                    }
                    result.put(id, uname != null ? uname : id);
                }
            }
        } catch (Exception e) {
            // Don't fail hard if Keycloak DB not reachable; return empty map.
            System.err.println("Warning: could not lookup keycloak usernames: " + e.getMessage());
        }

        return result;
    }
}

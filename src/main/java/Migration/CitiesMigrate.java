package Migration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;
import java.sql.*;

public class CitiesMigrate {
    private final String tableName;

    public CitiesMigrate(String tableName) {
        this.tableName = tableName;
    }

    public void performMigration() throws IOException {
        // Read citites
        final String dir = System.getProperty("user.dir");
        final String path = dir + "/data/cities.csv";

        try (FileInputStream stream = new FileInputStream(path)) {
            String[] strCommands = createMigration(new InputStreamReader(stream)).split("\n");
            Collection<String> commands = Arrays.asList(strCommands);

            // Loop over the commands in paralllel
            commands.parallelStream().forEach(command -> {
                // Fire the command against the DB

                try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/testprojekt3", "root",
                        "123123qwe")) {
                    try (Statement st = con.createStatement()) {
                        st.execute(command);
                        System.out.println("Executed!");
                    }
                } catch (SQLException ex) {
                    System.out.println("Could not fire \"" + command + "\" - " + ex.getMessage());
                }
            });
        }

        // System.out.println(dir);
    }

    public String createMigration(InputStreamReader readerStream) throws IOException {
        StringBuilder strBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(readerStream)) {
            String line = reader.readLine();

            while (line != null) {
                String[] parts = line.split(",");

                String id = parts[0];
                String name = parts[1];
                double latitude = Double.parseDouble(parts[2]);
                double longitude = Double.parseDouble(parts[3]);

                strBuilder.append(createSqlString(id, name, latitude, longitude) + "\n");

                line = reader.readLine();
            }
        }

        return strBuilder.toString();
    }

    public String createSqlString(String id, String name, double latitude, double longitude) {
        return "INSERT INTO " + this.tableName + " (id, name, location) VALUES (" + id + ", '"
                + name.replace("'", "\\'") + "', GeomFromText(CONCAT('POINT (', " + longitude + ", ' ', " + latitude + ", ')')));";
    }
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Migration;

/**
 *
 * @author Kasper
 */
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.*;
import org.neo4j.driver.v1.*;

public class CitiesMigrateNeo {


    public void performMigration() throws IOException {
        // Read citites
        final String dir = System.getProperty("user.dir");
        final String path = dir + "/data/cities.csv";

        try (FileInputStream stream = new FileInputStream(path)) {
            String[] strCommands = createMigration(new InputStreamReader(stream)).split("\n");
            Collection<String> commands = Arrays.asList(strCommands);
            Driver driver = GraphDatabase.driver(
                    "bolt://localhost:7687",
                    AuthTokens.basic("neo4j", "class"));
            // Loop over the commands in paralllel
            commands.parallelStream().forEach(command -> {
                // Fire the command against the DB

                try (Session session = driver.session()) {
                    StatementResult result = session.run(command);

                    System.out.println("Executed!");
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
                String name = parts[1].replace("'", "`");
                double latitude = Double.parseDouble(parts[2]);
                double longitude = Double.parseDouble(parts[3]);

                strBuilder.append(createCypherString(id, name, latitude, longitude) + "\n");

                line = reader.readLine();
            }
        }

        return strBuilder.toString();
    }

    public String createCypherString(String id, String name, double latitude, double longitude) {
        return "CREATE (c:City {id: '" + id + "', name: '" + name + "', lat: '" + latitude + "', lng: '" + longitude + "'})";
    }
}

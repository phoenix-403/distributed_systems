package app_kvServer;

import logger.LogSetup;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Persist {

    // Save data into multiple DB_FILES - 1 for each letter and 1 extra for all other
    private static final String ROOT_PATH = "ds_data";
    private static final String DB_FILE_PATH = "/db";
    private static final String DB_FILE_NAME = "data.db";
    private static final String DELIMITER = "~*~*";
    private static final String DELIMITER_PATTERN = Pattern.quote(DELIMITER);
    // logger
    private static Logger logger = LogManager.getLogger(Persist.class);
    private static volatile File dbFile;


    private Persist() {
    }


    /**
     * Initiates directory and DB_FILES on the server to allow persisting
     *
     * @param serverName
     * @return true if it server is ready to persist data, false otherwise
     */
    public static boolean init(String serverName) {

        // creating directory if needed
        File directory = new File(ROOT_PATH + serverName + DB_FILE_PATH);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                logger.error("Unable to create required directory to save DB_FILES");
                return false;
            }
        }

        // creating files if needed
        try {
            dbFile = new File(ROOT_PATH + serverName + DB_FILE_PATH + "/" + DB_FILE_NAME);
            dbFile.createNewFile();
        } catch (IOException e) {
            logger.error("Unable to create DB_FILES " + e.getMessage());
            return false;
        }
        logger.info("Server ready to persist data");
        return true;
    }


    /**
     * checks if a key exists in db;
     *
     * @return value if key-value pair is found else null
     * @throws IOException if unable to to check if key exists in db due to db DB_FILES not opening
     */
    public static boolean checkIfExists(String key) throws IOException {
        return !(read(key) == null);
    }

    /**
     * reads value from database given a key; cache get updated from cach
     *
     * @return value if key-value pair is found else null
     * @throws IOException if unable to to check if key exists in db due to db DB_FILES not opening
     */
    public static synchronized String read(String key) throws IOException {

        ArrayList<String> fileLines = (ArrayList<String>) Files.readAllLines(dbFile.toPath());
        ArrayList<String> keys = new ArrayList<>();

        for (String keyValue : fileLines) {
            keys.add(keyValue.split(DELIMITER_PATTERN)[0]);
        }

        int index = keys.indexOf(key);
        if (index != -1) {
            logger.info("Found key " + key + " in database!");
            return fileLines.get(index).split(DELIMITER_PATTERN)[1];
        }

        logger.info("key \"" + key + "\" not found in database!");
        return null;
    }

    /**
     * writes value into database given a key; updates cache as well
     *
     * @return true if it is a new value, false if it updated an existing one;
     * true if deleted false if not
     * @throws IOException if unable to to check if key exists in db due to db DB_FILES not opening
     */
    public static synchronized boolean write(String key, String value) throws IOException {
        ArrayList<String> fileLines = (ArrayList<String>) Files.readAllLines(dbFile.toPath());
        ArrayList<String> keys = new ArrayList<>();


        for (String keyValue : fileLines) {
            keys.add(keyValue.split(DELIMITER_PATTERN)[0]);
        }

        int index = keys.indexOf(key);
        // scenario1: key does not exist
        if (index == -1) {
            //1.1 should not delete a none existent value
            if (StringUtils.isEmpty(value)) {
                logger.warn("Trying to delete a non existing key");
                return false;
            }
            //1.2 write non existent key at end of file
            fileLines.add(key + DELIMITER + value);
            Files.write(dbFile.toPath(), fileLines);
            logger.info("added new key: " + key + " with value: " + value);
            Cache.updateCache(key, value);
            return true;
        }

        // scenario2: key exists
        // 2.1 delete value
        if (StringUtils.isEmpty(value)) {
            fileLines.remove(index);
            Files.write(dbFile.toPath(), fileLines);
            logger.info("deleted key: " + key);
            Cache.remove(key);
            return true;
        }
        // 2.2 modify value
        fileLines.set(index, key + DELIMITER + value);
        Files.write(dbFile.toPath(), fileLines);
        logger.info("Modified key: " + key + " with value of: " + value);
        Cache.updateCache(key, value);
        return false;
    }



    public static void clearStorage() {
        PrintWriter writer;
        try {
            writer = new PrintWriter(dbFile);
            writer.print("");
            writer.close();
        } catch (FileNotFoundException e) {
            logger.error("Unable to clear storage");
        }


        // clearing cache
        Cache.clearCache();
    }


    public static void main(String[] args) throws IOException {
        new LogSetup("logs/server/server.log", Level.ALL);
        if (init("")) {
//            System.out.println(read("hi"));
//            System.out.println(read("acd"));
//            System.out.println(read("ax~~"));
//            System.out.println(checkIfExists("ax~~"));
//            System.out.println(checkIfExists("hi"));

            write("ax~~", "hh");
            System.out.println(checkIfExists("ax~~"));

            write("ax~~", "hh");
            System.out.println(checkIfExists("ax~~"));

            write("ax~~", null);
            System.out.println(checkIfExists("ax~~"));

            write("ax~~", null);


            write("ax~~", "ssc");
            System.out.println(checkIfExists("ax~~"));

            clearStorage();

//            System.out.println(checkIfExists("ax~~"));
//
//            write("AYOLO", "kjk);
        }
    }
}

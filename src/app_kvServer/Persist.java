package app_kvServer;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Persist {

    // logger
    private static Logger logger = LogManager.getLogger(Persist.class);

    // Save data into multiple DB_FILES - 1 for each letter and 1 extra for all other
    // TODO -optional- lock one file
    private static volatile File[] DB_FILES = new File[27];
    private static final String DB_FILE_PATH = "db/";

    private static final String DELIMITER = "~*~*";
    private static final String DELIMITER_PATTERN = Pattern.quote(DELIMITER);


    private Persist() {
    }


    /**
     * Initiates directory and DB_FILES on the server to allow persisting
     *
     * @return true if it server is ready to persist data, false otherwise
     */
    public static boolean init() {
        int name = 97;

        // creating directory if needed
        File directory = new File(DB_FILE_PATH);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                logger.error("Unable to create required directory to save DB_FILES");
                return false;
            }
        }

        // creating files if needed
        try {
            int index = 0;
            for (File file : DB_FILES) {
                file = new File(DB_FILE_PATH + (name++) + ".db");
                file.createNewFile();
                DB_FILES[index++] = file;
            }
        } catch (IOException e) {
            logger.error("Unable to create DB_FILES: " + e.getMessage());
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
    public static String read(String key) throws IOException {

        ArrayList<String> fileLines = (ArrayList<String>) Files.readAllLines(getFileKeyStoredIn(key).toPath());
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
        File savedToFile = getFileKeyStoredIn(key);

        ArrayList<String> fileLines = (ArrayList<String>) Files.readAllLines(savedToFile.toPath());
        ArrayList<String> keys = new ArrayList<>();


        for (String keyValue : fileLines) {
            keys.add(keyValue.split(DELIMITER_PATTERN)[0]);
        }

        int index = keys.indexOf(key);
        // scenario1: key does not exist
        if (index == -1) {
            //1.1 should not delete a none existent value
            if (value == null) {
                logger.warn("Trying to delete a non existing key");
                return false;
            }
            //1.2 write non existent key at end of file
            fileLines.add(key + DELIMITER + value);
            Files.write(savedToFile.toPath(), fileLines);
            logger.info("added new key: " + key + " with value: " + value);
            Cache.updateCache(key, value);
            return true;
        }

        // scenario2: key exists
        // 2.1 delete value
        if (value == null) {
            fileLines.remove(index);
            Files.write(savedToFile.toPath(), fileLines);
            logger.info("deleted key: " + key);
            Cache.remove(key);
            return true;
        }
        // 2.2 modify value
        fileLines.set(index, key + DELIMITER + value);
        Files.write(savedToFile.toPath(), fileLines);
        logger.info("Modified key: " + key + " with value of: " + value);
        Cache.updateCache(key, value);
        return false;
    }


    /**
     * Initiates get file name depending on key
     *
     * @return File where key would/should be stored
     */
    private static File getFileKeyStoredIn(String key) {
        int charAscii = Character.toLowerCase(key.charAt(0));
        if (charAscii > 96 && charAscii < 123) {
            return DB_FILES[charAscii - 97];
        }
        return DB_FILES[26];
    }

    public static void clearStorage() {
        // deleting bin directory
        deleteDirectory(new File(DB_FILE_PATH));
        logger.info("Cleared Persisted files. Reinitializing it...");

        // reinitializing storage
        init();

        // clearing cache
        Cache.clearCache();
    }

    private static void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            directory.delete();
        }

    }


    public static void main(String[] args) throws IOException {
        new LogSetup("logs/server/server.log", Level.ALL);
        if (init()) {
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

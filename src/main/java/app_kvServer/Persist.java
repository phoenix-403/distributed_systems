package app_kvServer;

import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class Persist {

    // logger
    private static Logger logger = LogManager.getLogger(Persist.class);

    // Save data into multiple files - 1 for each letter and 1 extra for all other
    private final static File[] files = new File[27];
    private final static String filePath = "db/";


    private Persist() {
    }


    /**
     * Initiates directory and files on the server to allow persisting
     *
     * @return true if it server is ready to persist data, false otherwise
     */
    public static boolean init() {
        int name = 97;

        // creating directory if needed
        File directory = new File(filePath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                logger.error("Unable to create required directory to save files");
                return false;
            }
        }


        try {
            int index = 0;
            for (File file : files) {
                file = new File(filePath + (name++) + ".db");
                file.createNewFile();
                files[index++] = file;
            }
        } catch (IOException e) {
            logger.error("Unable to create files: " + e.getMessage());
            return false;
        }
        logger.info("Server ready to persist data");
        return true;
    }

    public static boolean checkIfExist() {
        // todo
        return false;
    }

    public static boolean write(String key, String value) {
        // todo
        return false;
    }

    public static String read(String key) {
        // todo
        return null;
    }

    /**
     * Initiates get file name depending on key
     *
     * @return File where key would/should be stored
     */
    private static File getFileKeyStoredIn(String key) {
        int charAscii = Character.toLowerCase(key.charAt(0));
        if (charAscii > 96 && charAscii < 123){
            return files[charAscii - 97];
        }
        return files[26];
    }


    public static void main(String[] args) throws IOException {
        new LogSetup("logs/server/server.log", Level.ALL);
        if (init()) {
            System.out.println("yay");
            System.out.println(getFileKeyStoredIn("Abc").getName());
            System.out.println(getFileKeyStoredIn("abc").getName());
            System.out.println(getFileKeyStoredIn("bcd").getName());
            System.out.println(getFileKeyStoredIn("cde").getName());

            System.out.println(getFileKeyStoredIn("~de").getName());
            System.out.println(getFileKeyStoredIn("\n").getName());
        }
    }
}

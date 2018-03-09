package common.helper;

import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.Files.setPosixFilePermissions;

public class Script {

    public static void createBashScript(String path, String content, Logger logger) {
        String scriptHeader = "#!/usr/bin/env bash";

        PrintWriter out = null;
        try {
            out = new PrintWriter(path);
            out.println(scriptHeader);
            out.println(content);
            out.flush();
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        } finally {
            if (out != null)
                out.close();
        }
    }


    public static Process runScript(String script, Logger logger) {
        // changing file permissions
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);

        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);

        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        try {
            setPosixFilePermissions(new File(script).toPath(), perms);
        } catch (IOException e) {
            logger.error("Unable to change file permissions - " + script + " might not run!");
        }


        Process process;
        try {
            Runtime runtime = Runtime.getRuntime();
            process = runtime.exec(script);

            // logging output of script
            logger.info("~~~~~" + script + " output~~~~~");
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            log(stdInput, logger);

            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            log(stdError, logger);
            logger.info("~~~~~End of " + script + " output~~~~~");


        } catch (Throwable t) {
            logger.error("Unable to run " + script);
            process = null;
        }
        return process;
    }


    private static void log(BufferedReader bufferedReader, Logger logger) throws IOException {
        if (logger != null) {
            String log;
            while ((log = bufferedReader.readLine()) != null) {
                logger.info(log);
            }
        }
    }

}

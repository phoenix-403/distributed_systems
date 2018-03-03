package common.helper;

import org.apache.log4j.Logger;

import java.io.*;

public class Script {

    public static void createBashScript(String path, String content, Logger logger){
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

    // todo add doc
    public static Process runScript(String script, Logger logger) {
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

    // todo add doc
    private static void log(BufferedReader bufferedReader, Logger logger) throws IOException {
        if (logger != null) {
            String log;
            while ((log = bufferedReader.readLine()) != null) {
                logger.info(log);
            }
        }
    }

}

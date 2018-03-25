package test.testConfigs;

import java.io.*;

public class GenerateConfig {
    public static void main(String[] args) throws IOException {
        File file = new File("./src/test/testConfigs/ecsTest"+ args[0] + ".config");
        file.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(file);

        for (int i=0; i<Integer.parseInt(args[0]); i++)
            writer.write("server" + i + " " + args[1] +" 50" + String.format("%03d", i) + "\n");
        writer.close();
    }
}

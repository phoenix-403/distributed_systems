package common.messages;

public class ErrorMessage {
    private static final String PROMPT = "EchoClient> ";


    public ErrorMessage(){

    }

    public void printLengthError(int size, int max, String arg){
        String unit;

        if (size >=1024){
            unit = (int) (size / 1024) +  "KB";
        } else
            unit = size + "B";

        if (size > max)
            printError("The length of " + arg + " cannot exceed " + max + unit);

    }

    public void printMissingArgumentsPut(int length){
        int total = 3;
        printError("Missing "+ (total - length) + " of " + total + " arguments | Usage:" + "put <key> <value>");

    }

    public void printError(String message){
        System.out.println(PROMPT + "Error! " +  message);
    }


}

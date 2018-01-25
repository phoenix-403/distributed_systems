package app_kvClient;

import common.messages.KVMessage;

public class ErrorMessage {
    private static final String PROMPT = "KV_Client> ";
    private static final String KEY = "<key> ";
    private static final String VALUE = "<value> ";
    private static final int KEY_SIZE = 20;
    private static final int VALUE_SIZE = 120*1024;

    ErrorMessage(){

    }

    public void printLengthError(int size, int max, String arg){
        String unit;

        if (size >=1024){
            unit = size / 1024 +  "KB";
        } else
            unit = size + "B";

        if (size > max)
            printError("The length of " + arg + " cannot exceed " + max + unit);

    }

    public void printMissingArguments(int length, KVMessage.StatusType type){
        int total = 0;
        String usage = null;

        switch (type){
            case PUT:
                total = 3;
                usage = "put <key> <value>";
                break;
            case GET:
                total = 2;
                usage = "get <key>";
        }

        int difference = total - length;
        if (difference > 0)
            printError("Missing "+ (total - length) + " of " + total + " arguments | Usage:" + usage);
        else
            printError((total - length)*-1 + " extra arguments | Usage:" + usage);
    }

    public void printNotConnectedError(){printError("Please Connect First!");}

    public void printUnableToConnectError(String errorMessage){printError("Unable to Connect - " + errorMessage);}

    public boolean checkLength(int size, int max, String arg){
        if (size > max) {
            printLengthError(size, max, arg);
            return false;
        }

        return true;
    }

    public boolean checkArgs(int size, KVMessage.StatusType type){

        switch (type){
            case PUT:
                if (size >= 2)
                    return true;

                break;
            case GET:
                if (size == 2)
                    return true;
        }

        printMissingArguments(size, type);
        return false;
    }

    public boolean validateServerCommand(String [] tokens, KVMessage.StatusType type){
        switch (type){
            case PUT:
                if (checkArgs(tokens.length, type) && checkLength(tokens[0].length(), KEY_SIZE, KEY))
                    return true;
                break;
            case GET:
                if (checkArgs(tokens.length, type) && checkLength(tokens[0].length(), KEY_SIZE, KEY)
                        && checkLength(tokens[0].length(), VALUE_SIZE, VALUE))
                    return true;
                break;
        }

        return false;
    }

    public void printError(String message){
        System.out.println(PROMPT + "Error! " +  message);
    }


}

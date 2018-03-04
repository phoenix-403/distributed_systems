package common.messages;

public class ZkServerCommunication {

    // requests
    public enum Request {

        START("STR"),
        STOP("STP"),
        SHUTDOWN("SHD");

        String value;

        Request(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }


    // response
    enum Response {
        START_SUCCESS(""),
        START_FAIL_fzz(""),

        STOP_SUCCESS(""),
        STOP_FAIL(""),

        SHUTDOWN_SUCCESS(""),
        SHUTDOWN_FAIL("");

        String value;

        Response(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}

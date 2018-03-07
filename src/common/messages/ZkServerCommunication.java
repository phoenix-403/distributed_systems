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
    public enum Response {
        START_SUCCESS("STS"),
        STOP_SUCCESS("SPS"),
        SHUTDOWN_SUCCESS("SDS");

        String value;

        Response(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}

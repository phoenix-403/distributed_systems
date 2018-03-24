package common.messages.zk_server;

public class ZkServerCommunication {

    // requests
    public enum Request {

        START("STR"),
        STOP("STP"),
        SHUTDOWN("SHD"),
        REMOVE_NODES("RMS"),
        TRANSFER_BACKUP_DATA("TBD");

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
        SHUTDOWN_SUCCESS("SDS"),
        REMOVE_NODES_SUCCESS("RNS"),
        REMOVE_NODES_FAIL("RNF");

        String value;

        Response(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}

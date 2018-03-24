package common.messages.server_server;

public class SrvSrvCommunication {

    // requests
    public enum Request {

        TRANSFER_DATA("TFD"),
        REPLICATE_DATA("RPD");

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
        TRANSFERE_SUCCESS("TFS"),
        TRANSFERE_FAIL_LOCK("TFL"),
        TRANSFERE_FAIL("TFF");

        String value;

        Response(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}

package common.messages;

public class Response implements KVMessage {

    private long id;

    private String key;
    private String value;
    private StatusType statusType;

    public Response(long id, String key, String value, StatusType statusType) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.statusType = statusType;
    }

    public long getId() {
        return id;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public StatusType getStatus() {
        return statusType;
    }

    @Override
    public String toString() {

        switch (statusType) {
            case GET_SUCCESS:
                return "(" + id + ")-" + statusType.toString() + "<" + key + "," + value + ">";
            case GET_ERROR:
                return "(" + id + ")-" + statusType.toString() + "<" + key + ">";

            case PUT_SUCCESS:
            case PUT_UPDATE:
                return "(" + id + ")-" + statusType.toString() + "<" + key + "," + value + ">";
            case PUT_ERROR:
                return "(" + id + ")-" + statusType.toString() + "<" + key + "," + value + ">";

            case DELETE_SUCCESS:
                return "(" + id + ")-" + statusType.toString() + "<" + key + ">";
            case DELETE_ERROR:
                return "(" + id + ")-" + statusType.toString() + "<" + key + ">";

            case INVALID_REQUEST:
                return "(" + id + ")-" + statusType.toString();
            case INVALID_RESPONSE:
                return "(" + id + ")-" + statusType.toString();

            case TIME_OUT:
                return "(" + id + ")-" + statusType.toString();
            case CONNECTION_DROPPED:
                return "(" + id + ")-" + statusType.toString() + "_OR_CLOSED. Please reconnect!";
        }

        return super.toString();
    }
}

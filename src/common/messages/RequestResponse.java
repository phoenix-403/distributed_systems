package common.messages;

import ecs.IECSNode;

public class RequestResponse implements KVMessage {

    private long id;

    private String key;
    private String value;
    private StatusType statusType;

    public RequestResponse(long id, String key, String value, StatusType statusType) {
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
    public IECSNode getResponsibleServer() {
        return null;
    }

    @Override
    public String toString() {

        switch (getStatus()) {
            case GET_SUCCESS:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + "," + getValue() + ">";
            case GET_ERROR:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + ">";

            case PUT_SUCCESS:
            case PUT_UPDATE:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + "," + getValue() + ">";
            case PUT_ERROR:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + "," + getValue() + ">";

            case DELETE_SUCCESS:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + ">";
            case DELETE_ERROR:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + ">";

            case INVALID_REQUEST:
                return "(" + getId() + ")-" + getStatus().toString();
            case INVALID_RESPONSE:
                return "(" + getId() + ")-" + getStatus().toString();

            case TIME_OUT:
                return "(" + getId() + ")-" + getStatus().toString();
            case CONNECTION_DROPPED:
                return "(" + getId() + ")-" + getStatus().toString() + "_OR_CLOSED. Please reconnect!";
        }

        return super.toString();
    }

}

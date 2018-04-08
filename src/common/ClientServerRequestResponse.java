package common;

import common.messages.Metadata;
import common.messages.server_client.ClientMetadata;
import ecs.IECSNode;
import org.apache.commons.lang3.StringUtils;

public class ClientServerRequestResponse implements KVMessage {

    private long id;

    private String key;
    private String value;
    private StatusType statusType;
    private Metadata metadata;
    private ClientMetadata clientMetadata;

    public ClientServerRequestResponse(long id, String key, String value, StatusType statusType, Metadata metadata,
                                       ClientMetadata clientMetadata) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.statusType = statusType;
        this.metadata = metadata;
        this.clientMetadata = clientMetadata;
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
        return metadata.getResponsibleServer(key);
    }

    public Metadata getMetadata() {
        return metadata;
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

            case SERVER_STOPPED:
                return "(" + getId() + ")-" + getStatus().toString() + "!";
            case SERVER_NOT_RESPONSIBLE:
                if (StringUtils.isEmpty(getKey())) {
                    return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + ">";
                } else {
                    return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + "," + getValue() + ">";
                }
            case SERVER_WRITE_LOCK:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + "," + getValue() + ">";


            case WATCH_SUCCESS:
            case WATCH_FAIL:
            case UNWATCH_SUCCESS:
            case UNWATCH_FAIL:
                return "(" + getId() + ")-" + getStatus().toString() + "<" + getKey() + ">";


        }

        return super.toString();
    }

}

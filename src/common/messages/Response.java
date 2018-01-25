package common.messages;

public class Response extends RequestResponse {


    public Response(long id, String key, String value, StatusType statusType) {
        super(id, key, value, statusType);
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
                return "(" + super.getId() + ")-" + getStatus().toString();
            case CONNECTION_DROPPED:
                return "(" + super.getId() + ")-" + getStatus().toString() + "_OR_CLOSED. Please reconnect!";
        }

        return super.toString();
    }
}

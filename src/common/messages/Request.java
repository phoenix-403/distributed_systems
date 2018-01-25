package common.messages;

public class Request extends RequestResponse {

    public Request(long id, String key, String value, StatusType statusType) {
        super(id, key, value, statusType);
    }
}

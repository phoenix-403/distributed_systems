package common.messages;

public interface KVMessage {

    enum StatusType {
        GET,             /* REQ => Get - request */
        GET_ERROR,       /* RESP => requested tuple (i.e. value) not found */
        GET_SUCCESS,     /* RESP => requested tuple (i.e. value) found */

        PUT,             /* REQ => Put - request */
        PUT_SUCCESS,     /* RESP => Put - request successful, tuple inserted */
        PUT_UPDATE,      /* RESP => Put - request successful, i.e. value updated */
        PUT_ERROR,       /* RESP => Put - request unsuccessful */
        DELETE_SUCCESS,  /* RESP => Delete - request successful */
        DELETE_ERROR,    /* RESP => Delete - request unsuccessful */

        INVALID_REQUEST,  /* server can not parse string into an appropriate request */
        INVALID_RESPONSE, /* client can not parse string into an appropriate response */

        SERVER_ERROR,     /* An error occurred on server side */
        TIME_OUT,         /* client timeout */
        CONNECTION_DROPPED


    }

    /**
     * @return the key that is associated with this message,
     * null if not key is associated.
     */
    String getKey();

    /**
     * @return the value that is associated with this message,
     * null if not value is associated.
     */
    String getValue();

    /**
     * @return a status string that is used to identify request types,
     * response types and error types associated to the message.
     */
    StatusType getStatus();

}



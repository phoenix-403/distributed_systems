package common.messages;

import ecs.IECSNode;

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

        WRITE_LOCK,
        WRITE_UNLOCK,
        TRANSFER_DATA,

        INVALID_REQUEST,  /* server can not parse string into an appropriate request */
        INVALID_RESPONSE, /* client can not parse string into an appropriate response */

        SERVER_ERROR,     /* An error occurred on server side */
        TIME_OUT,         /* client timeout */
        CONNECTION_DROPPED,

        TEST_METADATA,

        SERVER_STOPPED,         /* Server is stopped, no requests are processed */
        SERVER_WRITE_LOCK,      /* Server locked for out, only get possible */
        SERVER_NOT_RESPONSIBLE  /* Request not successful, server not responsible for key */


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

    /**
     * @return  the responsible server node
     */
    IECSNode getResponsibleServer();

}



package common.messages.server_server;

import java.util.HashMap;

public class SrvSrvRequest {
    private String serverName;
    private String targetServer;
    private String[] hashRange;
    private SrvSrvCommunication.Request request;

    private HashMap<String, String> kvToImport;

    public SrvSrvRequest(String serverName, String targetServer, String[] hashRange,
                         SrvSrvCommunication.Request request, HashMap<String, String> kvToImport) {
        this.serverName = serverName;
        this.targetServer = targetServer;
        this.request = request;
        this.hashRange = hashRange;
        this.kvToImport = kvToImport;
    }

    public SrvSrvCommunication.Request getRequest() {
        return request;
    }

    public String getServerName() {
        return serverName;
    }

    public String[] getHashRange() {
        return hashRange;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public HashMap<String, String> getKvToImport() {
        return kvToImport;
    }
}

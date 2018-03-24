package common.messages.server_server;

public class SrvSrvResponse {

    private String serverName;
    private String targetServer;
    private SrvSrvCommunication.Response response;

    public SrvSrvResponse(String serverName, String targetServer, SrvSrvCommunication.Response response) {
        this.serverName = serverName;
        this.targetServer = targetServer;
        this.response = response;
    }

    public String getServerName() {
        return serverName;
    }

    public String getTargetServer() {
        return targetServer;
    }

    public SrvSrvCommunication.Response getResponse() {
        return response;
    }
}

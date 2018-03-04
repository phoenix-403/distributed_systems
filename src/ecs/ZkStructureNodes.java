package ecs;

public enum ZkStructureNodes {
    ROOT("/"),
    HEART_BEAT("/HB"),
    ZK_SERVER_REQUESTS("/ZSREQ"),
    ZK_SERVER_RESPONSE("/ZSRES"),
    SERVER_SERVER_COMMANDS("/SSC"),
    METADATA("/MD"),
    REQUEST("/REQ"),
    RESPONSE("/RES");

    String value;

    ZkStructureNodes (String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

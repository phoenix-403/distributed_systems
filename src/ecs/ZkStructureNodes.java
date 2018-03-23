package ecs;

public enum ZkStructureNodes {
    ROOT("/"),
    NONE_HEART_BEAT("/NHB"),
    HEART_BEAT("/HB"),
    ZK_SERVER_REQUEST("/ZSREQ"),
    ZK_SERVER_RESPONSE("/ZSRES"),
    SERVER_SERVER_REQUEST("/SSREQ"),
    SERVER_SERVER_RESPONSE("/SSRES"),
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

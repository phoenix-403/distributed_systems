package ecs;

public enum ZkStructureNodes {
    ROOT("/"),
    HEART_BEAT("/HB"),
    GLOBAL_STATUS("/GS"),
    SERVER_NODES("/SN"),
    METADATA("/MD");

    String value;

    ZkStructureNodes (String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

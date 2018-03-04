package common.messages;

import app_kvECS.EcsException;
import client.KVStore;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class Metadata {

    TreeMap<String,MetadataEntry> map = new TreeMap<String,MetadataEntry>();


    public Metadata (String json) {
        //TODO
    }

    public TreeMap<String, MetadataEntry> getMap() {
        return map;
    }

    public void setMap(TreeMap<String, MetadataEntry> map) {
        this.map = map;
    }

    public class MetadataEntry {
        String host;
        int port;
        String hashRange;
        MetadataEntry(String host, int port, String hashRange) {
            this.host = host;
            this.port = port;
            this.hashRange = hashRange;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getHashRange() {
            return hashRange;
        }

        public void setHashRange(String hashRange) {
            this.hashRange = hashRange;
        }
    }

    public MetadataEntry getServer(String key) {
//        MetadataEntry closest= map.get(0);
//        for(Map.Entry<String,MetadataEntry> entry : map.entrySet()) {
//            if (key.compareTo(closest.getHashRange()) >= 0
//                    && (entry.getValue().getHashRange().compareTo(closest.getHashRange()) -
//                    entry.getValue().getHashRange().compareTo(closest.getHashRange())) > 0
//            ) {
//
//            }
//            Integer value = entry.getValue();
//
//            System.out.println(key + " => " + value);
//        }
//        return closest;
        //TODO after Chewbaka finishes wrap around
        return null;
    }


}

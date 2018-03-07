package common.helper;


public class IDHashPair implements Comparable<IDHashPair> {
    private String id;
    private String hashVal;

    public IDHashPair(String id, String hashVal) {
        this.id = id;
        this.hashVal = hashVal;
    }

    public String getId() {
        return id;
    }

    public String getHashVal() {
        return hashVal;
    }

    @Override
    public int compareTo(IDHashPair o) {
        if (this.getHashVal().compareTo(o.getHashVal()) < 0) {
            return -1;
        } else if (this.getHashVal().compareTo(o.getHashVal()) > 0) {
            return 1;
        }
        return 0;
    }
}

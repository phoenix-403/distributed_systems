package app_kvServer;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;

import static app_kvServer.IKVServer.CacheStrategy;

public class Cache {

    private static Logger logger = LogManager.getLogger(Cache.class);

    private static int size;
    private static CacheStrategy cacheStrategy;
    private static HashMap<String, String[]> cache = new HashMap<>();

    private static boolean isCacheSetup = false;

    private Cache() {
    }

    /**
     * sets-up cache
     *
     * @param sizze    specifies how many key-value pairs the server is allowed
     *                 to keep in-memory
     * @param strategy specifies the cache replacement strategy in case the cache
     *                 is full and there is a GET- or PUT-request on a key that is
     *                 currently not contained in the cache. Options are "FIFO", "LRU",
     *                 and "LFU".
     */
    public static void setup(int sizze, CacheStrategy strategy) {
        logger.info("Initializing cache");
        if (size > 0 && !CacheStrategy.None.equals(cacheStrategy)) {
            size = sizze;
            cacheStrategy = strategy;
            isCacheSetup = true;
            logger.info("Cache initialized!");
        }
        logger.warn("Unable to initialize cache. Either size was not greater than 0 or cache strategy was none");
    }

    /**
     * Check if key is in cache.
     * NOTE: does not modify any other properties
     *
     * @return true if key in storage, false otherwise
     */
    public static boolean inCache(String key) {
        return cache.containsKey(key);
    }

    /**
     * Clears the cache
     */
    public static void clearCache() {
        cache = new HashMap<>();
        logger.info("Cache cleared!");
    }

    /**
     * Looks up value in cache and updates cache using cache strategy if needed - will get value from disk if needed
     * If cache is disabled, it will look up the value from disk
     * If string is empty or null return null
     *
     * @param key key to lookup value in cache or disk
     * @return looked up value if it finds key in cache or disk, if miss in both will return null
     * @throws IOException if unable to read from disk
     */
    public static String lookup(String key) throws IOException {

        // lookup from cache -- incache will return false if cache is not setup
        if (inCache(key)) {
            logger.info("Cache hit for key");
            return cache.get(key)[0];
        }

        // lookup disk and if cache is setup update it
        String value = Persist.read(key);
        if (isCacheSetup && value != null) {
            updateCache(key, value);
        }

        return value;
    }

    // todo - have a cache for write and create a thread to periodically write to disk???

    private static void updateCache(String key, String value) {
        if (cache.size() < size) {
            // just add it since it is less than size
        } else {
            // TODO Implement cache strategy
            switch (cacheStrategy) {
                case LFU:
                    break;
                case LRU:
                    break;
                case FIFO:
                    break;
            }

        }
    }


}

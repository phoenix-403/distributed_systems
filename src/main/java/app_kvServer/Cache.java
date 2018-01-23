package app_kvServer;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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
    public static boolean setup(int sizze, CacheStrategy strategy) {
        logger.info("Initializing cache");
        if (size > 0 && !CacheStrategy.None.equals(cacheStrategy)) {
            size = sizze;
            cacheStrategy = strategy;
            isCacheSetup = true;
            return true;
        }

        logger.warn("Unable to initialize cache. Either size was not greater than 0 or cache strategy was none");
        return false;

    }

    /**
     *  Looks up value in cache and updates cache using cache strategy if needed - will get value from disk if needed
     *  If cache is disabled, it will look up the value from disk
     *  If string is empty or null return null
     *
     * @param key    key to lookup value in cache or disk
     *
     * @return looked up value if it finds key in cache or disk and null if it misses it from cache
     */
    public static String lookup(String key)  {

        // lookup from cache and disk if it misses
        if(isCacheSetup && StringUtils.isEmpty(key)){
            cache.get(key); /// ===> returns it if not null other wise get from disk
            //// ==== if disk is null return null else update cache and return
            // todo ^^
        }

        // lookup from disk
        if(!isCacheSetup && StringUtils.isEmpty(key)){
            // todo
        }

        // invalid string
        return null;
    }

    private static void updateCache(){
        if (cache.size() < size){
            // todo just add it since it is less than size
        }
        else{
            // todo use cache strategy
        }
    }


}

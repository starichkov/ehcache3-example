package org.starichkov.java.ehcache3;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.spi.loaderwriter.CacheLoaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * @author Vadim Starichkov
 * @since 31.10.2018 15:22
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger("logger");
    private static final String CACHE_ALIAS = "ehcache3example";

    public static void main(String[] args) throws InterruptedException {
        new Main().run(args);
    }

    private void run(String[] args) throws InterruptedException {
        boolean enableReload = false;
        if (args.length > 0) {
            enableReload = Boolean.valueOf(args[0]);
        }

        final List<String> keys = Arrays.asList("a", "b", "c");
        final List<String> values = Arrays.asList("d", "e", "f");

        CacheConfigurationBuilder<String, String> cacheConfigurationBuilder =
                CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class, ResourcePoolsBuilder.heap(1000))
                        .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(3)));

        if (enableReload) {
            cacheConfigurationBuilder = cacheConfigurationBuilder.withLoaderWriter(createCacheLoaderWriter(keys, values));
        }
        CacheConfiguration<String, String> cacheConfiguration = cacheConfigurationBuilder.build();

        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache(CACHE_ALIAS, cacheConfiguration)
                .build(true);

        Cache<String, String> cache = cacheManager.getCache(CACHE_ALIAS, String.class, String.class);

        if (!enableReload) {
            LOGGER.info("Reloading disabled, manually populating cache...");
            for (int i = 0; i < keys.size(); i++) {
                cache.put(keys.get(i), values.get(i));
            }
        }

        LOGGER.info("Cache initialized, let's check what's in cache!");
        printCacheValues(cache, keys);

        LOGGER.info("Sleep for 5 seconds giving our cache time to expire...");
        Thread.sleep(5000);

        LOGGER.info("Woke up, let's check what's in cache!");
        printCacheValues(cache, keys);

        cacheManager.close();
    }

    private void printCacheValues(Cache<String, String> cache, List<String> keys) {
        for (String key : keys) {
            LOGGER.info("Value for key '{}': '{}'", key, cache.get(key));
        }
    }

    private CacheLoaderWriter<String, String> createCacheLoaderWriter(List<String> keys, List<String> values) {
        return new CacheLoaderWriter<String, String>() {

            @Override
            public String load(String key) {
                LOGGER.info("Loading value for key '{}'...", key);
                if (keys.contains(key)) {
                    return values.get(keys.indexOf(key));
                }
                return null;
            }

            @Override
            public void write(String key, String value) {
                LOGGER.info("Writing value '{}' for key '{}'...", value, key);
            }

            @Override
            public void delete(String key) {
                LOGGER.info("Deleting value for key '{}'...", key);
            }
        };
    }
}

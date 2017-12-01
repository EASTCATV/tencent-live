package cn.godsdo.cache;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;


/**
 * @author yang
 *
 */
public class CustomizeRedisCache extends RedisCache {

    private RedisCacheWriter cacheWriter;

    private RedisCacheConfiguration configuration;

    /**
     *校验规则：获取时间
     */
    static String REGEX_STR = ".*\\&(\\-)?\\d+$";

    /**
     * 值过期时间
     */
    private final Long ttl;


    protected CustomizeRedisCache(String name, RedisCacheWriter redisCacheWriter, RedisCacheConfiguration cacheConfig) {
        super(analysisName(name), redisCacheWriter, cacheConfig);
        cacheWriter = redisCacheWriter;
        configuration = cacheConfig;
        this.ttl = analysisTtl(name);
    }

    /**
     * put 时进行判断
     * @param key
     * @param value
     */
    @Override
    public void put(Object key, Object value) {
        String name = super.getName();
        //自定义设置过期时间
        if (ttl != null) {
            Object cacheValue = preProcessCacheValue(value);
            if (!isAllowNullValues() && cacheValue == null) {
                throw new IllegalArgumentException(String.format(
                        "Cache '%s' does not allow 'null' values. Avoid storing null via '@Cacheable(unless=\"#result == null\")' or configure RedisCache to allow 'null' via RedisCacheConfiguration.",
                        name));
            }
            //插入时添加时间
            cacheWriter.put(name, serializeCacheKey(createMyCacheKey(key)), serializeCacheValue(cacheValue), Duration.ofSeconds(ttl));
        } else {
            //原来逻辑处理
            super.put(key, value);
        }

    }

    /**
     * 解析名称
     * @param name
     * @return
     */
    private static String analysisName(String name){
        if (Pattern.matches(REGEX_STR, name)) {
            List<String> keyList = Lists.newArrayList(Splitter.on("&").split(name));
            //ttl = Long.valueOf(keyList.get(1));
            name = keyList.get(0);
        }
        return name;
    }

    /**
     * 解析名称
     * @param name
     * @return
     */
    private static Long analysisTtl(String name){
        if (Pattern.matches(REGEX_STR, name)) {
            List<String> keyList = Lists.newArrayList(Splitter.on("&").split(name));
            return Long.valueOf(keyList.get(1));
        }
        return null;
    }

    /**
     * 清除缓存适配 通配符
     * 使用样例：@CacheEvict(cacheNames = "loginCache", key = "test-*")
     * @param key
     */
    @Override
    public void evict(Object key) {
        if (key instanceof String && ((String) key).endsWith("*")) {// 通配符删除
            byte[] pattern = getCacheConfiguration().getConversionService().convert(this.createCacheKey(key), byte[].class);
            this.cacheWriter.clean(this.getName(), pattern);
        } else {// 单一key删除
            this.cacheWriter.remove(this.getName(), this.serializeCacheKey(this.createCacheKey(key)));
        }
    }

    /**
     * key过滤掉时间后缀
     * @param key
     * @return
     */
    protected String createMyCacheKey(Object key) {
        String convertedKey = convertKey(key);
        if (!configuration.usePrefix()) {
            return convertedKey;
        }
        return prefixCacheKey(convertedKey);
    }

    /**
     * 去除时间后缀
     * @param key
     * @return
     */
    private String prefixCacheKey(String key) {
        String name = super.getName();
        if (Pattern.matches(REGEX_STR, name)) {
            List<String> keyList = Lists.newArrayList(Splitter.on("&").split(name));
            String finalName = keyList.get(0);
            return configuration.getKeyPrefixFor(finalName) + key;
        }
        // allow contextual cache names by computing the key prefix on every call.
        return configuration.getKeyPrefixFor(name) + key;
    }
}

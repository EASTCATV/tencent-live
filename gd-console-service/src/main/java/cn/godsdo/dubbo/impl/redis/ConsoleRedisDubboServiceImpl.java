package cn.godsdo.dubbo.impl.redis;

import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.nosql.RedisUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author : CR7
 */
@DubboService
public class ConsoleRedisDubboServiceImpl implements RedisDubboService {
    @Resource
    private RedisUtils redisUtil;

    @Override
    public void set(String key, String value) {
        redisUtil.set(key, value);
    }

    @Override
    public void setex(String key, String value, long time) {
        redisUtil.set(key, value, time);
    }

    @Override
    public Object get(String key) {
        return redisUtil.get(key);
    }

    @Override
    public void del(String... key) {
        redisUtil.del(key);
    }

    @Override
    public List<String> lGet(String tokenKey, int start, long end) {
        try {
//            return redisTemplate.opsForList().range(key, start, end);
            return redisUtil.lGet(tokenKey, start, end);
        } catch (Exception e) {
//            log.error("lGet error!", e);
            return null;
        }
    }

    @Override
    public Map<String, String> hgetAll(Integer dbIndex, String key) {
        try {
            if (ObjectUtils.isEmpty(dbIndex)) {
                dbIndex = 0;
            }
            return redisUtil.hgetAll(dbIndex, key);
        } catch (Exception e) {
//            log.error("lGet error!", e);
            return null;
        }
    }

    @Override
    public void setex(Integer dbIndex, String key, String value, int time) {
        try {
            if (ObjectUtils.isEmpty(dbIndex)) {
                dbIndex = 0;
            }
            redisUtil.setEx(dbIndex, key, value, time);
        } catch (Exception e) {
//            log.error("lGet error!", e);
        }
    }

    @Override
    public void set(Integer dbIndex, String key, String value) {
        if (ObjectUtils.isEmpty(dbIndex)) {
            dbIndex = 0;
        }
        redisUtil.set(dbIndex, key, value);
    }

    @Override
    public Object get(Integer dbIndex, String key) {
        try {
            if (ObjectUtils.isEmpty(dbIndex)) {
                dbIndex = 0;
            }
            return redisUtil.get(dbIndex, key);
        } catch (Exception e) {
//            log.error("lGet error!", e);
        }
        return null;
    }

    @Override
    public void del(Integer dbIndex, String key) {
        try {
            if (ObjectUtils.isEmpty(dbIndex)) {
                dbIndex = 0;
            }
            redisUtil.del(dbIndex, key);
        } catch (Exception e) {
//            log.error("lGet error!", e);
        }
    }

    @Override
    public String hget(Integer dbIndex, String key, String filed) {
        if (ObjectUtils.isEmpty(dbIndex)) {
            dbIndex = 0;
        }
        return redisUtil.hget(dbIndex, key, filed);
    }

    @Override
    public void hset(Integer dbIndex, String key, String filed, String value) {
        if (ObjectUtils.isEmpty(dbIndex)) {
            dbIndex = 0;
        }
        redisUtil.hset(dbIndex, key, filed, value);
    }

    @Override
    public void hset(Integer dbIndex, String key, String filed, String value, Long time) {
        if (ObjectUtils.isEmpty(dbIndex)) {
            dbIndex = 0;
        }
        redisUtil.hset(dbIndex, key, filed, value, time.intValue());
    }

    @Override
    public void hDel(Integer dbIndex, String key, String field) {
        if (ObjectUtils.isEmpty(dbIndex)) {
            dbIndex = 0;
        }
        redisUtil.hdel(dbIndex, key, field);
    }

    @Override
    public Set<String> keys(int index, String key) {
        return redisUtil.keys(index,key);
    }

    @Override
    public long lLenByIndex(int index, String key) {
        return redisUtil.lLenByIndex(index, key);
    }

    @Override
    public String lIndexByIndex(int index, String key, long count) {
        return redisUtil.lindexByIndex(index, key, count);
    }

    @Override
    public String lTrimByIndex(int index, String key, long start,long end) {
        return redisUtil.ltrimByIndex(index, key, start, end);
    }

    @Override
    public Long lPushByIndexEx(Integer index, String key, String value, long seconds) {
        return redisUtil.lPushByIndexEx(index, key, value, seconds);
    }

}

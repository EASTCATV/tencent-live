package cn.godsdo.service.impl.redis;

import cn.godsdo.nosql.RedisUtils;
import cn.godsdo.service.RedisService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/**
 * @author W~Y~H
 * @Date : 2018/11/18
 */
@Service
public class RedisServiceImpl implements RedisService {
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
}

package cn.godsdo.config.wx;

import cn.godsdo.config.RedisConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author hdk
 * @create 2024/2/23
 */

@Configuration
public class JedisConfig implements CachingConfigurer {
    protected static final Logger logger = Logger.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.jedis.pool.max-active}")
    private int maxTotal;

    @Value("${spring.data.redis.jedis.pool.max-idle}")
    private int maxIdle;

    @Value("${spring.data.redis.jedis.pool.min-idle}")
    private int minIdle;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.timeout}")
    private int timeout;
    private static JedisPool jedisPool;

    public JedisPool redisPoolFactory() {
        logger.info("*********************** redisPoolFactory init ***********************");
        logger.info("maxTotal：" + maxTotal + "  ,maxIdle:" + maxIdle + "  ,minIdle:" + minIdle);
        logger.info("redis地址 host：" + host + " ,port :" + port + " ,timeout :" + timeout + " ,password :" + password);
        if (jedisPool == null) {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(maxTotal);
            jedisPoolConfig.setMaxIdle(maxIdle);
            jedisPoolConfig.setMinIdle(minIdle);
            if (StringUtils.isNotBlank(password)) {
                jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout, password);
            }else{
                jedisPool = new JedisPool(jedisPoolConfig, host, port, timeout);
            }

            logger.info("JedisPool注入成功！！");
            logger.info("redis地址：" + host + ":" + port);
        }

        return jedisPool;
    }

}

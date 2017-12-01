package cn.godsdo.service.impl.shortRedis;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @Author : yang
 * @Date : 2023/3/8
 * @ApiNote : 配制文件redisServer: 49.234.105.173_6379_master_eDDxWJfwYsDhD
 */
@Order(1)
@Configuration
public class ShortRedisProxyConfig {

    @Value("${redisServer}")
    private String redisService;


    @Bean(name = "poolConfig")
    public JedisPoolConfig poolConfig(){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(8);
        jedisPoolConfig.setMaxTotal(100);
        jedisPoolConfig.setMaxWaitMillis(3000);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(3000);
        return jedisPoolConfig;
    }
    @Bean(name="shortRedisProxy")
    public ShortRedisProxy shortRedisProxy(@Qualifier("poolConfig") JedisPoolConfig poolConfig){
        ShortRedisProxy ocRedisProxy = new ShortRedisProxy(redisService,poolConfig );
        return ocRedisProxy;
    }





}

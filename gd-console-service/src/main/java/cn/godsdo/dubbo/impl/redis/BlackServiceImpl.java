package cn.godsdo.dubbo.impl.redis;

import cn.godsdo.dubbo.com.BlackService;
import cn.godsdo.service.impl.shortRedis.ShortRedisProxy;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @Author : yang
 * @Date : 2018/3/27
 * @ApiNote :系统黑名单
 */
@Slf4j
@DubboService
public class BlackServiceImpl implements BlackService {
    @Autowired
    ShortRedisProxy redisProxy;

    @Override
    public Boolean IdIsBlack(String id) {
        return null;
    }
    //超级校验
    @Override
    public Boolean IpIsBlack(String ip) {
        System.out.println("ip is black:"+ip);
        //判断是不是终极黑名单中------redis
        if(ip.equals("  ")){
            return false;
        }
        //判断是不在用户端的封禁黑名单中---系统判定的观看端---redis
        if(ip.equals("  ")){
            return false;
        }
        return true;
    }

    @Override
    public Boolean addIPBlack(String ip) {
        return null;
    }

    /**
     * 短链存储
     * @param key
     * @param value
     */
    @Override
    public void setShare(String key, String value) {
//        log.info("==========setShare key:{},value:{}",key,value);
        redisProxy.set(key,value);
    }

    /**
     * 删除短链缓存
     * @param key
     */
    @Override
    public void delShare(String key) {
        redisProxy.del(key);
    }


    @Override
    public Object get(String key) {
        return redisProxy.get(key);
    }


}

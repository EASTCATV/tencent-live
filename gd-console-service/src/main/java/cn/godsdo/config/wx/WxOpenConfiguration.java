//package cn.godsdo.config.wx;
//
//import jakarta.annotation.PostConstruct;
//import lombok.Getter;
//import lombok.extern.slf4j.Slf4j;
//import me.chanjar.weixin.open.api.impl.WxOpenInRedisConfigStorage;
//import me.chanjar.weixin.open.api.impl.WxOpenMessageRouter;
//import me.chanjar.weixin.open.api.impl.WxOpenServiceImpl;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import redis.clients.jedis.JedisPool;
//
//@Slf4j
//@Component
//public class WxOpenConfiguration extends WxOpenServiceImpl {
//
//    @Autowired
//    private JedisConfig redisConfig;
//
//    private volatile static JedisPool jedisPool;
//
//    @Getter
//    private WxOpenMessageRouter wxOpenMessageRouter;
//
//    private static final WxOpenConfiguration wxOpenConfiguration = new WxOpenConfiguration();
//
//    public WxOpenConfiguration() {
//    }
//
//    public static WxOpenConfiguration getInstance() {
//        return wxOpenConfiguration;
//    }
//
//
//    /**
//     * JedisPool 无法通过@Autowired注入，可能由于是方法bean的原因，此处可以先注入RedisConfig，
//     * 然后通过@PostConstruct初始化的时候将factory直接赋给jedisPool
//     */
//    @PostConstruct
//    public void init() {
//        log.info("******************************************* WxOpenConfiguration init*******************************************");
//        if (null == jedisPool) {
//            jedisPool = redisConfig.redisPoolFactory();
//        }
//    }
//
//    public void initWxOpenInRedisConfig(String openAppId, String openAppSecret, String openToken, String openKey) {
//        log.info("*******************************************initWxOpenInRedisConfig*******************************************");
//        log.info("openAppId {},  openAppSecret {},  openToken {},  openKey {} ",openAppId,  openAppSecret,  openToken,  openKey);
//        log.info("jedisPool {} ",jedisPool);
//
//        WxOpenInRedisConfigStorage inRedisConfigStorage = new WxOpenInRedisConfigStorage(jedisPool);
//        inRedisConfigStorage.setComponentAppId(openAppId);
//        inRedisConfigStorage.setComponentAppSecret(openAppSecret);
//        inRedisConfigStorage.setComponentToken(openToken);
//        inRedisConfigStorage.setComponentAesKey(openKey);
//        setWxOpenConfigStorage(inRedisConfigStorage);
//
//        log.info("*******************************************initWxOpenInRedisConfig*******************************************");
//
//        wxOpenMessageRouter = new WxOpenMessageRouter(this);
//        wxOpenMessageRouter.rule().handler((wxMpXmlMessage, map, wxMpService, wxSessionManager) -> {
//            log.info("\n接收到 {} 公众号请求消息，内容：{}", wxMpService.getWxMpConfigStorage().getAppId(), wxMpXmlMessage);
//            return null;
//        }).next();
//    }
//
//}

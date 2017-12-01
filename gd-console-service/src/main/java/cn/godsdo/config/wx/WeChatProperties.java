//package cn.godsdo.config.wx;
//
//import lombok.Getter;
//import lombok.Setter;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
//
///**
// * 微信相关属性
// *
// * @author gechenpeng
// * @date 2024-04-09
// */
//@Getter
//@Setter
//@RefreshScope
//@ConfigurationProperties("wechat")
//public class WeChatProperties {
//
//    private Merchant defaultMerchant;
//
//    private Pay pay;
//
//    private Mini mini;
//
//    @Getter
//    @Setter
//    public static class Pay {
//
//        private String notifyUrl;
//    }
//
//    @Getter
//    @Setter
//    public static class Merchant {
//
//        private String wxJsapiAppid;
//
//        private String wxMchId;
//
//        private String wxMchKey;
//    }
//    @Getter
//    @Setter
//    public static class Mini {
//        private String appid;
//        private String secret;
//    }
//}

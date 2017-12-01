//package cn.godsdo.util;
//
//import cn.godsdo.config.StaticTemplateProperties;
//import com.maxmind.geoip2.DatabaseReader;
//import com.maxmind.geoip2.model.CityResponse;
//import jakarta.annotation.Resource;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.stereotype.Component;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.net.InetAddress;
//
///**
// * @Author: CR7
// * @Date: 2019/5/5 15:47
// * @Description: 根据IP地址获取详细的地域信息
// */
//@Slf4j
//@Component
//public class AddressUtils {
//
//    @Resource
//    private StaticTemplateProperties templateProperties;
//
////    @Resource
////    private DatabaseReader databaseReader;
//
//    public String parseIp(String ip) {
//        log.info("当前IP：{}", ip);
//        String ipInfo = "";
//        try {
//            FileInputStream database = new FileInputStream(new File(templateProperties.getDatabaseUrl()));
//            // 创建数据库
//            DatabaseReader databaseReader = new DatabaseReader.Builder(database).build();
//
//            // 获取 IP 地址信息
//            InetAddress ipAddress = InetAddress.getByName(ip);
//            // 获取查询信息
//            CityResponse response = databaseReader.city(ipAddress);
//
//            String countryNameZ = response.getCountry().getNames().get("zh-CN"); // '国家'
//            String subdivisionNameZ = response.getLeastSpecificSubdivision().getNames().get("zh-CN"); // '省份'
//            String cityNameZ = response.getCity().getNames().get("zh-CN"); // '城市'
//            ipInfo = countryNameZ + "_" + subdivisionNameZ + "_" + cityNameZ;
//        } catch (Exception e) {
//            log.error("获取IP信息失败：{}", e.getMessage());
////            e.printStackTrace();
//        }
//        return ipInfo;
//    }
//
//    public String parseIp(HttpServletRequest request) {
//        String ip = getUserIpAddress(request);
//        log.info("当前IP：{}", ip);
//        String ipInfo = "";
//        try {
//            FileInputStream database = new FileInputStream(new File(templateProperties.getDatabaseUrl()));
//            // 创建数据库
//            DatabaseReader databaseReader = new DatabaseReader.Builder(database).build();
//
//            // 获取 IP 地址信息
//            InetAddress ipAddress = InetAddress.getByName(ip);
//            // 获取查询信息
//            CityResponse response = databaseReader.city(ipAddress);
//
//            String countryNameZ = response.getCountry().getNames().get("zh-CN"); // '国家'
//            String subdivisionNameZ = response.getLeastSpecificSubdivision().getNames().get("zh-CN"); // '省份'
//            String cityNameZ = response.getCity().getNames().get("zh-CN"); // '城市'
//            ipInfo = countryNameZ + "_" + subdivisionNameZ + "_" + cityNameZ;
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ipInfo;
//    }
//
//    public static void main(String[] args) {
////        AddressUtils t = new AddressUtils();
//////        System.out.println(t.getCityInfo("114.84.100.200"));
////        System.out.println(t.getCityInfo("111.18.134.223"));
//        try {
////            FileInputStream database = new FileInputStream(new File(templateProperties.getDatabaseUrl()));
////            FileInputStream database = new FileInputStream(new File("D:\\hdk\\geo-lite.mmdb-download\\GeoLite2-City.mmdb"));
//            FileInputStream database = new FileInputStream(new File("/Users/yang/Downloads/geo-lite.mmdb-download/GeoLite2-City.mmdb"));
//            DatabaseReader reader = new DatabaseReader.Builder(database).build();
//
//            // 获取 IP 地址信息
//            //InetAddress ipAddress = InetAddress.getByName("111.18.134.223");
//            InetAddress ipAddress = InetAddress.getByName("150.158.80.182");
//            // 获取查询信息
//            CityResponse response = reader.city(ipAddress);
//            String countryNameZ = response.getCountry().getNames().get("zh-CN"); // '国家'
//            String subdivisionNameZ = response.getLeastSpecificSubdivision().getNames().get("zh-CN"); // '省份'
//            String cityNameZ = response.getCity().getNames().get("zh-CN"); // '城市'
//            System.out.println("所在国家：" + countryNameZ+"，您当前所在省份：" + subdivisionNameZ +"，您当前所在城市：" + cityNameZ);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
////    public CityResponse getInfo(String ip) throws IOException, GeoIp2Exception {
////        File database = new File("D:\\hdk\\geo-lite.mmdb-download\\GeoLite2-City.mmdb"); // GeoLite2数据库文件路径
////        DatabaseReader reader = new DatabaseReader.Builder(database).build();
////        InetAddress ipAddress = InetAddress.getByName(ip);
////        CityResponse response = reader.city(ipAddress);
////        return response;
////    }
//
////    public String getCityInfo(String ipAddress) {
////        CityResponse cityResponse;
////        try {
////            cityResponse = getInfo(ipAddress);
//////            String cityName = cityResponse.getCity().getName(); // '英文城市'
//////            String countryName = cityResponse.getCountry().getName(); // '英文国家'
////            String countryNameZ = cityResponse.getCountry().getNames().get("zh-CN"); // '国家'
////            String subdivisionNameZ = cityResponse.getLeastSpecificSubdivision().getNames().get("zh-CN"); // '省份'
////            String cityNameZ = cityResponse.getCity().getNames().get("zh-CN"); // '城市'
//////            System.out.println("所在国家：" + countryNameZ+"，您当前所在省份：" + subdivisionNameZ +"，您当前所在城市：" + cityNameZ);
////            return countryNameZ + "_" + subdivisionNameZ + "_" + cityNameZ;
////        } catch (IOException | GeoIp2Exception e) {
////            return "获取城市信息失败：" + e.getMessage();
////        }
////    }
//
////    public String getCityInfo(HttpServletRequest request) {
////        String ipAddress = getUserIpAddress(request);
////        CityResponse cityResponse;
////        String cityName =null;
////        String countryName =null;
////        try {
////            cityResponse = getInfo(ipAddress);
////            cityResponse.getCity().getName();
////            cityResponse.getCountry().getName();
////        } catch (IOException e) {
////            return "获取城市信息失败：" + e.getMessage();
////        } catch (GeoIp2Exception e) {
////            e.printStackTrace();
////        }
////        return "您当前所在城市：" + cityName + "，所在国家：" + countryName;
////    }
//
//    /**
//     * 获取Ip地址
//     * @param request
//     * @return
//     */
//    public String getUserIpAddress(HttpServletRequest request) {
//        String ipAddress = request.getHeader("X-Forwarded-For");
//        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("Proxy-Client-IP");
//        }
//        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
//            ipAddress = request.getRemoteAddr();
//        }
//        return ipAddress;
//    }
//
//    /**
//     * 获取Ip地址 - 备用
//     * @param request
//     * @return
//     */
//    private static String getIpAdrress(HttpServletRequest request) {
//        String Xip = request.getHeader("X-Real-IP");
//        String XFor = request.getHeader("X-Forwarded-For");
//        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
//            //多次反向代理后会有多个ip值，第一个ip才是真实ip
//            int index = XFor.indexOf(",");
//            if(index != -1){
//                return XFor.substring(0,index);
//            }else{
//                return XFor;
//            }
//        }
//        XFor = Xip;
//        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
//            return XFor;
//        }
//        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
//            XFor = request.getHeader("Proxy-Client-IP");
//        }
//        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
//            XFor = request.getHeader("WL-Proxy-Client-IP");
//        }
//        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
//            XFor = request.getHeader("HTTP_CLIENT_IP");
//        }
//        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
//            XFor = request.getHeader("HTTP_X_FORWARDED_FOR");
//        }
//        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
//            XFor = request.getRemoteAddr();
//        }
//        return XFor;
//    }
//
//}

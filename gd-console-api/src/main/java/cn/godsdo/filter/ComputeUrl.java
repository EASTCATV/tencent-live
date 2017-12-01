package cn.godsdo.filter;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Author : yang
 * @Date : 2018/3/27
 * @ApiNote :放行的URL
 */
@Configuration
@PropertySource(value = {"classpath:exclude-url.yml"}, factory = PropertySourceFactory.class)
public class ComputeUrl {
    @Getter
    private static String PASSURL;
    @Getter
    private static String REQUESTURL;
    @Getter
    private static String RESPONSEURL;

    @Value("${pass.url}")
    public void setPassUrl(String param){
        PASSURL = param;
    }


    @Value("${pass.response_url}")
    public void seResponseURl(String param){
        RESPONSEURL = param;
    }

    @Value("${pass.request_url}")
    public void setRequestUrl(String param){
        REQUESTURL = param;
    }

    public static List<String> getPassUrl(){
        String[] strs = PASSURL.split(",");
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(strs)) ;
        return list;
    }
    public static List<String> getResponseUrl(){
        String[] strs = RESPONSEURL.split(",");
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(strs)) ;
        return list;
    }
    public static List<String> getRequestUrl(){
        String[] strs = REQUESTURL.split(",");
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(strs)) ;
        return list;
    }

}

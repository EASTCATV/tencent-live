package cn.godsdo.dubbo.impl.sms;

import cn.godsdo.dubbo.SmsSendUtil;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.hutool.json.JSONException;
import com.github.qcloudsms.SmsSingleSender;
import com.github.qcloudsms.SmsSingleSenderResult;
import com.github.qcloudsms.httpclient.HTTPException;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;

/**
 * @Author : yang
 * @Date : 2018/3/27
 * @ApiNote :短信服务
 */
@Slf4j
@DubboService
public class SmsSendUtilImpl implements SmsSendUtil {


    @Value("${Tencent.sms.appId}")
    private int appId;
    @Value("${Tencent.sms.appKey}")
    private String appKey;
    @Value("${Tencent.sms.templateId}")
    private int templateId;
    @Value("${Tencent.sms.sign}")
    private String sign;
    //@Value("${sms.extend}")
    //private String extend;
    //@Value("${sms.ext}")
    //private String ext;
    //@Value("${sms.nationCode}")
    //private String nationCode;


    @DubboReference
    private RedisDubboService redisService;

    @Override
    public boolean smsSend(String phone, String verificationCode) {

        SmsSingleSender ssender = new SmsSingleSender(appId, appKey);
        Integer time = 10;
        String[] params = {verificationCode};
        try {
            //打印参数:"86", phone, templateId, params, sign
            log.info("发送短信参数：{} {} {} {} {}", "86", phone, templateId, params, sign);
            SmsSingleSenderResult rst = ssender.sendWithParam("86", phone, templateId, params, sign, "", "");
            if (rst.result == 0) {
                log.info("发送成功");
                return true;
            } else {
                log.info("发送失败");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public Object getRedisValueByPhone(String key) {
        return redisService.get(key);
    }

    @Override
    public void delRedisValueByPhone(String key) {
        redisService.del(key);
    }

    @Override
    public void setRedisValueByPhone(String key, String value) {
        redisService.set(key, value);
    }

    @Override
    public void setRedisValueByTime(String key, String value, Long time) {
        redisService.setex(key, value, time);

    }

    @Override
    public boolean sendSms(String phone, String verificationCode, int appId, String nationCode, String appKey, int templateId, String sign, String extend, String ext) {

        SmsSingleSender ssender = new SmsSingleSender(appId, appKey);
        int time = 10;
        String[] params = {verificationCode, "" + time};
        try {
            SmsSingleSenderResult rst = ssender.sendWithParam(nationCode, phone, templateId, params, sign, extend, ext);
            if (rst.result == 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

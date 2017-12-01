package cn.godsdo.config.tencent;

import cn.godsdo.util.tencent.CosHelperUtil;
import com.y20y.constant.Constants;
import com.y20y.utils.AESUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RefreshScope
public class CosBean {
    @Value("${Tencent.cos.endPoint}")
    public  String END_POINT;
    @Value(value = "${Tencent.secretId}")
    public  String SECRET_ID;
    @Value(value = "${Tencent.secretKey}")
    public  String SECRET_KEY;
    @Value(value = "${Tencent.cos.bucket}")
    public  String BUCKET;
    @Value(value = "${Tencent.cos.cosRealUrl}")
    public  String COS_REAL_URL;
    @Value(value = "${Tencent.cos.cosFakeUrl}")
    public  String COS_FAKE_URL;
    @Bean("CosBean")
    public CosHelperUtil initBean() {
        CosHelperUtil cosHelperUtil = new CosHelperUtil(END_POINT, SECRET_ID, SECRET_KEY, BUCKET, COS_REAL_URL, COS_FAKE_URL);
        return cosHelperUtil;
    }
}

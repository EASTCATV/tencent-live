package cn.godsdo.dubbo.impl;

import cn.godsdo.demo.DemoService;
import cn.godsdo.util.R;
import cn.godsdo.vo.Demo;
import cn.godsdo.vo.GetCommodityListVo;
import com.y20y.interfaces.IdService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * @Author : yang
 * @Date : 2017/3/10
 * @ApiNote :
 */
@DubboService
@RefreshScope
public class DemoImpl implements DemoService {
    @DubboReference(check = false)
    private IdService idService;
    //@Value("${play.domain}")
    //private String playDomain="asdasd";
    @Override
    public String sayHello(String name) {
        long l = idService.nextId();
        //String aa= playDomain+l;
        return l+"";
    }

    @Override
    public R t1(String aa) {

        Demo demo = new Demo(aa, "b", 1L, "c");

        GetCommodityListVo getCommodityListVo = new GetCommodityListVo();
        getCommodityListVo.setId(1L);
        getCommodityListVo.setCommodity("a");
        getCommodityListVo.setGroupId(1L);
        getCommodityListVo.setGroupName("b");
        getCommodityListVo.setCommodityPrice(null);
        getCommodityListVo.setCommodityLineationPrice(null);
        getCommodityListVo.setImage("c");
        return R.ok(getCommodityListVo);
        //return R.EOK(getCommodityListVo);
        //return  R.EOK(demo);
    }


}

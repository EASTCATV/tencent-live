package cn.godsdo.controller.cache;

import cn.godsdo.dubbo.cache.DelComCache;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author : just do it
 * @ApiNote : 清除系统缓存
 */
@RestController
@CrossOrigin
@RequestMapping("/clearCache")
public class ClearCache {

    @DubboReference
    DelComCache delcomcache;


    @PostMapping("/del")
    public R del(){
        Long comId = ShiroUtil.getComId();
        delcomcache.del(comId);
        return R.ok();
    }



}

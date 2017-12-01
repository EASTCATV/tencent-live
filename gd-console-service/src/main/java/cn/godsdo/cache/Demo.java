package cn.godsdo.cache;

import cn.godsdo.constant.CacheConstants;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

/**
 * @Author : yang
 * @Date : 2018/3/28
 * @ApiNote :
 */
public class Demo {
    /**
     * 缓存注解使用示例
     * @param comId
     * @return
     */
    @Cacheable(key = "#comId",value = CacheConstants.COMMISSION_RANK_BROKER,unless = "#result==null")
    public String demo(Long comId){
        return "sting";
    }



    /**
     * 缓存注解清除示例
     * @param comId
     * @return
     */
    @CacheEvict(key = "#comId",value = CacheConstants.COMMISSION_RANK_BROKER)
    public String delete(Long comId){
        return "sting";
    }
}

package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dubbo.com.ComOpenAuthListService;
import cn.godsdo.entity.com.ComOpenAuthList;
import cn.godsdo.mapper.com.ComOpenAuthListMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 微信开放平台权限集 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-05-14
 */
@DubboService
public class ComOpenAuthListServiceImpl extends ServiceImpl<ComOpenAuthListMapper, ComOpenAuthList> implements ComOpenAuthListService {

    @Override
    public int delById(Long comId) {
        return this.baseMapper.delete(Wrappers.<ComOpenAuthList>lambdaQuery().eq(ComOpenAuthList::getComId, comId));
    }
}

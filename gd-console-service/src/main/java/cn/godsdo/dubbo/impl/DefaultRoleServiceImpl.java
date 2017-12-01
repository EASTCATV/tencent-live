package cn.godsdo.dubbo.impl;

import cn.godsdo.dubbo.DefaultRoleService;
import cn.godsdo.entity.DefaultRole;
import cn.godsdo.mapper.DefaultRoleMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * <p>
 * 智客默认角色库,创建账户时默认映射库 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@DubboService
public class DefaultRoleServiceImpl extends ServiceImpl<DefaultRoleMapper, DefaultRole> implements DefaultRoleService {

}

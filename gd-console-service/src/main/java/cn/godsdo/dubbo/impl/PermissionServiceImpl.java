package cn.godsdo.dubbo.impl;

import cn.godsdo.dubbo.PermissionService;
import cn.godsdo.entity.Permission;
import cn.godsdo.mapper.PermissionMapper;
import cn.godsdo.enums.PermissionsTypeEnum;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

/**
 * <p>
 * 后台权限表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@Slf4j
@DubboService
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    @Override
    public List<Permission> getSysPermissions(List<Long> ids) {
        if (ObjectUtil.isEmpty(ids)) {
            log.info("权限id为空，ids={}",ids);
            return null;
        }
        return this.baseMapper.selectList(new LambdaQueryWrapper<Permission>()
                .in(Permission::getId, ids).eq(Permission::getDeleteFlg, false)
                .eq(Permission::getPermissionsType, PermissionsTypeEnum.CONTROL.getValue()));
    }

    @Override
    public List<Permission> getAll() {
        return this.baseMapper.selectList(new LambdaQueryWrapper<Permission>().eq(Permission::getDeleteFlg, false)
                .eq(Permission::getPermissionsType, PermissionsTypeEnum.CONTROL.getValue()));
    }
}

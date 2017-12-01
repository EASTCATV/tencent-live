package cn.godsdo.dubbo.impl;

import cn.godsdo.dubbo.RolePermissionService;
import cn.godsdo.entity.Permission;
import cn.godsdo.entity.RolePermission;
import cn.godsdo.mapper.RolePermissionMapper;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 角色-权限关联表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-03-26
 */
@DubboService
public class RolePermissionServiceImpl extends ServiceImpl<RolePermissionMapper, RolePermission> implements RolePermissionService {


    @Override
    public List<RolePermission> getSysRolePermissions(Long roleId) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId).eq(RolePermission::getDeleteFlg, false));
    }

    @Override
    public R getPermissionListByRoleId(Long roleId) {
        List<Permission> permissionListByRoleId = this.baseMapper.getPermissionListByRoleId(roleId);
        return R.ok(permissionListByRoleId);
    }

    @Override
    public List<String> getPermissionsByRoleId(Long roleId, Long comId) {
        return this.baseMapper.getPermissionsByRoleId(comId,roleId);
    }

    @Override
    public List<String> getAllPermissions(ArrayList<Long> ids) {
        return this.baseMapper.getAllPermissions(ids);
    }
}

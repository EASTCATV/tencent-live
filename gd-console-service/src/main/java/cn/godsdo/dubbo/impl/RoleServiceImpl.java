package cn.godsdo.dubbo.impl;

import cn.godsdo.base.BasePage;
import cn.godsdo.constant.RoleConstants;
import cn.godsdo.dubbo.RoleService;
import cn.godsdo.entity.*;
import cn.godsdo.enums.PermissionsTypeEnum;
import cn.godsdo.enums.RoleTypeEnum;
import cn.godsdo.enums.live.LiveRoleEnum;
import cn.godsdo.mapper.*;
import cn.godsdo.util.R;
import cn.godsdo.vo.GetRoleDataVo;
import cn.godsdo.vo.GetRoleListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * 《用户角色管理表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-03-26
 */
@DubboService
@Slf4j
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Resource()
    DefaultRoleMapper defaultRoleMapper;

    @Resource()
    RolePermissionMapper rolePermissionMapper;
    @Resource()
    PermissionMapper permissionMapper;
    @Resource()
    AccountDatMapper accountDatMapper;
    @Resource()
    PermissionDatMapper permissionDatMapper;
    @DubboReference
    IdService idService;

    @Override
    public Role getSysRole(Long roleId) {
        return this.baseMapper.selectById(roleId);
    }

    @Override
    public boolean checkAdmin(Long roleId, Long comId) {
        try {
            Role role = this.baseMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getId, roleId)
                    .eq(Role::getComId, comId).eq(Role::getDeleteFlg, false));
            if (ObjectUtils.isEmpty(role)) {
                return false;
            } else {
                Integer roleType = role.getRoleType();
                if (0 == roleType) {
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("判断是否为大账户报错 romId{},comId{}", roleId, comId);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void initRoleByRegister(Long comId, Long roleId) {
        List<Role> rolesList = new ArrayList<>();
        // 生成管理员角色
        Role role = new Role(roleId, comId, RoleConstants.DEFAULT_ROLE_NAME, RoleConstants.DEFAULT_ROLE_TYPE, RoleConstants.DEFAULT_ROLE_NAME);
        rolesList.add(role);
//        //创建默认员工角色和权限
        List<DefaultRole> defaultRoles = defaultRoleMapper.selectList(new LambdaQueryWrapper<DefaultRole>());
        for (DefaultRole d : defaultRoles) {
            if (!d.getRoleType().equals(RoleTypeEnum.MANGER.getValue())) {
                long id = idService.nextId();
                Role other = new Role(id, comId, d.getRoleName(), d.getRoleType(), d.getRoleComment());
                rolesList.add(other);
                //创建班主任权限 讲师权限
                initRolePermissionByRegister(comId, id);
            }
        }
        this.baseMapper.insertBatch(rolesList);
    }

    @Override
    public void initRolePermissionByRegister(Long comId, Long roleId) {
        List<PermissionDat> list = permissionDatMapper.selectList(new LambdaQueryWrapper<PermissionDat>().eq(PermissionDat::getDeleteFlag, false));
        List<RolePermission> permissionsS = new ArrayList<>();
        for (PermissionDat role : list) {
            RolePermission sysRolePermission = new RolePermission();
            sysRolePermission.setPermissionId(role.getId());
            sysRolePermission.setComId(comId);
            sysRolePermission.setRoleId(roleId);
            sysRolePermission.setType(role.getType());
            sysRolePermission.setId(idService.nextId());
            permissionsS.add(sysRolePermission);
        }
        if (ObjectUtils.isNotEmpty(permissionsS)) {
            rolePermissionMapper.insertBatch(permissionsS);
        }
    }

    @Override
    public R getRoleList(Long comId, BasePage basePage) {
        Page<Role> page = new Page<>(basePage.getPage(), basePage.getPageSize());
        IPage<GetRoleListVo> vo = this.baseMapper.getRoleList(page, comId);
        return R.ok(vo.getRecords(), vo.getTotal());
    }

    @Override
    public R getRoleBySelect(Long comId) {
        List<Role> vo = getRoles(comId);
        Role role = new Role(0L, comId, "全部", 0, "全部");
        vo.add(0, role);
        return R.ok(vo);
    }

    @Override
    public R getRoleByUpdate(Long comId) {
        List<Role> vo = getRoles(comId);
        return R.ok(vo);
    }

    /**
     * 获取角色
     * @param comId
     * @return
     */
    private List<Role> getRoles(Long comId) {
        List<Role> vo = this.baseMapper.selectList(new LambdaQueryWrapper<Role>()
                        .select(Role::getId, Role::getRoleName)
                        .eq(Role::getDeleteFlg, false).eq(Role::getComId, comId)
        );
        return vo;
    }

    @Override
    public R getRoleBySelectByAdd(Long comId) {
        List<Role> vo = this.baseMapper.selectList(new LambdaQueryWrapper<Role>()
                .select(Role::getId, Role::getRoleName)
                .eq(Role::getDeleteFlg, false).eq(Role::getComId, comId)
                .ne(Role::getRoleType, RoleTypeEnum.MANGER.getValue())
        );
//        Role role = new Role(0L, comId, "全部", 0, "全部");
//        vo.add(0, role);
        return R.ok(vo);
    }

    @Override
    public R delRole(Long comId, Long accountId, Long roleId) {
        Role role = this.baseMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getId, roleId)
                .eq(Role::getDeleteFlg, false).eq(Role::getComId, comId));
        if (ObjectUtils.isEmpty(role)) {
            return R.failed("角色不存在");
        }

        List<AccountDat> accountDats = accountDatMapper.selectList(new LambdaQueryWrapper<AccountDat>().eq(AccountDat::getRoleId, roleId)
                .eq(AccountDat::getComId, comId).eq(AccountDat::getDeleteFlg, false));
        if (ObjectUtils.isNotEmpty(accountDats)) {
            return R.failed("该角色已绑定员工，不可删除");
        }
        role.setDeleteFlg(1);
        role.setUpdateBy(accountId);
        this.baseMapper.updateById(role);
        return R.ok();
    }

    @Override
    public R addRole(Long comId, Long accountId, String roleName) {
        Role role = this.baseMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleName, roleName)
                .eq(Role::getDeleteFlg, false).eq(Role::getComId, comId));
        if (ObjectUtils.isNotEmpty(role)) {
            return R.failed("角色已存在");
        }
        Role role1 = new Role();
        role1.setId(idService.nextId());
        role1.setComId(comId);
        role1.setRoleName(roleName);
        role1.setRoleType(1);
        role1.setUpdateBy(accountId);
        this.baseMapper.insert(role1);
        return R.ok();
    }

    @Override
    public R getRoleData(Long roleId, Long comId) {
        Role role = this.baseMapper.selectById(roleId);
        if (ObjectUtils.isEmpty(role)) {
            return R.failed("角色信息不存在");
        }
        GetRoleDataVo getRoleDataVo = new GetRoleDataVo();
        getRoleDataVo.setId(role.getId());
        getRoleDataVo.setRoleName(role.getRoleName());
        List<Long> assistantListByRoleId = rolePermissionMapper.getListByRoleId(roleId, comId, PermissionsTypeEnum.assistant.getValue());
        List<Long> consoleListByRoleId = rolePermissionMapper.getListByRoleId(roleId, comId, PermissionsTypeEnum.CONTROL.getValue());
        getRoleDataVo.setAssistantList(assistantListByRoleId);
        getRoleDataVo.setConsoleList(consoleListByRoleId);
        return R.ok(getRoleDataVo);
    }

    @Override
    public R updateRoleData(Long comId, Long accountId, GetRoleDataVo vo) {
        Long id = vo.getId();
        Role role = this.baseMapper.selectById(vo.getId());
        if (ObjectUtils.isEmpty(role)) {
            return R.failed("角色信息不存在");
        }
        role.setUpdateBy(accountId);
        role.setRoleName(vo.getRoleName());
        this.baseMapper.updateById(role);
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id)
                .eq(RolePermission::getDeleteFlg, false).eq(RolePermission::getComId, comId));
        List<RolePermission> list = new ArrayList<>();
        for (Long PermissionId : vo.getConsoleList()) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setId(idService.nextId());
            rolePermission.setRoleId(id);
            rolePermission.setPermissionId(PermissionId);
            rolePermission.setComId(comId);
            rolePermission.setType(PermissionsTypeEnum.CONTROL.getValue());
            rolePermission.setCreateBy(accountId);
            list.add(rolePermission);
        }
        for (Long PermissionId : vo.getAssistantList()) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setId(idService.nextId());
            rolePermission.setRoleId(id);
            rolePermission.setPermissionId(PermissionId);
            rolePermission.setComId(comId);
            rolePermission.setType(PermissionsTypeEnum.assistant.getValue());
            rolePermission.setCreateBy(accountId);
            list.add(rolePermission);
        }
        rolePermissionMapper.insertBatch(list);
        return R.ok();
    }

    //初始化讲师权限
    public void defaultTeacherPermission(Long roleId) {
        // todo 确认权限列表之后进行修改
        List<Integer> permissionIds = Arrays.asList(243, 242, 255, 261, 260, 262, 254, 23, 24, 102, 103, 104, 237, 213, 227, 214, 215, 216, 217, 218, 219, 220, 48, 43, 124, 253, 257);
        List<RolePermission> addPermissions = new ArrayList<>();
        for (Integer sysPermissionId : permissionIds) {
            RolePermission sysRolePermission = new RolePermission();
            sysRolePermission.setPermissionId(sysPermissionId.longValue());
            sysRolePermission.setRoleId(roleId);
            sysRolePermission.setId(idService.nextId());
            addPermissions.add(sysRolePermission);
        }
        //批量添加
        if (ObjectUtils.isNotEmpty(addPermissions)) {
            rolePermissionMapper.insertBatch(addPermissions);
        }
    }
}

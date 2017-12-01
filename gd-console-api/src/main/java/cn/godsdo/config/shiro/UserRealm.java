package cn.godsdo.config.shiro;

import cn.godsdo.config.jwt.JWTToken;
import cn.godsdo.config.jwt.JWTUtil;
import cn.godsdo.dubbo.com.AccountDatService;
import cn.godsdo.dubbo.PermissionService;
import cn.godsdo.dubbo.RolePermissionService;
import cn.godsdo.dubbo.RoleService;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.entity.Permission;
import cn.godsdo.entity.Role;
import cn.godsdo.entity.RolePermission;
import cn.godsdo.util.TracerContextUtils;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.y20y.constant.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.Set;

/**
 * @Author : li
 * @Date : 2018/11/18
 * @ApiNote :
 */
@Slf4j
public class UserRealm extends AuthorizingRealm {

    @DubboReference
    private AccountDatService accountService;
    @DubboReference
    private RolePermissionService rolePermissionService;
    @DubboReference
    private RoleService roleService;
    @DubboReference
    private PermissionService permissionService;


    /**
     * 必须重写此方法，不然会报错
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JWTToken;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        // 权限认证
        String username = JWTUtil.getUsername(principals.toString());
        Long comId = JWTUtil.getComId(principals.toString());
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        // 查询用户信息
        AccountDat accountDat = accountService.getAccountByUserNameAndComId(username, comId);
        //获得角色
        List<RolePermission> rolePermissions = rolePermissionService.getSysRolePermissions(accountDat.getRoleId().longValue());
        List<Long> ids = Lists.newArrayList();
        for (RolePermission sysRolePermission : rolePermissions) {
            ids.add(sysRolePermission.getPermissionId());
        }

        // 检索权限id对应的权限code
        List<Permission> sysPermissions = permissionService.getSysPermissions(ids);
        Role sysRole = roleService.getSysRole(accountDat.getRoleId());
        Set<String> permissions = Sets.newHashSet();
        List<String> menus = Lists.newArrayList();
        //循环获取权限code和菜单信息
        if (sysPermissions != null) {
            for (Permission sysPermission : sysPermissions) {
                permissions.add(sysPermission.getPermissionCode());
                menus.add(sysPermission.getMenuCode());
            }
        }
        Set<String> roleSet = Sets.newHashSet();
        roleSet.add(sysRole.getRoleName());
        // 设置该用户拥有的角色和权限
        info.setRoles(roleSet);
        info.setStringPermissions(permissions);
        return info;
    }

    /**
     * 验证当前登录的Subject
     * LoginController.login()方法中执行Subject.login()时 执行此方法
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {
        String token = (String) authcToken.getCredentials();
        // 解密获得username，用于和数据库进行对比
        String username = JWTUtil.getUsername(token);

        Long comId = JWTUtil.getComId(token);
        if (username == null || !JWTUtil.verify(token, username)) {
            throw new AuthenticationException("token认证失败！");
        }
        AccountDat accountDat = accountService.getAccountByUserNameAndComId(username, comId);
        if (accountDat == null) {
            throw new AuthenticationException("该用户不存在！");
        }
        AccountDat accountDat1 = new AccountDat();
        BeanUtils.copyProperties(accountDat, accountDat1);
        accountDat1.setPassword("******");
        TracerContextUtils.setTracerId(JSONObject.toJSONString(accountDat1));
        if (accountDat != null && accountDat.getStatus().equals("INVALID")) {
            throw new AuthenticationException("该用户已被冻结！");
        }

        boolean b = roleService.checkAdmin(accountDat.getRoleId(), comId);
        JSONObject accountJson = (JSONObject) JSONObject.toJSON(accountDat);
        accountJson.put("isAdmin", b);
        SecurityUtils.getSubject().getSession().setAttribute(Constants.SESSION_ACCOUNT_INFO, accountJson);
//        List<Permission> sysPermissions = null;
//        if (b) {
//            // 主账户获取所有权限code
//            sysPermissions = permissionService.getAll();
//        } else {
//            // 根据用户的角色id,获取改权限所有的菜单id
//            List<RolePermission> rolePermissions = rolePermissionService.getSysRolePermissions(accountDat.getRoleId().longValue());
//
//            List<Long> ids = Lists.newArrayList();
//            for (RolePermission sysRolePermission : rolePermissions) {
//                ids.add(sysRolePermission.getPermissionId());
//            }
//            //检索权限id对应的权限code
//            sysPermissions = permissionService.getSysPermissions(ids);
//        }
//        List<String> permissions = Lists.newArrayList();
//        List<String> menus = Lists.newArrayList();
        //循环获取权限code和菜单信息
//        if (ObjectUtils.isNotEmpty(sysPermissions)) {
//            for (Permission sysPermission : sysPermissions) {
//                permissions.add(sysPermission.getPermissionCode());
//                menus.add(sysPermission.getMenuCode());
//            }
//        }
//        SecurityUtils.getSubject().getSession().setAttribute(Constants.SESSION_ACCOUNT_MENU, menus);
//        SecurityUtils.getSubject().getSession().setAttribute(Constants.SESSION_ACCOUNT_PERMISSION, permissions);
        return new SimpleAuthenticationInfo(token, token, "MyRealm");
    }
}

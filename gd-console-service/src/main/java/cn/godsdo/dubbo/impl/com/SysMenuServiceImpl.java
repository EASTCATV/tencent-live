package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dubbo.com.SysMenuService;
import cn.godsdo.entity.com.SysMenu;
import cn.godsdo.mapper.com.SysMenuMapper;
import cn.godsdo.util.R;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import lombok.AllArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.baomidou.mybatisplus.extension.toolkit.Db.updateById;

/**
 * 菜单管理
 *
 * @author 阿沐 babamu@126.com
 * <a href="https://maku.net">MAKU</a>
 */
@DubboService
@AllArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
//    private final SysRoleMenuService sysRoleMenuService;

//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public void save(SysMenuVO vo) {
//        SysMenuEntity entity = SysMenuConvert.INSTANCE.convert(vo);
//
//        // 保存菜单
//        baseMapper.insert(entity);
//    }
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public void update(SysMenuVO vo) {
//        SysMenuEntity entity = SysMenuConvert.INSTANCE.convert(vo);
//
//        // 上级菜单不能为自己
//        if (entity.getId().equals(entity.getPid())) {
//            throw new ServerException("上级菜单不能为自己");
//        }
//
//        // 更新菜单
//        updateById(entity);
//    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 删除菜单
        removeById(id);

        // 删除角色菜单关系
//        sysRoleMenuService.deleteByMenuId(id);
    }

    @Override
    public List<SysMenu> getMenuList(Integer type) {
        List<SysMenu> menuList = this.baseMapper.getMenuList(type);

        return menuList;
    }

    @Override
    public List<SysMenu> getUserMenuList(Integer isAdmin, Integer type) {
        List<SysMenu> menuList;

//        if (isAdmin == Constants.ADMIN_PERMISSION) {
            menuList = this.baseMapper.getMenuList(type);
//        }else{
//            menuList = baseMapper.getUserMenuList(user.getId(), type);
//        }


        return menuList;
    }

    @Override
    public Long getSubMenuCount(Long pid) {
        return count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getPid, pid));
    }
//
//    @Override
//    public Set<String> getUserAuthority(UserDetail user) {
//        // 系统管理员，拥有最高权限
//        List<String> authorityList;
//        if (user.getSuperAdmin().equals(SuperAdminEnum.YES.getValue())) {
//            authorityList = baseMapper.getAuthorityList();
//        } else {
//            authorityList = baseMapper.getUserAuthorityList(user.getId());
//        }
//
//        // 用户权限列表
//        Set<String> permsSet = new HashSet<>();
//        for (String authority : authorityList) {
//            if (StrUtil.isBlank(authority)) {
//                continue;
//            }
//            permsSet.addAll(Arrays.asList(authority.trim().split(",")));
//        }
//
//        return permsSet;
//    }

}
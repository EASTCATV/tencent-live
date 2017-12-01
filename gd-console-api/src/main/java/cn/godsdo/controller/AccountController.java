package cn.godsdo.controller;

import cn.godsdo.base.BasePage;
import cn.godsdo.dto.user.AddAccountDto;
import cn.godsdo.dto.user.GetAccountListDto;
import cn.godsdo.dubbo.RolePermissionService;
import cn.godsdo.dubbo.RoleService;
import cn.godsdo.dubbo.com.AccountDatService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.vo.GetRoleDataVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * @author W~Y~H
 * @Date : 2018/11/18
 */
@RestController
@CrossOrigin
@RequestMapping("/account")
public class AccountController {
    @DubboReference
    AccountDatService accountDatService;
    @DubboReference
    RoleService roleService;
    @DubboReference
    RolePermissionService rolePermissionService;

    /**
     * 获取讲师列表
     *
     * @return
     */
    @GetMapping("/getLecturerList")
    public R getLecturerList() {
        Long comId = ShiroUtil.getComId();
        return accountDatService.getLecturerList(comId);
    }


    /**
     * 修改密码
     *
     * @return
     */
    @GetMapping("/updatePassword")
    public R updatePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return accountDatService.updatePassword(accountId, comId, oldPassword, newPassword);
    }


    /**
     * 获取权限列表
     *
     * @return
     */
    @PostMapping("/getRoleList")
    public R getRoleList(@RequestBody BasePage basePage) {
        Long comId = ShiroUtil.getComId();
        return roleService.getRoleList(comId, basePage);
    }


    /**
     * 删除权限数据
     *
     * @return
     */
    @GetMapping("/delRole")
    public R delRole(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return roleService.delRole(comId, accountId, id);
    }

    /**
     * 新增权限数据
     *
     * @return
     */
    @GetMapping("/addRole")
    public R addRole(@RequestParam("roleName") String roleName) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return roleService.addRole(comId, accountId, roleName);
    }

    @GetMapping("/getPermissionListByRoleId")
    public R getPermissionListByRoleId(@RequestParam("roleId") Long roleId) {
        return rolePermissionService.getPermissionListByRoleId(roleId);
    }


    /**
     * 获取员工列表
     *
     * @return
     */
    @PostMapping("/getAccountList")
    public R getAccountList(@RequestBody GetAccountListDto dto) {
        Long comId = ShiroUtil.getComId();
        return accountDatService.getAccountList(dto, comId);
    }

    /**
     * 新增员工
     *
     * @return
     */
    @PostMapping("/addAccount")
    public R addAccount(@RequestBody AddAccountDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return accountDatService.addAccount(dto, accountId, comId);
    }

    /**
     * 获取员工信息
     *
     * @return
     */
    @GetMapping("/getAccountInfo")
    public R getAccountInfo(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        return accountDatService.getAccountInfo(comId, id);
    }

    /**
     * 修改员工列表
     *
     * @return
     */
    @PostMapping("/updateAccount")
    public R updateAccount(@RequestBody AddAccountDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return accountDatService.updateAccount(dto, accountId, comId);
    }

    /**
     * 删除员工列表
     *
     * @return
     */
    @GetMapping("/delAccount")
    public R delAccount(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return accountDatService.delAccount(id, accountId, comId);
    }

    /**
     * 获取角色列表
     *
     * @return
     */
    @GetMapping("/getRoleBySelect")
    public R getRoleBySelect() {
        Long comId = ShiroUtil.getComId();
        return roleService.getRoleBySelect(comId);
    }
    /**
     * 获取角色列表
     *
     * @return
     */
    @GetMapping("/getRoleByUpdate")
    public R getRoleByUpdate() {
        Long comId = ShiroUtil.getComId();
        return roleService.getRoleByUpdate(comId);
    }

    @GetMapping("/getRoleBySelectByAdd")
    public R getRoleBySelectByAdd() {
        Long comId = ShiroUtil.getComId();
        return roleService.getRoleBySelectByAdd(comId);
    }

    /**
     * 获取角色信息
     *
     * @return
     */
    @GetMapping("/getRoleData")
    public R getRoleData(@RequestParam("roleId") Long roleId) {
        Long comId = ShiroUtil.getComId();
        return roleService.getRoleData(roleId, comId);
    }

    /**
     * 修改角色信息
     *
     * @return
     */
    @PostMapping("/updateRoleData")
    public R updateRoleData(@RequestBody GetRoleDataVo vo) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return roleService.updateRoleData(comId, accountId, vo);
    }

    /**
     * 获取所有员工列表。用于渠道
     *
     * @return
     */
    @GetMapping("/getAllAccountByChannel")
    public R getAllAccountByChannel() {
        Long comId = ShiroUtil.getComId();
        return accountDatService.getAllAccountByChannel(comId);
    }
}

package cn.godsdo.dubbo.impl;

import cn.godsdo.constant.RedisConstants;
import cn.godsdo.constant.RoleConstants;
import cn.godsdo.dto.GetAccountInfoDto;
import cn.godsdo.dto.assistan.BanIpDto;
import cn.godsdo.dto.user.AddAccountDto;
import cn.godsdo.dto.user.GetAccountListDto;
import cn.godsdo.dubbo.channel.ComChannelDatService;
import cn.godsdo.dubbo.com.AccountDatService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.entity.AccountProjectDat;
import cn.godsdo.entity.Role;
import cn.godsdo.entity.channel.ComChannelDat;
import cn.godsdo.entity.com.ComAccountProject;
import cn.godsdo.entity.com.CompanyDat;
import cn.godsdo.entity.live.LiveDat;
import cn.godsdo.enums.DataPermissionsEnun;
import cn.godsdo.mapper.AccountDatMapper;
import cn.godsdo.mapper.AccountProjectDatMapper;
import cn.godsdo.mapper.RoleMapper;
import cn.godsdo.mapper.channel.ComChannelDatMapper;
import cn.godsdo.mapper.com.ComAccountProjectMapper;
import cn.godsdo.mapper.com.CompanyDatMapper;
import cn.godsdo.mapper.live.LiveDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.tencent.CosHelperUtil;
import cn.godsdo.vo.GetAccountListVo;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONObject;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import com.y20y.utils.AESUtil;
import com.y20y.utils.ToBase62;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 《账户管理表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@Slf4j
@DubboService
public class AccountDatServiceImpl extends ServiceImpl<AccountDatMapper, AccountDat> implements AccountDatService {


    @Resource
    ComAccountProjectMapper comAccountProjectMapper;
    @Resource
    CompanyDatMapper companyDatMapper;
    @DubboReference
    ComChannelDatService comChannelDatService;
    @DubboReference
    IdService idService;
    @Resource
    RoleMapper roleMapper;
    @Resource
    LiveDatMapper liveDatMapper;
    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;
    @DubboReference(check = false)
    private RedisDubboService redisDubboService;
    @Resource
    AccountProjectDatMapper accountProjectDatMapper;

    @Resource
    ComChannelDatMapper comChannelDatMapper;

    @Override
    public AccountDat getAccountDatByLogin(String login) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<AccountDat>().eq(AccountDat::getLogin, login).eq(AccountDat::getDeleteFlg, false));
    }

    @Override
    public AccountDat getAccountByUserNameAndComId(String username, Long comId) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<AccountDat>().eq(AccountDat::getLogin, username).eq(AccountDat::getComId, comId).eq(AccountDat::getDeleteFlg, false));
    }

    @Override
    public R getLecturerList(Long comId) {
        List<AccountDat> vo = this.baseMapper.getLecturerList(comId);
        return R.ok(vo);
    }

    @Override
    public R updatePassword(Long accountId, Long comId, String oldPassword, String newPassword) {
        // 根据账号ID和公司ID查询账号信息
        AccountDat accountDat = this.baseMapper.selectOne(new LambdaQueryWrapper<AccountDat>().eq(AccountDat::getId, accountId).eq(AccountDat::getComId, comId).eq(AccountDat::getDeleteFlg, false));
        // 如果账号信息为空，则返回失败结果并提示账户不存在
        if (ObjectUtil.isEmpty(accountDat)) {
            return R.failed("账户不存在");
        }
        String pwd = "";
        try {
            // 解析用户加密的密码
            pwd = AESUtil.Decrypt(accountDat.getPassword(), Constants.PASSWORD_KEY);

        } catch (Exception e) {
            e.printStackTrace();
        }
        // 如果输入的旧密码与解析后的密码不匹配，则返回失败结果并提示旧密码错误
        if (!pwd.equals(oldPassword)) {
            return R.failed("旧密码错误");
        }
        String encrypt = "";
        try {
            encrypt = AESUtil.Encrypt(newPassword, Constants.PASSWORD_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 更新账号密码为新密码
        accountDat.setPassword(encrypt);
        this.baseMapper.updateById(accountDat);
        // 返回成功结果
        return R.ok();
    }

    @Override
    public R getAccountList(GetAccountListDto dto, Long comId) {
//        Long projectId = dto.getRoleId();
//        if (projectId != 0) {
//            // 获取项目信息
//            ComAccountProject comAccountProject = getComAccountProject(comId, projectId);
//            if (comAccountProject.getParentId() == 0) {
//                // 获取子项目的ID列表
//                List<Long> childrenIds = comAccountProjectMapper.getChildrenIds(projectId, comId);
//                childrenIds.add(projectId);
//                // 设置项目ID列表到dto中
//                dto.setProjectIds(childrenIds);
//            } else {
//                // 设置单个项目ID到dto中
//                dto.setProjectIds(Arrays.asList(projectId));
//            }
//        } else {
//            // 项目ID为0时，将0L放入项目ID列表
//            dto.setProjectIds(null);
//        }

        Page<AccountDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 获取账户列表数据
        IPage<GetAccountListVo> vo = this.baseMapper.getAccountList(page, dto, comId);
        // 查询公司信息
        CompanyDat cm = companyDatMapper.selectOne(new LambdaQueryWrapper<CompanyDat>()
                .eq(CompanyDat::getId, comId));
        // 获取公司名称
        String company = cm.getCompany();
        Long total = vo.getTotal();
        Long adminRoleId;
        if (total > 0) {
            Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>()
                    .eq(Role::getRoleType, 0)
                    .eq(Role::getComId, comId)
            );
            if (role != null) {
                adminRoleId = role.getId();
            } else {
                adminRoleId = -1L;
            }
        } else {
            adminRoleId = -1L;
        }
        List<GetAccountListVo> collect = vo.getRecords().stream().map(e -> {
            if (e.getRoleId().equals(adminRoleId)) {
                e.setRoleName(RoleConstants.DEFAULT_ROLE_NAME);
                e.setIsAdmin(true);
            } else {
                e.setIsAdmin(false);
            }
            // 解析密码
            String password = e.getPassword();
            try {
                password = AESUtil.Decrypt(password, Constants.PASSWORD_KEY);
                e.setPassword(password);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            Long project = e.getProject();
            if (project == 0) {
                // 设置项目名称为公司名称
                e.setProjectName(company);
            } else {
                // 获取项目信息
                ComAccountProject comAccountProject = getComAccountProject(comId, project);
                Long parentId = comAccountProject.getParentId();
                if (parentId != 0) {
                    ComAccountProject comAccountProjectParent = getComAccountProject(comId, parentId);
                    // 设置项目名称为公司名称->父项目名称->子项目名称
                    e.setProjectName(company + "->" + comAccountProjectParent.getTitle() + "->" + comAccountProject.getTitle());
                } else {
                    // 设置项目名称为公司名称->子项目名称
                    e.setProjectName(company + "->" + comAccountProject.getTitle());
                }
            }
            return e;
            // 转换为List并返回处理后的数据
        }).collect(Collectors.toList());
        return R.ok(collect, total);
    }

    @Override
    public R addAccount(AddAccountDto dto, Long accountId, Long comId) {
        String login = dto.getLogin();
        String s = ToBase62.encodeToBase62(comId);
        login = login + "@" + s;
        AccountDat accountDat1 = this.baseMapper.selectOne(new LambdaQueryWrapper<AccountDat>().eq(AccountDat::getLogin, login)
                .eq(AccountDat::getComId, comId).eq(AccountDat::getDeleteFlg, false));
        if (ObjectUtil.isNotEmpty(accountDat1)) {
            return R.failed("该账号已存在");
        }
        String employeeNum = dto.getEmployeeNum();
        List<AccountDat> accountDats = this.baseMapper.selectList(new LambdaQueryWrapper<AccountDat>()
                .eq(AccountDat::getComId, comId).eq(AccountDat::getEmployeeNum, employeeNum).eq(AccountDat::getDeleteFlg, false));
        if (ObjectUtil.isNotEmpty(accountDats)) {
            return R.failed("员工工号不能重复");
        }
        Integer dataPermissions = dto.getDataPermissions();
        if (DataPermissionsEnun.CUSTOM.getValue() == dataPermissions) {
            List<Long> ids = dto.getIds();
            if (ObjectUtil.isEmpty(ids)) {
                return R.failed("请选择项目权限");
            }
        }
        // 在函数/方法/类级别上添加注释
        Long userId = idService.nextId();
        // 创建一个 AccountDat 实例
        AccountDat accountDat = new AccountDat();
        // 设置 AccountDat 实例的属性值
        accountDat.setId(userId);
        accountDat.setName(dto.getName());
//        String s = ToBase62.encodeToBase62(comId);
        accountDat.setLogin(login);
        String image = dto.getProfile();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadCover(image, comId);
        }
        accountDat.setProfile(image);
        String password = dto.getPassword();
        try {
            password = AESUtil.Encrypt(password, Constants.PASSWORD_KEY);
            accountDat.setPassword(password);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        accountDat.setRoleId(dto.getRoleId());
        accountDat.setEmployeeNum(employeeNum);

        accountDat.setDataPermissions(dataPermissions);
        accountDat.setProject(dto.getProject());
        accountDat.setCreateBy(accountId);
        accountDat.setComId(comId);
        // 将 AccountDat 实例插入数据库
        this.baseMapper.insert(accountDat);
        if (DataPermissionsEnun.CUSTOM.getValue() == dataPermissions) {
            List<Long> ids = dto.getIds();
            List<AccountProjectDat> list = new ArrayList<>();
            for (Long id : ids) {
                AccountProjectDat apd = new AccountProjectDat();
                apd.setId(idService.nextId());
                apd.setComId(comId);
                apd.setAccountId(userId);
                apd.setProjectId(id);
                apd.setCreateBy(accountId);
                list.add(apd);
            }
            accountProjectDatMapper.insertBatch(list);
        }

        // 如果需要创建渠道
        if (dto.getCreateChannle()) {
            // 创建一个 ComChannelDat 实例
            ComChannelDat comChannelDat = new ComChannelDat();
            // 设置 ComChannelDat 实例的属性值
            comChannelDat.setId(idService.nextId());
            comChannelDat.setName(dto.getName());
            comChannelDat.setComId(comId);
            comChannelDat.setGroupId(dto.getGroupId());
            comChannelDat.setAccountId(userId);
            comChannelDat.setCreateBy(accountId);
            // 获取随机渠道号
            String randomNum = comChannelDatService.getRandomNum(comId);
            comChannelDat.setChannelNo(randomNum);
            // 保存 ComChannelDat 实例
            comChannelDatService.save(comChannelDat);
        }

        // 返回操作成功的响应
        return R.ok();

    }

    @Override
    public R updateAccount(AddAccountDto dto, Long accountId, Long comId) {
        Long id = dto.getId();
        AccountDat accountDat = this.baseMapper.selectOne(new LambdaQueryWrapper<AccountDat>()
                .eq(AccountDat::getComId, comId).eq(AccountDat::getId, id).eq(AccountDat::getDeleteFlg, false));
        if (ObjectUtil.isEmpty(accountDat)) {
            return R.failed("账户不存在");
        }
        String employeeNum = dto.getEmployeeNum();
        List<AccountDat> accountDats = this.baseMapper.selectList(new LambdaQueryWrapper<AccountDat>().ne(AccountDat::getId, id)
                .eq(AccountDat::getComId, comId).eq(AccountDat::getEmployeeNum, employeeNum).eq(AccountDat::getDeleteFlg, false));
        if (ObjectUtil.isNotEmpty(accountDats)) {
            return R.failed("员工工号不能重复");
        }
        Integer dataPermissions = dto.getDataPermissions();
        if (DataPermissionsEnun.CUSTOM.getValue() == dataPermissions) {
            List<Long> ids = dto.getIds();
            if (ObjectUtil.isEmpty(ids)) {
                return R.failed("请选择项目权限");
            }
        }
        accountDat.setName(dto.getName());
        accountDat.setLogin(dto.getLogin());
        // 解析密码
        String password = dto.getPassword();
        try {
            password = AESUtil.Encrypt(password, Constants.PASSWORD_KEY);
            accountDat.setPassword(password);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        accountDat.setPassword(dto.getPassword());
        accountDat.setRoleId(dto.getRoleId());
        accountDat.setDataPermissions(dto.getDataPermissions());
        // 修改头像
        String image = dto.getProfile();
        if (ObjectUtils.isNotEmpty(image) && !image.startsWith("http")) {
            image = cosHelperUtil.uploadCover(image, comId);
        }
        accountDat.setProfile(image);

        accountDat.setEmployeeNum(employeeNum);
        accountDat.setProject(dto.getProject());
        accountDat.setUpdateBy(accountId);
        this.baseMapper.updateById(accountDat);
        if (DataPermissionsEnun.CUSTOM.getValue() == dataPermissions) {
            accountProjectDatMapper.update(null, new LambdaUpdateWrapper<AccountProjectDat>()
                    .set(AccountProjectDat::getDeleteFlg, true)
                    .set(AccountProjectDat::getUpdateBy, accountId)
                    .eq(AccountProjectDat::getDeleteFlg, false)
                    .eq(AccountProjectDat::getComId, comId)
                    .eq(AccountProjectDat::getAccountId, id)
            );
            List<Long> ids = dto.getIds();
            List<AccountProjectDat> list = new ArrayList<>();
            for (Long projectId : ids) {
                AccountProjectDat apd = new AccountProjectDat();
                apd.setId(idService.nextId());
                apd.setComId(comId);
                apd.setAccountId(id);
                apd.setProjectId(projectId);
                apd.setCreateBy(accountId);
                list.add(apd);
            }
            accountProjectDatMapper.insertBatch(list);
        }
        return R.ok();
    }

    @Override
    public R getAllAccountByChannel(Long comId) {
        List<AccountDat> list = this.baseMapper.selectList(new LambdaQueryWrapper<AccountDat>()
                .select(AccountDat::getId, AccountDat::getName)
                .eq(AccountDat::getComId, comId).eq(AccountDat::getDeleteFlg, false));
        return R.ok(list);
    }

    @Override
    public Boolean isLecturer(Long accountId, Long comId) {
        int count = this.baseMapper.isLecturer(accountId, comId);
        return count > 0;
    }

    @Override
    public R banIp(Long comId, Long accountId, BanIpDto dto) {

        Long liveId = dto.getLiveId();
        String ip = dto.getIp();
        Integer type = dto.getType();
        String key = RedisConstants.BACK_IP_LIST_USER_LIVE + liveId;
        if (type == 1) {
            AccountDat accountDat = this.baseMapper.selectById(accountId);
            String result = redisDubboService.hget(2, key, ip);
            if (StringUtils.isBlank(result)) {
                String today = DateUtil.now();
                String name = accountDat.getName();
                log.info("助理:{},封禁ip:{}，时间:{}", name, ip, today);
                JSONObject json = new JSONObject();
                json.set("学员昵称", dto.getUserName());
                Long channel = dto.getChannel();
                if (ObjectUtils.isNotEmpty(channel)) {
                    json.set("渠道", channel);
                }
                json.set("时间", today);
                json.set("ip", ip);
                json.set("城市", dto.getCity());
                json.set("操作人", name);
                redisDubboService.hset(2, key, ip, JSON.toJSONString(json));
            }
        } else {
            redisDubboService.hDel(2, key, ip);
        }
        return R.ok();
    }

    @Override
    public R banIpList(Long comId, Long accountId, Long liveId) {
        Map<String, String> stringStringMap = redisDubboService.hgetAll(2, RedisConstants.BACK_IP_LIST_USER_LIVE + liveId);
        List<BanIpDto> ipList = new ArrayList<>();
        for (String key : stringStringMap.keySet()) {
//            com.alibaba.fastjson.JSONObject json = JSON.parseObject(stringStringMap.get(key));
            BanIpDto vo = new BanIpDto();
            vo.setIp(key);
//            vo.set("desc",json);
            ipList.add(vo);
        }
        return R.ok(ipList);
    }

    @Override
    public String checkVersion() {
        return "_s1.11";
    }

    @Override
    public R getAccountInfo(Long comId, Long id) {
        AccountDat accountDat =
                this.baseMapper.selectOne(new LambdaQueryWrapper<AccountDat>()
                        .eq(AccountDat::getComId, comId).eq(AccountDat::getId, id).eq(AccountDat::getDeleteFlg, false));

        if (ObjectUtil.isEmpty(accountDat)) {
            return R.failed("账户不存在");
        }
        GetAccountInfoDto getAccountInfoDto = new GetAccountInfoDto();
        BeanUtils.copyProperties(accountDat, getAccountInfoDto);
        // 解析密码
        String password = accountDat.getPassword();
        try {
            password = AESUtil.Decrypt(password, Constants.PASSWORD_KEY);
            getAccountInfoDto.setPassword(password);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Integer dataPermissions = accountDat.getDataPermissions();
        if (DataPermissionsEnun.CUSTOM.getValue() == dataPermissions) {
            List<Long> accountProject = accountProjectDatMapper.getAccountProject(comId, id);
            getAccountInfoDto.setIds(accountProject);
        }
        return R.ok(getAccountInfoDto);
    }

    @Override
    public R delAccount(Long id, Long accountId, Long comId) {
        AccountDat accountDat =
                this.baseMapper.selectOne(new LambdaQueryWrapper<AccountDat>()
                        .eq(AccountDat::getComId, comId).eq(AccountDat::getId, id).eq(AccountDat::getDeleteFlg, false));

        if (ObjectUtil.isEmpty(accountDat)) {
            return R.failed("账户不存在");
        }
        // 判断是否有绑定直播间
        List<LiveDat> liveDats = liveDatMapper.selectList(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getComId, comId).eq(LiveDat::getDeleteFlg, false)
                .eq(LiveDat::getLecturerId, id));
        if (ObjectUtil.isNotEmpty(liveDats)) {
            return R.failed("该讲师已绑定直播间，不可删除");
        }
//        List<LiveDat> liveDats = liveDatMapper.selectList(new LambdaQueryWrapper<LiveDat>().eq(LiveDat::getComId, comId).eq(LiveDat::getDeleteFlg, false)
//                .eq(LiveDat::getLecturerId, id));
        List<ComChannelDat> comChannelDats = comChannelDatMapper.selectList(new LambdaQueryWrapper<ComChannelDat>()
                .eq(ComChannelDat::getComId, comId).eq(ComChannelDat::getAccountId, id).eq(ComChannelDat::getDeleteFlag, false));
        if (ObjectUtil.isNotEmpty(comChannelDats)) {
            return R.failed("该员工存在渠道绑定，不可删除");
        }
        accountDat.setDeleteFlg(1);
        accountDat.setUpdateBy(accountId);
        this.baseMapper.updateById(accountDat);
        return R.ok();
    }


    /**
     * 获取公司项目信息
     *
     * @param comId
     * @param projectId
     * @return
     */
    private ComAccountProject getComAccountProject(Long comId, Long projectId) {
        ComAccountProject comAccountProject = comAccountProjectMapper.selectOne(new LambdaQueryWrapper<ComAccountProject>()
                .eq(ComAccountProject::getComId, comId).eq(ComAccountProject::getId, projectId)
                .eq(ComAccountProject::getDeleteFlg, false));
        return comAccountProject;
    }


}

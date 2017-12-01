package cn.godsdo.controller.login;

import cloud.tianai.captcha.common.response.ApiResponse;
import cn.godsdo.config.jwt.JWTUtil;
import cn.godsdo.dto.SliderVerifyDto;
import cn.godsdo.dto.com.ComRegisterDto;
import cn.godsdo.dubbo.RolePermissionService;
import cn.godsdo.dubbo.RoleService;
import cn.godsdo.dubbo.SmsSendUtil;
import cn.godsdo.dubbo.com.AccountDatService;
import cn.godsdo.dubbo.com.ComAccountProjectService;
import cn.godsdo.dubbo.com.ComDefultSettingService;
import cn.godsdo.dubbo.com.CompanyDatService;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.entity.com.ComAccountProject;
import cn.godsdo.entity.com.ComDefultSetting;
import cn.godsdo.entity.com.CompanyDat;
import cn.godsdo.enums.SliderVerifyType;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.validator.ValidatorUtils;
import cn.godsdo.vo.GetMenuListVo;
import cn.godsdo.vo.LoginVo;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.y20y.constant.Constants;
import com.y20y.utils.AESUtil;
import com.y20y.utils.ToBase62;
import jakarta.servlet.http.HttpServletRequest;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.shiro.SecurityUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author W~Y~H
 * @Date : 2018/11/18
 */
@Slf4j
@RestController
@RequestMapping("/init")
@CrossOrigin
public class LoginController {
    @DubboReference(check = false)
    private AccountDatService accountDatService;
    @DubboReference(check = false)
    private CompanyDatService companyDatService;
    @DubboReference
    private ComAccountProjectService comAccountProjectService;
    @DubboReference(check = false)
    private SmsSendUtil smsSendUtil;
    @DubboReference(check = false)
    private RolePermissionService  rolePermissionService;
    @DubboReference(check = false)
    private RoleService roleService;
    @DubboReference(check = false)
    private ComDefultSettingService comDefultSettingService;
    /**
     * 发布版本校验
     * @return 版本号
     */
    @GetMapping("/checkVersion")
    public R checkVersion() {
        return R.ok("V7.22.1_" + accountDatService.checkVersion());
    }

    /**
     * 账号密码登录
     *
     * @param login
     * @param password
     * @return
     */
    @GetMapping("/login")
    public R login(@RequestParam("login") String login, @RequestParam("password") String password) {
        // 非空判断
        if (StringUtil.isBlank(login) || StringUtil.isBlank(password)) {
            return R.failed("账号密码不能为空");
        }
        // 数据获取
        AccountDat accountDatByLogin = accountDatService.getAccountDatByLogin(login);
        if (ObjectUtil.isEmpty(accountDatByLogin)) {
            return R.failed("用户名错误！");
        }
        if (accountDatByLogin.getStatus().equals("INVALID")) {
            log.info("账号已被禁用，login={}", login);
            return R.failed("账号已被禁用！");
        }

        String pwd = null;
        try {
            // 解析用户密码
            pwd = AESUtil.Decrypt(accountDatByLogin.getPassword(), Constants.PASSWORD_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 密码比对
        if (!password.equals(pwd)) {
            log.info("密码错误，login={}", login);
            return R.failed("密码错误！");
        }
        Long project = accountDatByLogin.getProject();
        if (project != 0) {
            ComAccountProject comProjectById = comAccountProjectService.getComProjectById(accountDatByLogin.getComId(), project);
            if (ObjectUtil.isEmpty(comProjectById)) {
                return R.failed("登录失败，项目不存在！");
            }

            if (comProjectById.getType()) {
                return R.failed("登录失败！");
            }
        }

        // 登录完成，token获取
        LoginVo loginVo = getLoginVo(login, accountDatByLogin);
        // 登陆成功后添加客户登陆记录
        // TODO
//        comLoginDat comLoginDat = new comLoginDat();
//        comLoginDat.setId(idWorkService.nextId());
//        comLoginDat.setcomId(comId);
//        comLoginDat.setAccountId(accountDat.getId());
//        comLoginDat.setCreateAt(new Date());
//        comLoginDat.setDeleteFlg(false);
//        Integer in = comLoginDatService.addcomLongDat(comLoginDat);
        return R.ok(loginVo);
    }

    /**
     * 手机验证码登录
     *
     * @param login
     * @param verificationCode
     * @return
     */
    @GetMapping("/loginByPhone")
    public R loginByPhone(@RequestParam("login") String login, @RequestParam("verificationCode") String verificationCode) {
        // 非空判断
        if (StringUtil.isBlank(login) || StringUtil.isBlank(verificationCode)) {
            return R.failed("账号验证码不能为空");
        }
        // 非空判断
        if (StringUtil.isBlank(verificationCode)) {
            return R.failed("账号验证码不能为空");
        }
        // 数据获取
        AccountDat accountDatByLogin = accountDatService.getAccountDatByLogin(login);
        if (ObjectUtil.isEmpty(accountDatByLogin)) {
            return R.failed("手机号不存在！");
        }
        if (accountDatByLogin.getStatus().equals("INVALID")) {
            log.info("账号已被禁用，login={}", login);
            return R.failed("账号已被禁用！");
        }
        // 获取短信验证码
        String key = "login" + StringPool.DASH + login;
        Object codeObj = smsSendUtil.getRedisValueByPhone(key);
        if (codeObj == null) {
            return R.failed("验证码已失效");
        }
        String saveCode = codeObj.toString();
        if (StringUtil.isBlank(saveCode)) {
            smsSendUtil.delRedisValueByPhone(key);
            return R.failed("验证码已失效");
        }
        // 验证码校验
        if (!saveCode.equals(verificationCode)) {
            return R.failed("验证码不正确");
        }
        Long project = accountDatByLogin.getProject();
        if (project != 0) {
            ComAccountProject comProjectById = comAccountProjectService.getComProjectById(accountDatByLogin.getComId(), project);
            if (ObjectUtil.isEmpty(comProjectById)) {
                return R.failed("登录失败，项目不存在！");
            }

            if (comProjectById.getType()) {
                return R.failed("登录失败！");
            }
        }

        // 登录完成，token获取
        LoginVo loginVo = getLoginVo(login, accountDatByLogin);
        // 完成登录操作，删除验证码
        smsSendUtil.delRedisValueByPhone(key);
        return R.ok(loginVo);
    }

    /**
     * 登录完成，token获取
     *
     * @param login
     * @param accountDatByLogin
     * @return
     */
    @NotNull
    private LoginVo getLoginVo(String login, AccountDat accountDatByLogin) {
        String name = accountDatByLogin.getName();
        // 用户ID
        Long id = accountDatByLogin.getId();
        Long comId = accountDatByLogin.getComId();
        String s = ToBase62.encodeToBase62(comId);
        // 获取大账号信息
        CompanyDat byId = companyDatService.getById(comId);
        // 生成token
        String token = JWTUtil.createToken(login, comId, id);
        LoginVo loginVo = new LoginVo(byId.getCompany(), name, id, token,"");
        // 登陆成功后添加客户登陆记录
        // TODO
//        comLoginDat comLoginDat = new comLoginDat();
//        comLoginDat.setId(idWorkService.nextId());
//        comLoginDat.setcomId(comId);
//        comLoginDat.setAccountId(accountDat.getId());
//        comLoginDat.setCreateAt(new Date());
//        comLoginDat.setDeleteFlg(false);
//        Integer in = comLoginDatService.addcomLongDat(comLoginDat);
        Boolean isAdmin = roleService.checkAdmin(accountDatByLogin.getRoleId(), comId);
        loginVo.setIsAdmin(isAdmin);
        loginVo.setUniqueIdentifier(s);
        loginVo.setProfile(accountDatByLogin.getProfile());
        return loginVo;
    }

    /**
     * 退出登陆
     */
    @GetMapping("/logout")
    public R logout() {
        log.info("init...logout...Start");

        try {
            // 清除登陆状态的session
            SecurityUtils.getSubject().logout();
        } catch (Exception e) {
            log.error("退出登录失败！");
            return R.ok("退出成功！");
        }

        log.info("init...logout...End.");
        return R.ok("退出成功！");
    }

    /**
     * 获取用户可展示菜单列表
     *
     * @return
     */
    @GetMapping("/authorityMenuList")
    @ResponseBody
    public R menuList() {
         Boolean isAdmin = ShiroUtil.getIsAdmin();
        Long accountId = ShiroUtil.getAccountId();
        Long comId = ShiroUtil.getComId();

        AccountDat accountDat = accountDatService.getById(accountId);
        CompanyDat companyDat = companyDatService.getById(accountDat.getComId());
        List<String> menus = new ArrayList<>();
        // 获取权限列表
        if(isAdmin){
            ComDefultSetting info = comDefultSettingService.getInfo(comId);
            // 智能直播
//            20,21,22,23
            // 场控 16 助理端 92、93、94
            ArrayList<Long> ids = new ArrayList<>();
            if(info.getIntelligentStatus()==0){
                ids.add(20L);
                ids.add(21L);
                ids.add(22L);
                ids.add(23L);
            }
            if(info.getBotStatus()==0){
                ids.add(16L);
                ids.add(92L);
                ids.add(93L);
                ids.add(94L);
            }
            menus = rolePermissionService.getAllPermissions(ids);
        }else{
            menus = rolePermissionService.getPermissionsByRoleId(accountDat.getRoleId(),accountDat.getComId());
        }

        GetMenuListVo getMenuListVo = new GetMenuListVo(menus, companyDat.getCompany(), accountDat.getLogin(), accountDat.getName(), accountDat.getId());
        log.info("init...menuList...End.");
        return R.ok(getMenuListVo);
    }


    /**
     * 滑块后发送短信
     *
     * @param
     * @return
     */
    @PostMapping("/sendVerify")
    public ApiResponse<?>  sendVerifyByRegister(@RequestBody SliderVerifyDto x ,HttpServletRequest request) {
        String id = x.getId();
        Object redisValueByPhone = smsSendUtil.getRedisValueByPhone(id);
        if (redisValueByPhone == null) {
            return ApiResponse.ofError("请重新开始滑块验证");
        }
        //发短信
        send(x);
        return ApiResponse.ofSuccess();
    }

    /**
     * 滑块后发送短信（手机验证码登录）
     *
     * @param
     * @return
     */
    //@PostMapping("/sendVerifyByPhoneLogin")
    //public R sendVerifyByPhoneLogin(@RequestBody SliderVerifyDto dto) {
    //    CaptchaResponse<ImageCaptchaVO> x = dto.getX();
    //    String phone = dto.getPhone();
    //    if (ObjectUtil.isEmpty(x)) {
    //        return R.failed("滑块验证失败");
    //    }
    //    if (StringUtil.isEmpty(phone) || !PHONE_NUMBER_PATTERN.matcher(phone).matches()) {
    //        return R.failed("请确认手机号是否正确！");
    //    }
    //    // 判断手机号是否存在
    //    AccountDat accountDatByLogin = accountDatService.getAccountDatByLogin(phone);
    //    if (ObjectUtil.isEmpty(accountDatByLogin)) {
    //        return R.failed("手机号不存在！");
    //    }
    //    dto.setType(SliderVerifyType.LOGIN.getDescription());
    //    return send(dto, x, phone);
    //}

    @NotNull
    private R send(SliderVerifyDto x) {
        String phone = x.getPhone();
        String id = x.getId();
        String code = RandomUtil.randomNumbers(6);
        log.info("发送短信验证码给手机号：{}", phone);
        log.info("验证码==="+code);
        boolean is = smsSendUtil.smsSend(phone, code);
        if (is) {
            String type = x.getType();
            String redisKey = "register" + StringPool.DASH + phone;
            // 登录
            if (SliderVerifyType.LOGIN.getDescription().equals(type)) {
                redisKey = "login" + StringPool.DASH + phone;
            }
            // 短信发送成功之后清空redis
            smsSendUtil.delRedisValueByPhone(id);
            smsSendUtil.setRedisValueByPhone(redisKey, code);
            return R.ok("发送成功！");
        } else {
            return R.ok("发送失败，请重试！");
        }
    }

    @PostMapping("/testsms")
    public R testsms(String phone,String code){
        boolean b = smsSendUtil.smsSend(phone, code);
        return R.ok(b);
    }

    /**
     * 客户注册
     *
     * @param dto
     * @return
     */
    @PostMapping("/comRegister")
    public R comRegister(@RequestBody ComRegisterDto dto) {
        //非空校验
        String message = ValidatorUtils.validateEntity(dto);
        if (StringUtils.isNotBlank(message)) {
            return R.failed(message);
        }
        return companyDatService.comRegister(dto);
    }


}

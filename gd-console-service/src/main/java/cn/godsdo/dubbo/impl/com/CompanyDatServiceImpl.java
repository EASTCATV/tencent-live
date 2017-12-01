package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dto.com.ComRegisterDto;
import cn.godsdo.dubbo.RoleService;
import cn.godsdo.dubbo.com.ComBotService;
import cn.godsdo.dubbo.com.CompanyDatService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.entity.AccountDat;
import cn.godsdo.entity.camp.TrainingCom;
import cn.godsdo.entity.com.ComDefultSetting;
import cn.godsdo.entity.com.ComMarquee;
import cn.godsdo.entity.com.ComWatchChat;
import cn.godsdo.entity.com.CompanyDat;
import cn.godsdo.mapper.AccountDatMapper;
import cn.godsdo.mapper.camp.TrainingComMapper;
import cn.godsdo.mapper.com.ComDefultSettingMapper;
import cn.godsdo.mapper.com.ComWatchChatMapper;
import cn.godsdo.mapper.com.CompanyDatMapper;
import cn.godsdo.mapper.live.ComMarqueeMapper;
import cn.godsdo.util.R;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import com.y20y.utils.AESUtil;
import com.y20y.utils.ToBase62;
import jakarta.annotation.Resource;
import jodd.util.StringUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.Random;

/**
 * <p>
 * 《客户信息主表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/11/18
 */
@RefreshScope
@DubboService
public class CompanyDatServiceImpl extends ServiceImpl<CompanyDatMapper, CompanyDat> implements CompanyDatService {

    @Resource
    private RedisDubboService redisService;

    @DubboReference(check = false)
    private IdService idService;
    @DubboReference(check = false)
    private ComBotService comBotService;

    @Resource
    private AccountDatMapper accountDatMapper;
    @Resource
    private ComDefultSettingMapper comDefultSettingMapper;
    @Resource
    private ComMarqueeMapper comMarqueeMapper;
    @Resource
    private ComWatchChatMapper comWatchChatMapper;
    @Resource
    private TrainingComMapper trainingComMapper;

    @DubboReference(check = false)
    private RoleService roleService;
    @Value("${Tencent.wxAuthUrl}")
    private String wxAuthUrl;


    @Override
    public R comRegister(ComRegisterDto dto) {
        String phone = dto.getPhone();
//        // 验证是否已经注册
        CompanyDat dat = this.baseMapper.selectOne(new LambdaQueryWrapper<CompanyDat>().eq(CompanyDat::getContact, phone).eq(CompanyDat::getDeleteFlg, false));
        if (ObjectUtil.isNotEmpty(dat)) {
            return R.failed("该手机号已注册");
        }
        String key = "register" + StringPool.DASH + phone;
        Object codeObj = redisService.get(key);
        if (codeObj == null) {
            return R.failed("验证码已失效");
        }
        String saveCode = codeObj.toString();
        if (StringUtil.isBlank(saveCode)) {
            redisService.del(key);
            return R.failed("验证码已失效");
        }
        // 验证码
        String verificationCode = dto.getVerificationCode();
        if (!saveCode.equals(verificationCode)) {
            return R.failed("验证码不正确");
        }
        Long accountId = idService.nextId();
//       验证成功开始插入
        CompanyDat companyDat = new CompanyDat();
        Long comId = idService.nextId();
        companyDat.setId(comId);
        companyDat.setName(dto.getName());
        companyDat.setCompany(dto.getCompany());
        companyDat.setContact(phone);
        companyDat.setAccountId(accountId);
        this.baseMapper.insert(companyDat);
//        生成角色信息
        Long roleId = idService.nextId();
        roleService.initRoleByRegister(comId, roleId);
        //生成角色权限信息
        roleService.initRolePermissionByRegister(comId, roleId);

//         子账号插入
        AccountDat accountDat = new AccountDat();
        accountDat.setId(accountId);
        accountDat.setComId(comId);
        accountDat.setName(dto.getName());
        // 生产随机密码并加密
        try {
            String password = ToBase62.encodeToBase62(idService.nextId());
            accountDat.setPassword(AESUtil.Encrypt(password, Constants.PASSWORD_KEY));
        } catch (Exception e) {
            e.printStackTrace();
        }
        accountDat.setContact(phone);
        accountDat.setLogin(phone);
        accountDat.setRoleId(roleId);
        accountDatMapper.insert(accountDat);
//         完成登录操作，删除验证码
        redisService.del(key);

        ComDefultSetting comDefultSetting = new ComDefultSetting();
        comDefultSetting.setId(idService.nextId());
        comDefultSetting.setComId(comId);
        comDefultSettingMapper.insert(comDefultSetting);
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                 insertSetting(comId, accountId);
                comBotService.addComBotByRegister(comId, accountId);

            }
        });
        return R.ok("注册成功");
    }

    /**
     * 导入基本配置
     *
     * @param comId
     * @param accountId
     */
    private void insertSetting(Long comId, Long accountId) {
        ComMarquee comMarquee = new ComMarquee();
        // 设置新对象的id属性
        comMarquee.setId(idService.nextId());
        // 设置新对象的comId属性
        comMarquee.setComId(comId);
        comMarquee.setMode(1);
        comMarquee.setStatement("欢迎来到直播间！直播内容版权归提供者所有，仅限个人学习，严禁任何形式的录制，传播和账号分享。一经发现将依法保留追究权，情节严重者将承担法律责任。请未成年人在监护人陪同下观看。直播间内严禁出现违法违规、低俗色情、吸烟酗酒、人身伤害等内容。");
        comMarquee.setContentType(1);
        // 设置新对象的createBy属性
        comMarquee.setCreateBy(accountId);
        // 将新对象插入数据库
        comMarqueeMapper.insert(comMarquee);
        // 房间聊天
        ComWatchChat comWatchChat = new ComWatchChat();
        comWatchChat.setComId(comId);
        comWatchChat.setCreateBy(accountId);
        comWatchChat.setId(idService.nextId());
        comWatchChatMapper.insert(comWatchChat);
        TrainingCom trainingCom = new TrainingCom();
        trainingCom.setComId(comId);
        trainingCom.setComKey(generateRandomString(36));
        trainingComMapper.insert(trainingCom);
    }

    @Override
    public Integer getAuthorityStatus(Long comId) {
        CompanyDat dat = this.baseMapper.selectOne(new LambdaQueryWrapper<CompanyDat>().eq(CompanyDat::getId, comId).eq(CompanyDat::getDeleteFlg, false));

        return dat != null ? dat.getAuthorityStatus() : null;
    }

    @Override
    public R getCompanyInfo(Long comId) {
        CompanyDat companyDat = this.baseMapper.selectOne(new LambdaQueryWrapper<CompanyDat>()
                .select(CompanyDat::getCompany, CompanyDat::getId)
                .eq(CompanyDat::getId, comId));
        return R.ok(companyDat);
    }

    @Override
    public R getWxAuthUrl(Long comId) {
        String code = RandomStringUtils.randomAlphanumeric(16);
        String url = wxAuthUrl + code;
        redisService.setex(Constants.WX_AUTH + code, comId.toString(), 3600);
        JSONObject json = new JSONObject();
        json.put("wxAuthUrl", url);
        return R.ok(json);
    }

    public static void main(String[] args) {
        String s = RandomStringUtils.randomAlphanumeric(16);
        String code = s;
        System.out.println(code);
    }
    public static String generateRandomString(int length) {
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            char randomChar = characters.charAt(index);
            sb.append(randomChar);
        }

        return sb.toString();
    }

}

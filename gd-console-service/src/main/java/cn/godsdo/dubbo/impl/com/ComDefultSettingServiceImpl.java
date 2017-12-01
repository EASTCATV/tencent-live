package cn.godsdo.dubbo.impl.com;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dubbo.com.ComDefultSettingService;
import cn.godsdo.dubbo.wx.WxOpenService;
import cn.godsdo.entity.com.ComDefultSetting;
import cn.godsdo.mapper.com.ComDefultSettingMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.user.GetAuthorizerInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import me.chanjar.weixin.open.bean.result.WxOpenAuthorizerInfoResult;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * <p>
 * 账户默认配置表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-05-01
 */
@DubboService
public class ComDefultSettingServiceImpl extends ServiceImpl<ComDefultSettingMapper, ComDefultSetting> implements ComDefultSettingService {
    @DubboReference
    WxOpenService wxOpenService;


    @Override
    public ComDefultSetting getCdnInfoByComId(Long comId) {
        ComDefultSetting comDefultSetting = this.baseMapper.selectOne(new LambdaQueryWrapper<ComDefultSetting>()
                .select(ComDefultSetting::getComId, ComDefultSetting::getPushCdnId, ComDefultSetting::getRtmpPullDomain,
                        ComDefultSetting::getFlvPullDomain, ComDefultSetting::getM3u8PullDomain, ComDefultSetting::getBindId, ComDefultSetting::getTemplateId)
                .eq(ComDefultSetting::getComId, comId));
        return comDefultSetting;
    }

    @Override
    public List<ComDefultSetting> getSettingByJsAppId(String authorizerAppid) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<ComDefultSetting>().eq(ComDefultSetting::getWxJsapiAppid, authorizerAppid));
    }

    @Override
//    @Cacheable(key = "#comId", value = CacheConstants.GET_COM_DEFULT, unless = "#result==null")
    public ComDefultSetting getInfo(Long comId) {
        return this.baseMapper.selectOne(new LambdaQueryWrapper<ComDefultSetting>().eq(ComDefultSetting::getComId, comId));
    }

    @Override
    public String getDefaultMpWxappId() {

        return null;
    }

    @Override
    public R getWxInfo(Long comId) {
        // 从数据库中获取默认设置信息
        ComDefultSetting comDefultSetting = this.baseMapper.selectOne(new LambdaQueryWrapper<ComDefultSetting>()
                .eq(ComDefultSetting::getComId, comId));
        // 如果获取的默认设置信息为空，则返回错误响应
        if (comDefultSetting == null) {
            return R.failed("用户信息错误");
        }
        // 获取微信公众号的Appid
        String wxJsapiAppid = comDefultSetting.getWxJsapiAppid();
        // 通过微信开放平台服务获取授权方的信息
        WxOpenAuthorizerInfoResult wxOpenAuthorizerInfoResult = wxOpenService.getAuthorizerInfo(wxJsapiAppid);
        // 创建获取授权方信息的VO对象
        GetAuthorizerInfoVo vo = new GetAuthorizerInfoVo();
        vo.setWxMchKey(comDefultSetting.getWxMchKey());
        vo.setWxMchId(comDefultSetting.getWxMchId());
        // 如果微信公众号授权状态为1
        if (1 == comDefultSetting.getMpAuthStatus()) {
            // 如果授权方信息不为空，则设置昵称
            if (ObjectUtils.isNotEmpty(wxOpenAuthorizerInfoResult)) {
                vo.setNickName(wxOpenAuthorizerInfoResult.getAuthorizerInfo().getNickName());
            }
            // 设置二维码URL
            vo.setQrcodeUrl(comDefultSetting.getMpQrcode());
            vo.setDisabled(false);
        } else {
            // 如果授权状态不为1，则设置为不可用
            vo.setDisabled(true);
        }

        return R.ok(vo);
    }

    @Override
    public R updateWxpayInfo(String wxMchId, String wxMchKey, Long accountId, Long comId) {
        ComDefultSetting comDefultSetting = this.baseMapper.selectOne(new LambdaQueryWrapper<ComDefultSetting>()
                .eq(ComDefultSetting::getComId, comId));
        if (comDefultSetting == null) {
            return R.failed("用户信息错误");
        }
        comDefultSetting.setWxMchKey(wxMchKey);
        comDefultSetting.setWxMchId(wxMchId);
        comDefultSetting.setUpdateBy(accountId);
        this.baseMapper.updateById(comDefultSetting);
        return R.ok();
    }

    @Override
    public String getDefaultMpAppid(Long liveId) {
        return this.baseMapper.getDefaultMpAppid(liveId);
    }
}

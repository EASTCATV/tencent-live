package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dubbo.com.ComDefultSettingService;
import cn.godsdo.dubbo.com.ComUrlConfigService;
import cn.godsdo.entity.com.ComUrlConfig;
import cn.godsdo.mapper.com.ComUrlConfigMapper;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Value;

/**
 * <p>
 * 账号路径配置表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-05-08
 */
@DubboService
public class ComUrlConfigServiceImpl extends ServiceImpl<ComUrlConfigMapper, ComUrlConfig> implements ComUrlConfigService {
    @DubboReference
    IdService idService;
    @DubboReference
    ComDefultSettingService comDefultSettingService;
    @Value(value = "${defaultRequestAppId}")
    public String defaultRequestAppId;
    @Value(value = "${defaultRequestHeader}")
    public String defaultRequestHeader;
    @Value(value = "${requestAuthUrl}")
    public String requestAuthUrl;

    @Override
    public ComUrlConfig getInfo(Long comId) {
        return this.baseMapper.selectOne(Wrappers.<ComUrlConfig>lambdaQuery().eq(ComUrlConfig::getComId,comId));
    }

    @Override
    public R updateShareUrl(Long comId, String newWxJsapiAppid) {
        ComUrlConfig info = this.getInfo(comId);
        if(ObjectUtils.isEmpty(info)){
            String appId = defaultRequestAppId;
            if (org.apache.commons.lang3.ObjectUtils.isNotEmpty(newWxJsapiAppid)) {
                    appId = newWxJsapiAppid;
            }
            // 如果公司URL配置信息为空，则生成默认的路径
            String shareClientDomain = defaultRequestHeader + appId + Constants.dot + requestAuthUrl;
            // 创建默认的公司URL配置信息
            info = new ComUrlConfig();
            info.setId(idService.nextId());
            info.setComId(comId);
            info.setShareClientDomain(shareClientDomain);
            info.setCreateBy(comId);
            this.baseMapper.insert(info);
        }else{
            String shareClientDomain = defaultRequestHeader + newWxJsapiAppid + Constants.dot + requestAuthUrl;
            info.setShareClientDomain(shareClientDomain);
            this.baseMapper.updateById(info);
        }
        return R.ok();
    }
}

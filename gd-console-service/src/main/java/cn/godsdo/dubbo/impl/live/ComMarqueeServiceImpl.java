package cn.godsdo.dubbo.impl.live;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dubbo.live.ComMarqueeService;
import cn.godsdo.entity.com.ComMarquee;
import cn.godsdo.mapper.live.ComMarqueeMapper;
import cn.godsdo.util.R;
import cn.godsdo.util.tencent.CosHelperUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.util.ObjectUtils;

/**
 * <p>
 * 全局跑马灯表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-10
 */
@DubboService
public class ComMarqueeServiceImpl extends ServiceImpl<ComMarqueeMapper, ComMarquee> implements ComMarqueeService {


    @DubboReference
    private IdService idService;
    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;



//todo 删除系统级别缓存
    @Override
    //@CacheEvict(key = "#comId", value = CacheConstants.COM_MARQUEE_VALUE)
    public R getComMarquee(Long comId, Long accountId) {
        // 从数据库中查询ComMarquee对象
        ComMarquee comMarquee = this.baseMapper.selectOne(new LambdaQueryWrapper<ComMarquee>().eq(ComMarquee::getComId, comId));
        // 如果查询结果不为空
        if (ObjectUtils.isEmpty(comMarquee)) {
            // 创建一个新的ComMarquee对象
            comMarquee = new ComMarquee();
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
            this.baseMapper.insert(comMarquee);
        }
        return R.ok(comMarquee);

    }

    @Override
    //todo 删除系统级别缓存
    @CacheEvict(key = "#comId", value = CacheConstants.COM_LOGIN_IMAGE)
    public R saveLoginImage(Long comId, Long accountId, ComMarquee comMarquee) {
        String loginImage = comMarquee.getLoginImage();

        // 从数据库中查询ComMarquee对象
        ComMarquee info = this.baseMapper.selectOne(new LambdaQueryWrapper<ComMarquee>().eq(ComMarquee::getComId, comId));
        if (org.apache.commons.lang3.ObjectUtils.isNotEmpty(loginImage) && !loginImage.startsWith("http")) {
            loginImage = cosHelperUtil.uploadCover(loginImage, comId);
        }
        // 如果查询结果不为空
        if (ObjectUtils.isEmpty(info)) {
            // 创建一个新的ComMarquee对象
            info = new ComMarquee();
            // 设置新对象的id属性
            info.setId(idService.nextId());
            // 设置新对象的comId属性
            info.setComId(comId);
            info.setLoginImage(loginImage);
            info.setMode(1);
            info.setContentType(1);
            // 设置新对象的createBy属性
            info.setCreateBy(accountId);
            // 将新对象插入数据库
            this.baseMapper.insert(info);
        }else{
            info.setLoginImage(loginImage);
            info.setUpdateBy(accountId);
            this.baseMapper.updateById(info);
        }
        return R.ok();
    }

    @Override
    public R updateComMarquee(Long comId, Long accountId, ComMarquee dto) {
        // 查询相应的商品跑马灯信息
        ComMarquee comMarquee = this.baseMapper.selectOne(new LambdaQueryWrapper<ComMarquee>().eq(ComMarquee::getComId, comId));
        if (ObjectUtils.isEmpty(comMarquee)) {
            // 如果商品跑马灯信息不存在，则新增一条新的商品跑马灯信息
            comMarquee = new ComMarquee();
            BeanUtils.copyProperties(dto, comMarquee);
            comMarquee.setId(idService.nextId());
            comMarquee.setComId(comId);
            comMarquee.setCreateBy(accountId);
            this.baseMapper.insert(comMarquee);
        } else {
            // 如果商品跑马灯信息存在，则更新相应的信息
            this.update(new LambdaUpdateWrapper<ComMarquee>()
                    .set(ComMarquee::getMarqueeEnable, dto.getMarqueeEnable())
                    .set(ComMarquee::getContentType, dto.getContentType())
                    .set(ComMarquee::getCustomContent, dto.getCustomContent())
                    .set(ComMarquee::getMode, dto.getMode())
                    .set(ComMarquee::getFontSize, dto.getFontSize())
                    .set(ComMarquee::getFontColor, dto.getFontColor())
                    .set(ComMarquee::getRollSpeed, dto.getRollSpeed())
                    .set(ComMarquee::getUpdateBy, accountId)
                    .eq(ComMarquee::getComId, comId));
        }
        return R.ok();
    }

    @Override
    @CacheEvict(key = "#comId", value = CacheConstants.COM_STATEMENT)
    public R updateComStatement(Long comId, Long accountId, ComMarquee dto) {
        ComMarquee comMarquee = this.baseMapper.selectOne(new LambdaQueryWrapper<ComMarquee>().eq(ComMarquee::getComId, comId));
        if (ObjectUtils.isEmpty(comMarquee)) {
            // 如果商品跑马灯信息不存在，则新增一条新的商品跑马灯信息
            comMarquee = new ComMarquee();
            comMarquee.setId(idService.nextId());
            // 设置新对象的comId属性
            comMarquee.setComId(comId);
            comMarquee.setMode(1);
            comMarquee.setContentType(1);
            comMarquee.setStatement(dto.getStatement());
            comMarquee.setCreateBy(accountId);
            this.baseMapper.insert(comMarquee);
        } else {
            // 如果商品跑马灯信息存在，则更新相应的信息
            this.update(new LambdaUpdateWrapper<ComMarquee>()
                    .set(ComMarquee::getStatement, dto.getStatement())
                    .set(ComMarquee::getUpdateBy, accountId)
                    .eq(ComMarquee::getComId, comId));
        }
        return R.ok();
    }

}

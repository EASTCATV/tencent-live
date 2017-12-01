package cn.godsdo.dubbo.impl.live;

import cn.godsdo.base.BasePage;
import cn.godsdo.base.RedisLock;
import cn.godsdo.base.RedisLockIndex;
import cn.godsdo.dto.com.ComGiftConfigDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.live.ComGiftConfigService;
import cn.godsdo.entity.com.ComGiftConfig;
import cn.godsdo.mapper.live.ComGiftConfigMapper;
import cn.godsdo.service.cos.CosService;
import cn.godsdo.util.tencent.CosHelperUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import cn.godsdo.util.R;
import jakarta.annotation.Resource;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 用户礼物配置表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/13
 */
@DubboService
public class ComGiftConfigServiceImpl extends ServiceImpl<ComGiftConfigMapper, ComGiftConfig> implements ComGiftConfigService {
    @DubboReference
    CosService cosService;
    @DubboReference
    IdService idService;
    @DubboReference(check = false)
    ClearCache clearcache;
    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;

    @Override
    public R getGiftList(Long comId, Long accountId, BasePage basePage) {
        Page<ComGiftConfig> page = new Page<>(basePage.getPage(), basePage.getPageSize());
        Page<ComGiftConfig> giftList = this.baseMapper.selectPage(page, new LambdaQueryWrapper<ComGiftConfig>().eq(ComGiftConfig::getComId, comId).eq(ComGiftConfig::getDeleteFlg, false).orderByDesc(ComGiftConfig::getCreateAt));
        List<ComGiftConfig> records = giftList.getRecords();
        Long total = giftList.getTotal();
        if (ObjectUtils.isEmpty(records)) {
            // 没有数据 新建默认礼物配置
            records = addDefaultGifts(comId, accountId);
            total = Long.valueOf(records.size());
        }
        return R.ok(records, total);
    }

    @Override
    public List<ComGiftConfig> addDefaultGifts(Long comId, Long accountId) {
        List<ComGiftConfig> list = this.baseMapper.selectList(Wrappers.<ComGiftConfig>lambdaQuery().eq(ComGiftConfig::getComId, comId));
        // 存在礼物
        if (ObjectUtils.isNotEmpty(list)) {
            return list;
        }
        List<ComGiftConfig> newList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            //将图片转换为base64
            String path = getImgBase("gift/gift_" + i + ".png");
            ComGiftConfig gift = new ComGiftConfig();
            String image = cosHelperUtil.uploadGiftImg(path, comId);
            gift.setId(idService.nextId());
            gift.setComId(comId);
            gift.setGiftPicUrl(image);
            gift.setGiftType(0);
            gift.setCreateBy(accountId);
            gift.setGiftIndex(i - 1);
            switch (i) {
                case 1:
                    gift.setGiftName("小心心");
                    break;
                case 2:
                    gift.setGiftName("棒棒糖");
                    break;
                case 3:
                    gift.setGiftName("比心");
                    break;
                case 4:
                    gift.setGiftName("加一");
                    break;
                case 5:
                    gift.setGiftName("欢呼");
                    break;
                case 6:
                    gift.setGiftName("热气球");
                    break;
                case 7:
                    gift.setGiftName("跑车");
                    break;
                case 8:
                    gift.setGiftName("发财");
                    break;
                case 9:
                    gift.setGiftName("珍珠");
                    break;
                case 10:
                    gift.setGiftName("城堡");
                    break;
                default:
                    break;
            }

            gift.setGiftPrice(BigDecimal.ZERO);
            // 插入
            newList.add(gift);
        }
        this.baseMapper.insertBatch(newList);
        return newList;
    }

    @Override
    @RedisLock(lockIndex = {@RedisLockIndex(index = 0)}, leaseTime = 2, errorInfo = "其他人正在插入")
    public R addGift(Long comId, Long accountId, ComGiftConfigDto dto) {
        // 查询当前礼物配置中最大的礼物索引
        QueryWrapper<ComGiftConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("MAX(gift_index) as giftIndex");
        queryWrapper.eq("com_id", comId);
        queryWrapper.eq("delete_flg", false);
        ComGiftConfig cgcMax = this.baseMapper.selectOne(queryWrapper);
        int index = 0;
        // 如果存在最大的礼物索引，则基于最大的礼物索引生成新的索引
        if (cgcMax != null && ObjectUtils.isNotEmpty(cgcMax.getGiftIndex())) {
            index = cgcMax.getGiftIndex() + 1;
        }

        // 如果礼物图片存在且不以"http"开头，则上传至cos
        String image = dto.getGiftPicUrl();
        // 构建新的礼物配置对象并插入数据库
        ComGiftConfig cgc = new ComGiftConfig();
        cgc.setId(idService.nextId());
        cgc.setComId(comId);
        cgc.setGiftName(dto.getGiftName());
        cgc.setGiftIndex(index);
        cgc.setGiftPrice(dto.getGiftPrice());
        cgc.setGiftPicUrl(image);
        cgc.setCreateBy(accountId);
        this.baseMapper.insert(cgc);
        return R.ok();
    }


    @Override
    public R delGift(Long comId, Long accountId, Long id) {
        // 根据 comId 和 id 查询 ComGiftConfig 对象
        ComGiftConfig comGiftConfig = this.baseMapper.selectOne(new LambdaQueryWrapper<ComGiftConfig>().eq(ComGiftConfig::getComId, comId)
                .eq(ComGiftConfig::getId, id).eq(ComGiftConfig::getDeleteFlg, false));
        // 如果 comGiftConfig 为空则返回失败响应
        if (ObjectUtils.isEmpty(comGiftConfig)) {
            return R.failed("礼物不存在或已删除");
        }
        // 将 deleteFlg 标记为 true，并设置更新人为 accountId
        comGiftConfig.setDeleteFlg(1);
        comGiftConfig.setUpdateBy(accountId);
        // 更新数据库中的 comGiftConfig 对象
        this.baseMapper.updateById(comGiftConfig);
        return R.ok();

    }

    @Override
    public R updateGift(Long comId, Long accountId, ComGiftConfigDto dto) {
        // 根据 comId 和 dto.getId() 查询对应的 ComGiftConfig 对象
        ComGiftConfig comGiftConfig = this.baseMapper.selectOne(new LambdaQueryWrapper<ComGiftConfig>()
                .eq(ComGiftConfig::getComId, comId)
                .eq(ComGiftConfig::getId, dto.getId())
                .eq(ComGiftConfig::getDeleteFlg, false));

        // 如果未找到对应的 ComGiftConfig 对象，则返回失败信息
        if (ObjectUtils.isEmpty(comGiftConfig)) {
            return R.failed("礼物不存在或已删除");
        }

        // 更新 ComGiftConfig 对象的礼物名称和价格
        comGiftConfig.setGiftName(dto.getGiftName());
        comGiftConfig.setGiftPrice(dto.getGiftPrice());
        String image = dto.getGiftPicUrl();

        // 更新 ComGiftConfig 对象的礼物图片链接和更新人
        comGiftConfig.setGiftPicUrl(image);
        comGiftConfig.setUpdateBy(accountId);

        // 更新 ComGiftConfig 对象到数据库并返回操作结果
        this.baseMapper.updateById(comGiftConfig);
        return R.ok();

    }

    @Override
    public Long getGiftSum(Long comId) {
        // 查询当前礼物配置数量
        return this.baseMapper.selectCount(new LambdaQueryWrapper<ComGiftConfig>()
                .eq(ComGiftConfig::getComId, comId).eq(ComGiftConfig::getDeleteFlg, false));
    }

    @Override
    public R getGiftListByTemplate(Long comId) {
        List<ComGiftConfig> comGiftConfigs = this.baseMapper.selectList(new LambdaQueryWrapper<ComGiftConfig>()
                .eq(ComGiftConfig::getComId, comId)
                .eq(ComGiftConfig::getDeleteFlg, false));
        return R.ok(comGiftConfigs);
    }


    /**
     * 将图片转换为base64
     *
     * @return
     */
    public String getImgBase(String url) {
        // 将图片文件转化为二进制流
        InputStream in = null;
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        in = contextClassLoader.getResourceAsStream(url);
        // 将图片文件转化为字节数组字符串，并对其进行Base64编码处理
        byte[] data = null;
        // 读取图片字节数组
        try {
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = in.read(buff, 0, 100)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            data = swapStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return "data:image/jpeg;base64," + new String(Base64.encodeBase64(data));
    }
}

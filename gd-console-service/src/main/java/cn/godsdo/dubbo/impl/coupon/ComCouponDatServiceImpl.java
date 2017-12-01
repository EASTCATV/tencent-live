package cn.godsdo.dubbo.impl.coupon;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dto.coupon.AddCouponDatDto;
import cn.godsdo.dto.coupon.GetAllCouponDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.coupon.ComCouponDatService;
import cn.godsdo.entity.coupon.ComCouponCommodityDat;
import cn.godsdo.entity.coupon.ComCouponDat;
import cn.godsdo.entity.coupon.ComCouponLiveDat;
import cn.godsdo.mapper.coupon.ComCouponCommodityDatMapper;
import cn.godsdo.mapper.coupon.ComCouponDatMapper;
import cn.godsdo.mapper.coupon.ComCouponLiveDatMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.coupon.GetAllCouponVo;
import cn.godsdo.vo.coupon.GetComCouponCommodityVo;
import cn.godsdo.vo.coupon.GetCouponInfoVo;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.BeanUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.CacheEvict;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 优惠卷列表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-05
 */
@DubboService
public class ComCouponDatServiceImpl extends ServiceImpl<ComCouponDatMapper, ComCouponDat> implements ComCouponDatService {

    @DubboReference
    IdService idService;

    @Resource
    ComCouponCommodityDatMapper comCouponCommodityDatMapper;
    @Resource
    ComCouponLiveDatMapper comCouponLiveDatMapper;
    @DubboReference
    ClearCache clearCache;

    @Override
    public R addCoupon(Long comId, Long accountId, AddCouponDatDto dto) {
        // 创建优惠券数据对象
        ComCouponDat comCouponDat = new ComCouponDat();
        // 生成优惠券ID
        Long couponId = idService.nextId();
        comCouponDat.setId(couponId);
        comCouponDat.setComId(comId);
        comCouponDat.setName(dto.getName());
        comCouponDat.setCouponType(dto.getCouponType());
        comCouponDat.setCouponValue(dto.getCouponValue());
        comCouponDat.setRelationCommodity(dto.getRelationCommodity());
        comCouponDat.setAllowMultiple(dto.getAllowMultiple());
        comCouponDat.setCreateBy(accountId);
        // 插入优惠券数据
        this.baseMapper.insert(comCouponDat);
        // 获取关联商品ID列表
        List<ComCouponCommodityDat> data = dto.getData();
        if (ObjectUtils.isNotEmpty(data)) {
            List<ComCouponCommodityDat> list = new ArrayList<>();
            for (ComCouponCommodityDat info : data) {
                ComCouponCommodityDat comCouponCommodityDat = new ComCouponCommodityDat();
                comCouponCommodityDat.setId(idService.nextId());
                comCouponCommodityDat.setComId(comId);
                comCouponCommodityDat.setCommodityId(info.getCommodityId());
                comCouponCommodityDat.setCommodityPrice(info.getCommodityPrice());
                comCouponCommodityDat.setDiscountPrice(info.getDiscountPrice());
                comCouponCommodityDat.setCouponId(couponId);
                comCouponCommodityDat.setCreateBy(accountId);
                list.add(comCouponCommodityDat);
            }
            // 批量插入关联商品数据
            comCouponCommodityDatMapper.insertBatch(list);
        }
        return R.ok();
    }

    @Override
    public R getCouponInfo(Long comId, Long id) {
        ComCouponDat comCouponDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComCouponDat>().eq(ComCouponDat::getId, id)
                .eq(ComCouponDat::getComId, comId).eq(ComCouponDat::getDeleteFlag, false));
        if (ObjectUtils.isEmpty(comCouponDat)) {
            return R.failed("优惠券不存在");
        }
        GetCouponInfoVo vo = new GetCouponInfoVo();
        BeanUtil.copyProperties(comCouponDat, vo);
        Integer relationCommodity = comCouponDat.getRelationCommodity();
        // 存在绑定商品
        if (relationCommodity ==1) {
            List<GetComCouponCommodityVo> allBindCommodity = comCouponCommodityDatMapper.getAllBindCommodity(comId, id);
            vo.setCommodityData(allBindCommodity);
        }
        return R.ok(vo);
    }

    @Override
    public R getAllCoupon(Long comId, Long accountId, GetAllCouponDto dto) {
        // 根据页面和页面大小创建分页对象
        Page<ComCouponDat> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 调用baseMapper的getAllCoupon
        IPage<GetAllCouponVo> roomCommodityList = this.baseMapper.getAllCoupon(page, comId, dto);
        return R.ok(roomCommodityList.getRecords(), roomCommodityList.getTotal());
    }

    @Override
    @CacheEvict(value = CacheConstants.WATCH_BASE_COUPON_DATA, key = "#comId + '_' + #id")
    public R delCoupon(Long comId, Long accountId, Long id) {
        ComCouponDat comCouponDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComCouponDat>().eq(ComCouponDat::getId, id)
                .eq(ComCouponDat::getComId, comId).eq(ComCouponDat::getDeleteFlag, false));
        if (ObjectUtils.isEmpty(comCouponDat)) {
            return R.failed("优惠券不存在");
        }
        //
        List<ComCouponLiveDat> comCouponLiveDats = comCouponLiveDatMapper.selectList(new LambdaQueryWrapper<ComCouponLiveDat>().eq(ComCouponLiveDat::getCouponId, id).eq(ComCouponLiveDat::getComId, comId).eq(ComCouponLiveDat::getDeleteFlag, false));
        if(ObjectUtils.isNotEmpty(comCouponLiveDats)){
            return R.failed("该优惠券已绑定直播端，暂时无法删除");
        }
        List<ComCouponCommodityDat> comCouponCommodityDats = comCouponCommodityDatMapper.selectList(new LambdaQueryWrapper<ComCouponCommodityDat>().eq(ComCouponCommodityDat::getCouponId, id).eq(ComCouponCommodityDat::getDeleteFlag, false));
        if(ObjectUtils.isNotEmpty(comCouponCommodityDats)){
            for (ComCouponCommodityDat comCouponCommodityDat : comCouponCommodityDats) {
                clearCache.delCouponCommodity(comId,id,comCouponCommodityDat.getCommodityId());
            }
        }
        comCouponDat.setDeleteFlag(true);
        comCouponDat.setUpdateBy(accountId);
        this.baseMapper.updateById(comCouponDat);
        return R.ok();
    }

    @Override
    public R updateCoupon(Long comId, Long accountId, AddCouponDatDto dto) {
        Long id = dto.getId();
        ComCouponDat comCouponDat = this.baseMapper.selectOne(new LambdaQueryWrapper<ComCouponDat>().eq(ComCouponDat::getId, id)
                .eq(ComCouponDat::getComId, comId).eq(ComCouponDat::getDeleteFlag, false));
        if (ObjectUtils.isEmpty(comCouponDat)) {
            return R.failed("优惠券不存在");
        }
        comCouponDat.setName(dto.getName());
        comCouponDat.setCouponType(dto.getCouponType());
        comCouponDat.setCouponValue(dto.getCouponValue());
        comCouponDat.setRelationCommodity(dto.getRelationCommodity());
        comCouponDat.setAllowMultiple(dto.getAllowMultiple());
        comCouponDat.setUpdateBy(accountId);
        this.baseMapper.updateById(comCouponDat);
        List<ComCouponCommodityDat> comCouponCommodityDats = comCouponCommodityDatMapper.selectList(new LambdaQueryWrapper<ComCouponCommodityDat>()
                .eq(ComCouponCommodityDat::getCouponId, id).eq(ComCouponCommodityDat::getDeleteFlag, false));
        if (ObjectUtils.isNotEmpty(comCouponCommodityDats)) {
            comCouponCommodityDatMapper.update(null, new LambdaUpdateWrapper<ComCouponCommodityDat>()
                    .set(ComCouponCommodityDat::getDeleteFlag, true)
                    .set(ComCouponCommodityDat::getUpdateBy, accountId)
                    .eq(ComCouponCommodityDat::getDeleteFlag, false)
                    .eq(ComCouponCommodityDat::getCouponId, id)
                    .eq(ComCouponCommodityDat::getComId, comId)
            );
        }

        List<ComCouponCommodityDat> data = dto.getData();
        if (ObjectUtils.isNotEmpty(data)) {
            List<ComCouponCommodityDat> list = new ArrayList<>();
            for (ComCouponCommodityDat info : data) {
                ComCouponCommodityDat comCouponCommodityDat = new ComCouponCommodityDat();
                comCouponCommodityDat.setId(idService.nextId());
                comCouponCommodityDat.setComId(comId);
                comCouponCommodityDat.setCommodityId(info.getCommodityId());
                comCouponCommodityDat.setCommodityPrice(info.getCommodityPrice());
                comCouponCommodityDat.setDiscountPrice(info.getDiscountPrice());
                comCouponCommodityDat.setCouponId(id);
                comCouponCommodityDat.setCreateBy(accountId);
                list.add(comCouponCommodityDat);
            }
            // 批量插入关联商品数据
            comCouponCommodityDatMapper.insertBatch(list);
        }
        return R.ok();
    }
}

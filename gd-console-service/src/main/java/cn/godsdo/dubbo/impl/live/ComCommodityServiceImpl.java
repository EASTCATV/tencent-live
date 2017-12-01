package cn.godsdo.dubbo.impl.live;

import cn.godsdo.base.BasePage;
import cn.godsdo.dto.com.AddCommodityDto;
import cn.godsdo.dto.com.GetComCommodityDto;
import cn.godsdo.dubbo.live.ComCommodityService;
import cn.godsdo.entity.com.ComCommodity;
import cn.godsdo.entity.com.ComCommodityGroup;
import cn.godsdo.entity.live.LiveCommodity;
import cn.godsdo.mapper.live.ComCommodityGroupMapper;
import cn.godsdo.mapper.live.ComCommodityMapper;
import cn.godsdo.mapper.live.LiveCommodityMapper;
import cn.godsdo.service.cos.CosService;
import cn.godsdo.util.R;
import cn.godsdo.vo.GetCommodityListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 商品列表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/13
 */
@DubboService
public class ComCommodityServiceImpl extends ServiceImpl<ComCommodityMapper, ComCommodity> implements ComCommodityService {
    @Resource
    ComCommodityGroupMapper comCommodityGroupMapper;
    @DubboReference
    CosService cosService;
    @DubboReference
    IdService idService;
    @Resource
    LiveCommodityMapper liveCommodityMapper;

    @Override
    public R getListByLiveBind(Long comId, GetComCommodityDto dto) {
        // 根据页面和页面大小创建分页对象
        Page<ComCommodity> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 调用baseMapper的getListByRoomBind方法获取与房间关联的商品列表
        IPage<ComCommodity> roomCommodityList = this.baseMapper.getListByRoomBind(page, comId, dto);
        // 将获取的商品列表转换为GetCommodityListVo列表
        List<GetCommodityListVo> collect = roomCommodityList.getRecords().stream().map(e -> {
            GetCommodityListVo vo = new GetCommodityListVo();
            // 将ComCommodity对象的属性复制到GetCommodityListVo对象
            BeanUtils.copyProperties(e, vo);
            Long groupId = e.getGroupId();
            // 默认分组
            if (groupId == 0) {
                vo.setGroupName(Constants.DEFAULT_GROUP);
            } else {
                // 根据comId和groupId查询商品分组信息
                ComCommodityGroup comCommodityGroup = comCommodityGroupMapper.selectOne(new LambdaQueryWrapper<ComCommodityGroup>()
                        .eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getId, groupId));
                // 如果未能查询到商品分组信息，则将分组名设置为默认分组
                if (ObjectUtils.isEmpty(comCommodityGroup)) {
                    vo.setGroupName(Constants.DEFAULT_GROUP);
                } else {
                    // 否则将分组名设置为查询到的商品分组名
                    vo.setGroupName(comCommodityGroup.getGroupName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
        // 返回包含转换后的商品列表和总数的成功响应
        return R.ok(collect, roomCommodityList.getTotal());
    }

    @Override
    public R getListByCouponBind(Long comId, GetComCommodityDto dto) {
        // 根据页面和页面大小创建分页对象
        Page<ComCommodity> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 调用baseMapper的getListByRoomBind方法获取与房间关联的商品列表
        IPage<ComCommodity> roomCommodityList = this.baseMapper.getListByCouponBind(page, comId, dto);
        // 将获取的商品列表转换为GetCommodityListVo列表
        List<GetCommodityListVo> collect = roomCommodityList.getRecords().stream().map(e -> {
            GetCommodityListVo vo = new GetCommodityListVo();
            // 将ComCommodity对象的属性复制到GetCommodityListVo对象
            BeanUtils.copyProperties(e, vo);
            Long groupId = e.getGroupId();
            // 默认分组
            if (groupId == 0) {
                vo.setGroupName(Constants.DEFAULT_GROUP);
            } else {
                // 根据comId和groupId查询商品分组信息
                ComCommodityGroup comCommodityGroup = comCommodityGroupMapper.selectOne(new LambdaQueryWrapper<ComCommodityGroup>()
                        .eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getId, groupId));
                // 如果未能查询到商品分组信息，则将分组名设置为默认分组
                if (ObjectUtils.isEmpty(comCommodityGroup)) {
                    vo.setGroupName(Constants.DEFAULT_GROUP);
                } else {
                    // 否则将分组名设置为查询到的商品分组名
                    vo.setGroupName(comCommodityGroup.getGroupName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
        // 返回包含转换后的商品列表和总数的成功响应
        return R.ok(collect, roomCommodityList.getTotal());
    }


    @Override
    public R getAllList(Long comId, GetComCommodityDto dto) {
        // 获取分页对象
        Page<ComCommodity> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 调用 baseMapper 的 getAllList 方法获取商品列表
        IPage<GetCommodityListVo> comListVOIPage = this.baseMapper.getAllList(page, comId,dto);
        // 对商品列表进行处理，设置默认分组名
        List<GetCommodityListVo> collect = comListVOIPage.getRecords().stream().map(e -> {
            Long groupId = e.getGroupId();
            // 默认分组
            if (groupId == 0) {
                e.setGroupName(Constants.DEFAULT_GROUP);
            } else {
                // 根据商品id和分组id查询具体分组
                ComCommodityGroup comCommodityGroup = comCommodityGroupMapper.selectOne(new LambdaQueryWrapper<ComCommodityGroup>()
                        .eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getId, groupId));
                if (ObjectUtils.isEmpty(comCommodityGroup)) {
                    e.setGroupName(Constants.DEFAULT_GROUP);
                } else {
                    e.setGroupName(comCommodityGroup.getGroupName());
                }
            }
            return e;
        }).collect(Collectors.toList());
        // 封装处理后的商品列表和总数并返回结果
         return R.ok(collect, comListVOIPage.getTotal());
    }


    @Override
    public R addCommodity(Long comId, Long accountId, AddCommodityDto dto) {
        String image = dto.getImage();
        // 根据商品DTO更新商品信息或创建新的商品信息
        if (ObjectUtils.isNotEmpty(dto.getId())) {
            // 如果DTO中的ID不为空，则更新已存在的商品信息
            ComCommodity cc = this.baseMapper.selectOne(Wrappers.<ComCommodity>lambdaQuery().eq(ComCommodity::getId, dto.getId()).eq(ComCommodity::getDeleteFlag, false));
            if (ObjectUtils.isEmpty(cc)) {
                // 如果查询不到对应ID的商品信息，则返回失败结果并提示商品不存在
                return R.failed("商品不存在");
            }
            // 根据DTO和图片信息创建更新后的商品信息
            cc = createcomCommodity(dto, cc, image);
            cc.setUpdateBy(accountId);  // 设置更新者为当前账户ID
            this.baseMapper.updateById(cc);  // 更新商品信息
        } else {
            // 如果DTO中的ID为空，则创建新的商品信息
            ComCommodity ccNew = new ComCommodity();
            // 设置新商品的ID为下一个可用ID
            ccNew.setId(idService.nextId());
            // 设置商品ID
            ccNew.setComId(comId);
            // 设置创建者为当前账户ID
            ccNew.setCreateBy(accountId);
            // 根据DTO和图片信息创建新的商品信息
            ccNew = createcomCommodity(dto, ccNew, image);
            // 插入新的商品信息
            this.baseMapper.insert(ccNew);
        }
        return R.ok();
    }

    @Override
    public R delCommodity(Long comId, Long accountId, Long id) {
        ComCommodity cc = this.baseMapper.selectOne(Wrappers.<ComCommodity>lambdaQuery().eq(ComCommodity::getId, id)
                .eq(ComCommodity::getId, id).eq(ComCommodity::getDeleteFlag, false));
        if (ObjectUtils.isEmpty(cc)) {
            return R.ok();
        }
        List<LiveCommodity> liveRoomCommodities = liveCommodityMapper.selectList(new LambdaQueryWrapper<LiveCommodity>().eq(LiveCommodity::getCommodityId, id)
                .eq(LiveCommodity::getComId, comId).eq(LiveCommodity::getDeleteFlag, false));
        if(ObjectUtils.isNotEmpty(liveRoomCommodities)){
            return R.failed("该商品已绑定直播间，不可删除");
        }
        cc.setDeleteFlag(true);
        cc.setUpdateBy(accountId);
        this.baseMapper.updateById(cc);
        return R.ok();
    }

    @Override
    public R getCommodityListByTemplate(Long comId) {
        List<ComCommodity> vo = this.baseMapper.selectList(new LambdaQueryWrapper<ComCommodity>()
                .eq(ComCommodity::getComId, comId)
                .eq(ComCommodity::getDeleteFlag, false));
        return R.ok(vo);
    }


    private ComCommodity createcomCommodity(AddCommodityDto dto, ComCommodity cc, String image) {
        cc.setCommodity(dto.getCommodity());
        cc.setCommodityLineationPrice(dto.getCommodityLineationPrice());
        cc.setCommodityPrice(dto.getCommodityPrice());
        cc.setImage(image);
        cc.setOtherId(dto.getOtherId());
        cc.setJumpAddress(dto.getJumpAddress());
        cc.setPayType(dto.getPayType());
        cc.setGroupId(dto.getGroupId());
        return cc;
    }

}

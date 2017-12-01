package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dubbo.com.ComVideoGroupService;
import cn.godsdo.entity.com.ComVideoDat;
import cn.godsdo.entity.com.ComVideoGroup;
import cn.godsdo.mapper.com.ComVideoDatMapper;
import cn.godsdo.mapper.com.ComVideoGroupMapper;
import cn.godsdo.mapper.com.LiveBackVideoMapper;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 《客户视频素材分组表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@DubboService
public class ComVideoGroupServiceImpl extends ServiceImpl<ComVideoGroupMapper, ComVideoGroup> implements ComVideoGroupService {
    @DubboReference
    IdService idService;

    @Resource
    LiveBackVideoMapper liveBackVideoMapper;
    @Resource
    ComVideoDatMapper comVideoDatMapper;

    @Override
    public R getComVideoGroupList(Long comId) {
        List<ComVideoGroup> cvg = getComVideoGroups(comId);
        return R.ok(cvg);
    }

    @Override
    public R getComVideoGroupListBySetting(Long comId) {
        List<ComVideoGroup> list = getComVideoGroups(comId);
        list.add(0, new ComVideoGroup(Constants.DEFAULT_GROUP_VALUE, comId, "全部"));
        return R.ok(list);
    }

    @Override
    public R addComVideoGroup(String title, Long comId, Long accountId) {
        ComVideoGroup cvg = new ComVideoGroup();
        cvg.setComId(comId);
        cvg.setId(idService.nextId());
        cvg.setTitle(title);
        cvg.setCreateBy(accountId);
        this.baseMapper.insert(cvg);
        return R.ok();
    }

    @Override
    public R updateComVideoGroup(Long id, String title, Long accountId) {
        // 根据id查询后台视频分组信息
        ComVideoGroup cvg = this.baseMapper.selectOne(Wrappers.<ComVideoGroup>lambdaQuery()
                .eq(ComVideoGroup::getId, id)
                .eq(ComVideoGroup::getDeleteFlg, false));
        // 如果查询结果为空，则返回修改失败信息
        if (ObjectUtils.isEmpty(cvg)) {
            return R.failed("修改失败，查询不到分组信息");
        }
        // 更新分组标题和更新者信息
        cvg.setTitle(title);
        cvg.setUpdateBy(accountId);
        // 根据id更新后台视频分组信息
        this.baseMapper.updateById(cvg);
        // 返回操作成功信息
        return R.ok();
    }

    @Override
    public R deleteComVideoGroup(Long id, Long accountId) {
        // 根据id和删除标记查询媒体库视频分组
        ComVideoGroup cvg = this.baseMapper.selectOne(Wrappers.<ComVideoGroup>lambdaQuery()
                .eq(ComVideoGroup::getId, id)
                .eq(ComVideoGroup::getDeleteFlg, false));
        // 如果查询结果为空，则返回失败信息
        if (ObjectUtils.isEmpty(cvg)) {
            return R.failed("删除失败，查询不到分组信息");
        }

        // 查询该分组下未被删除的直播回放视频列表
        List<ComVideoDat> lrbvList = comVideoDatMapper.selectList(Wrappers.<ComVideoDat>lambdaQuery().eq(ComVideoDat::getGroupId, id).eq(ComVideoDat::getDeleteFlg, false));
        // 如果直播回放视频列表不为空，则更新视频的分组删除标记
        if (ObjectUtils.isNotEmpty(lrbvList)) {
            return R.failed("该分组下有视频，不可删除 ");
//            liveBackVideoMapper.updateVideoGroupForDelete(id, lrbvList);
        }
        // 设置回放视频分组的删除标记为true，并更新修改人
        cvg.setDeleteFlg(1);
        cvg.setUpdateBy(accountId);
        this.baseMapper.updateById(cvg);
        // 返回操作成功结果
        return R.ok();
    }

    /**
     * 获取媒体库视频分组列表
     *
     * @param comId 客户ID
     * @return 回放视频分组列表
     */
    private List<ComVideoGroup> getComVideoGroups(Long comId) {
        List<ComVideoGroup> vo = this.baseMapper.selectList(Wrappers.<ComVideoGroup>lambdaQuery()
                .eq(ComVideoGroup::getComId, comId)
                .eq(ComVideoGroup::getDeleteFlg, false));
        if(ObjectUtils.isEmpty(vo)){
            vo = new ArrayList<ComVideoGroup>();
        }
        // 增加默认分组
        vo.add(0, new ComVideoGroup(0L, comId, Constants.DEFAULT_GROUP));
        return vo;
    }
}

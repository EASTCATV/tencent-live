package cn.godsdo.dubbo.impl.com;

import cn.godsdo.dubbo.com.ComBackVideoGroupService;
import cn.godsdo.entity.com.ComBackVideoGroup;
import cn.godsdo.entity.com.LiveBackVideo;
import cn.godsdo.mapper.com.ComBackVideoGroupMapper;
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
 * 《客户回看视频分组表》 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@DubboService
public class ComBackVideoGroupServiceImpl extends ServiceImpl<ComBackVideoGroupMapper, ComBackVideoGroup> implements ComBackVideoGroupService {

    @DubboReference
    IdService idService;

    @Resource
    LiveBackVideoMapper liveBackVideoMapper;

    /**
     * 获取回放视频分组列表
     *
     * @param comId 公司ID
     * @return 回放视频分组列表
     */
    @Override
    public R getBackVideoGroupList(Long comId) {
        List<ComBackVideoGroup> cbvg = getBackVideoGroups(comId);
        return R.ok(cbvg);
    }

    /**
     * 根据直播间设置获取回放视频分组列表
     *
     * @param comId 公司ID
     * @return 回放视频分组列表
     */
    @Override
    public R getBackVideoGroupListBySetting(Long comId) {
        List<ComBackVideoGroup> cbvg = getBackVideoGroups(comId);
        // 增加默认分组
        cbvg.add(0, new ComBackVideoGroup(Constants.DEFAULT_GROUP_VALUE, comId, "全部"));
        return R.ok(cbvg);
    }


    /**
     * 添加回放视频分组
     *
     * @param title     标题
     * @param comId     公司ID
     * @param accountId 账户ID
     * @return 添加操作结果
     */
    @Override
    public R addBackVideoGroup(String title, Long comId, Long accountId) {
        ComBackVideoGroup cbvg = new ComBackVideoGroup();
        cbvg.setComId(comId);
        cbvg.setId(idService.nextId());
        cbvg.setTitle(title);
        cbvg.setCreateBy(accountId);
        this.baseMapper.insert(cbvg);
        return R.ok();
    }

    /**
     * 更新回放视频分组
     *
     * @param id        分组ID
     * @param title     标题
     * @param accountId 账户ID
     * @return 更新操作结果
     */
    @Override
    public R updateBackVideoGroup(Long id, String title, Long accountId) {
        // 根据id查询后台视频分组信息
        ComBackVideoGroup cbvg = this.baseMapper.selectOne(Wrappers.<ComBackVideoGroup>lambdaQuery()
                .eq(ComBackVideoGroup::getId, id)
                .eq(ComBackVideoGroup::getDeleteFlag, false));
        // 如果查询结果为空，则返回修改失败信息
        if (ObjectUtils.isEmpty(cbvg)) {
            return R.failed("修改失败，查询不到分组信息");
        }
        // 更新分组标题和更新者信息
        cbvg.setTitle(title);
        cbvg.setUpdateBy(accountId);
        // 根据id更新后台视频分组信息
        this.baseMapper.updateById(cbvg);
        // 返回操作成功信息
        return R.ok();
    }


    /**
     * 删除回放视频分组
     *
     * @param id        分组ID
     * @param accountId 账户ID
     * @return 删除操作结果
     */
    @Override
    public R deleteBackVideoGroup(Long id, Long accountId) {
        // 根据id和删除标记查询回放视频分组
        ComBackVideoGroup cbvg = this.baseMapper.selectOne(Wrappers.<ComBackVideoGroup>lambdaQuery()
                .eq(ComBackVideoGroup::getId, id)
                .eq(ComBackVideoGroup::getDeleteFlag, false));
        // 如果查询结果为空，则返回失败信息
        if (ObjectUtils.isEmpty(cbvg)) {
            return R.failed("删除失败，查询不到分组信息");
        }

        // 查询该分组下未被删除的直播回放视频列表
        List<LiveBackVideo> lrbvList = liveBackVideoMapper.selectList(Wrappers.<LiveBackVideo>lambdaQuery().eq(LiveBackVideo::getGroupId, id).eq(LiveBackVideo::getDeleteFlg, false));
        // 如果直播回放视频列表不为空，则更新视频的分组删除标记
        if (ObjectUtils.isNotEmpty(lrbvList)) {
            return R.failed("该分组下有视频，不可删除！！");
//            liveBackVideoMapper.updateVideoGroupForDelete(id, lrbvList);
        }
        // 设置回放视频分组的删除标记为true，并更新修改人
        cbvg.setDeleteFlag(true);
        cbvg.setUpdateBy(accountId);
        this.baseMapper.updateById(cbvg);
        // 返回操作成功结果
        return R.ok();
    }

    /**
     * 获取回放视频分组列表
     *
     * @param comId 客户ID
     * @return 回放视频分组列表
     */
    private List<ComBackVideoGroup> getBackVideoGroups(Long comId) {
        List<ComBackVideoGroup> cbvg = this.baseMapper.selectList(Wrappers.<ComBackVideoGroup>lambdaQuery()
                .eq(ComBackVideoGroup::getComId, comId)
                .eq(ComBackVideoGroup::getDeleteFlag, false));
        if(ObjectUtils.isEmpty(cbvg)){
            cbvg= new ArrayList<ComBackVideoGroup>();
        }
        // 增加默认分组
        cbvg.add(0, new ComBackVideoGroup(0L, comId, Constants.DEFAULT_GROUP));
        return cbvg;
    }
}

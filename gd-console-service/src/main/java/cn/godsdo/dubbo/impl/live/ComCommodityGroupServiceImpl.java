package cn.godsdo.dubbo.impl.live;

import cn.godsdo.base.BasePage;
import cn.godsdo.dubbo.live.ComCommodityGroupService;
import cn.godsdo.entity.com.ComCommodityGroup;
import cn.godsdo.mapper.live.ComCommodityGroupMapper;
import cn.godsdo.mapper.live.ComCommodityMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.GetAllGroupVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 商品分组表（默认分组为0） 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/13
 */
@DubboService
public class ComCommodityGroupServiceImpl extends ServiceImpl<ComCommodityGroupMapper, ComCommodityGroup> implements ComCommodityGroupService {

    @Resource
    ComCommodityMapper comCommodityMapper;

    @DubboReference
    IdService idService;

    /**
     * 根据商品ID获取所有分组
     *
     * @param comId 商品ID
     * @return 包含分组信息的R对象
     */
    @Override
    public R getAllGroupByCommodity(Long comId) {
        // 根据商品ID查询商品分组列表
        List<ComCommodityGroup> ccg = this.baseMapper.selectList(Wrappers.<ComCommodityGroup>lambdaQuery()
                .select(ComCommodityGroup::getId, ComCommodityGroup::getGroupName)
                .eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getDeleteFlag, false));
        // 创建默认分组对象
        ComCommodityGroup ccgNew = new ComCommodityGroup();
        ccgNew.setId(Constants.DEFAULT_GROUP_VALUE);
        ccgNew.setGroupName(Constants.DEFAULT_GROUP);
        // 将默认分组插入到列表首位
        ccg.add(0, ccgNew);
        // 返回包含分组信息的R对象
        return R.ok(ccg);
    }


    @Override
    public R getAllGroup(Long comId, BasePage dto) {
        // 获取分页对象
        Page<ComCommodityGroup> page = new Page<>(dto.getPage(), dto.getPageSize());
        // 查询商品分组信息
        IPage<GetAllGroupVo> comListVOIPage = this.baseMapper.getAllGroup(page, comId);
        // 获取查询结果列表
        List<GetAllGroupVo> collect = comListVOIPage.getRecords();
        // 如果列表为空，则初始化为空列表
        if (collect.size() == 0) {
            collect = new ArrayList<GetAllGroupVo>();
        }
        // 创建默认分组对象
        GetAllGroupVo ccgNew = new GetAllGroupVo();
        ccgNew.setId(Constants.DEFAULT_GROUP_VALUE);
        ccgNew.setGroupName(Constants.DEFAULT_GROUP);
        ccgNew.setCommoditySum(comCommodityMapper.getCommoditySum(Constants.DEFAULT_GROUP_VALUE, comId));
        // 将默认分组对象添加到列表头部
        collect.add(0, ccgNew);
        // 返回包含查询结果列表和总数的响应对象
        return R.ok(collect, comListVOIPage.getTotal() + 1);
    }

    @Override
    public R addGroup(Long comId, Long accountId, String groupName) {
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<ComCommodityGroup>().eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getDeleteFlag, false));
        if (count > 20) {
            return R.failed("分组数量不能超过20个");
        }
        ComCommodityGroup ccgNew = new ComCommodityGroup();
        ccgNew.setId(idService.nextId());
        ccgNew.setGroupName(groupName);
        ccgNew.setComId(comId);
        ccgNew.setCreateBy(accountId);
        // 执行插入操作
        this.baseMapper.insert(ccgNew);
        // 返回插入成功结果
        return R.ok();

    }


    @Override
    public R updateGroup(Long comId, Long accountId, Long groupId, String groupName) {
        // 查询指定条件的商品分组
        ComCommodityGroup ccg = this.baseMapper.selectOne(Wrappers.<ComCommodityGroup>lambdaQuery().eq(ComCommodityGroup::getId, groupId)
                .eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getDeleteFlag, false));
        // 如果商品分组不存在，则返回失败结果
        if (ObjectUtils.isEmpty(ccg)) {
            return R.failed("分组不存在");
        }
        // 更新商品分组的名称和更新人
        ccg.setGroupName(groupName);
        ccg.setUpdateBy(accountId);
        // 执行更新操作
        this.baseMapper.updateById(ccg);
        // 返回更新成功结果
        return R.ok();
    }

    @Override
    public R delGroup(Long comId, Long accountId, Long groupId) {
        // 通过comId、groupId查询商品分组信息
        ComCommodityGroup ccg = this.baseMapper.selectOne(Wrappers.<ComCommodityGroup>lambdaQuery().eq(ComCommodityGroup::getId, groupId)
                .eq(ComCommodityGroup::getComId, comId).eq(ComCommodityGroup::getDeleteFlag, false));
        // 如果商品分组信息为空，则返回失败结果
        if (ObjectUtils.isEmpty(ccg)) {
            return R.failed("分组不存在");
        }
        // 将商品分组的删除标志设置为true，并记录更新人员的账号ID
        ccg.setDeleteFlag(true);
        ccg.setUpdateBy(accountId);
        // 更新商品分组信息
        this.baseMapper.updateById(ccg);
        // 通过删除分组操作更新商品的分组ID
        comCommodityMapper.updateGroupIdByDelete(comId, groupId);
        // 返回操作成功结果
        return R.ok();
    }

}

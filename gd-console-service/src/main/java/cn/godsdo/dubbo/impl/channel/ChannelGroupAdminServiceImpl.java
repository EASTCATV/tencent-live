package cn.godsdo.dubbo.impl.channel;

import cn.godsdo.dto.channel.AddGroupAdminDto;
import cn.godsdo.dubbo.channel.ChannelGroupAdminService;
import cn.godsdo.entity.channel.ChannelGroupAdmin;
import cn.godsdo.mapper.channel.ChannelGroupAdminMapper;
import cn.godsdo.util.R;
import cn.godsdo.vo.channel.GetGroupAdminListVo;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.constant.Constants;
import com.y20y.interfaces.IdService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * 渠道分组管理员表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-04-23
 */
@DubboService
public class ChannelGroupAdminServiceImpl extends ServiceImpl<ChannelGroupAdminMapper, ChannelGroupAdmin> implements ChannelGroupAdminService {

    @DubboReference
    IdService idService;

    @Override
    public R getGroupAdminList(Long groupId, Long comId) {
        List<GetGroupAdminListVo> list = this.baseMapper.getGroupAdminList(groupId, comId);
        return R.ok(list);
    }

    @Override
    public R addGroupAdmin(AddGroupAdminDto dto, Long comId, Long accountId) {
        // 从dto中获取管理员的ids
        List<Long> ids = dto.getIds();
        // 如果ids为空，返回错误信息
        if (ObjectUtil.isEmpty(ids)) {
            return R.failed("请选择管理员");
        }
        // 从dto中获取groupId
        Long groupId = dto.getGroupId();
        // 查询符合条件的ChannelGroupAdmin列表
        List<ChannelGroupAdmin> list = this.baseMapper.selectList(new LambdaQueryWrapper<ChannelGroupAdmin>()
                .eq(ChannelGroupAdmin::getDeleteFlag, false)
                .eq(ChannelGroupAdmin::getGroupId, groupId)
                .eq(ChannelGroupAdmin::getComId, comId));
        // 创建一个空的结果列表
        ArrayList<ChannelGroupAdmin> result = new ArrayList<>();
        // 遍历ids列表
        for (Long id : ids) {
            // 使用Stream查找是否已存在对应id的ChannelGroupAdmin
            Optional<ChannelGroupAdmin> first = list.stream().filter(item -> item.getChannelId().equals(id)).findFirst();
            // 如果不存在，则创建新的ChannelGroupAdmin对象并添加到结果列表中
            if (!first.isPresent()) {
                ChannelGroupAdmin cga = new ChannelGroupAdmin();
                cga.setId(idService.nextId());
                cga.setChannelId(id);
                cga.setComId(comId);
                cga.setGroupId(dto.getGroupId());
                cga.setCreateBy(accountId);
                result.add(cga);
            }
        }
        // 如果结果列表为空，返回错误信息
        if (ObjectUtil.isEmpty(result)) {
            return R.failed("已存在，请重新选择");
        }
        // 计算结果列表和原列表的总长度，并检查是否超过最大限制
        int total = result.size() + list.size();
        if (total > Constants.CHANNEL_ADMIN_MAX) {
            return R.failed("管理员数量过多，最多" + Constants.CHANNEL_ADMIN_MAX + "个");
        }
        // 将结果列表批量插入数据库
        this.baseMapper.insertBatch(result);
        // 操作成功，返回成功信息
        return R.ok();

    }

    @Override
    public R delGroupAdmin(Long id, Long comId, Long accountId) {
        // 根据指定条件查询频道组管理员
        ChannelGroupAdmin channelGroupAdmin = this.baseMapper.selectOne(new LambdaQueryWrapper<ChannelGroupAdmin>()
                .eq(ChannelGroupAdmin::getDeleteFlag, false)
                .eq(ChannelGroupAdmin::getId, id)
                .eq(ChannelGroupAdmin::getComId, comId));

        // 如果频道组管理员信息为空，则返回失败信息
        if (ObjectUtil.isEmpty(channelGroupAdmin)) {
            return R.failed("信息不存在");
        }

        // 设置删除标志为true，并更新操作人信息
        channelGroupAdmin.setDeleteFlag(true);
        channelGroupAdmin.setUpdateBy(accountId);

        // 根据频道组管理员信息的ID更新数据库记录
        this.baseMapper.updateById(channelGroupAdmin);

        return R.ok();
    }

    @Override
    public List<ChannelGroupAdmin> getListByChannelId(String channelId) {
        return this.baseMapper.selectList(new LambdaQueryWrapper<ChannelGroupAdmin>()
                .eq(ChannelGroupAdmin::getDeleteFlag, false)
                .eq(ChannelGroupAdmin::getChannelId, channelId));
    }
}

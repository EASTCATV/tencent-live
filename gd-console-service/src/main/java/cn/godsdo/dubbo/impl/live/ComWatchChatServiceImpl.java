package cn.godsdo.dubbo.impl.live;

import cn.godsdo.constant.CacheConstants;
import cn.godsdo.dto.com.UpdateWatchChatDto;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.live.ComWatchChatService;
import cn.godsdo.entity.com.ComWatchChat;
import cn.godsdo.mapper.com.ComWatchChatMapper;
import cn.godsdo.util.R;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.cache.annotation.CacheEvict;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-10
 */
@DubboService
public class ComWatchChatServiceImpl extends ServiceImpl<ComWatchChatMapper, ComWatchChat> implements ComWatchChatService {

    @DubboReference
    IdService idService;
    @DubboReference(check = false)
    ClearCache clearcache;
    @Override
    public R getComWatchChat(Long comId, Long accountId) {
        // 通过ID获取观看聊天记录
        ComWatchChat watchChatByComId = getWatchChatByComId(comId);
        // 如果观看聊天记录不为空
//        if (ObjectUtils.isEmpty(watchChatByComId)) {
//            // 实例化观看聊天记录对象，并设置相应属性
//            watchChatByComId = new ComWatchChat();
//            watchChatByComId.setComId(comId);
//            watchChatByComId.setCreateBy(accountId);
//            watchChatByComId.setId(idService.nextId());
//            // 插入观看聊天记录
//            this.baseMapper.insert(watchChatByComId);
//        }
        // 返回包含观看聊天记录的R通用返回对象
        return R.ok(watchChatByComId);
    }

    @Override
    public R getComFree(Long comId, Long accountId) {
        // 通过ID获取观看聊天记录
//        ComWatchChat watchChatByComId = this.baseMapper.selectOne(Wrappers
//                .<ComWatchChat>query().lambda().select( ComWatchChat::getFreeWord)
//                .eq(ComWatchChat::getComId, comId).eq(ComWatchChat::getComId, comId));
        ComWatchChat watchChatByComId = getWatchChatByComId(comId);
        // 如果观看聊天记录不为空
//        if (ObjectUtils.isEmpty(watchChatByComId)) {
//            // 实例化观看聊天记录对象，并设置相应属性
//            watchChatByComId = new ComWatchChat();
//            watchChatByComId.setComId(comId);
//            watchChatByComId.setCreateBy(accountId);
//            watchChatByComId.setId(idService.nextId());
//            // 插入观看聊天记录
//            this.baseMapper.insert(watchChatByComId);
//        }
        return R.ok(watchChatByComId);
    }

    @Override
    public R getComSensitiveWord(Long comId, Long accountId) {
        // 通过ID获取观看聊天记录
//        ComWatchChat watchChatByComId = this.baseMapper.selectOne(Wrappers
//                .<ComWatchChat>query().lambda().select( ComWatchChat::getSensitiveWord)
//                .eq(ComWatchChat::getComId, comId).eq(ComWatchChat::getComId, comId));
        ComWatchChat watchChatByComId = getWatchChatByComId(comId);
        // 如果观看聊天记录不为空
//        if (ObjectUtils.isEmpty(watchChatByComId)) {
//            // 实例化观看聊天记录对象，并设置相应属性
//            watchChatByComId = new ComWatchChat();
//            watchChatByComId.setComId(comId);
//            watchChatByComId.setCreateBy(accountId);
//            watchChatByComId.setId(idService.nextId());
//            // 插入观看聊天记录
//            this.baseMapper.insert(watchChatByComId);
//        }
        return R.ok(watchChatByComId);
    }
    //todo 删除系统级别缓存

    @Override
    //@CacheEvict(key = "#comId", value = CacheConstants.COM_WATCH_CHAT)
    @CacheEvict(value = CacheConstants.COM_WATCH_BASE_CHAT, key = "#comId")
    public R updateComWatchChat(Long comId, Long accountId, UpdateWatchChatDto dto) {
        //删除缓存
        //clearcache.delLiveCache(comId, liveId);
        // 通过comId获取ComWatchChat信息
        ComWatchChat watchChatByComId = getWatchChatByComId(comId);
        if (ObjectUtils.isNotEmpty(watchChatByComId)) {
            // 如果ComWatchChat信息存在，则更新信息
            watchChatByComId.setFreeWord(dto.getFreeWord());
            watchChatByComId.setSensitiveOn(dto.getSensitiveOn());
            watchChatByComId.setSensitiveWord(dto.getSensitiveWord());
            watchChatByComId.setUpdateBy(accountId);
            this.baseMapper.updateById(watchChatByComId);
        }
//        else {
//            // 如果ComWatchChat信息不存在，则创建新的信息并插入数据库
//            watchChatByComId = new ComWatchChat();
//            watchChatByComId.setComId(comId);
//            watchChatByComId.setCreateBy(accountId);
//            watchChatByComId.setId(idService.nextId());
//            watchChatByComId.setFreeWord(dto.getFreeWord());
//            watchChatByComId.setSensitiveOn(dto.getSensitiveOn());
//            watchChatByComId.setSensitiveWord(dto.getSensitiveWord());
//            this.baseMapper.insert(watchChatByComId);
//        }
        return R.ok();
    }


    /**
     * 根据 comId 获取 ComWatchChat 对象
     * @param comId
     * @return
     */
    private ComWatchChat getWatchChatByComId(Long comId) {
        // 使用 baseMapper 执行查询操作，根据 comId 查询符合条件的一条记录
        return this.baseMapper.selectOne(new LambdaQueryWrapper<ComWatchChat>()
                .eq(ComWatchChat::getComId, comId));
    }


}

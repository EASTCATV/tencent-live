package cn.godsdo.dubbo.impl.live;

import cn.godsdo.dto.live.UpdateLiveTagsDto;
import cn.godsdo.dto.msg.MyCustomData;
import cn.godsdo.dubbo.cache.ClearCache;
import cn.godsdo.dubbo.live.LiveTagService;
import cn.godsdo.entity.live.LiveTag;
import cn.godsdo.mapper.live.LiveTagMapper;
import cn.godsdo.util.R;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import io.github.doocs.im.ClientConfiguration;
import io.github.doocs.im.ImClient;
import io.github.doocs.im.constant.OnlineOnlyFlag;
import io.github.doocs.im.model.message.TIMMsgElement;
import io.github.doocs.im.model.message.TIMTextMsgElement;
import io.github.doocs.im.model.request.SendGroupMsgRequest;
import io.github.doocs.im.model.response.SendGroupMsgResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 房间自定义菜单配置表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018/10/11
 */
@DubboService
@Slf4j
public class LiveTagServiceImpl extends ServiceImpl<LiveTagMapper, LiveTag> implements LiveTagService {

    @DubboReference
    IdService idService;
    @DubboReference(check = false)
    ClearCache clearcache;

    @Override
//    @Cacheable(key = "#roomId", value = CacheConstants.ROOM_TAGS_VALUE, unless = "#roomId==null")
    public List<LiveTag> getLiveTags(Long liveId) {
        List<LiveTag> tags = this.baseMapper.selectList(Wrappers.<LiveTag>lambdaQuery().eq(LiveTag::getLiveId, liveId).orderByAsc(LiveTag::getTagOrder));
        return tags;
    }


    @Override
    public R UpdateLiveTags(UpdateLiveTagsDto dto, Long comId, Long accountId) {
        Long liveId = dto.getLiveId();
        //删除缓存
        clearcache.delLiveCache(comId, liveId);

        // 如果传入的dto对象不为空则继续执行
        if (ObjectUtils.isNotEmpty(dto)){
            try {
                // 解析标签信息
                dto.decodeTagInfos();
                // 清除之前的标签页
                this.baseMapper.delete(Wrappers.<LiveTag>update().lambda()
                        .eq(LiveTag::getLiveId, dto.getLiveId()));
                List<LiveTag> roomTagList = dto.getRoomTags();
                int i = 0;
                for (LiveTag roomTag : roomTagList) {
                    roomTag.setId(idService.nextId());
                    roomTag.setComId(comId);
                    roomTag.setTagOrder(i);
                    i++;
                    roomTag.setCreateBy(accountId);
                }
                // 批量插入新的标签
                this.baseMapper.insertBatch(roomTagList);
            }catch (Exception e){
                // 如果出现异常，记录错误日志并打印异常堆栈信息

                log.error("直播间菜单保存失败，报错如下：");
                e.printStackTrace();
                return R.failed("直播间菜单保存失败");
            }

        }else {
            // 如果dto对象为空，返回保存失败的响应
            return R.failed("保存失败");
        }
        // 执行成功后返回成功的响应
        return R.ok();
    }

}

package cn.godsdo.dubbo.impl.com;


import cn.godsdo.dto.mediaLibrary.ChunkInfoDto;
import cn.godsdo.dubbo.com.ComChunkInfoService;
import cn.godsdo.entity.com.ComChunkInfo;
import cn.godsdo.mapper.com.ComChunkInfoMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;

import java.util.List;

/**
 * <p>
 * 视频分块上传记录表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-12
 */
@DubboService
public class ComChunkInfoServiceImpl extends ServiceImpl<ComChunkInfoMapper, ComChunkInfo> implements ComChunkInfoService {

    @DubboReference
    IdService idService;

    @Override
    public int delChunk(Long comId, String identifier) {
        return this.baseMapper.delete(Wrappers.<ComChunkInfo>lambdaQuery().eq(ComChunkInfo::getComId,comId).eq(ComChunkInfo::getIdentifier,identifier));

    }

    @Override
    public int saveChunk(ChunkInfoDto chunk) {
        ComChunkInfo comChunkInfo = new ComChunkInfo();
        BeanUtils.copyProperties(chunk, comChunkInfo);
        comChunkInfo.setId(String.valueOf(idService.nextId()));
        comChunkInfo.setCurrentChunksize(chunk.getCurrentChunkSize());
        return this.baseMapper.insert(comChunkInfo);
    }


    @Override
    public List<Integer> checkChunk(Long comId, String identifier) {
        return this.baseMapper.checkChunk(comId, identifier);
    }
}

package cn.godsdo.dubbo.impl.intelligent;

import cn.godsdo.dto.live.StartIntelligentDto;
import cn.godsdo.dubbo.intelligent.IntelligentLiveRecordService;
import cn.godsdo.entity.intelligent.IntelligentLiveRecord;
import cn.godsdo.entity.intelligent.IntelligentTemplateDat;
import cn.godsdo.mapper.intelligent.IntelligentLiveRecordMapper;
import cn.godsdo.mapper.intelligent.IntelligentTemplateDatMapper;
import cn.godsdo.util.R;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Date;

/**
 * <p>
 * 智能直播开播记录表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2024-06-26
 */
@DubboService
public class IntelligentLiveRecordServiceImpl extends ServiceImpl<IntelligentLiveRecordMapper, IntelligentLiveRecord> implements IntelligentLiveRecordService {

    @DubboReference
    IdService idService;

    @Resource
    IntelligentTemplateDatMapper intelligentTemplateDatMapper;


    @Override
    public R addIntelligentLiveRecord(StartIntelligentDto dto) {
        Long templateId = dto.getTemplateId();
        IntelligentTemplateDat dat = intelligentTemplateDatMapper.selectById(templateId);
        IntelligentLiveRecord intelligentLiveRecord = new IntelligentLiveRecord();
        intelligentLiveRecord.setTemplateId(templateId);
        intelligentLiveRecord.setType(dto.getLiveType());
        String startTime = dto.getStartTime();
//        DateTime parse = DateUtil.parse(startTime);
        intelligentLiveRecord.setStartTime(startTime);
        String endTime = dto.getEndTime();
        intelligentLiveRecord.setEndTime(endTime);
        if(dto.getLiveType()==1){
            intelligentLiveRecord.setJobId(dto.getJobId());
            intelligentLiveRecord.setTime(dto.getTime());
        }
        intelligentLiveRecord.setStatus(0);
        intelligentLiveRecord.setTemplateId(dto.getTemplateId());
        intelligentLiveRecord.setLiveId(dto.getLiveId());
        intelligentLiveRecord.setVideoDuration(dat.getVideoDuration());
        intelligentLiveRecord.setComId(dto.getComId());
        intelligentLiveRecord.setCreateBy(dto.getAccountId());
        intelligentLiveRecord.setName(dat.getName());
//        intelligentLiveRecord.set(dto.getTime());
        this.baseMapper.insert(intelligentLiveRecord);
        return R.ok();
    }
}

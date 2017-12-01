package cn.godsdo.controller.channel;


import cn.godsdo.dto.GetComChannelRankingChartDto;
import cn.godsdo.dubbo.channel.ChannelRankingStatisticsService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 渠道排行榜统计 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-07-12
 */

@RestController
@RequestMapping("/channelRankingStatistics")
@CrossOrigin
public class ChannelRankingStatisticsController {

    @DubboReference
    ChannelRankingStatisticsService channelRankingStatisticsService;

    /**
     * 获取渠道排行榜柱状图                                                                                                                                                                              定该渠道的直播间列表
     *
     * @return
     */
    @PostMapping("/getComChannelRankingChart")
    public R getComChannelRankingChart(@RequestBody GetComChannelRankingChartDto vo) {
        Long comId = ShiroUtil.getComId();
        return channelRankingStatisticsService.getComChannelRankingChart(comId, vo);
    }
    /**
     * 获取渠道排行榜                                                                                                                                                                          定该渠道的直播间列表
     *
     * @return
     */
    @PostMapping("/getComChannelRankingData")
    public R getComChannelRankingData(@RequestBody GetComChannelRankingChartDto vo) {
        Long comId = ShiroUtil.getComId();
        return channelRankingStatisticsService.getComChannelRankingData(comId, vo);
    }

    /**
     * 获取渠道详情                                                                                                                                                                          定该渠道的直播间列表
     *
     * @return
     */
    @PostMapping("/getComChannelRankingInfo")
    public R getComChannelRankingInfo(@RequestBody GetComChannelRankingChartDto vo) {
        Long comId = ShiroUtil.getComId();
        return channelRankingStatisticsService.getComChannelRankingInfo(comId, vo);
    }
}

package cn.godsdo.controller.camp.assistant;

import cn.godsdo.dto.GetPayOrdersDto;
import cn.godsdo.dto.QueryLiveUserWatchDto;
import cn.godsdo.dto.StaticOnlineDto;
import cn.godsdo.dubbo.LiveStatisticsDatService;
import cn.godsdo.dubbo.PayOrderService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.util.interfaces.RateLimit;
import cn.godsdo.vo.QueryLiveUserWatchVO;
import cn.hutool.core.lang.Assert;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: CR7
 * @Date: 2019/5/5 16:26
 * @Description: 统计相关接口
 */
@RestController
@RequestMapping("camp/statistics")
@CrossOrigin
public class CampStatisticsController {

    @DubboReference
    LiveStatisticsDatService liveStatisticsService;

    @DubboReference
    PayOrderService payOrderService;

    /**
     * 在线人数统计
     */
    @PostMapping("/onLineData")
    public R onLineData(@RequestBody StaticOnlineDto dto) {
        Long comId = ShiroUtil.getComId();
        dto.setComId(comId);
        return liveStatisticsService.onLineData(dto);
    }

    /**
     * 历史在线人数
     */
    @PostMapping("/onLineDataHistory")
    public R onLineDataHistory(@RequestBody StaticOnlineDto dto) {
        return liveStatisticsService.onLineDataHistory(dto);
    }

    /**
     * 用户观看列表
     */
    @PostMapping("/queryLiveUserWatchList")
    public R queryLiveUserWatchList(@RequestBody QueryLiveUserWatchDto query) {
        Long comId = ShiroUtil.getComId();
        query.setComId(comId);
        return liveStatisticsService.queryLiveUserWatchQuery(query);
    }

    /**
     * 用户列表-直播观看数据
     */
    @PostMapping("/queryLiveUserWatchAll")
    public R queryLiveUserWatchAll(@RequestBody QueryLiveUserWatchDto query) {
        return liveStatisticsService.queryLiveUserWatchAll(query);
    }

    /**
     * 用户观看详情
     */
    @PostMapping("/queryLiveUserWatchDetail")
    public R queryLiveUserWatchDetail(@RequestBody QueryLiveUserWatchDto query) {
        if (query.getUserId() == null) {
            return R.failed("userId不能为空");
        }
        List<QueryLiveUserWatchVO> queryLiveUserWatchVOList = liveStatisticsService.queryLiveUserWatchDetail(query);
        return R.ok(queryLiveUserWatchVOList,queryLiveUserWatchVOList.size());
    }

    /**
     * 用户订单数据
     */
    @PostMapping("/queryPayOrderData")
    public R queryPayOrderData(@RequestBody GetPayOrdersDto query) {
        if (query.getLiveId() == null) {
            return R.failed("liveId不能为空");
        }
        Long comId = ShiroUtil.getComId();
        query.setComId(comId);
        return payOrderService.queryPayOrderData(query);
    }

    /**
     * 用户订单数据-统计
     */
    @RateLimit(value = 100)
    @PostMapping("/queryPayOrderSum")
    public R queryPayOrderSum(@RequestBody GetPayOrdersDto query) {
        if (query.getLiveId() == null) {
            return R.failed("liveId不能为空");
        }
        return payOrderService.queryPayOrderSum(query);
    }

    /**
     * 用户观看统计
     */
    @PostMapping("/queryLiveUserWatchSum")
    public R queryLiveUserWatchSum(@RequestBody QueryLiveUserWatchDto query) {
        Long comId = ShiroUtil.getComId();
        query.setComId(comId);
        return liveStatisticsService.queryLiveUserWatchSum(query);
    }

    /**
     * 数据分析-直播概况
     */
    @PostMapping("/queryLiveUserWatchGeneral")
    public R queryLiveUserWatchGeneral(@RequestBody QueryLiveUserWatchDto query) {
        try {
            Assert.notNull(query.getQueryDate(),"查询时间不能为空");
            Assert.notNull(query.getLiveId(),"查询直播间不能为空");
        } catch (IllegalArgumentException e) {
            return R.failed(e.getMessage());
        }
        return liveStatisticsService.queryLiveUserWatchGeneral(query);
    }

    /**
     * 用户观看列表下载
     */
    @GetMapping("/downLiveUserWatchList")
    public void downLiveUserWatchList(HttpServletResponse response,@RequestParam("liveId") Long liveId, @RequestParam("queryDate") String queryDate) throws IOException {
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("用户观看列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        QueryLiveUserWatchDto watchQuery = new QueryLiveUserWatchDto();
        watchQuery.setLiveId(liveId);
        watchQuery.setQueryDate(queryDate);
        List<QueryLiveUserWatchVO> queryLiveUserWatchVOList = liveStatisticsService.downLiveUserWatchList(watchQuery);

        EasyExcel.write(response.getOutputStream(), QueryLiveUserWatchVO.class).sheet("Sheet1").doWrite(queryLiveUserWatchVOList);
    }

    /**
     * 用户观看详情下载
     */
    @GetMapping("/downLiveUserWatchDetail")
    public void downLiveUserWatchDetail(HttpServletResponse response, @RequestParam("liveId") Long liveId, @RequestParam("queryDate") String queryDate,
                                        @RequestParam("userIds") String userIds) {
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("用户观看详情", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        String[] users = userIds.split(",");

        QueryLiveUserWatchDto query = new QueryLiveUserWatchDto();
        query.setLiveId(liveId);
        query.setQueryDate(queryDate);
        // 这里 指定文件
        try (ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), QueryLiveUserWatchVO.class).build()) {
            // 去调用写入,这里我调用了五次，实际使用时根据数据库分页的总的页数来。这里最终会写到5个sheet里面
            for (int i = 0; i < users.length; i++) {
                String userId = users[i];
                query.setUserId(Long.parseLong(userId));
                List<QueryLiveUserWatchVO> data = liveStatisticsService.queryLiveUserWatchDetail(query);
                // 每次都要创建writeSheet 这里注意必须指定sheetNo 而且sheetName必须不一样。这里注意DemoData.class 可以每次都变，我这里为了方便 所以用的同一个class
                WriteSheet writeSheet = EasyExcel.writerSheet(i, "Sheet" + i).head(QueryLiveUserWatchVO.class).build();
                excelWriter.write(data, writeSheet);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}

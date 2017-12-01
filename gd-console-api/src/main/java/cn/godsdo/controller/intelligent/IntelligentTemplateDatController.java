package cn.godsdo.controller.intelligent;


import cn.godsdo.config.listener.LiveInteractMsgListener;
import cn.godsdo.constant.MongoConstant;
import cn.godsdo.dto.intelligent.GetTemplateInfoDto;
import cn.godsdo.dto.intelligent.GetTemplateListDto;
import cn.godsdo.dto.intelligent.ImportMessagesDataDto;
import cn.godsdo.dto.intelligent.UpdateTemplateDto;
import cn.godsdo.dubbo.com.ComBotService;
import cn.godsdo.dubbo.intelligent.IntelligentTemplateDatService;
import cn.godsdo.dubbo.live.ComCommodityService;
import cn.godsdo.dubbo.live.ComGiftConfigService;
import cn.godsdo.dubbo.live.LiveMsgService;
import cn.godsdo.util.R;
import cn.godsdo.util.ShiroUtil;
import cn.godsdo.vo.intelligent.LiveCommodityMsgVo;
import cn.godsdo.vo.intelligent.LiveInteractMsgVo;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>
 * 智能模板列表 前端控制器
 * </p>
 *
 * @author W~Y~H
 * @since 2024-06-04
 */
@CrossOrigin
@RestController
@RequestMapping("/intelligentTemplateDat")
public class IntelligentTemplateDatController {
    @DubboReference
    IntelligentTemplateDatService intelligentTemplateDatService;
    @DubboReference
    LiveMsgService liveMsgService;
    @DubboReference
    ComGiftConfigService comGiftConfigService;
    @DubboReference
    ComCommodityService comCommodityService;
    @DubboReference
    ComBotService comBotService;

    /**
     * 新建智能模版
     *
     * @param videoId
     * @param type
     * @return
     */
    @GetMapping("addTemplate")
    public R addTemplate(@RequestParam("videoId") Long videoId, @RequestParam("type") Integer type) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return intelligentTemplateDatService.addTemplate(videoId, type, accountId, comId);
    }

    /**
     * 获取模版列表
     *
     * @param dto
     * @return
     */
    @PostMapping("getTemplateList")
    public R getTemplateList(@RequestBody GetTemplateListDto dto) {
        Long comId = ShiroUtil.getComId();
        return intelligentTemplateDatService.getTemplateList(comId, dto);
    }

    @PostMapping("getTemplateListByOpenLive")
    public R getTemplateListByOpenLive(@RequestBody GetTemplateListDto dto) {
        Long comId = ShiroUtil.getComId();
        return intelligentTemplateDatService.getTemplateListByOpenLive(comId, dto);
    }
    /**
     * 删除模版
     *
     * @return
     */
    @GetMapping("delete")
    public R delete(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return intelligentTemplateDatService.delete(comId, accountId, id);
    }

    /**
     * 获取模版信息
     *
     * @return
     */
    @GetMapping("getTemplateInfo")
    public R getTemplateInfo(@RequestParam("id") Long id) {
        Long comId = ShiroUtil.getComId();
        return intelligentTemplateDatService.getTemplateInfo(comId, id);
    }
    /**
     * 修改模版名称
     *
     * @return
     */
    @GetMapping("updateTemplateName")
    public R updateTemplateName(@RequestParam("id") Long id, @RequestParam("name") String name) {
        Long accountId = ShiroUtil.getAccountId();
        return intelligentTemplateDatService.updateTemplateName(accountId, id, name);
    }

    /**
     * 智能模板消息查询接口
     *
     * @return
     */
    @PostMapping("/getTemplateMsg")
    public R batchImportInteractMsg(@Validated @RequestBody GetTemplateInfoDto dto) {
        Long comId = ShiroUtil.getComId();
        dto.setType(MongoConstant.LIVE_TEMPLATE_MESSAGE);
        return liveMsgService.getTemplateMsg(comId, dto);
    }


    /**
     * 新增智能模版互动消息
     *
     * @return
     */
    @PostMapping("/addInteractMsg")
    public R addInteractMsg(@RequestBody LiveInteractMsgVo dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        liveMsgService.addInteractMsg(comId, accountId, dto);
        return R.ok();
    }

    /**
     * 更新智能模版互动消息
     *
     * @return
     */
    @PostMapping("updateInteractMsg")
    public R updateInteractMsg(@RequestBody LiveInteractMsgVo dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveMsgService.updateInteractMsg(comId, accountId, dto);
    }

    /**
     * 删除智能模版互动消息
     *
     * @return
     */
    @GetMapping("deleteInteractMsg")
    public R deleteInteractMsg(@RequestParam("id") String id, @RequestParam("templateId") Long templateId) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveMsgService.deleteInteractMsg(comId, accountId, templateId, id);
    }

    /**
     * 多选删除智能模版互动消息
     *
     * @return
     */
    @PostMapping("batchDeleteInteractMsg")
    public R batchDeleteInteractMsg(@RequestBody UpdateTemplateDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveMsgService.batchDeleteInteractMsg(comId, accountId, dto);
    }
    /**
     * 多选移动智能模版互动消息
     *
     * @return
     */
    @PostMapping("batchMoveInteractMsg")
    public R batchMoveInteractMsg(@RequestBody UpdateTemplateDto dto) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveMsgService.batchMoveInteractMsg(comId, accountId, dto);
    }
    /**
     * 新增智能模版互动消息
     *
     * @return
     */
    @GetMapping("/test")
    public R addInteractMsg() {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        LiveInteractMsgVo liveInteractMsgVO = new LiveInteractMsgVo();
        liveInteractMsgVO.setTemplateId(133121331L);
        liveInteractMsgVO.setText("asdhbsadb");
        liveMsgService.addInteractMsg(comId, accountId, liveInteractMsgVO);
        return R.ok();
    }

    /**
     * 查询智能模板商品消息
     *
     * @return
     */
    @GetMapping("/getProductMsg")
    public R getProductMsg(@RequestParam("templateId") Long templateId) {
        Long comId = ShiroUtil.getComId();
        return liveMsgService.getProductMsg(comId, templateId);
    }

    /**
     * 保存智能模板商品消息
     *
     * @return
     */
    @PostMapping("/saveProductMsg")
    public R saveProductMsg(@RequestBody LiveCommodityMsgVo vo) {
        Long comId = ShiroUtil.getComId();
        Long accountId = ShiroUtil.getAccountId();
        return liveMsgService.saveProductMsg(comId,accountId,vo);
    }

    /**
     * 获取礼物列表
     * @return
     */
    @GetMapping("getGiftListByTemplate")
    public R getGiftListByTemplate() {
        Long comId = ShiroUtil.getComId();
        return comGiftConfigService.getGiftListByTemplate(comId);
    }

    /**
     * 获取商品列表
     * @return
     */
    @GetMapping("getCommodityListByTemplate")
    public R getCommodityListByTemplate() {
        Long comId = ShiroUtil.getComId();
        return comCommodityService.getCommodityListByTemplate(comId);
    }

    /**
     * 获取机器人列表
     * @return
     */
    @GetMapping("getBotListByTemplate")
    public R getBotListByTemplate() {
        Long comId = ShiroUtil.getComId();
        return comBotService.getBotListByTemplate(comId);
    }
    /**
     * 智能模版消息下载
     */
    @GetMapping("/downMessagesByTemplate")
    public void downMessages(HttpServletResponse response, @RequestParam("templateId") Long templateId) throws IOException {
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
        String fileName = URLEncoder.encode("用户消息列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

//        QueryLiveUserWatchDto query = new QueryLiveUserWatchDto();
//        query.setLiveId(templateId);
//        query.setQueryDate(queryDate);
        List<LiveInteractMsgVo> liveMessageList = liveMsgService.downMessagesByTemplate(templateId);

        EasyExcel.write(response.getOutputStream(), LiveInteractMsgVo.class).sheet("Sheet1").doWrite(liveMessageList);
    }

    /**
     * 导入数据
     * */
    @PostMapping(value = "/importMessagesData")
    public R importMessagesData(MultipartFile file,@RequestParam("templateId") Long templateId){
        try {
            Long comId = ShiroUtil.getComId();
            //获取文件的输入流
            InputStream inputStream = file.getInputStream();
            //调用read方法
            List<ImportMessagesDataDto> list = EasyExcel.read(inputStream)
                    //注册自定义监听器，字段校验可以在监听器内实现
                    .registerReadListener(new LiveInteractMsgListener())
                    //对应导入的实体类
                    .head(ImportMessagesDataDto.class)
                    //导入数据的sheet页编号，0代表第一个sheet页，如果不填，则会导入所有sheet页的数据
                    .sheet(0)
                    //列表头行数，1代表列表头有1行，第二行开始为数据行
                    .headRowNumber(1)
                    //开始读Excel，返回一个List<T>集合，继续后续入库操作
                    .doReadSync();
            return liveMsgService.importMessagesData(templateId,list,comId);
//            //模拟导入数据库操作
//            for (ImportMessagesDataDto userDO:lst){
//
//                System.out.println(templateId);
//                System.out.println(userDO.toString());
//            }

        }catch (IOException exception){
            throw new  RuntimeException(exception);
        }
    }

}

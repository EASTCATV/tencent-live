package cn.godsdo.dubbo.impl.com;

import cn.godsdo.base.BasePage;
import cn.godsdo.constant.MsgTypeConstants;
import cn.godsdo.constant.RedisConstants;
import cn.godsdo.dto.assistan.GetBotListByAssistanDto;
import cn.godsdo.dto.assistan.SendMsgByBotDto;
import cn.godsdo.dto.com.UpdateBotListDto;
import cn.godsdo.dto.msg.MyCustomData;
import cn.godsdo.dubbo.com.ComBotService;
import cn.godsdo.dubbo.crossService.RedisDubboService;
import cn.godsdo.dubbo.im.ImService;
import cn.godsdo.entity.com.ComBot;
import cn.godsdo.entity.com.DefaultBot;
import cn.godsdo.entity.live.LiveBot;
import cn.godsdo.enums.im.SendUserRole;
import cn.godsdo.enums.im.TIMSendTypeEnum;
import cn.godsdo.mapper.DefaultBotMapper;
import cn.godsdo.mapper.com.ComBotMapper;
import cn.godsdo.mapper.live.LiveBotMapper;
import cn.godsdo.query.im.ImMsgQuery;
import cn.godsdo.util.R;
import cn.godsdo.util.tencent.CosHelperUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.emoji.EmojiUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.y20y.interfaces.IdService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.*;

/**
 * <p>
 * 用户机器人列表 服务实现类
 * </p>
 *
 * @author W~Y~H
 * @since 2018-04-11
 */
@Slf4j
@DubboService
public class ComBotServiceImpl extends ServiceImpl<ComBotMapper, ComBot> implements ComBotService {

    @Resource
    DefaultBotMapper defaultBotMapper;
    @Resource
    ComBotMapper comBotMapper;
    @Resource
    LiveBotMapper liveBotMapper;

    @DubboReference
    IdService idService;
    @DubboReference(check = false, retries = 0)
    private ImService imService;
    @DubboReference(check = false)
    private RedisDubboService redisService;
    @DubboReference(check = false)
    private ComBotService comBotService;

    @Resource
    @Qualifier("CosBean")
    private CosHelperUtil cosHelperUtil;

    @Override
    public R getAllBot(BasePage dto, Long accountId, Long comId) {
        // 创建一个分页对象
        Page<ComBot> datPage = new Page<>(dto.getPage(), dto.getPageSize());
        // 查询数据库获取 ComBot 对象的分页信息
        Page<ComBot> vo = this.baseMapper.selectPage(datPage, Wrappers.<ComBot>lambdaQuery()
                .eq(ComBot::getComId, comId)
                .orderByAsc(ComBot::getSequence)
        );
        // 获取查询结果的记录列表和总数
        List<ComBot> records = vo.getRecords();
        Long total = vo.getTotal();
        if (ObjectUtils.isEmpty(records)) {
            // 若记录为空，则进行第一次插入，插入 100 条记录
            records = addComBot(comId, accountId);
            total = 100L;
        }
        // 返回结果
        return R.ok(records, total);
    }


    // 向 comBot 列表中添加机器人
    @Override
    public List<ComBot> addComBot(Long comId, Long accountId) {
        // 初始化结果集合
        List<ComBot> collect = new ArrayList<>();
        int i = 1;

        // 初始化列表和映射
        List<ComBot> list = new ArrayList<>();
        Map<Long, String> map = new HashMap();
        // 从数据库获取符合条件的默认机器人列表
        List<DefaultBot> ageBot = defaultBotMapper.getBot(comId, 100);
        for (DefaultBot bot : ageBot) {
            Long id = idService.nextId();
            // 分段上传腾讯云服务器
            if (collect.size() < 11) {
                String image = cosHelperUtil.uploadCover(bot.getHeadUrl(), comId);
                // 添加机器人信息到 collect 列表中
                collect.add(new ComBot(id, comId, bot.getNickname(), image, i, accountId));
            } else {
                map.put(id, bot.getHeadUrl());
                // 添加机器人信息到 list 列表中
                list.add(new ComBot(id, comId, bot.getNickname(), bot.getHeadUrl(), i, accountId));
            }
            i++;
        }

        // 批量插入10条数据直接返回，减少反应时间
        log.info(collect.toString());
        this.baseMapper.insertBatch(collect);
        // 异步插入剩余机器人信息
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    // 初始化新列表
                    List<ComBot> newList = new ArrayList<>();
                    // 上传剩余机器人的图片并添加到新列表中
                    for (ComBot bot : list) {
                        String image = cosHelperUtil.uploadCover(bot.getHeadUrl(), comId);
                        bot.setHeadUrl(image);
                        newList.add(bot);
                    }
                    // 批量插入新列表中的机器人信息
                    comBotMapper.insertBatch(newList);
                }
            }
        });
        return collect;
    }

    @Override
    public void addComBotByRegister(Long comId, Long accountId) {
        int i = 1;

        // 初始化列表和映射
        List<ComBot> list = new ArrayList<>();
        // 从数据库获取符合条件的默认机器人列表
        List<DefaultBot> ageBot = defaultBotMapper.getBot(comId, 100);
        for (DefaultBot bot : ageBot) {
            Long id = idService.nextId();
            // 分段上传腾讯云服务器
            String image = cosHelperUtil.uploadCover(bot.getHeadUrl(), comId);
            // 添加机器人信息到 list 列表中
            list.add(new ComBot(id, comId, bot.getNickname(), image, i, accountId));
            i++;
        }

        this.baseMapper.insertBatch(list);
    }


    @Override
    public R getDefaultBot(int limit, Long comId) {
        // 获取默认机器人信息并返回结果
        return R.ok(defaultBotMapper.getBot(comId, limit));
    }


    @Override
    public R saveBot(ComBot vo, Long accountId, Long comId) {
        String image = vo.getHeadUrl();

        // 如果机器人ID为空，则新增机器人信息
        if (ObjectUtils.isEmpty(vo.getId())) {
            ComBot cb = new ComBot(idService.nextId(), comId, vo.getNickname(), image, getSequence(comId), accountId);
            this.baseMapper.insert(cb);
        } else {
            // 如果机器人ID不为空，则更新机器人信息
            ComBot comBot = this.baseMapper.selectOne(Wrappers.<ComBot>lambdaQuery()
                    .eq(ComBot::getComId, comId)
                    .eq(ComBot::getId, vo.getId())
            );
            // 如果未找到要更新的机器人，则返回失败结果
            if (ObjectUtils.isEmpty(comBot)) {
                return R.failed("机器人不存在");
            }
            comBot.setHeadUrl(image);
            comBot.setNickname(vo.getNickname());
            this.baseMapper.updateById(comBot);
        }
        // 返回成功结果
        return R.ok();
    }


    @Override
    public R updateBotList(UpdateBotListDto dto, Long accountId, Long comId) {
        // 判断是否要添加机器人ID
        boolean idAdd = "1".equals(dto.getType());
        if (idAdd) {
            // 查询公司ID对应的机器人数量
            Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<ComBot>()
                    .eq(ComBot::getComId, comId));
            // 如果机器人数量超过1000，则返回失败的响应
            if (count > 1000) {
                return R.failed("最多只能添加1000个机器人");
            }
        }
        // 获取要操作的机器人ID列表
        List<Long> ids = dto.getIds();
        // 获取机器人信息
        List<DefaultBot> bot = defaultBotMapper.getBotByIds(ids);
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    // 初始化机器人列表
                    List<ComBot> list = new ArrayList<>();
                    // 获取公司ID对应的机器人最大序号
                    int i = comBotMapper.getMaxSequence(comId);
                    if (idAdd) {
                        // 添加机器人
                        for (DefaultBot defaultBot : bot) {
                            // 生成机器人ID
                            Long id = idService.nextId();
                            String image = cosHelperUtil.uploadCover(defaultBot.getHeadUrl(), comId);
                            // 将机器人信息添加到列表中
                            list.add(new ComBot(id, comId, defaultBot.getNickname(), image, i, accountId));
                            i++;
                        }
                        // 批量插入机器人列表
                        comBotMapper.insertBatch(list);
                    } else {
                        // 查询指定范围内的机器人信息列表
                        List<ComBot> cbList = comBotMapper.selectList(Wrappers.<ComBot>lambdaQuery()
                                .eq(ComBot::getComId, comId)
                                .between(ComBot::getSequence, dto.getStart(), dto.getEnd())
                        );
                        for (int y = 0; y < cbList.size(); y++) {
                            ComBot cb = cbList.get(y);
                            DefaultBot db = bot.get(y);

                            // 删除oss图片
                            if (org.apache.commons.lang3.ObjectUtils.isNotEmpty(cb.getHeadUrl())) {
                                cosHelperUtil.deleteCosFile(cb.getHeadUrl());
                            }

                            String image = cosHelperUtil.uploadCover(db.getHeadUrl(), comId);
                            // 更新机器人昵称和头像URL
                            cb.setNickname(db.getNickname());
                            cb.setHeadUrl(image);
//                            comBotMapper.updateById(cb);
                            cbList.set(y, cb);
                        }
//                        comBotMapper.u
                        // 批量更新机器人信息
                        comBotService.updateBatchById(cbList);
//                        comBotMapper.updateBatch(cbList);
//                        comBotMapper.
                    }
                }
            }
        });

        // 返回成功的响应
        return R.ok();
    }

    @Override
    public R getBotListByAssistan(GetBotListByAssistanDto dto, Long accountId, Long comId) {
        Long count = this.baseMapper.selectCount(new LambdaQueryWrapper<ComBot>().eq(ComBot::getComId, comId));
        List<ComBot> records = new ArrayList<>();
        Long total = 0L;
        if (count == 0) {
            records = addComBot(comId, accountId);
            total = 50L;
        } else {
            IPage<ComBot> iPage = new Page<>(dto.getPage(), dto.getPageSize());
            IPage<ComBot> comBotIPage = this.baseMapper.getBotListByAssistan(iPage, comId, dto.getLiveId(), dto.getName());
            total = comBotIPage.getTotal();
            records = comBotIPage.getRecords();
        }
        return R.ok(records, total);
    }


    @Override
    public R aloneSend(SendMsgByBotDto dto, Long accountId, Long comId) {
        Long liveId = dto.getLiveId();
        // 获取机器人id
        Long botId = dto.getId();
        String msg = dto.getMsg();
        LiveBot liveBot = liveBotMapper.selectOne(new LambdaQueryWrapper<LiveBot>().eq(LiveBot::getLiveId, liveId)
                .eq(LiveBot::getBotId, botId).eq(LiveBot::getCreateBy, accountId));
        if (ObjectUtils.isEmpty(liveBot)) {
            return R.failed("发送失败");
        }
        // 判断消息是否保留
        Integer isRetain = dto.getIsRetain();
        if (isRetain == 1 && !msg.equals(liveBot.getMsg())) {
            liveBot.setMsg(msg);
        }else{
            liveBot.setMsg(null);
        }
        liveBot.setUpdateBy(accountId);
        liveBotMapper.updateById(liveBot);
        // 获取IM群组ID
        String imGroupId = dto.getImGroupId();

        // 创建用户信息对象
        MyCustomData.UserInfo userInfo = new MyCustomData.UserInfo();
        // 设置发送用户头像
        userInfo.setSendUserAvatar(EmojiUtil.toHtml(dto.getHeadUrl()));
        userInfo.setSendUserId(botId);
        // 设置发送用户昵称
        userInfo.setSendNickName(EmojiUtil.toHtml(dto.getNickname()));
        // 设置操作用户昵称为空
        userInfo.setOperateNickName("");
        // 设置发送用户角色为观众
        userInfo.setSendUserRole(SendUserRole.ROBOT.getCode());
        // 设置公司ID
        userInfo.setComId(comId);
        senMsgByBot(msg, userInfo, imGroupId);
        return R.ok();
    }

    /**
     * 发送直播间消息
     *
     * @param userInfo
     * @param imGroupId
     */
    private void senMsgByBot(String msg, MyCustomData.UserInfo userInfo, String imGroupId) {
        // 创建群组消息查询对象
        ImMsgQuery msgQuery = new ImMsgQuery();
        // 创建自定义数据对象
        MyCustomData text = new MyCustomData();
        // 创建消息信息对象
        MyCustomData.MessageInfo messageInfo = new MyCustomData.MessageInfo();
        // 设置通知类型为字段控制类型
        messageInfo.setNoticeType(TIMSendTypeEnum.FIELD_CONTROL.getValue());

        // 设置用户信息
        text.setUserInfo(userInfo);
        // 设置消息信息
        text.setMessageInfo(messageInfo);
        // 将自定义数据对象转换为JSON字符串
        String json = JSON.toJSONString(text);
        // 设置云自定义数据
        msgQuery.setCloudCustomData(json);
        // 设置消息内容
        msgQuery.setMsgContent(msg);
        // 设置消息类型为文本消息
        msgQuery.setMsgType(MsgTypeConstants.TIM_TEXT_ELEM);
        // 设置群组ID
        msgQuery.setGroupId(imGroupId);
        // 设置发送者ID为管理员
        msgQuery.setFromUserId("administrator");
        // 调用消息服务发送群组消息
        imService.sendGroupMsg(msgQuery);
    }


    @Override
    public R listSend(SendMsgByBotDto dto, Long accountId, Long comId) {
        Long liveId = dto.getLiveId();
        Integer status = dto.getStatus();
        String redisKey = RedisConstants.BOT_SWITCH_MODE_LIST + liveId;
        // 开启任务
        if (status.equals(1)) {
            Integer count = dto.getCount();
            Object value = redisService.get(redisKey);
            if (ObjectUtils.isNotEmpty(value)) {
                return R.failed("当前任务已启动");
            }
            if (count > 200) {
                return R.failed("发送的条数不能大于200条");
            }
            List<ComBot> comBots = getComBots(comId, count);
            // 需要确认一下是否再次拉下数据
            // redis失效时间 一秒发送两条 防止条数为单数，除后加1
            int time = (int) Math.ceil((double) count / (double) 2) + 2;
            // 存入redis，防止重复提交   存入时间，方便获取时间倒计时
            DateTime dateTime = DateUtil.offsetSecond(new Date(), time);
            String format = DateUtil.format(dateTime, "yyyy-MM-dd HH:mm:ss");
            redisService.set(redisKey, format);
            // 每秒2条。最多200条
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // 获取IM群组ID
                    String imGroupId = dto.getImGroupId();
                    // 创建用户信息对象
                    MyCustomData.UserInfo userInfo = new MyCustomData.UserInfo();
                    // 设置公司ID
                    userInfo.setComId(comId);
//                ThreadLocalRandom random = ThreadLocalRandom.current();
//                random.nextInt(10);
                    int i = 1;
                    log.info("发送开始了");
                    for (ComBot comBot : comBots) {
                        if (i <= count) {
                            try {
                                ThreadUtil.execAsync(() -> {
                                    // 设置发送用户头像
                                    userInfo.setSendUserAvatar(EmojiUtil.toAlias(comBot.getHeadUrl()));
                                    // 设置发送用户昵称
                                    userInfo.setSendNickName(EmojiUtil.toHtml(comBot.getNickname()));
                                    userInfo.setSendUserId(comBot.getId());
                                    // 设置操作用户昵称为空
                                    userInfo.setOperateNickName(EmojiUtil.toHtml(comBot.getNickname()));
                                    // 设置发送用户角色为观众
                                    userInfo.setSendUserRole(SendUserRole.ROBOT.getCode());
                                    // 创建消息信息对象
                                    senMsgByBot(dto.getMsg(), userInfo, imGroupId);
                                });
                                if (i % 4 == 0) {
                                    // 中止发送任务
                                    Object value = redisService.get(redisKey);
                                    if (ObjectUtils.isEmpty(value)) {
                                        log.info("发送条数中断：{}", i);
                                        break;
                                    }
                                }
                                Thread.sleep(500);
                                i++;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    // 发送完成，删除redis
                    redisService.del(redisKey);
                    log.info("发送结束了：{}", i);
                }
            }).start();
        } else {
            redisService.del(redisKey);
            log.info("取消场控信息：：操作人：{}", accountId);
        }
        return R.ok();
    }

    public List<ComBot> getComBots(Long comId, Integer count) {
        List<ComBot> comBots = this.baseMapper.selectList(Wrappers.<ComBot>lambdaQuery()
                .eq(ComBot::getComId, comId)
                .last("ORDER BY RAND() LIMIT 0," + count));
        if (ObjectUtils.isEmpty(comBots)) {
            return comBots;
        }
        int size = comBots.size();
        if (size < count) {
            int i = count - size;
            List<ComBot> comBots1 = getComBots(comId, i);
            if (ObjectUtils.isNotEmpty(comBots1)) {
                comBots.addAll(comBots1);
            }
        }
        return comBots;
    }

    @Override
    public R getListSendCountdown(Long liveId) {
        String redisKey = RedisConstants.BOT_SWITCH_MODE_LIST + liveId;
        Object value = redisService.get(redisKey);
        if (ObjectUtils.isNotEmpty(value)) {
            String format = value.toString();
            Date date = DateUtil.parse(format);
            Long between = DateUtil.between(new Date(), date, DateUnit.SECOND);
            return R.ok(between);
        } else {
            return R.ok();
        }
    }

    @Override
    public R getBotListByTemplate(Long comId) {
        List<ComBot> vo = this.baseMapper.getRandomList(comId, 50);
        return R.ok(vo);
    }


    private Integer getSequence(Long comId) {
        // 获取公司ID的最大序列号
        return this.baseMapper.getMaxSequence(comId);
    }


}

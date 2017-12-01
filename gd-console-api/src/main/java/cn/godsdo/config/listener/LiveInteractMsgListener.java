package cn.godsdo.config.listener;

import cn.godsdo.dto.intelligent.ImportMessagesDataDto;
import cn.godsdo.vo.intelligent.LiveInteractMsgVo;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义监听器，对下载的excel中的数据进行校验
 *
 * @author W~Y~H
 * @Date : 2018/05/18
 */
public class LiveInteractMsgListener extends AnalysisEventListener {

    /**
     * 每解析一行，回调该方法
     *
     * @param data
     * @param context
     */
    @Override
    public void invoke(Object data, AnalysisContext context) {
        //校验名称
        String name = ((ImportMessagesDataDto) data).getUserName();
        if (StrUtil.isBlank(name)) {
            throw new RuntimeException(String.format("第%s行名称为空，请核实", context.readRowHolder().getRowIndex() + 1));
        }
//        if (names.contains(name)) {
//            throw new RuntimeException(String.format("第%s行名称已重复，请核实", context.readRowHolder().getRowIndex() + 1));
//        } else {
//            names.add(name);
//        }
    }

    /**
     * 出现异常回调
     *
     * @param exception
     * @param context
     * @throws Exception
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        if (exception instanceof ExcelDataConvertException) {
            /**从0开始计算*/
            Integer columnIndex = ((ExcelDataConvertException) exception).getColumnIndex() + 1;
            Integer rowIndex = ((ExcelDataConvertException) exception).getRowIndex() + 1;
            String message = "第" + rowIndex + "行，第" + columnIndex + "列" + "数据格式有误，请核实";
            throw new RuntimeException(message);
        } else if (exception instanceof RuntimeException) {
            throw exception;
        } else {
            super.onException(exception, context);
        }
    }

    /**
     * 解析完,全部回调
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        //解析完,全部回调逻辑实现
//        names.clear();
    }
}

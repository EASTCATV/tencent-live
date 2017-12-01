package cn.godsdo.filter;

import com.y20y.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;

/**
 * @Author : yang
 * @Date : 2018/2/9
 * @ApiNote :
 */
@Slf4j
//@Activate(group = { Constants.PROVIDER, Constants.CONSUMER })
@Activate
@Order(1)
public class DubboServiceFilter implements Filter {
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * rpc日志最长字符串
     */
    private final static Integer MAX_LOG_LENGTH=5000;
    /**
     * rpc日志超过长度截取长度
     */
    private final static Integer REMAINING_LOG_LENGTH=1000;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        // 获取rpcContext
        //RpcContext context = RpcContext.getContext();
         //获取rpcTraceId
        //String rpcTraceId = context.getAttachment("Token");
        //用户信息
        String rpcTraceId = MDC.get("traceId");
        if (StringUtils.isBlank(rpcTraceId)) {
            log.info("用户信息为空");
        }
        String methodName = invocation.getMethodName();
        Object[] arguments = invocation.getArguments();
        String className = invoker.getInterface().getName();
        String callMethod = className + "." + methodName;
        //String argsJson = JSON.toJSONString(arguments);
        String argsJson =JsonUtils.toJsonString(arguments);
        //logger.info("rpc接口callMethod:{}>>入参:{}", callMethod, argsJson);
        long start = System.currentTimeMillis();
        AsyncRpcResult result = (AsyncRpcResult)invoker.invoke(invocation);
        if(result.hasException()){
            logger.info("rpc接口callMethod:{},接口耗时:{},异常:{},", callMethod,  System.currentTimeMillis() - start,result.getException().getMessage());
        } else {
            Object resultString =JsonUtils.toJsonString(result.getAppResponse().getValue());
            //Object resultString = JSON.toJSON(result.getAppResponse().getValue());
            if(resultString!=null&&resultString.toString().length()>MAX_LOG_LENGTH){
                resultString=resultString.toString().substring(0,REMAINING_LOG_LENGTH)+"...";
            }
            //logger.info("rpc接口callMethod:{},出参:{},接口耗时:{}", callMethod,resultString , System.currentTimeMillis() - start);
        }
        return result;
    }



}

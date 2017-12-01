package cn.godsdo.filter;

import cn.godsdo.dubbo.com.BlackService;
import cn.godsdo.log.OperationLog;
import cn.godsdo.util.http.RequestWrapper;
import cn.godsdo.util.http.ResponseWrapper;
import cn.godsdo.util.ip.IpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * @Author : yang
 * @Date : 2018/3/27
 * @ApiNote :
 */
@Slf4j
@Order(1)
@Configuration
public class BlackFilter extends OncePerRequestFilter {
    public static final String OFFICEDOCUMENT_SPREADSHEETML_SHEET =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String CONTENT_DISPOSITION = "Content-disposition";
    public static final String ATTACHMENT_FILENAME_UTF_8 = "attachment;filename*=utf-8''";
    @DubboReference
    private BlackService blackService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Long time = System.currentTimeMillis();
        String path = request.getQueryString();
        String servletPath = request.getServletPath();
        String url = request.getRequestURI();
        RequestWrapper requestWrapper = null;
        //获取路径
        String requestURI = request.getRequestURI();
        //用户信息
        OperationLog operationLog = new OperationLog();
        operationLog.setCreateTime(DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        operationLog.setOperationType( request.getMethod());
        operationLog.setRequestType(request.getMethod());
        String token = request.getHeader("Token");
        if(token!=null){
            operationLog.setToken(token);
        }
        String netIpAddr = IpUtils.getNetIpAddr(request);
        log.info("请求路径：{}，请求参数：{}，请求方式：{}，请求时间：{}，请求ip：{}", requestURI, path, request.getMethod(), time, netIpAddr);
        //日志请求url
        //Boolean aBoolean = blackService.IpIsBlack(netIpAddr);
        Boolean aBoolean = true;
        System.out.println("ip is black:"+aBoolean);

        if(aBoolean){
            //获取路径
            if(ComputeUrl.getResponseUrl().contains(requestURI)){
                //完全放行
                StringBuilder sb = new StringBuilder();
                if (request instanceof HttpServletRequest) {
                    requestWrapper = new RequestWrapper(request);
                    BufferedReader bufferedReader = requestWrapper.getReader();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                filterChain.doFilter(requestWrapper, response);
            }else {
                ResponseWrapper responseWrapper=new ResponseWrapper(response);
                filterChain.doFilter(request, responseWrapper);
                String result=new String(responseWrapper.getResponseData());
                ServletOutputStream outputStream = response.getOutputStream();
                outputStream.write(result.getBytes());
                outputStream.flush();
                outputStream.close();
                operationLog.setReturnValue(result);
            }

        }


    }
}

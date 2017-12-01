package cn.godsdo.filter;

import cn.godsdo.log.OperationLog;
import cn.godsdo.util.http.RequestWrapper;
import cn.godsdo.util.http.ResponseWrapper;
import cn.godsdo.util.ip.IpUtils;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Map;
/**
 * @Author : yang
 * @Date : 2018/2/13
 * @ApiNote :
 */

/**
 * 注册过滤器
 * */
@Slf4j
@Order(0)
@Configuration
//@WebFilter(filterName = "RequestResponseLogFilter", urlPatterns = "/*")
public class RequestFilter extends OncePerRequestFilter {

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
        //日志请求url
        operationLog.setRequestUrl(requestURI);

        if(ComputeUrl.getRequestUrl().contains(requestURI)){
            ResponseWrapper responseWrapper=new ResponseWrapper(response);
            filterChain.doFilter(request, responseWrapper);
            String result=new String(responseWrapper.getResponseData());
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(result.getBytes());
            outputStream.flush();
            outputStream.close();
            operationLog.setReturnValue(result);
        }else if(ComputeUrl.getResponseUrl().contains(requestURI)){
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
        }else if (null == request) {
            filterChain.doFilter(request, response);
        } else {
            StringBuilder sb = new StringBuilder();
            if (request instanceof HttpServletRequest) {
                requestWrapper = new RequestWrapper(request);
                BufferedReader bufferedReader = requestWrapper.getReader();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
            }
            log.info("======request param======: {}",sb.toString());
            ResponseWrapper responseWrapper=new ResponseWrapper( response);
            filterChain.doFilter(requestWrapper, responseWrapper);
            String result=new String(responseWrapper.getResponseData());
            ServletOutputStream outputStream = response.getOutputStream();
            outputStream.write(result.getBytes());
            outputStream.flush();
            outputStream.close();
            operationLog.setReturnValue(result);
        }
      if(requestWrapper!=null){
        // 读取json数据
        String openApiRequestData = getJSON(requestWrapper);
        if (openApiRequestData != null) {
            operationLog.setRequestJson(JSONUtil.toJsonStr(openApiRequestData));
        }
        // 请求参数
        Map<String, String[]> requestParams = requestWrapper.getParameterMap();
        operationLog.setRequestParam(JSONUtil.toJsonStr(requestParams));
      }
        // 执行时间
        time = System.currentTimeMillis() - time;
        operationLog.setCostTime(time.intValue());
        String user = MDC.get("traceId");
        operationLog.setUser(user);
        operationLog.setIp( IpUtils.getNetIpAddr(request));
        System.out.println("requestURI:"+requestURI);
        if(!"/".equals(requestURI)){
            log.info(operationLog.toString());
        }
    }

    public String getJSON(ServletRequest request) {
        ServletInputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader streamReader = null;
        StringBuilder responseStrBuilder = new StringBuilder();
        try {
            inputStream = request.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            streamReader = new BufferedReader(inputStreamReader);
            String inputStr;
            while ((inputStr = streamReader.readLine()) != null) {
                responseStrBuilder.append(inputStr);
            }
        } catch (IOException ioException) {
            //ioException.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
            try {
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
            try {
                if (streamReader != null) {
                    streamReader.close();
                }
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }
        return responseStrBuilder.toString();
    }


}

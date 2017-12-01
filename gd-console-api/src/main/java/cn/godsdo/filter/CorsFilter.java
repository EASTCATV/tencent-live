package cn.godsdo.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Author : yang
 * @Date : 2018/10/17
 * @ApiNote :在 JDK 17 中，您可以使用 javax.servlet 包来开发基于 Java 的 Web 应用程序。这个包通常用于在 Servlet 容器中开发和部署 Web 应用程序。如果您想在 JDK 17 中使用 javax.servlet 包，您需要确保在项目的构建路径中包含 servlet 相关的 JAR 文件。
 *
 * 通常情况下，您需要一个 Servlet 容器（比如 Tomcat、Jetty 等）来运行基于 Servlet 的 Web 应用程序。在项目中引入 javax.servlet 包后，您可以编写 Servlet 类、Filter 类等来处理 HTTP 请求和响应。
 *
 * 需要注意的是，从 Java EE 8 开始，Servlet API 已经包含在 Jakarta EE 中，被迁移到了 jakarta.servlet 包。因此，如果您使用的是较新的 Jakarta EE 平台，建议使用 jakarta.servlet 包而不是 javax.servlet 包。
 */
//@Component
//public class CorsFilter implements Filter {
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//        HttpServletResponse httpResponse = (HttpServletResponse) response;
//        HttpServletRequest httpRequest = (HttpServletRequest) request;
//        //HttpServletResponse httpResponse = (HttpServletResponse) response;
//        // 获取请求路径
//        String requestURI = httpRequest.getRequestURI();
//        System.out.println("=====requestURI: " + requestURI);
//        // 如果请求路径以 "/tt" 开头，则不进行跨域处理
//        if (requestURI.startsWith("/tt")) {
//            chain.doFilter(request, response);
//            return;
//        }
//        httpResponse.setHeader("Access-Control-Allow-Origin", "*");
//        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
//        httpResponse.setHeader("Access-Control-Allow-Headers", "*");
//        httpResponse.setHeader("Access-Control-Max-Age", "3600");
//        chain.doFilter(request, response);
//    }
//}
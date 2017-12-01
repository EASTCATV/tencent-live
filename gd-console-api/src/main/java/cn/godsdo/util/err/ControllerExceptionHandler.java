//package cn.godsdo.util.err;
//
//import cn.godsdo.util.R;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.ResponseBody;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import javax.servlet.http.HttpServletRequest;
//
//@ControllerAdvice
//@ResponseBody
////@RestControllerAdvice
//@Slf4j
//public class ControllerExceptionHandler {
//
//    //@ExceptionHandler(ServicerunException.class)
//    @ExceptionHandler(Exception.class)
//    //@ExceptionHandler(Throwable.class)
//    @ResponseBody
//    public R handleServiceException(Exception e){
//        e.printStackTrace();
//        System.out.println(e);
//        System.out.println("=================11321313213132131==============");
//        log.info("发生异常：{}", e.getMessage());
//
//
//        return R.failed("请求频繁,请稍后再试");
//
//    }
//    //@ExceptionHandler(Array(classOf[Throwable]))
//    //@ResponseBody
//    //def handleException(request: HttpServletRequest, response: HttpServletResponse, e: Throwable): String = {
//    //    // 处理异常逻辑
//    //    println(s"发生异常：${e.getMessage}")
//    //    // 返回错误码和提示信息
//    //    response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
//    //    "服务器内部错误，请稍后再试"
//    //}
//
//    //@ExceptionHandler(Exception.class)
//    //public R handleException(HttpServletRequest request, Exception e)  {
//    //    e.printStackTrace();
//    //    System.out.println(e);
//    //    System.out.println("=================11321313213132131==============");
//    //    log.info("发生异常：{}", e.getMessage());
//    //    return R.failed("请求频繁,请稍后再试");
//    //    //throw new Exception("");
//    //}
//
//}

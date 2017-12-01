//package cn.godsdo.controller.demo;
//
//import com.alibaba.excel.annotation.ExcelIgnore;
//import com.alibaba.excel.annotation.ExcelProperty;
//import com.fasterxml.jackson.annotation.JsonFormat;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.io.Serializable;
//import java.time.LocalDateTime;
//
///**
// * 直播间商品浏览数据
// *
// * @author gechenpeng
// * @date 2024-04-02
// */
//@Data
//@Builder
//@AllArgsConstructor
//@NoArgsConstructor
//public class RoomProdViewVO implements Serializable {
//
//    @ExcelIgnore
//    private Long userId;
//
//    @ExcelProperty(value = "用户", order = 1)
//    private String userName;
//
//    @ExcelProperty(value = "商品名称", order = 5)
//    private String productName;
//
//    @ExcelProperty(value = "点击次数", order = 2)
//    private Long clickNum;
//
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    @ExcelProperty(value = "最后点击时间", order = 4)
//    private LocalDateTime lastClickTime;
//
//
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
//    @ExcelIgnore
//    private LocalDateTime payTime;
//}

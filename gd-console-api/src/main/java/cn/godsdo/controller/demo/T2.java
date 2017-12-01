//package cn.godsdo.controller.demo;
//
//import cn.hutool.poi.excel.BigExcelWriter;
//import cn.hutool.poi.excel.ExcelUtil;
//import com.alibaba.excel.EasyExcel;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
////import org.apache.commons.io.FileUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.poi.hssf.usermodel.*;
//import org.springframework.util.FileCopyUtils;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import java.io.File;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.io.UnsupportedEncodingException;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Paths;
//import java.time.LocalDateTime;
//import java.util.*;
//import org.apache.commons.io.FileUtils;
///**
// * @Author : yang
// * @Date : 2024/4/19
// * @ApiNote :
// */
//@RestController
//@RequestMapping("/tt")
//public class T2 {
//
//    public static final String OFFICEDOCUMENT_SPREADSHEETML_SHEET =
//            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
//
//    public static final String CONTENT_DISPOSITION = "Content-disposition";
//
//    public static final String ATTACHMENT_FILENAME_UTF_8 = "attachment;filename*=utf-8''";
//    public static final String ATTACHMENT_FILENAME_UTF_8_FORMAT = "attachment; filename=\"%s%s\"";
//    public static final String EXCEL_SURFIX = ".xlsx";
//
//    public static final String SHEET_NAME = "Sheet1";
//    @RequestMapping("/tt")
//    public String html() {
//        return "1212.html";
//    }
//    @RequestMapping(value = "/exceldownload2")
//    public void fypclXls2(HttpServletRequest request, HttpServletResponse response) throws IOException {
//
//        List<RoomProdViewVO> roomProdViewVOS = new ArrayList<RoomProdViewVO>();
//        RoomProdViewVO build1 = RoomProdViewVO.builder().clickNum(1L).productName("1").userName("1").userId(1L).build();
//        RoomProdViewVO build2 = RoomProdViewVO.builder().clickNum(2L).productName("2").userName("2").userId(2L).build();
//        RoomProdViewVO build3 = RoomProdViewVO.builder().clickNum(3L).productName("3").userName("3").userId(3L).build();
//        roomProdViewVOS.add(build1);
//        roomProdViewVOS.add(build2);
//        roomProdViewVOS.add(build3);
//
//        response.setContentType(OFFICEDOCUMENT_SPREADSHEETML_SHEET);
//        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
//        String fileName = URLEncoder.encode("商品点击数据", StandardCharsets.UTF_8)
//                .replaceAll("\\+", "%20");
//        //response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME_UTF_8_FORMAT.replace("%s", encodeFileName(fileName)));
//        response.setHeader(CONTENT_DISPOSITION,
//                ATTACHMENT_FILENAME_UTF_8 + fileName + EXCEL_SURFIX);
//        EasyExcel.write(response.getOutputStream(), RoomProdViewVO.class)
//                .sheet(SHEET_NAME).doWrite(roomProdViewVOS);
//    }
//    private static String encodeFileName(String fileName) {
//        try {
//            return URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException("UTF-8 encoding is not supported", e);
//        }
//    }
//
//    /**
//     *  获取文件名
//     * @param fileName
//     * @return
//     */
//    private String  getTempFileName(String fileName){
//        // 获得系统属性集
//        Properties props = System.getProperties();
//        String osName = props.getProperty("os.name");
//        String tempDirectoryPath = Paths.get(System.getProperty("java.io.tmpdir")).toString();
//        String tempFile = tempDirectoryPath ;
//        //String tempDirectoryPath = FileUtils.getTempDirectoryPath();
//        //String tempFile= FileUtils.getTempDirectoryPath() ;//+ IOUtils.DIR_SEPARATOR + fileName;
//
//
//        //String tempDirectoryPath = Paths.get(System.getProperty("java.io.tmpdir")).toString();
//
//
//
//        if (!osName.toLowerCase().contains("windows")) {
//            tempFile = System.getProperty("java.io.tmpdir")+ UUID.randomUUID()+ File.separator ;
//            tempFile = "/Users/yang/Downloads/aaa";
//            long currentTimeMillis = System.currentTimeMillis();
//             //tempFile = tempFile +File.separator+ "liveTeacherEvaluate" + currentTimeMillis + ".xlsx";
////            tempFile = "d:/temp/";
//            File file = new File(tempFile);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//            tempFile += fileName;
//        }else {
//            tempFile = "d:/temp/";
//            File file = new File(tempFile);
//            if (!file.exists()) {
//                file.mkdirs();
//            }
//            tempFile += fileName;
//        }
//        return  tempFile;
//    }
//
//    public static void main(String[] args) {
//
//
//
//        }
//
//
//    @RequestMapping(value = "/exceldownload1")
//    public void fypclXls(HttpServletRequest request, HttpServletResponse response) {
//        //业务查询出的数据
//        //标题
//        String[] title = {"邀请码"};
//        //xls表名
//        String fileName = "邀请码.xls";
//        String sheetName = "邀请码";
//        String[][] content = new String[15][title.length];
//        for (int i = 0; i < 15; i++) {
//            //把数据遍历添加到数组中
//            content[i][0] = "1111";
//        }
//        try {
//            //使用方法得到api对象
//            HSSFWorkbook hssfWorkbook = getHSSFWorkbook(sheetName, title, content, null);
//            //实现页面下载
//            setResponseHeader(request, response, fileName);
//            jakarta.servlet.ServletOutputStream outputStream = response.getOutputStream();
//            //创建页面输出流对象
//            //ServletOutputStream outputStream =
//            //把文件写入输出流的对象中
//            hssfWorkbook.write(outputStream);
//            //outputStream.flush();
//            outputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//    public HSSFWorkbook getHSSFWorkbook(String sheetName, String[] title, String[][] values, HSSFWorkbook workbook) {
//        //sheetName 表名称
//        //title 表格第一行的表头名称
//        //values 存放的内容
//        //workbook 实现api的对象
//        if (workbook == null) {
//            workbook = new HSSFWorkbook();
//        }
//        HSSFSheet sheet = workbook.createSheet(sheetName);
//        HSSFRow row = sheet.createRow(0);
//        HSSFCellStyle cellStyle = workbook.createCellStyle();
//        HSSFCell cell = null;
//        for (int i = 0; i < title.length; i++) {
//            cell = row.createCell(i);
//            cell.setCellValue(title[i]);
//            cell.setCellStyle(cellStyle);
//        }
//        for (int i = 0; i < values.length; i++) {
//            row = sheet.createRow(i + 1);
//            for (int j = 0; j < values[i].length; j++) {
//                row.createCell(j).setCellValue(values[i][j]);
//            }
//        }
//        return workbook;
//    }
//    public void setResponseHeader(HttpServletRequest request, HttpServletResponse response, String fileName) {
//        try {
//            String agent = request.getHeader("USER-AGENT").toLowerCase();
//            if (StringUtils.contains(agent, "Mozilla")) {
//                fileName = new String(fileName.getBytes(), "ISO8859-1");
//            } else {
//                fileName = URLEncoder.encode(fileName, "utf8");
//            }
//            response.setCharacterEncoding("UTF-8");
//            response.setContentType("application/vnd.ms-excel;charset=utf-8");// 设置contentType为excel格式
//            response.setHeader("Content-Disposition", "Attachment;Filename=" + fileName);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//}

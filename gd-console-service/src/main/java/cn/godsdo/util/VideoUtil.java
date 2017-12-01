package cn.godsdo.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频工具类
 *
 * @Author: hdk
 *
 */
@Slf4j
@Component
public class VideoUtil {
    /**
     * 通过Javacv的方式获取视频截图
     *
     * @param filePath 视频文件路径
     * @return Map<String, Object>
     */
    public  Map<String, Object> getScreenshot(String filePath, String targerFilePath ,String targetFileName) {
        try {
            log.info("截取视频截图开始：" + System.currentTimeMillis());
            log.info("filePath：" + filePath);
            log.info("targerFilePath：" + targerFilePath);
            log.info("targetFileName：" + targetFileName);
            Map<String, Object> result = new HashMap<>();
            FFmpegFrameGrabber grabber = FFmpegFrameGrabber.createDefault(filePath);
            log.info("计算第一帧图片存储位置");
            // 第一帧图片存储位置
//             targerFilePath = filePath.substring(0, filePath.lastIndexOf("\\"));
            // 视频文件名
//             String fileName = filePath.substring(filePath.lastIndexOf("\\") + 1);
            // 图片名称
            targetFileName = targetFileName.substring(0, targetFileName.lastIndexOf("."));
            log.info("视频路径是：" + targerFilePath);
            //log.info("视频文件名：" + fileName);
            log.info("图片名称是：" + targetFileName);

            grabber.start();
            //设置视频截取帧（默认取第一帧）
            Frame frame = grabber.grabImage();
            //视频旋转度
            String rotate = grabber.getVideoMetadata("rotate");
            Java2DFrameConverter converter = new Java2DFrameConverter();
            //绘制图片
            BufferedImage bi = converter.getBufferedImage(frame);
            if (rotate != null) {
                // 旋转图片
                bi = rotate(bi, Integer.parseInt(rotate));
            }
            //图片的类型
            String imageMat = "jpg";
            //图片的完整路径
            String imagePath = targerFilePath + File.separator + targetFileName + "." + imageMat;
            //创建文件
            File output = new File(imagePath);
            ImageIO.write(bi, imageMat, output);

            //拼接Map信息
            result.put("videoWide", bi.getWidth());
            result.put("videoHigh", bi.getHeight());
            double duration = (double) grabber.getLengthInTime() / (1000 * 1000);
            result.put("rotate", StringUtils.isBlank(rotate) ? "0" : rotate);
            result.put("format", grabber.getFormat());
            result.put("imgPath", output.getPath());
            result.put("time", Math.round(duration));
            log.info("视频的宽:" + bi.getWidth());
            log.info("视频的高:" + bi.getHeight());
            log.info("视频的旋转度：" + rotate);
            log.info("视频的格式：" + grabber.getFormat());
            log.info("此视频时长（s/秒）：" + Math.round(duration));
            grabber.stop();
            log.info("截取视频截图结束：" + System.currentTimeMillis());
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            log.info("VideoUtil getScreenshot fail: {}", e.toString());
//            LOGGER.error("VideoUtil getScreenshot fail: {}", ExceptionStackTrace.printStackTrace(e));
            return null;
        }
    }

    /**
     * 根据视频旋转度来调整图片
     *
     * @param src   BufferedImage
     * @param angel angel	视频旋转度
     * @return BufferedImage
     */
    public  BufferedImage rotate(BufferedImage src, int angel) {
        int src_width = src.getWidth(null);
        int src_height = src.getHeight(null);
        int type = src.getColorModel().getTransparency();
        Rectangle rect_des = calcRotatedSize(new Rectangle(new Dimension(src_width, src_height)), angel);
        BufferedImage bi = new BufferedImage(rect_des.width, rect_des.height, type);
        Graphics2D g2 = bi.createGraphics();
        g2.translate((rect_des.width - src_width) / 2, (rect_des.height - src_height) / 2);
        g2.rotate(Math.toRadians(angel), src_width / 2, src_height / 2);
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return bi;
    }


    /**
     * 计算图片旋转大小
     *
     * @param src   Rectangle
     * @param angel int
     * @return Rectangle
     */
    public  Rectangle calcRotatedSize(Rectangle src, int angel) {
        if (angel >= 90) {
            if (angel / 90 % 2 == 1) {
                int temp = src.height;
                src.height = src.width;
                src.width = temp;
            }
            angel = angel % 90;
        }
        double r = Math.sqrt(src.height * src.height + src.width * src.width) / 2;
        double len = 2 * Math.sin(Math.toRadians(angel) / 2) * r;
        double angel_alpha = (Math.PI - Math.toRadians(angel)) / 2;
        double angel_dalta_width = Math.atan((double) src.height / src.width);
        double angel_dalta_height = Math.atan((double) src.width / src.height);
        int len_dalta_width = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_width));
        int len_dalta_height = (int) (len * Math.cos(Math.PI - angel_alpha - angel_dalta_height));
        int des_width = src.width + len_dalta_width * 2;
        int des_height = src.height + len_dalta_height * 2;
        return new Rectangle(new Dimension(des_width, des_height));
    }

}
 
 
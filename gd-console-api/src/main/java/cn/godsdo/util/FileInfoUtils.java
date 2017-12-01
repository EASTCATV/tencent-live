package cn.godsdo.util;

import cn.godsdo.dto.mediaLibrary.ChunkInfoDto;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;


/**
 * 文件工具类
 *
 * @author 洋葱骑士
 */
@Slf4j
public class FileInfoUtils {

//    private final static Logger logger = LoggerFactory.getLogger(FileInfoUtils.class);

    public static String generatePath(String uploadFolder, ChunkInfoDto chunk, Long clientId) {
        StringBuilder sb = new StringBuilder();
        sb.append(uploadFolder).append("/").append(clientId).append("/").append(chunk.getIdentifier());
        //判断uploadFolder/identifier 路径是否存在，不存在则创建
        if (!Files.isWritable(Paths.get(sb.toString()))) {
            log.info("path not exist,create path: {}", sb.toString());
            try {
                Files.createDirectories(Paths.get(sb.toString()));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        return sb.append("/")
                .append(chunk.getFilename())
                .append("-")
                .append(chunk.getChunkNumber()).toString();
    }

    /**
     * 文件合并
     *
     * @param folder
     */
    public static String merge(String file, String folder, String filename) {
        //默认合并成功
        String rlt = "200";

        try {
            //先判断文件是否存在
            if (fileExists(file)) {
                //文件已存在
                rlt = "300";
            } else {
                log.info("FileInfoUtils-merge:::开始合并");
                File dirs = new File(folder);
                // 判断文件夹是否存在
                if (!dirs.exists()) {
                    log.info("FileInfoUtils-merge:::合并时文件夹不存在，进行新建文件夹");
                    dirs.mkdirs();
                }
                //不存在的话，进行合并
                Files.createFile(Paths.get(file));

                Files.list(Paths.get(folder))
                        .filter(path -> !path.getFileName().toString().equals(filename))
                        .sorted((o1, o2) -> {
                            String p1 = o1.getFileName().toString();
                            String p2 = o2.getFileName().toString();
                            int i1 = p1.lastIndexOf("-");
                            int i2 = p2.lastIndexOf("-");
                            return Integer.valueOf(p2.substring(i2)).compareTo(Integer.valueOf(p1.substring(i1)));
                        })
                        .forEach(path -> {
                            try {
                                //以追加的形式写入文件
                                Files.write(Paths.get(file), Files.readAllBytes(path), StandardOpenOption.APPEND);
                                //合并后删除该块
                                Files.delete(path);
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);
                            }
                        });
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            //合并失败
            rlt = "500";
        }

        return rlt;
    }

    /**
     * 根据文件的全路径名判断文件是否存在
     *
     * @param file
     * @return
     */
    public static boolean fileExists(String file) {
        boolean fileExists = false;
        Path path = Paths.get(file);
        fileExists = Files.exists(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS});
        return fileExists;
    }
}

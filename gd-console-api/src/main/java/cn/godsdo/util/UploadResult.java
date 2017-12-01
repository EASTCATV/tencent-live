package cn.godsdo.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件上传结果
 *
 * @author
 */
public class UploadResult implements Serializable {
/**
 * 序列化ID
 */
@Serial
private static final long serialVersionUID = 5630054007842738449L;

/**
 * 是否跳过上传（已上传的可以直接跳过，达到秒传的效果）
 */
private boolean skipUpload;

/**
 * 已经上传的文件块编号，可以跳过，断点续传
 */
private List<Integer> uploadedChunks;

/**
 * 返回结果码
 */
private String status;

/**
 * 返回结果信息
 */
private String message;

/**
 * 已上传完整附件的地址
 */
private String location;/**
 * 获取是否跳过上传
 * @return 是否跳过上传
 */
public boolean isSkipUpload() {
    return skipUpload;
}

/**
 * 设置是否跳过上传
 * @param skipUpload 是否跳过上传
 */
public void setSkipUpload(boolean skipUpload) {
    this.skipUpload = skipUpload;
}

/**
 * 获取已上传的文件块编号
 * @return 已上传的文件块编号
 */
public List<Integer> getUploadedChunks() {
    return uploadedChunks;
}

/**
 * 设置已上传的文件块编号
 * @param uploadedChunks 已上传的文件块编号
 */
public void setUploadedChunks(List<Integer> uploadedChunks) {
    this.uploadedChunks = uploadedChunks;
}

/**
 * 获取返回结果码
 * @return 返回结果码
 */
public String getStatus() {
    return status;
}

/**
 * 设置返回结果码
 * @param status 返回结果码
 */
public void setStatus(String status) {
    this.status = status;
}

/**
 * 获取返回结果信息
 * @return 返回结果信息
 */
public String getMessage() {
    return message;
}

/**
 * 设置返回结果信息
 * @param message 返回结果信息
 */
public void setMessage(String message) {
    this.message = message;
}

/**
 * 获取已上传完整附件的地址
 * @return 已上传完整附件的地址
 */
public String getLocation() {
    return location;
}

/**
 * 设置已上传完整附件的地址
 * @param location 已上传完整附件的地址
 */
public void setLocation(String location) {
    this.location = location;
}
}

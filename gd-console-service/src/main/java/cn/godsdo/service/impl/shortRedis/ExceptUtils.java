package cn.godsdo.service.impl.shortRedis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExceptUtils {
	static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static Logger logger = LogManager.getLogger("Except");

	/**
	 * 调用此接口把异常输出到远程日志服务器。本地console也会显示出来
	 * 
	 * @param e
	 */
	public static void printStackTrace(Exception e,String... args) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream s = new PrintStream(out);
		e.printStackTrace(s);
		String log = out.toString();
		System.err.println("Exception [" + sdf.format(new Date()) + " ] -- " + log);
		
		if (log != null && log.length() > 500) {
			log = log.substring(0, 500);
		}
		logger.error("Client  {} Exception -- {}", args, log);
	}

	public static void printStackTraceLocal(Exception e) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream s = new PrintStream(out);
		e.printStackTrace(s);
		String log = out.toString();

		System.err.println("Exception [" + sdf.format(new Date()) + " ] -- " + log);
	}
}

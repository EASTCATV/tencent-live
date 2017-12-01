package cn.godsdo.service.impl.shortRedis;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Component
//配制文件redisServer: 49.234.105.173_6379_master_eDDxWJfwYsDhD
//@Configuration
public class ShortRedisProxy {

	protected Logger logger = LogManager.getLogger("RedisProxy");
	// 切片连接池
	private JedisPool shardedJedisPool;
	private QinPlusTransCoder coder = new QinPlusTransCoder();
	private String redisServers;
	private JedisPoolConfig poolConfig;
	//默认数据库
	private int DEFAULT_INDEX=0;

	public ShortRedisProxy(String redisServers, JedisPoolConfig poolConfig) {
		this.redisServers = redisServers;
		this.poolConfig = poolConfig;
		initialShardedPool();
	}

	/**
	 * 初始化切片池
	 */
	public synchronized void initialShardedPool() {
		// slave链接
		List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
		String[] servers = redisServers.split("_");
//		for(String server:servers){
//			String[] arr = server.split("_");
//			JedisShardInfo info = new JedisShardInfo(arr[0], Integer.parseInt(arr[1]), arr[2]);
//			info.setPassword(arr[3]);
//			info.setSoTimeout(100);
//			shards.add(info);
//		}
		// 构造池
		//shardedJedisPool = new JedisPool(poolConfig,servers[0],Integer.valueOf(servers[1]),10000,servers[3]);
		//shardedJedisPool = new JedisPool(poolConfig,servers[0],Integer.valueOf(servers[1]),10000,servers[3]);
		shardedJedisPool = new JedisPool(poolConfig, servers[0], Integer.valueOf(servers[1]), 10000);
	}
	
	private synchronized Jedis getResource(){
		return shardedJedisPool.getResource();
	}
	
	public void returnResource(String method,Jedis shardedJedis){
		if(shardedJedis!=null) {
			shardedJedis.close();
//			logger.info(method+"-RedisProxy-活跃连接数:"+shardedJedisPool.getNumActive());
//			logger.info(method+"-RedisProxy-空闲连接数:"+shardedJedisPool.getNumIdle());
//			logger.info(method+"-RedisProxy-等待连接数:"+shardedJedisPool.getNumWaiters());
		}
	}

	public void set(String key) throws IOException{
		if(key==null) return;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Jedis shardedJedis = getResource();
		try {
			shardedJedis.set(key.getBytes(), baos.toByteArray());
		} catch (Exception e) {
			ExceptUtils.printStackTrace(e);
		}finally{
			returnResource("RedisProxy",shardedJedis);
		}
	}
	public Object get(String key){
		if(key==null) return null;
		Jedis shardedJedis = getResource();
		try {

			shardedJedis.select(1);
			return shardedJedis.get(key);
		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			returnResource("RedisProxy",shardedJedis);
		}
		return null;
	}
	public void set(String key,String value){
		if(key==null) return;
		Jedis shardedJedis = getResource();
		try {
			shardedJedis.select(1);
			shardedJedis.set(key,value);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			returnResource("RedisProxy",shardedJedis);
		}
	}
	public void del(String key){
		if(key==null) return;
		Jedis shardedJedis = getResource();
		try {
			shardedJedis.select(1);
			shardedJedis.del(key);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			returnResource("RedisProxy",shardedJedis);
		}
	}
	/**
	 * 删除指定索引之外的所有元素-指定数据库
	 * @param index
	 * @param key
	 * @return
	 */
	public  String ltrimByIndex(Integer index,String key,long start,long end){

		if(StringUtils.isBlank(key)){
			return  "";
		}
		Jedis shardedJedis = getResource();
		try {
			if(index==null){
				index=DEFAULT_INDEX;
			}
			shardedJedis.select(index);
			return shardedJedis.ltrim(key,start,end);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			returnResource("RedisProxy",shardedJedis);
		}
		return "";
	}
	
	public void addPackageType(short packageType,Class<?> obj){
		coder.addPackageType(packageType, obj);
	}
	
	class QinPlusTransCoder {

		private Map<String,Short> pkgNameIndex;
		private Map<Short,String> pkgIdIndex;
		
		public QinPlusTransCoder()
		{
			pkgNameIndex=new HashMap<String,Short>();
			pkgIdIndex=new HashMap<Short,String>();
		}
		
		public void addPackageType(short packageType,Class<?> obj)
		{
			Short id=packageType;
			String className=obj.getName();
			pkgIdIndex.put(id,className);
			pkgNameIndex.put(className,id);
		}


	}
	
}

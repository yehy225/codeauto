package com.yehongyu.gencode.dao;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.yehongyu.gencode.util.FileUtil;
import com.yehongyu.gencode.util.PropertiesUtil;

public class SysInit {
	/** 日志记录 */
	private final static Logger logger = Logger.getLogger(SysInit.class);

	/** 数据库驱动类名 */
	private String dbDriverClass;
	/** 数据库类型 */
	private int dbType;
	/** 数据库服务器 */
	private String dbServer;
	/** 数据库名 */
	private String dbName;
	/** 数据库用户名 */
	private String dbUser;
	/** 数据库用户密码 */
	private String dbPwd;
	/** DAO模版路径，如dao/ibatisdao */
	private String tmplPath;
	/** Java_Package包路径，如:com.taobao.trip.agent */
	private String javaPackage;
	/** 指定生成表  */
	private List<String> tableList = new ArrayList<String>();	//数据库配置的表名
	/** 代码输出路径 */
	private String genPath;

	public static final int DB_TYPE_XML = 0;	//Xml
	public static final int DB_TYPE_ORACLE = 1;	//Oracle
	public static final int DB_TYPE_MYSQL = 2;	//Mysql

	/**
	 * 初始化系统参数
	 * 
	 * @param dbname
	 * @return
	 */
	public boolean initSystemParam() {
		boolean blnRet = true;
		
		String daopath = "dao/";
		String path = ClassLoader.getSystemResource(daopath).getPath();
		logger.info("开始初始化数据库环境,路径为【" + path + "】");
		//加载DB属性文件
		List<String> files = FileUtil.getFileListWithExt(path, ".properties");
		String propertiesFilename = null;
		if(files!=null&&files.size()==1){
			propertiesFilename = files.get(0);
			logger.info("找到DB属性配置文件,文件名为【" + path + propertiesFilename + "】");
		}
		if(propertiesFilename==null){
			logger.error("OUT---[false]DB属性配置文件在["+path+"]找不到！");
			return false;
		}
		//解析属性文件
		Properties prop = PropertiesUtil.getPropertiesByResourceBundle(daopath + FileUtil.getFilenameWithoutExt(propertiesFilename));
		if (prop == null) {
			logger.error("OUT---[false]属性配置文件内容解析为空！");
			return false;
		}
		
		//设置DB类型及数据库连接信息
		String type = (String) prop.get("DB_TYPE");
		if("Mysql".equals(type)){
			dbType = SysInit.DB_TYPE_MYSQL;
		}else if("Oracle".equals(type)){
			dbType = SysInit.DB_TYPE_ORACLE;
		}else if("Xml".equals(type)){
			dbType = SysInit.DB_TYPE_XML;
		}else{
			logger.error("OUT---[false]属性配置文件指定的DB_TYPE不存在！type：" + type);
			blnRet = false;
		}
		dbServer = (String) prop.get("DB_SERVER");
		dbName = (String) prop.get("DB_NAME");
		dbUser = (String) prop.get("DB_USER");
		dbPwd = (String) prop.get("DB_PWD");
		//设置生成指定表、使用模版、Java包路径、代码输出路径
		String tablestr = (String) prop.get("DB_TABLES");
		if(tablestr!=null){
			String[] tables = tablestr.split(",");
			for(int i=0;i<tables.length;i++){
				tableList.add(tables[i]);
			}
			logger.info("指定生成表：" + tableList.toString());
		}
		javaPackage = (String) prop.get("JAVA_PACKAGE");
		String tmpl = (String) prop.get("USE_TMPL");
		if(StringUtils.isBlank(tmpl)){
			tmplPath = daopath + "ibatisdao";
			logger.warn("DAO模版未指定，使用默认模版：" + tmplPath);
		}else{
			tmplPath = daopath + tmpl;
			logger.info("使用模版：" + tmplPath);
			//TODO 校验模版路径及内容结构是否正确完整
		}
		String gendir = (String) prop.get("GEN_PATH");
		if(StringUtils.isBlank(gendir)){
			genPath = System.getProperty("user.dir") + "/gendir/";
			logger.warn("代码生成输出路径未指定，使用默认路径：" + genPath);
		}else{
			File f = new File(gendir);
			if(!f.exists()||!f.isDirectory()){
				genPath = System.getProperty("user.dir") + "/gendir/";
				logger.warn("指定的代码生成输出路径不存在，使用默认路径：" + genPath);
			}else{
				genPath = gendir;
				logger.info("使用指定的代码生成输出路径：" + genPath);
			}
		}
		
		//打印数据库DAO代码生成环境配置信息
		Iterator<Entry<Object,Object>> it = prop.entrySet().iterator();
		logger.info("数据库DAO代码生成环境配置信息如下：");
		while (it.hasNext()) {
			Entry<Object, Object> en = it.next();
			logger.info(en.getKey() + "=" + en.getValue());
		}
		logger.info("结束初始化数据库DAO代码生成环境【" + path + propertiesFilename + "】！");
		return blnRet;
	}
	
	public int getDbType() {
		return dbType;
	}

	public String getDbServer() {
		return dbServer;
	}

	public String getDbName() {
		return dbName;
	}

	public String getDbUser() {
		return dbUser;
	}

	public String getDbPwd() {
		return dbPwd;
	}
	
	public String getTmplPath() {
		return tmplPath;
	}

	public String getJavaPackage() {
		return javaPackage;
	}

	public List<String> getTableList() {
		return tableList;
	}

	public String getGenPath() {
		return genPath;
	}
	
	public String getDbDriverClass() {
		return dbDriverClass;
	}

	public void setDbDriverClass(String dbDriverClass) {
		this.dbDriverClass = dbDriverClass;
	}

}

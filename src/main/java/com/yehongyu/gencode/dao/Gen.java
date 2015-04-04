package com.yehongyu.gencode.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.VelocityContext;

import com.yehongyu.gencode.dao.bean.GlobalBean;
import com.yehongyu.gencode.dao.bean.TableBean;
import com.yehongyu.gencode.util.CharsetConvert;
import com.yehongyu.gencode.util.FileUtil;
import com.yehongyu.gencode.util.StringUtil;
import com.yehongyu.gencode.util.VelocityTemplate;

/**
 * 代码生成入口
 * @author yingyang
 * @since 2011-10-14
 */
public class Gen {
	/** 日志记录 */
	private final static Logger logger = Logger.getLogger(Gen.class);
	//Source目录，从ClassPath中获取
	private final static String SOURCE_IN_PATH = ClassLoader.getSystemResource("").getPath();
	
	//生成的Maven结构的代码路径
	private final static String PATH_JAVA = "/src/main/java/";
	private final static String PATH_RESOURCES = "/src/main/resources/";
	private final static String PATH_TEST_JAVA = "/src/test/java/";
//	private final static String PATH_TEST_RESOURCES = "/src/test/resources/";
	
	private GenTable genTable;
	private GlobalBean globalBean;
	private SysInit sysInit;
	
	public Gen(DbConn dbConn){
		genTable = new GenTable(dbConn);
		globalBean = new GlobalBean();
		sysInit = dbConn.getSysInit();
	}

	/**
	 * 生成入口
	 * @param args
	 */
	public static void main(String[] args) {
		//生成DAO代码
		Gen.doGenDB();
	}

	private static void doGenDB() {
		logger.info("开始执行DAO代码生成===========================");
		//初始化系统环境
		SysInit sysInit = new SysInit();
		if(!sysInit.initSystemParam()){
			logger.error("系统初始化失败！");
			return;
		}
		//初始化数据库连接
		DbConn dbConn = new DbConn(sysInit);
		if(!dbConn.isInit()){
			logger.error("数据库连接创建失败！");
			return;
		}
		Gen gen = new Gen(dbConn);
		//设置工程的全局变量
		gen.globalBean.setNowDate(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));//设置系统生成时间
		gen.globalBean.setUserName(System.getenv().get("USERNAME"));//设置系统当前用户
		gen.globalBean.setPackageName(sysInit.getJavaPackage());//设置Java_Package路径
		//生成指定数据库的指定表或所有表数据访问层代码
		String tabName;
		List<String> tableList = gen.genTable.getAllTableName();
		//创建系统目录结构
		FileUtil.copyDirectiory(SOURCE_IN_PATH + sysInit.getTmplPath(), sysInit.getGenPath() + sysInit.getTmplPath());
		FileUtil.delExtFile(sysInit.getGenPath() + sysInit.getTmplPath(), ".vm");	//删除拷贝过去的VM文件
		FileUtil.copyDirectiory(SOURCE_IN_PATH + "dao/dbinfo", sysInit.getGenPath() + "dao/dbinfo");
		FileUtil.delExtFile(sysInit.getGenPath() + "dao/dbinfo", ".vm");	//删除拷贝过去的VM文件
		// 循环生成所有表数据访问代码，返回类对象并设置类对象列表
		logger.info("共有" + tableList.size() + "个表需要生成数据访问层.");
		List<TableBean> tbList = new ArrayList<TableBean>();
		TableBean tb;
		for (int i = 0; i < tableList.size(); i++) {
			tabName = tableList.get(i);
			logger.info("第" + (i+1) + "个表["+tabName+"]数据访问层正在生成中...");
			tb = gen.doGenTable(tabName);
			if(tb!=null)tbList.add(tb);
		}
		logger.info("实际生成" + tbList.size() + "个表的数据访问层！");
		//根据类对象列表，生成全局配置及基类代码文件
		gen.doGenCFG(tbList);
		//根据类对象列表，生成表结构及DDL语句
		gen.doGenDbInfo(tbList);
		dbConn.closeConn();	//关闭数据库链接
		
		logger.info("所映射的字段有：");
		if(!gen.genTable.as.isEmpty()){
			Iterator<Entry<Integer, String>> it = gen.genTable.as.entrySet().iterator();
			while(it.hasNext()){
				Entry<Integer, String> entry = it.next();
				logger.info("sqltype:" + entry.getKey()+" -> "+entry.getValue());
			}
		}
		logger.info("结束执行DAO代码生成===========================");
	}

	/**
	 * 生成指定表的数据访问层
	 * @param tablename
	 * @return 表类名
	 */
	private TableBean doGenTable(String tablename) {
		TableBean tableBean = genTable.getTableBean(tablename);
		if(tableBean==null){
			return null;
		}
		VelocityContext ctx = new VelocityContext();
		ctx.put("tbb", tableBean);	//设置表对象
		ctx.put("gb", globalBean); //设置全局信息
		ctx.put("sysInit", sysInit);	//设置系统信息
		ctx.put("stringUtil", new StringUtil()); //设置StringUtil
		try {
			//生成Java代码
			String javaVmDir = SOURCE_IN_PATH + sysInit.getTmplPath() + PATH_JAVA;
			String javaDir = sysInit.getGenPath() + sysInit.getTmplPath() + PATH_JAVA;
			List<String> javaVmList = FileUtil.getFileListWithExt(javaVmDir,".vm");
			String vmFilename,createFilename,packageDir = "",packageStr;
			for(int i=0;i<javaVmList.size();i++){
				vmFilename = javaVmList.get(i);
				if(vmFilename.startsWith("Base")) continue;	 //基类代码跳过
				createFilename = FileUtil.getFilenameWithoutExt(vmFilename);
				packageStr = FileUtil.findLine(javaVmDir + "/" + vmFilename, "package");
				if(StringUtils.isNotBlank(packageStr)){
					packageStr = packageStr.substring(packageStr.indexOf("$!{gb.packageName}"),packageStr.indexOf(";"));
					packageDir = packageStr.replace("$!{gb.packageName}", globalBean.getPackageName()).replace(".", "/");
				}
				FileUtil.mkDirs(javaDir + packageDir);
				VelocityTemplate.mergeTemplate(sysInit.getTmplPath() + PATH_JAVA + "/" + vmFilename, javaDir + packageDir + "/" + tableBean.getClassName() + createFilename, ctx);
			}
			//生成Java测试代码
			String javaTestVmDir = SOURCE_IN_PATH + sysInit.getTmplPath() + PATH_TEST_JAVA;
			String javaTestDir = sysInit.getGenPath() + sysInit.getTmplPath() + PATH_TEST_JAVA;
			List<String> javaTestVmList = FileUtil.getFileListWithExt(javaTestVmDir,".vm");
			for(int i=0;i<javaTestVmList.size();i++){
				vmFilename = javaTestVmList.get(i);
				if(vmFilename.startsWith("Base")) continue;	 //基类代码跳过
				createFilename = FileUtil.getFilenameWithoutExt(vmFilename);
				packageStr = FileUtil.findLine(javaTestVmDir + "/" + vmFilename, "package");
				if(StringUtils.isNotBlank(packageStr)){
					packageStr = packageStr.substring(packageStr.indexOf("$!{gb.packageName}"),packageStr.indexOf(";"));
					packageDir = packageStr.replace("$!{gb.packageName}", globalBean.getPackageName()).replace(".", "/");
				}
				FileUtil.mkDirs(javaTestDir + packageDir);
				VelocityTemplate.mergeTemplate(sysInit.getTmplPath() + PATH_TEST_JAVA + "/" + vmFilename,javaTestDir + packageDir + "/" + tableBean.getClassName() + createFilename, ctx);
			}
			//生成SqlMap配置文件
			String sqlmapVm = sysInit.getTmplPath() + PATH_RESOURCES + "dal/sqlmap/-sqlmap.xml.vm";
			String sqlmapDir = sysInit.getGenPath() + sysInit.getTmplPath() + PATH_RESOURCES + "dal/sqlmap/";
			VelocityTemplate.mergeTemplate(sqlmapVm, sqlmapDir + tableBean.getTableName() + "-sqlmap.xml", ctx);
		} catch (Exception e) {
			logger.error("表[" + tablename +"]生成出错，异常是:",e);
		}
		return tableBean;
	}
	
	/**
	 * 生成所有表的IbatisDAO配置及基类代码文件
	 * @param tbList
	 */
	private void doGenCFG(List<TableBean> tbList){
		VelocityContext ctxCfg = new VelocityContext();
		ctxCfg.put("tbbList", tbList);
		ctxCfg.put("gb", globalBean); //设置全局信息
		ctxCfg.put("sysInit", sysInit);	//设置系统信息
		try {
			//生成persistence配置文件
			String persistenceVM = sysInit.getTmplPath() + PATH_RESOURCES + "dal/persistence/persistence.xml.vm";
			String persistenceFile = sysInit.getGenPath() + sysInit.getTmplPath() + PATH_RESOURCES + "dal/persistence/persistence.xml";
			VelocityTemplate.mergeTemplate(persistenceVM, persistenceFile, ctxCfg);
			//生成SqlMapConfig配置文件
			String sqlmapconfVm = sysInit.getTmplPath() + PATH_RESOURCES + "dal/persistence/sqlmap-config.xml.vm";
			String sqlmapconfFile = sysInit.getGenPath() + sysInit.getTmplPath() + PATH_RESOURCES + "dal/persistence/sqlmap-config.xml";
			VelocityTemplate.mergeTemplate(sqlmapconfVm, sqlmapconfFile, ctxCfg);
			//生成biz-dao配置文件
			String bizdaoVm = sysInit.getTmplPath() + PATH_RESOURCES + "dal/biz-dao.xml.vm";
			String bizdaoFile = sysInit.getGenPath() + sysInit.getTmplPath()  + PATH_RESOURCES + "dal/biz-dao.xml";
			VelocityTemplate.mergeTemplate(bizdaoVm, bizdaoFile, ctxCfg);

			//生成Java基类代码
			String javaVmDir = SOURCE_IN_PATH + sysInit.getTmplPath() + PATH_JAVA;
			String javaDir = sysInit.getGenPath() + sysInit.getTmplPath() + PATH_JAVA;
			List<String> javaVmList = FileUtil.getFileListWithExt(javaVmDir,".vm");
			String vmFilename,createFilename,packageDir = "",packageStr;
			for(int i=0;i<javaVmList.size();i++){
				vmFilename = javaVmList.get(i);
				if(!vmFilename.startsWith("Base")) continue;	 //非基类代码跳过
				createFilename = FileUtil.getFilenameWithoutExt(vmFilename);
				packageStr = FileUtil.findLine(javaVmDir + "/" + vmFilename, "package");
				if(StringUtils.isNotBlank(packageStr)){
					packageStr = packageStr.substring(packageStr.indexOf("$!{gb.packageName}"),packageStr.indexOf(";"));
					packageDir = packageStr.replace("$!{gb.packageName}", globalBean.getPackageName()).replace(".", "/");
				}
				FileUtil.mkDirs(javaDir + packageDir);
				VelocityTemplate.mergeTemplate(sysInit.getTmplPath() + PATH_JAVA + "/" + vmFilename, javaDir + packageDir + "/" + createFilename, ctxCfg);
			}
			//生成Java测试基类代码
			String javaTestVmDir = SOURCE_IN_PATH + sysInit.getTmplPath() + PATH_TEST_JAVA;
			String javaTestDir = sysInit.getGenPath() + sysInit.getTmplPath() + PATH_TEST_JAVA;
			List<String> javaTestVmList = FileUtil.getFileListWithExt(javaTestVmDir,".vm");
			for(int i=0;i<javaTestVmList.size();i++){
				vmFilename = javaTestVmList.get(i);
				if(!vmFilename.startsWith("Base")) continue;	 //非基类代码跳过
				createFilename = FileUtil.getFilenameWithoutExt(vmFilename);
				packageStr = FileUtil.findLine(javaTestVmDir + "/" + vmFilename, "package");
				if(StringUtils.isNotBlank(packageStr)){
					packageStr = packageStr.substring(packageStr.indexOf("$!{gb.packageName}"),packageStr.indexOf(";"));
					packageDir = packageStr.replace("$!{gb.packageName}", globalBean.getPackageName()).replace(".", "/");
				}
				FileUtil.mkDirs(javaTestDir + packageDir);
				VelocityTemplate.mergeTemplate(sysInit.getTmplPath() + PATH_TEST_JAVA + "/" + vmFilename,javaTestDir + packageDir + "/" + createFilename, ctxCfg);
			}
		} catch (Exception e) {
			logger.error("生成所有表的IbatisDAO配置及基类代码文件，异常是:",e);
		}
	}
	
	/**
	 * 生成DB结构信息
	 * @param tbList
	 */
	private void doGenDbInfo(List<TableBean> tbList){
		VelocityContext ctxCfg = new VelocityContext();
		ctxCfg.put("tbbList", tbList);
		ctxCfg.put("gb", globalBean); //设置全局信息
		ctxCfg.put("sysInit", sysInit);	//设置系统信息
		try {
			//生成dbstructure.xml表结构文件
			String dbstructureVM = "dao/dbinfo/dbstructure.xml.vm";
			String dbstructureFile = sysInit.getGenPath() + "dao/dbinfo/dbstructure.xml";
			VelocityTemplate.mergeTemplate(dbstructureVM, dbstructureFile, ctxCfg);
			if(sysInit.getDbType()==SysInit.DB_TYPE_MYSQL){
				//生成ddlformysql.sql表DDL语句
				String ddlformysqlVM = "dao/dbinfo/ddlformysql.sql.vm";
				String ddlformysqlFile = sysInit.getGenPath() + "dao/dbinfo/ddlformysql.sql";
				VelocityTemplate.mergeTemplate(ddlformysqlVM, ddlformysqlFile, ctxCfg);
			}else if(sysInit.getDbType()==SysInit.DB_TYPE_ORACLE){
				//生成ddlfororacle.sql表DDL语句
				String ddlfororacleVM = "dao/dbinfo/ddlfororacle.sql.vm";
				String ddlfororacleFile = sysInit.getGenPath() + "dao/dbinfo/ddlfororacle.sql";
				VelocityTemplate.mergeTemplate(ddlfororacleVM, ddlfororacleFile, ctxCfg);
			}
		} catch (Exception e) {
			logger.error("生成所有表结构配置及DDL语句出错，异常是:",e);
		}
	}

}

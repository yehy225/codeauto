package com.yehongyu.gencode.dao;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.yehongyu.gencode.dao.bean.ColBean;
import com.yehongyu.gencode.dao.bean.IndexBean;
import com.yehongyu.gencode.dao.bean.TableBean;
import com.yehongyu.gencode.util.StringUtil;

/**
 * 表对象生成
 * @author yingyang
 * @since 2011-10-17
 */
public class GenTable {
	
	//注意：静态类变量在类加载是最先执行，其次类字段实在对象初始化是执行，最后对象初始化在执行构造函数。

	/** 日志记录 */
	private final Logger logger = Logger.getLogger(GenTable.class);
	
	private DbConn conn;
	private SysInit sysInit;
	private List<String> allTableName;

	private String alltables;
	private Map<String,String> allTablePK;
	private Map<String,String> allTableComment;
	@Deprecated
	private Map<String,String> allTableColumeComment;
	
	public GenTable(DbConn dbConn){
		conn = dbConn;
		sysInit = dbConn.getSysInit();
		allTableName = getAllTableName();
		alltables = allTableName.toString().replaceAll("\\[", "'").replaceAll("\\]", "'").replaceAll(", ", "','");
		allTablePK = getAllTablePK();
		allTableComment = getAllTableComment();
//		allTableColumeComment = getAllTableColumnComment();
	}
	

	/**
	 * 根据表名生成表对象
	 * @param tableName
	 * @return
	 */
	public TableBean getTableBean(String tableName) {
		//验证及转换表名
		if(StringUtils.isBlank(tableName)) return null;
//		tableName = tableName.toLowerCase();  有些数据库表名为大写
		logger.info("开始生成表对象，表名：" + tableName);
		if(!allTableName.contains(tableName)){
			logger.error("表["+tableName+"]不存在,不生成此表DAO层代码！");
			return null;
		}
		TableBean tableBean = new TableBean();
		//设置表名及表注释
		tableBean.setTableName(tableName);
		tableBean.setClassName(TableBean.getClassName(tableBean.getTableName()));
		tableBean.setObjectName(TableBean.getObjectName(tableBean.getClassName()));
		String tabelComment = allTableComment.get(tableName);
		tableBean.setTableComment(StringUtils.isBlank(tabelComment)?tableName:tabelComment);
		//设置各个列对象	
		this.setTableColumn(tableBean);
		//设置索引
		this.setTableIndex(tableBean);
		//转换Bean对象
		this.convertTableBean(tableBean);
		//校验Bean对象
		if(!checkTableBean(tableBean)){
			return null;
		}
		logger.info("生成表对象成功，表名：" + tableName);
		return tableBean;
	}
	
	private boolean checkTableBean(TableBean tableBean){
		// 校验主键是否为空
		if (tableBean.getPkcol() == null) {
			logger.error("表["+tableBean.getTableName()+"]自增主键不存在,不生成此表DAO层代码！");
			return false;
		}
		//TODO 验证主键是否自增（Mysql）或有Seq为 seq_{表名}_id（Oracle）
		// 校验主键是否命名为id
		if (!"id".equals(tableBean.getPkcol().getColName())) {
			logger.error("表["+tableBean.getTableName()+"]主键命名不是id,不生成此表DAO层代码！");
			return false;
		}
		// 校验字段中是否含有gmt_create或gmt_modified
		List<ColBean> cbList = tableBean.getColList();
		boolean hasGmtCreate = false;
		boolean hasGmtModified = false;
		for(ColBean cb:cbList){
			if("gmtCreate".equals(cb.getPropertyName())) hasGmtCreate = true;
			if("gmtModified".equals(cb.getPropertyName())) hasGmtModified = true;
		}
		if(!hasGmtCreate||!hasGmtModified){
			logger.error("表["+tableBean.getTableName()+"]没有gmt_create或gmt_modified字段,不生成此表DAO层代码！");
			return false;
		}
		return true;
	}

	/**
	 * 转化表对象各个字段列类型
	 * @param tableBean
	 * @return
	 */
	private TableBean convertTableBean(TableBean tableBean) {
		// 设置表对象主键名为id(按源自段设置）
		if (tableBean.getPkcol() != null) {
			ColBean cb = tableBean.getPkcol();
			cb.setPropertyName(ColBean.getPropName(cb.getColName()));
//			cb.setPropertyName("id");
			cb.setMethodName(ColBean.getMethodName(cb.getPropertyName()));
			cb.setPropertyType(FieldMapping.mapJavaType(cb.getColSQLType()));
//			cb.setHibernateType(FieldMapping.mapHibernateType(cb.getColSQLType()));
			if(!"Long".equals(cb.getPropertyType())) cb.setPropertyType("Long");	//如果主键不是被映射成Long，则使用Long型。
			tableBean.setPkcol(cb);
		}
		// 设置表对象其他字段
		List<ColBean> ll = tableBean.getColList();
		for (Iterator<ColBean> it = ll.iterator(); it.hasNext();) {
			ColBean cb = it.next();
			cb.setPropertyName(ColBean.getPropName(cb.getColName()));
			cb.setMethodName(ColBean.getMethodName(cb.getPropertyName()));
			cb.setPropertyType(FieldMapping.mapJavaType(cb.getColSQLType()));
//			cb.setHibernateType(FieldMapping.mapHibernateType(cb.getColSQLType()));
		}
		// 将主键加入字段列表
		if (tableBean.getPkcol() != null) {
			ll.add(0, tableBean.getPkcol());
		}
		return tableBean;
	}

	/**
	 * 获取数据库中所有表名
	 * @return
	 */
	public List<String> getAllTableName() {
		if(allTableName!=null&&!allTableName.isEmpty()) return allTableName;
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;
		List<String> tableList = new ArrayList<String>();
		try {
			dbmd = conn.getDatabaseMetaData();	

			/*获取所有指定表的元信息，参数(catalog,schemaPattern,tableNamePattern,types)
			 * 返回5列数据,如下所示(schema,catalog,table_name,table_type,?)*/
			String[] types = { "TABLE" };	//类型，可以是"TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"
			rs = dbmd.getTables(null, null, "%", types);
			logger.info("============================获取所有表结构信息：");
			while (rs.next()) {
				tableList.add(rs.getString(3).trim());
				logger.info("Schema名【" + StringUtil.genLengthStr(rs.getString(1),10)
						+"】表名【"+ StringUtil.genLengthStr(rs.getString(3),25)
						+"】表类型【"+ StringUtil.genLengthStr(rs.getString(4),10)
						+"】表注释【"+ StringUtil.genLengthStr(rs.getString(5),30)+"】");	//Mysql无法通过此方式获取表注释
//				logger.info(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3) 	//schema,catalog,table_name
//						+ "|" + rs.getString(4) + "|" + rs.getString(5) + "|"); 	//table_type,table_comment?
			}
		} catch (SQLException e) {
			logger.error("获取所有指定表的元信息出错", e);
		}
		if(!sysInit.getTableList().isEmpty()){	//如果指定了表，则返回指定表
			List<String> tabs = new ArrayList<String>();
			for(String t:sysInit.getTableList()){
				if(tableList.contains(t)){
					tabs.add(t);
				}else{
					logger.error("指定的表["+t+"]不存在，不生成此表！");
				}
			}
			tableList = tabs;	//根据指定参数返回生成表
		}
		return tableList;
	}

	/**
	 * 获取所有表注释,新的方式无法获取表注释，只能用旧的方式
	 * @return
	 */
	public Map<String, String> getAllTableComment(){
		HashMap<String, String> map = new HashMap<String, String>();
		String sql;
		if(sysInit.getDbType()==SysInit.DB_TYPE_MYSQL){
			sql = "SELECT TABLE_NAME,TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_NAME in("+alltables+")";
		}else if(sysInit.getDbType()==SysInit.DB_TYPE_ORACLE){
			sql = "SELECT TABLE_NAME,COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME in("+alltables+")";
		}else{
			return null;
		}
		try {
			ResultSet rs = null;
			rs = conn.executeQuery(sql);
			while (rs.next()) {
				String tableName,tabComment;
				tableName = rs.getString(1);
				tabComment = rs.getString(2);
				if(tabComment!=null&&tabComment.indexOf(";")>=0){
					tabComment = tabComment.substring(0,tabComment.indexOf(";"));
				}
				map.put(tableName.toLowerCase(), tabComment);
			}
			rs.close();
		} catch (SQLException e) {
			logger.error("获取所有表注释出错！异常是：", e);
		} finally {
			conn.closeStmt();
		}
		return map;
	}

	/**
	 * 获取所有表的主键
	 * @return
	 */
	private Map<String, String> getAllTablePK(){
		HashMap<String, String> map = new HashMap<String, String>();
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;
		try {
			dbmd = conn.getDatabaseMetaData();	
			/*获取表的主键信息，参数(catalog,schemaPattern,tableName)一定要指定表名称，否则返回将出错。
			 *返回6列数据,如下所示*/
			logger.info("============================获取所有表主键信息：");
			for(String t:allTableName){
				rs = dbmd.getPrimaryKeys(null, null, t);
				while (rs.next()) {
					map.put(rs.getString(3).toLowerCase(), rs.getString(4).toLowerCase());
					logger.info("Schema名【" + StringUtil.genLengthStr(rs.getString(1),10)
							+"】表名【"+ StringUtil.genLengthStr(rs.getString(3),25)
							+"】主键列名【"+ StringUtil.genLengthStr(rs.getString(4),10)
							+"】主键列序号【"+ StringUtil.genLengthStr(rs.getString(5),2)
							+"】主键名称【"+ StringUtil.genLengthStr(rs.getString(6),15)+"】");
	//				logger.info(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3));	//schema,catalog,table_name 
	//				logger.info(rs.getString(4) + "|" + rs.getString(5) + "|" + rs.getString(6));	//col_name,pri_seq,pri_name(PRIMARY)
				}
			}
		} catch (SQLException e) {
			logger.error("获取所有指定表的主键信息出错", e);
		}
		return map;
	}

	/**
	 * 获取所有指定表的索引
	 * @return
	 */
	private TableBean setTableIndex(TableBean tableBean){
		HashMap<String, IndexBean> map = null;
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;
		try {
			dbmd = conn.getDatabaseMetaData();	
			/*获取表的索引信息，参数(catalog,schema ,tableName,unique,approximate)unique为false表示返回所有索引。
			 *返回13列数据,如下所示*/
//			for(String t:allTableName){
				rs = dbmd.getIndexInfo(null, null, tableBean.getTableName(), false, true);
				map = new HashMap<String, IndexBean>();
				while (rs.next()) {
					String indexName = rs.getString(6);
					if(map.get(indexName)!=null){	//已有字段索引
						IndexBean ib = map.get(indexName);
						ib.setIndexColumns(ib.getIndexColumns() + "," + rs.getString(9));
						map.put(indexName, ib);
					}else{	//新字段索引
						IndexBean ib = new IndexBean();
						ib.setIndexName("PRIMARY".equals(indexName)?"PK_"+indexName:indexName);
						ib.setIndexColumns(rs.getString(9));
						ib.setUnique(rs.getBoolean(4)?false:true);
						ib.setPrimary("PRIMARY".equals(indexName)?true:false);
						map.put(indexName, ib);
					}
					logger.info("表名【"+ StringUtil.genLengthStr(rs.getString(3),25)
							+"】不是唯一索引【"+ StringUtil.genLengthStr(rs.getBoolean(4),5)
							+"】索引名称【"+ StringUtil.genLengthStr(rs.getString(6),30)
							+"】索引类型【"+ StringUtil.genLengthStr(rs.getString(7),2)
							+"】索引列序号【"+ StringUtil.genLengthStr(rs.getString(8),2)
							+"】索引列名【"+ StringUtil.genLengthStr(rs.getString(9),15)+"】");
//					logger.info(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3));	//schema,catalog,table_name 
//					logger.info(rs.getString(4) + "|" + rs.getString(5) + "|" + rs.getString(6));	//non_unique,?,index_name
//					logger.info(rs.getString(7) + "|" + rs.getString(8) + "|" + rs.getString(9));	//index_type,order_persition,idx_col
//					logger.info(rs.getString(10) + "|" + rs.getString(11) + "|" + rs.getString(12));	//*
//					logger.info(rs.getString(13));	//*
				}
				//将索引List加入到当前表对象中
				for(IndexBean ib : map.values()){	//先加主键
					if(ib.isPrimary()) tableBean.addIndexBean(ib);
				}
				for(IndexBean ib : map.values()){	//再加唯一键
					if(ib.isUnique()&!ib.isPrimary()) tableBean.addIndexBean(ib);
				}
				for(IndexBean ib : map.values()){	//最后加索引
					if(!ib.isUnique()&!ib.isPrimary()) tableBean.addIndexBean(ib);
				}
//			}
		} catch (SQLException e) {
			logger.error("获取所有指定表的索引出错", e);
		}
		return tableBean;
	}

	/**
	 * 获取所有指定表的字段
	 * @return
	 */
	private TableBean setTableColumn(TableBean tableBean){
		DatabaseMetaData dbmd = null;
		ResultSet rs = null;
		try {
			dbmd = conn.getDatabaseMetaData();	
			/*获取所有指定表字段的元信息，参数(catalog,schemaPattern,tableNamePattern,types)
			 * 返回23列数据,如下所示*/
			rs = dbmd.getColumns(null, null, tableBean.getTableName(), "%");
			while (rs.next()) {
				logger.info("表名【" + StringUtil.genLengthStr(rs.getString(3),25)
						+"】列名【"+ StringUtil.genLengthStr(rs.getString(4),15)
						+"】列sqltype【"+ StringUtil.genLengthStr(rs.getInt(5),3)
						+"】列typename【"+ StringUtil.genLengthStr(rs.getString(6),10)
						+"】列precision【"+ StringUtil.genLengthStr(rs.getInt(7),5)
						+"】列scale【"+ StringUtil.genLengthStr(rs.getInt(9),5)
						+"】列isNullable【"+ StringUtil.genLengthStr(rs.getInt(11),2)
						+"】列isNullable2【"+ StringUtil.genLengthStr(rs.getString(18),3)
						+"】列comment【"+ StringUtil.genLengthStr(rs.getString(12),20)
						+"】列defaultValue【"+ StringUtil.genLengthStr(rs.getString(13),5)
						+"】列isAutocrement【"+ StringUtil.genLengthStr(rs.getString(23),5)+"】");
//				logger.info(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3));	//schema,catalog,table_name 
//				logger.info(rs.getString(4) + "|" + rs.getString(5) + "|" + rs.getString(6));	//col_name,col_sqltype,col_typename
//				logger.info(rs.getString(7) + "|" + rs.getString(8) + "|" + rs.getString(9));	//col_precision,?,col_scale
//				logger.info(rs.getString(10) + "|" + rs.getString(11) + "|" + rs.getString(12));	//?,isNullable,comment
//				logger.info(rs.getString(13) + "|" + rs.getString(18) + "|" + rs.getString(23));	//default_value,is_null,is_autocrement
				ColBean cb = new ColBean();
				String colName = rs.getString(4);
				cb.setColName(colName.toLowerCase());	//列名小写
				cb.setColType(rs.getString(6));	//列类型
//				String colComment = allTableColumeComment.get(tableBean.getTableName()+"."+colName);
				cb.setColComment(StringUtils.isBlank(rs.getString(12))?cb.getColName():rs.getString(12));	//列注释
				cb.setNullable(rs.getInt(11)==0?false:true);	//是否可为空
				cb.setDefaultValue(rs.getString(13));
				cb.setPrecision(rs.getInt(7));	//字段长度	
				cb.setScale(rs.getInt(9));
				cb.setAutoIncrement("YES".equals(rs.getString(23))?true:false);//是否自增字段
				// 设置列SQL类型
				int sqltype = rs.getInt(5);
				cb.setColSQLType(sqltype);
				if(!as.containsKey(sqltype)){
					as.put(sqltype, "typeName:" + rs.getString(6) + " -> className:【无】");
				}
				// 设置主键并添加进表对象
				String pkfieldname = allTablePK.get(tableBean.getTableName());
				if (colName.equalsIgnoreCase(pkfieldname)) {	//有主键并且要自增的才可以
					cb.setPK(true);
					tableBean.setPkcol(cb);
				} else {
					cb.setPK(false);
					tableBean.addColBean(cb);
				}
			}
		} catch (SQLException e) {
			logger.error("获取所有指定表的字段出错", e);
		}
		return tableBean;
	}
	
	//本次执行所有字段映射类型
	public Map<Integer,String> as = new HashMap<Integer,String>();
	
	/**
	 * 获取数据库中所有表名,旧的方式被废弃
	 * @return
	 * @see getAllTableNameOld
	 */
	@Deprecated
	public List<String> getAllTableNameOld() {
		ArrayList<String> list = new ArrayList<String>();
		try {
			ResultSet rs = null;
			if(sysInit.getDbType()==SysInit.DB_TYPE_MYSQL){
				rs = conn.executeQuery("show tables");
			}else{
				rs = conn.executeQuery("select table_name from user_all_tables");
			}
			while (rs.next()) {
				//如果指定了表，则跳过其他表
				if(!sysInit.getTableList().isEmpty()
						&&!sysInit.getTableList().contains(rs.getString(1))) continue;
				list.add(rs.getString(1));
			}
			if(list.size()>100){
				logger.warn("所查超过100张表，如果一直没反应，请用[DB_TABLES]参数指定较少的表！");
			}
			rs.close();
		} catch (SQLException e) {
			logger.error("获取数据库中所有表名!",e);
		} finally {
			conn.closeStmt();
		}
		return list;
	}
	
	/**
	 * 获取所有表的主键,旧的方式被废弃
	 * @return
	 * @see getAllTablePK
	 */
	@Deprecated
	public Map<String,String> getAllTablePKOld(){
		HashMap<String, String> map = new HashMap<String, String>();
		String sql;
		if(sysInit.getDbType()==SysInit.DB_TYPE_MYSQL){
			sql = "SELECT TABLE_NAME,COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE where CONSTRAINT_NAME = 'PRIMARY' AND TABLE_NAME in("+alltables+")";
		}else if(sysInit.getDbType()==SysInit.DB_TYPE_ORACLE){
			sql = "select b.*,C.data_type from user_constraints A, USER_CONS_COLUMNS B,user_tab_columns C "
					+ "where A.owner = B.owner "
					+ "AND A.CONSTRAINT_TYPE = 'P' AND "
					+ "A.CONSTRAINT_NAME = B.CONSTRAINT_NAME "
					+ "and B.COLUMN_NAME=C.COLUMN_NAME "
					+ "and B.table_name=C.table_name "
					+ "and A.table_name=C.table_name AND A.TABLE_NAME in("+alltables+")";
		}else{
			return null;
		}
		try {
			ResultSet rs = null;
			rs = conn.executeQuery(sql);
			while (rs.next()) {
				String tablename, pkfieldname;
				tablename = rs.getString("TABLE_NAME");
				pkfieldname = rs.getString("COLUMN_NAME");
				map.put(tablename.toLowerCase(), pkfieldname.toLowerCase());
			}
			rs.close();
		} catch (SQLException e) {
			logger.error("获取所有表的主键发生异常!",e);
		} finally {
			conn.closeStmt();
		}
		return map;
	}
	
	/**
	 * 为表对象设置列对象，废弃。但可以取到更详细的列信息，如：className:" + rmd.getColumnClassName(i);
	 * @param tableBean
	 * @return 设置列对象后的表对象
	 * @see GenTable.setTableColumn()
	 */
	@Deprecated
	public TableBean setTableColumnOld(TableBean tableBean){
		String tableName = tableBean.getTableName();
		try {
			ResultSetMetaData rmd = conn.getTableMetaData("select * from " + tableName + " where 1<>1");
			//设置各个列对象
			int colcnt = rmd.getColumnCount();
			logger.info("表["+tableBean.getTableName()+"]有【"+colcnt+"】列");
			for (int i = 1; i <= colcnt; i++) {
				logger.info("列【" + StringUtil.genLengthStr(rmd.getColumnName(i),20)
//						+"】getCatalogName【"+rmd.getCatalogName(i)	//表示数据库的Schema名，如:tripagent
						+"】getColumnClassName【"+ StringUtil.genLengthStr(rmd.getColumnClassName(i),20) //列所映射的Java类名，如：java.lang.Long
//						+"】getColumnDisplaySize【"+ StringUtil.genLengthStr(rmd.getColumnDisplaySize(i),12) //列的数据存储长度，如：4000（varchar(4000)）
//						+"】getColumnLabel【"+ StringUtil.genLengthStr(rmd.getColumnLabel(i),20) 	//表示取到的列名as的别名，如果没有与列名同。
						+"】getColumnType【"+ StringUtil.genLengthStr(rmd.getColumnType(i),4) 	//sqltype,JDBC中的java.sql.Types的int表示。
						+"】getColumnTypeName【"+ StringUtil.genLengthStr(rmd.getColumnTypeName(i),10) //列的数据库类型名，为大写。
						+"】getPrecision【"+ StringUtil.genLengthStr(rmd.getPrecision(i),12) //列的数据存储长度，与getColumnDisplaySize类似。
						+"】getScale【"+ StringUtil.genLengthStr(rmd.getScale(i),2) //列的小数点后的尺度。
//						+"】getSchemaName【"+ StringUtil.genLengthStr(rmd.getSchemaName(i),20) //table's schema.
//						+"】getTableName【"+ StringUtil.genLengthStr(rmd.getTableName(i),20) //列所在的表名
						+"】isAutoIncrement【"+ StringUtil.genLengthStr(rmd.isAutoIncrement(i),5) //是否自动增长列
						+"】isCaseSensitive【"+ StringUtil.genLengthStr(rmd.isCaseSensitive(i),5) //是否大小写敏感
						+"】isDefinitelyWritable【"+ StringUtil.genLengthStr(rmd.isDefinitelyWritable(i),5) //是否绝对可写
						+"】isWritable【"+ StringUtil.genLengthStr(rmd.isWritable(i),5)  //是否可写
						+"】isNullable【"+ StringUtil.genLengthStr(rmd.isNullable(i),1)	//是否可以为空:0、不为空；1、可为空；2、不知。
						+"】isReadOnly【"+ StringUtil.genLengthStr(rmd.isReadOnly(i),5) //是否只读，isDefinitelyWritable的相反。
						+"】isSearchable【"+ StringUtil.genLengthStr(rmd.isSearchable(i),5) //是否可做查询条件
						+"】isSigned【"+ StringUtil.genLengthStr(rmd.isSigned(i),5) //是否有分配默认值
						+"】");
				ColBean cb = new ColBean();
				String colName = rmd.getColumnName(i);
				String colType = rmd.getColumnTypeName(i);
				cb.setColName(colName.toLowerCase());	//列名小写
				cb.setColType(colType);
				String colComment = allTableColumeComment.get(tableName+"."+colName);
				cb.setColComment(StringUtils.isBlank(colComment)?cb.getColName():colComment);
				cb.setNullable(rmd.isNullable(i)==0?false:true);
				cb.setPrecision(rmd.getPrecision(i));
				// 设置列SQL类型
				int sqltype = rmd.getColumnType(i);
//				int scale = rmd.getScale(i);
//				if (scale > 0 && colType.startsWith("NUMBER")) {
//					cb.setColSQLType(Types.DECIMAL);
//				} else {
					cb.setColSQLType(sqltype);
//				}
				if(!as.containsKey(sqltype)){
					as.put(sqltype, "typeName:" + rmd.getColumnTypeName(i) + " -> className:" + rmd.getColumnClassName(i));
				}
				// 设置主键并添加进表对象
				String pkfieldname = allTablePK.get(tableBean.getTableName());
				if (colName.equalsIgnoreCase(pkfieldname)) {	//有主键并且要自增的才可以
					cb.setPK(true);
					tableBean.setPkcol(cb);
				} else {
					tableBean.addColBean(cb);
				}
			}
		} catch (SQLException e) {
			logger.error("表["+tableName+"]不存在！",e);
			return null;
		} finally {
			conn.closeStmt();
		}
		return tableBean;
	}

	/**
	 * 获取所有表的字段注释,旧的方式被废弃
	 * @return
	 */
	@Deprecated
	public Map<String, String> getAllTableColumnComment(){
		HashMap<String, String> map = new HashMap<String, String>();
		String sql;
		if(sysInit.getDbType()==SysInit.DB_TYPE_MYSQL){
			sql = "SELECT TABLE_NAME,COLUMN_NAME,COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_NAME in("+alltables+")";
		}else if(sysInit.getDbType()==SysInit.DB_TYPE_ORACLE){
			sql = "SELECT TABLE_NAME,COLUMN_NAME,COMMENTS FROM USER_COL_COMMENTS WHERE TABLE_NAME in("+alltables+")";
		}else{
			return null;
		}
		try {
			ResultSet rs = null;
			rs = conn.executeQuery(sql);
			while (rs.next()) {
				String tablename,colname,colcomment;
				tablename = rs.getString(1);
				colname = rs.getString(2);
				colcomment = rs.getString(3);
				map.put(tablename.toLowerCase()+"."+colname.toLowerCase(), colcomment);
			}
			rs.close();
		} catch (SQLException e) {
			logger.error("获取所有表的字段注释发生异常!", e);
		} finally {
			conn.closeStmt();
		}
		return map;
	}

}

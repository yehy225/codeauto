package com.yehongyu.gencode.dao;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class FieldMapping {

	/** 日志记录 */
	private final static Logger logger = Logger.getLogger(FieldMapping.class);
	
	/**
	 * Mysql数据库字段类型映射:
	 * mysql类型  <-> rs.getColumnTypeName <-> java.sql.Types <-> rs.getColumnClassName
	 * ===============================================================================
	 * tinyint(1) <-> BIT <-> Types.BIT(-7) -> java.lang.Integer
	 * tinyint(4) <-> TINYINT <-> Types.TINYINT(-6) <-> java.lang.Integer
	 * smallint(6) <-> SMALLINT <-> Types.SMALLINT(5) <-> java.lang.Integer
	 * mediumint(9) <-> MEDIUMINT <-> Types.INTEGER(4) <-> java.lang.Integer
	 * integer(11) <-> INT <-> Types.INTEGER(4) <-> java.lang.Integer(重复sqltype)
	 * bigint(20) <-> BIGINT <-> Types.BIGINT(-5) <-> java.lang.Long
	 * float(9,3) <-> FLOAT <-> Types.REAL(7) -> java.lang.Float
	 * double(15,3) <-> DOUBLE <-> Types.DOUBLE(8) <-> java.lang.Double
	 * decimal(11) <-> DECIMAL <-> Types.DECIMAL(3) <-> java.math.BigDecimal
	 * -----------------------------------------------------------------------
	 * date(10) <-> DATE <-> Types.DATE(91) <-> java.sql.Date
	 * year(4) <-> YEAR <-> Types.DATE(91) <-> java.sql.Date(重复sqltype)
	 * time(8) <-> TIME <-> Types.TIME(92) <-> java.sql.Time
	 * datetime(19) <-> DATETIME <-> Types.TIMESTAMP(93) <-> java.sql.Timestamp
	 * timestamp(19) <-> TIMESTAMP <-> Types.TIMESTAMP(93) <-> java.sql.Timestamp(重复sqltype)
	 * -----------------------------------------------------------------------
	 * char(1) <-> CHAR <-> Types.CHAR(1) <-> java.lang.String
	 * varchar(*) <-> VARCHAR <-> Types.VARCHAR(12) <-> java.lang.String
	 * ---------------------------------------------------
	 * tinytext(127) <-> VARCHAR <-> Types.LONGVARCHAR(-1) <-> java.lang.String
	 * text(8388607) <-> VARCHAR <-> Types.LONGVARCHAR(-1) <-> java.lang.String(重复sqltype)
	 * mediumtext(8388607) <-> VARCHAR <-> Types.LONGVARCHAR(-1) <-> java.lang.String(重复sqltype)
	 * longtext(1073741823) <-> VARCHAR <-> Types.LONGVARCHAR(-1) <-> java.lang.String(重复sqltype)
	 * -----------------------------------------------------------------------
	 * blob(65535) <-> BLOB <-> Types.LONGVARBINARY(-4) <-> [B(本程序映射为java.sql.Blob)
	 * mediumblob(16777215) <-> MEDIUMBLOB <-> Types.LONGVARBINARY(-4) <-> [B(本程序映射为java.sql.Blob)
	 * longblob(2147483647) <-> LONGBLOB <-> Types.LONGVARBINARY(-4) <-> [B(本程序映射为java.sql.Blob)
	 * tinyblob(255) <-> TINYBLOB <-> Types.VARBINARY(-3) <-> [B(本程序映射为java.sql.Blob)
	 * ------------------------------------------------
	 * varbinary(1) <-> VARBINARY <-> Types.VARBINARY(-3) <-> [B(本程序映射为java.sql.Blob)
	 * geometry(2147483647) <-> GEOMETRY <-> Types.BINARY(-2) <-> [B(本程序映射为java.sql.Blob)
	 * binary(1) <-> BINARY <-> Types.BINARY(-2) <-> [B(本程序映射为java.sql.Blob)
	 * ===============================================================================
	 * Oracle数据库字段类型映射:
	 * oracle类型  <-> rs.getColumnTypeName <-> java.sql.Types <-> rs.getColumnClassName
	 * ===============================================================================
	 * NUMBER <-> Types.NUMERIC(2) <-> java.math.BigDecimal(本程序映射为java.lang.Long)
	 * VARCHAR2 <-> Types.VARCHAR(12) <-> java.lang.String
	 * DATE <-> Types.DATE(91) <-> java.sql.Timestamp
	 * CLOB <-> Types.CLOB(2005) <-> java.sql.CLOB
	 * ===============================================================================
	 */
	/** 
	 * 本程序sqltypeToJava转换：
	 * 公共：
	 * =================================================
	 * <li> Types.VARCHAR(12) -> java.lang.String </li>
	 * <li> Types.DATE(91) -> java.sql.Timestamp </li>
	 * <li> Types.DECIMAL(3) -> java.math.BigDecimal </li>
	 * =================================================
	 * Mysql：
	 * ===============================================================================
	 * <li> Types.BIT(-7) -> java.lang.Integer </li>
	 * <li> Types.TINYINT(-6) -> java.lang.Integer </li>
	 * <li> Types.SMALLINT(5) -> java.lang.Integer </li>
	 * <li> Types.INTEGER(4) -> java.lang.Integer </li>
	 * <li> Types.BIGINT(-5) -> java.lang.Long </li>
	 * <li> Types.REAL(7) -> java.lang.Float </li>
	 * <li> Types.DOUBLE(8) -> java.lang.Double </li>
	 * ----------------------------------------------------------------------------
	 * <li> Types.CHAR(1) <-> java.lang.String </li>
	 * <li> Types.LONGVARCHAR(-1) <-> java.lang.String </li>
	 * ----------------------------------------------------------------------------
	 * <li> Types.BINARY(-2) <-> [B(本程序映射为java.sql.Blob) </li>
	 * <li> Types.VARBINARY(-3) <-> [B(本程序映射为java.sql.Blob) </li>
	 * <li> Types.LONGVARBINARY(-4) <-> [B(本程序映射为java.sql.Blob) </li>
	 * ----------------------------------------------------------------------------
	 * <li> Types.TIME(92) <-> java.sql.Time </li>
	 * <li> Types.TIMESTAMP(93) -> java.sql.Timestamp </li>
	 * ===============================================================================
	 * Oracle:
	 * ===============================================================================
	 * <li> Types.NUMERIC(2) -> java.math.BigDecimal(本程序映射为java.lang.Long) </li>
	 * <li> Types.FLOAT(6) -> java.math.BigDecimal </li>
	 * ------------------------------------------------
	 * <li> Types.CLOB(2005) -> java.sql.Clob </li>
	 * ===============================================================================
	 */
	private static Map<Integer,String> sqltypeToJava = new HashMap<Integer,String>();
	static {
		//合集10种{Integer\Long\Float\Double\String|Clob\Blob\Time\Timestamp|BigDecimal}
		//common
		sqltypeToJava.put(Types.VARCHAR, "String");
		sqltypeToJava.put(Types.DATE, "Timestamp");	//java.sql.Timestamp
		//mysql
		sqltypeToJava.put(Types.BIT, "Integer");
		sqltypeToJava.put(Types.TINYINT, "Integer");
		sqltypeToJava.put(Types.SMALLINT, "Integer");
		sqltypeToJava.put(Types.INTEGER, "Integer");
		sqltypeToJava.put(Types.BIGINT, "Long");
		sqltypeToJava.put(Types.REAL, "Float");
		sqltypeToJava.put(Types.DOUBLE, "Double");
		
		sqltypeToJava.put(Types.CHAR, "String");
		sqltypeToJava.put(Types.LONGVARCHAR, "String");
		
		sqltypeToJava.put(Types.BINARY, "Blob");	//java.sql.Blob
		sqltypeToJava.put(Types.VARBINARY, "Blob");	//java.sql.Blob
		sqltypeToJava.put(Types.LONGVARBINARY, "Blob");	//java.sql.Blob
		
		sqltypeToJava.put(Types.TIME, "Time");	//java.sql.Time
		sqltypeToJava.put(Types.TIMESTAMP, "Timestamp");	//java.sql.Timestamp
		//oracle
		sqltypeToJava.put(Types.NUMERIC, "Long");
		sqltypeToJava.put(Types.FLOAT, "BigDecimal");	//java.math.BigDecimal
		sqltypeToJava.put(Types.DECIMAL, "BigDecimal");	//java.math.BigDecimal
		sqltypeToJava.put(Types.CLOB, "Clob");	//java.sql.Clob
	}
	/** 
	 * 本程序javaToMysql转换：
	 * 
	 */
	private static Map<String,String> javaToMysql = new HashMap<String,String>();
	static{
		javaToMysql.put("String", "VARCHAR");
		javaToMysql.put("Long", "BIGINT");
		javaToMysql.put("Integer", "TINYINT");
		javaToMysql.put("Double", "Double");
		javaToMysql.put("Timestamp", "DATETIME");
	}
	private static Map<String,String> javaToOracle = new HashMap<String,String>();
	static{
		javaToOracle.put("String", "VARCHAR2");
		javaToOracle.put("Long", "NUMBER");
		javaToOracle.put("Integer", "NUMBER");
		javaToOracle.put("CLOB", "CLOB");
		javaToOracle.put("Timestamp", "DATE");
	}
	
	/**
	 * <ul>未定义的Types
	 * <li> Types.NULL(0) -> </li>
	 * <li> Types.OTHER(1111) -> </li>
	 * <li> Types.JAVA_OBJECT(2000) -> </li>
	 * <li> Types.DISTINCT(2001) -> </li>
	 * <li> Types.STRUCT(2002) -> </li>
	 * <li> Types.ARRAY(2003) -> </li>
	 * <li> Types.BLOB(2004) -> </li>
	 * <li> Types.REF(2006) -> </li>
	 * <li> Types.DATALINK(70) -> </li>
	 * <li> Types.BOOLEAN(16) -> </li>
		//------------------------- JDBC 4.0 -----------------------------------
	 * <li> Types.ROWID(-8) -> </li>
	 * <li> Types.NCHAR(-15) -> </li>
	 * <li> Types.NVARCHAR(-9) -> </li>
	 * <li> Types.LONGNVARCHAR(-16) -> </li>
	 * <li> Types.NCLOB(2011) -> </li>
	 * <li> Types.SQLXML(2009) -> </li>
	 * </ul>
	 * @param sqltype
	 * @return
	 */
	public static String mapJavaType(int sqltype) {
		String javaType = sqltypeToJava.get(sqltype);
		if(StringUtils.isNotBlank(javaType)){
			return javaType;
		}else{
			logger.error("字段没有对应的Java类型，SQL_TYPE：" + sqltype);
			return "String";
		}
	}

	/**
	 * 字段根据数据库类型转成hibernate类型
	 * @param sqltype
	 * @return
	 */
	public static String mapHibernateType(int sqltype) {
		String s = "string";
		switch (sqltype) {
		case Types.CLOB:
			s = "clob";
			break;
		case Types.DECIMAL:
			s = "big_decimal";
			break;
		case Types.NUMERIC:
			s = "long";
			break;
		case Types.TIMESTAMP:
			s = "timestamp";
			break;
		case Types.DATE:
			s = "timestamp";
			break;
		default:
			s = "string";
		}
		return s;
	}


}

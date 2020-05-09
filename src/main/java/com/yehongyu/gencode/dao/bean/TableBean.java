package com.yehongyu.gencode.dao.bean;

import java.util.ArrayList;
import java.util.List;

import com.yehongyu.gencode.util.CharUtil;


public class TableBean {
	/**================表结构信息======================*/
	/** 表名 */
	private String tableName;
	/** 表注释 */
	private String tableComment;

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableComment() {
		return tableComment;
	}

	public void setTableComment(String tableComment) {
		this.tableComment = tableComment;
	}
	
	/**================根据表结构生成表对象及字段信息======================*/
	/** 类名 */
	private String className;
	/** 类对象名 */
	private String objectName;

	/** 主键 */
	private ColBean pkcol;
	/** 字段列表 */
	private List<ColBean> colList = new ArrayList<ColBean>();

	/** 索引列表 */
	private List<IndexBean> indexList = new ArrayList<IndexBean>();

	public List<IndexBean> getIndexList() {
		return indexList;
	}

	public void setIndexList(List<IndexBean> indexList) {
		this.indexList = indexList;
	}

	public void addIndexBean(IndexBean bean) {
		this.indexList.add(bean);
	}

	public List<ColBean> getColList() {
		return colList;
	}

	public void setColList(List<ColBean> colList) {
		this.colList = colList;
	}

	public void addColBean(ColBean bean) {
		this.colList.add(bean);
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public ColBean getPkcol() {
		return pkcol;
	}

	public void setPkcol(ColBean pkcol) {
		this.pkcol = pkcol;
	}

	/**
	 * 根据表名生成对象类名,例如：account_audit -> AcountAudit
	 * @param tablename
	 * @return
	 */
	public static String getClassName(String tablename) {
		if ("".equals(tablename) || tablename == null)
			return "";
		char[] a = tablename.toLowerCase().toCharArray();
		StringBuffer sb = new StringBuffer();
		// 首字母转大写
		sb.append(CharUtil.toUpperCase(a[0]));
		//下划线首字母转大写
		for (int i = 1; i < a.length; i++) {
			char c = a[i];
			if (c == '_' && i < a.length - 1) {
				a[i + 1] = CharUtil.toUpperCase(a[i + 1]);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * 根据类名生成对象名,例如：FullName->fullName
	 * @param classname
	 * @return
	 */
	public static String getObjectName(String classname) {
		char[] a = classname.toCharArray();
		a[0] = CharUtil.toLowerCase(a[0]);
		return new String(a);
	}

}
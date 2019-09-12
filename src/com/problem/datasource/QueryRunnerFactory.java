package com.problem.datasource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.QueryRunner;

public class QueryRunnerFactory {
	private static MyDataSource ms = new MyDataSource();
	public static QueryRunner newQueryRunner() {
		return new QueryRunner(ms);
	}
	
	public static <T> T getOne(Class<T> cls,ResultSet rs) throws Exception {
		if(rs.next()) {
			return getResult(cls, rs);
		}
		return null;
	}
	public static <T> List<T> getList(Class<T> cls,ResultSet rs) throws Exception {
		List<T> list = new ArrayList<T>();
		while(rs.next()) {
			list.add( getResult(cls, rs) );
		}
		return list;
	}
	
	private static <T> T getResult(Class<T> cls,ResultSet rs) throws Exception {
		T obj = cls.newInstance();// ���䴴������
		// �� ��ǰcls���ڵ��࣬��������Щ���ԡ�����֪��setter���������֡�
		// �� ÿ��������ʲô���͵ġ�����֪��Ҫ����rs.getInt����rs.getString ...
		Field[] fs = cls.getDeclaredFields();// ��ȡcls������ж�������ԡ�
		for (Field f : fs) {
			String fieldname = f.getName();// ���Ե����֡�
			Class<?> type = f.getType();// ���Ե����͵�class���� int -- int.class
										// String --String.class
			String setter = "set" + fieldname.substring(0, 1).toUpperCase()
					+ fieldname.substring(1, fieldname.length());
			Method settermethod = cls.getMethod(setter, type);
			Object value =  null;
			if (settermethod == null)
				continue;// ���������������������
		    if(type == String.class) {
				value =  rs.getString( fieldname );
			}else if ( type == int.class || type == Integer.class ) {
				value =  rs.getInt( fieldname );
			}else if ( type == long.class || type == Long.class) {
				value =  rs.getLong( fieldname );
			}else if(  type == float.class || type == Float.class) {
				value =  rs.getFloat( fieldname );
			}else if(type == Date.class) {
				value =  rs.getDate( fieldname );
				
			}else if(type == List.class ) {
				
			}else if(type == Map.class) {
				
			}else {
				
			}
			settermethod.invoke(obj, value);
		}
		return obj;
	}
}

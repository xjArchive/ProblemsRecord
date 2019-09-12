package com.problem.web;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.JSONObject;
import com.problem.ano.Param;
import com.problem.ano.RequestMapping;
import com.problem.ano.ResponseBody;
import com.problem.ano.URLMapping;
 
 
public abstract class DispatcherServlet implements Servlet {
	private ServletConfig cfg;
	private Map<String,URLMapping> mapping = new HashMap<String,URLMapping>();
	public void init(ServletConfig cfg) throws ServletException {
		this.cfg = cfg;
		Method [] ms = this.getClass().getDeclaredMethods();
		for(Method m : ms) {
			RequestMapping rqm = m.getAnnotation( RequestMapping.class );
			if(rqm != null) {
				URLMapping urlmp = new URLMapping();
				urlmp.setInvokeMethod(m);
				//˵���������һ�������RequestMappingע�⡣
				String url = rqm.value();//ע���е��ַ���������key��
				url = trim(url);
				urlmp.setValue(url);
				urlmp.setMethod(rqm.method());
				mapping.put(url, urlmp);
			}
		}
		
	}

	//ȥ��ǰ��б�ܡ�
	private static String trim(String url) {
		StringBuilder sb = new StringBuilder(url);
		while(sb.charAt(0)=='/' || sb.charAt(0) =='\\') {
			 sb.delete(0, 1);
		}
		while(sb.charAt(sb.length()-1)=='/' || sb.charAt(sb.length()-1) =='\\') {
			 sb.delete(sb.length()-1, sb.length());
		}
		return sb.toString();
	}
	
	
	@Override
	public ServletConfig getServletConfig() {
		return cfg;
	}

	@Override
	public void service(ServletRequest rq, ServletResponse rp) throws ServletException, IOException {
		try {
			dispatch((HttpServletRequest)rq, (HttpServletResponse)rp);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getServletInfo() {
		return cfg.getServletName();
	}

	@Override
	public void destroy() {}
	 
	private void dispatch(HttpServletRequest rq, HttpServletResponse rp) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		String servletpath = rq.getServletPath();
		String uri = rq.getRequestURI();//��ȡURI��   
		int index = uri.indexOf( servletpath );
		index +=(servletpath.length() +1 );
		String suffix = uri.substring(index, uri.length());
		///test1/hello.action 
		//ȥ��/test1������   hello.action 
		//System.out.println("suffix="+suffix);
		URLMapping urlm = mapping.get(suffix);
		if( urlm != null ) {
				Method method = urlm.getInvokeMethod();
			    if("*".equals( urlm.getMethod() )) {//Ĭ�϶����󷽷�����Ҫ��
			    	invokeMethod(method,rq,rp);
			    }else if(rq.getMethod().equalsIgnoreCase(urlm.getMethod() )){//���󷽷�ƥ���ϡ�
			    	invokeMethod(method,rq,rp);
			    }else {
			    	methodNotAllow(rq, rp);
			    }
		}else {
			notFound(rq,rp);
		}
	}
	
	private void invokeMethod(Method method, HttpServletRequest rq, HttpServletResponse rp) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		//1 ������䡣
		Object [] parameter_values = fillParameter(method,rq,rp); 
		//System.out.println(Arrays.toString(parameter_values));
		//2 ���÷�����
		Object rs = method.invoke(this, parameter_values);
		//3 ���ؽ����
		ResponseBody resbody = null;
		if( (resbody=method.getAnnotation(ResponseBody.class) ) != null ) {
			//3.1 ����JSON��
			returnJSONObject( rs,resbody.charset() ,rp );
		}else if(rs != null && rs instanceof String){ //û��ResponseBodyע�⡣
			//3ת�������ض���
			String url = rs.toString();
			 if( isRedirect ( url )) {
				 //���ض���
				 redirect(url,rq, rp);
			 }else {
				 //��ת����
				 requestDispather(url,rq,rp);
			 }
		}
		
	}
	 
	private void requestDispather(String url, HttpServletRequest rq, HttpServletResponse rp) {
		 try {
				rq.getRequestDispatcher(url).forward(rq, rp);
			} catch (ServletException | IOException e) {
				e.printStackTrace();
			}
	}

	private void redirect(String redirecturl,HttpServletRequest rq,HttpServletResponse rp) {
		try {
			rp.setStatus(302);
			rp.sendRedirect(redirecturl.replaceAll("redirect:", rq.getServletContext().getContextPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private  boolean isRedirect(String url) {
		return url.startsWith("redirect:");
	}
	//����JSON��
	private void returnJSONObject(Object result ,String charset, HttpServletResponse rp) {
		//����alibaba��JSON��ܡ�
		String jsonstr = JSONObject.toJSONString(result);
		try {
			byte[] bs = jsonstr.getBytes( charset );
			rp.setContentLength(  bs.length );
			rp.setCharacterEncoding(charset);
			rp.setContentType("application/json");
			rp.getOutputStream().write(bs);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * ������䡣
	 * ����ҽ�������Ϊ���������͡��������͡�javaBean��List��Map��String������ ��6���ࡣ
	 * �������ͣ�request,response,session,context����
	 * �������ͣ� 8��������� + ��װ���Ͷ��㡣
	 * List :  List<?>���͡� 
	 * Map :   Map<?,?>���͡�
	 * String �� �ַ������͡�
	 * javaBean �� ����@Paramע�Ⲣ��isjavabean����Ϊtrue�Ĳ������˲�����Ҫ���뷴�䣬����HTTP�����󶨡�
	 * �������ͣ� Date �ȡ�
	 * 
	 * 
	 * @param method
	 * @param rq
	 * @param rp
	 * @return
	 */
	private Object[] fillParameter(Method method,HttpServletRequest rq,HttpServletResponse rp) {
		Object []parameter_values = new Object[method.getParameterCount()];//����ֵ��
		Parameter[] params = method.getParameters();//��������
		for(int i=0; i< params.length;i++) {
			Parameter param =  params[i];
			//�������͸�ֵ��
			Class type = param.getType();
			Object value = null;
			//�������͡�
			boolean next_flag = false;
			if(  ServletRequest.class.isAssignableFrom( type ) ||  HttpServletRequest.class.isAssignableFrom( type )) {
				value = rq;
				next_flag = true;
			}else if(  ServletResponse.class.isAssignableFrom( type ) ||  HttpServletResponse.class.isAssignableFrom( type )) {
				value = rp;
				next_flag = true;
			}else if(HttpSession.class.isAssignableFrom( type ) ) {
				value = rq.getSession();
				next_flag = true;
			}else if(ServletContext.class.isAssignableFrom( type ) ) {
				value = rq.getServletContext();
				next_flag = true;
			} 
			//������������͡�����
		
			if(next_flag) {
				parameter_values[i] = value;
				continue;
			}
			
			//���������ע�⣬����ʹ�ò�����ע��ָ�������֡�
			Param pram_ano = param.getAnnotation( Param.class );
			String parameter_name = (pram_ano ==null ? param.getName() :   pram_ano.value() );//�Ƿ��ǲ�������
			value = rq.getParameter(parameter_name);//value�п���Ϊnull��

			if( pram_ano != null && pram_ano.isjavabean() ) {
				//��һ��ʵ���������Ҫ��rq�л�ȡ����������ʵ������С�
				try {
					value = invoteSetter(type, rq);
				}catch (Exception e) {
					e.printStackTrace();
				}
			}else  if ( type == int.class || type == Integer.class ) {
				if( isNotEmpty(value) )
				value =  Integer.parseInt( value +"" );
			}else if ( type == long.class || type == Long.class) {
				if( isNotEmpty(value) )
				value = Long.parseLong(value+"");
			}else if(  type == float.class || type == Float.class) {
				if( isNotEmpty(value) )
				value = Float.parseFloat( value +"");
			}else if(  type == boolean.class || type == Boolean.class) {
				if( isNotEmpty(value) )
				value = Boolean.parseBoolean(value+"");
			}else if(type == Date.class) {
					
			}else if(type == List.class ) {
				
			}else if(type == Map.class) {
				
			}
			parameter_values[i] = value;
		}
		return parameter_values;
	}
 
	private boolean isNotEmpty(Object str) {
		return !isEmpty(str);
	}
	private boolean isEmpty(Object str) {
		return str ==null || str.equals("");
	}
	private void notFound(HttpServletRequest rq, HttpServletResponse rp) {
		rp.setStatus(404);
		rp.setContentType("text/html;charset=UTF-8");
		try {
			rp.getWriter().append("��Ҫ�ҵ���Դ������Ŷ~����~");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void methodNotAllow(HttpServletRequest rq, HttpServletResponse rp) {
		rp.setStatus(404);
		rp.setContentType("text/html;charset=UTF-8");
		try {
			rp.getWriter().append("���󷽷���������ǰ���󷽷�Ϊ��"+rq.getMethod());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Object invoteSetter(Class cls,HttpServletRequest rq,int ...times) throws Exception {
		Object obj = cls.newInstance();// ���䴴������
		// �� ��ǰcls���ڵ��࣬��������Щ���ԡ�����֪��setter���������֡�
		// �� ÿ��������ʲô���͵ġ�����֪��Ҫ����rs.getInt����rs.getString ...
		Method []ms = cls.getMethods();
		for (Method m : ms) {
			
			if(!isSetterMethod(m))continue;
			String fieldname = getFieldName(m);// ���Ե����֡�
			Class<?> type = getFieldType(m);
			Method settermethod = m;
			Object value = rq.getParameter(fieldname);//��������Ϊ��������
			if ( type == int.class || type == Integer.class ) {
				if(isNotEmpty(value))
				value =  Integer.parseInt( value +"" );
			}else if ( type == long.class || type == Long.class) {
				if(isNotEmpty(value))
				value = Long.parseLong(value+"");
			}else if(  type == float.class || type == Float.class) {
				if(isNotEmpty(value))
				value = Float.parseFloat( value +"");
			}else if(  type == boolean.class || type == Boolean.class) {
				if(isNotEmpty(value))
				value = Boolean.parseBoolean(value+"");
			}else if(type == Date.class) {
				
			}else if(type == List.class ) {
				//���������
			}else if(type == Map.class) {
				//���������
			}else if(type == String.class){
				
			}else {
				if(times ==null || times.length == 0)times = new int[]{0};//��ֹ���������͵ݹ����
				if(times[0] >10) {
					throw new RuntimeException(type+"  ����δ��������¿�� ");
				}
				times[0]++;
				value = invoteSetter(type, rq,times);
			}
			if(isNotEmpty(value))
			settermethod.invoke(obj, value);
		}
		return obj;
	}
	
	public static boolean isSetterMethod(Method settermethod) {
		return settermethod.getName().startsWith("set") && 
			   settermethod.getName().charAt(3)>='A' && 
			   settermethod.getName().charAt(3)<='Z' &&
			   settermethod.getParameterCount() ==1;
	}
	
	public static Class getFieldType(Method settermethod) {
		return settermethod.getParameterTypes()[0];
	}
	
	public static String getFieldName(Method settermethod) {
		String name = settermethod.getName();
		name = name.substring(3,name.length());
		name = name.substring(0, 1).toLowerCase() + name.substring(1, name.length());
		return name;
	}
	 
	 
}

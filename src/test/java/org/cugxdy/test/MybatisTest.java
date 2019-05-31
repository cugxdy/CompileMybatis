package org.cugxdy.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.ibatis.binding.MapperProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.cugxdy.bean.Blog;
import org.cugxdy.bean.User;
import org.cugxdy.interfaces.UserOperation;
import org.junit.Test;

public class MybatisTest {

	@Test
	public void  test() throws IOException {
		  
		String resource = "org/zcugxdy/file/MybatisConfig.xml";
		
	   	InputStream inputStream = Resources.getResourceAsStream(resource);
	   	
	   	
	   	// InputStream inputStream1 = MybatisTest.class.getClassLoader().getResourceAsStream(resource);

	   	SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
	  	SqlSession session = sqlSessionFactory.openSession();
		try {
			
			// User blog = (User) session.selectOne("org.cugxdy.interfaces.UserOperation.getUser", 12);
			MapperProxyFactory<UserOperation> test = new MapperProxyFactory<UserOperation>(UserOperation.class);
			Map<Object,Object> params = new HashMap<Object, Object>();
			
			params.put("id", 12);
			UserOperation operation = test.newInstance(session);
			List<?> blog = operation.getUser(params);
			
			Iterator<?> itor = blog.iterator();
			while(itor.hasNext()) {
				Object obj = itor.next();
				if(obj instanceof Map) {
					@SuppressWarnings("unchecked")
					Set<Entry<Object,Object>> entrySet = ((Map<Object, Object>) obj).entrySet();
					for(Entry<Object,Object> entry : entrySet) {
						System.out.println("Key = " + entry.getKey() + " value = " + entry.getValue());
					}
				}
			}
			System.out.println(blog);
		} finally {
	  		session.close();
		}
	}
}

package org.cugxdy.xmlParser;

import java.io.IOException;
import java.io.InputStream;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

public class XmlParser {

	
	@Test
	public void test() throws IOException {
		String resource = "org/zcugxdy/file/MybatisConfig.xml";
		
	   	InputStream inputStream = Resources.getResourceAsStream(resource);

	   	// InputStream inputStream1 = MybatisTest.class.getClassLoader().getResourceAsStream(resource);

	   	SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
	}
}

package org.cugxdy.xmlParser;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.builder.xml.XMLMapperEntityResolver;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.parsing.XPathParser;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XmlParserTest {
	
	private Document document;
	
	
	@Test
	public void test() throws IOException, ParserConfigurationException, XPathExpressionException {
		String resource = "org/zcugxdy/file/MybatisConfig.xml";
		
	   	InputStream inputStream = Resources.getResourceAsStream(resource);
//		
//		
//	   	XMLConfigBuilder bulider = new XMLConfigBuilder(inputStream);
//	   	
//	   	XPathParser pather = new XPathParser(inputStream, true, null, new XMLMapperEntityResolver());
//	   	
		this.document = createDocument(new InputSource(inputStream));
		
		XPathFactory facorty = XPathFactory.newInstance();
		
		XPath path = facorty.newXPath();
		
		Node node = (Node) path.evaluate("/configuration", this.document, XPathConstants.NODE);
		
		System.out.println("Node = " + node.getNodeName());
		Node node1 = (Node) path.evaluate("environments", node, XPathConstants.NODE);
		
		NamedNodeMap nameMap = node1.getAttributes();

		for(int i =0 ; i< nameMap.getLength() ;i++) {
			Node name = nameMap.item(i);
			System.out.println("Key = " + name.getNodeName() + " value = " + name.getNodeValue());
		}
		
		NodeList list =  node.getChildNodes();
		
		for(int j =0 ; j< list.getLength() ;j++) {
			System.out.println("NodeName = "  + list.item(j).getNodeName());
			System.out.println("NodeType = "  + list.item(j).getNodeType());
		}
	}


	private Document createDocument(InputSource inputSource) throws ParserConfigurationException {
		// TODO Auto-generated method stub
		
		System.out.println("创建Document对象");
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		
	      factory.setValidating(false);// 是否验证xml文件，这个验证是DTD验证，默认为false
	      
	      // xml解析器的相关配置设置(配置相关信息)(域名信息)
	      factory.setNamespaceAware(false);
	      factory.setIgnoringComments(true);
	      factory.setIgnoringElementContentWhitespace(false);
	      factory.setCoalescing(false);
	      factory.setExpandEntityReferences(true);
		
	      DocumentBuilder bulider = factory.newDocumentBuilder();
		
	      try {
			return bulider.parse(inputSource);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      
		return null;
	}
}

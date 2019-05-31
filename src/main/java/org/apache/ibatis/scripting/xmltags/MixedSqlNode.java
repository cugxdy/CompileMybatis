/**
 *    Copyright 2009-2017 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.scripting.xmltags;

import java.util.List;

/**
 * @author Clinton Begin
 */
public class MixedSqlNode implements SqlNode {
  private final List<SqlNode> contents;

  public MixedSqlNode(List<SqlNode> contents) {
    this.contents = contents;
  }

  @Override
  public boolean apply(DynamicContext context) {
	System.out.println("=========================MixedSqlNode=======================");
	System.out.println("MixedSqlNode = " + getClass().getName());
	System.out.println("length = " + contents.size());
	for(int i = 0 ;i < contents.size() ;i++) {
		System.out.println("Class = " + contents.get(i).getClass().getName());
		String className = contents.get(i).getClass().getName();
		if(className.substring(className.lastIndexOf(".")+1).equals("IfSqlNode")) {
			System.out.println("IfSqlNode:test = " + ((IfSqlNode)contents.get(i)).getText());
		}else if(className.substring(className.lastIndexOf(".")+1).equals("StaticTextSqlNode")){
			System.out.println("StaticTextSqlNode:text = " + ((StaticTextSqlNode)contents.get(i)).getText());
		}
	}
    for (SqlNode sqlNode : contents) {
      sqlNode.apply(context);
    }
    
    return true;
  }
}

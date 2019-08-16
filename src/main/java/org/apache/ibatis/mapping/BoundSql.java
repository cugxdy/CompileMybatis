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
package org.apache.ibatis.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.session.Configuration;

/**
 * An actual SQL String got from an {@link SqlSource} after having processed any dynamic content.
 * The SQL may have SQL placeholders "?" and an list (ordered) of an parameter mappings 
 * with the additional information for each parameter (at least the property name of the input object to read 
 * the value from). 
 * </br>
 * Can also have additional parameters that are created by the dynamic language (for loops, bind...).
 *
 * @author Clinton Begin
 */
// 实际可执行Sql的封装,它其中sql、传入参数,以及映射规则
public class BoundSql {
	
  // 该字段中记录了SQL语句，该SQL语句可能含有"?"占位符
  private final String sql;
  
  // SQL的参数属性集合，ParameterMapping的集合
  private final List<ParameterMapping> parameterMappings;
  
  // 客户端执行SQL时传入的实际参数
  private final Object parameterObject;
  
  // 空的HashMap集合，之后会复制DynamicContext.bindings集合中的内容
  private final Map<String, Object> additionalParameters;
  
  // additionalParameters集合对应的MetaObject对象
  private final MetaObject metaParameters;

  public BoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings, Object parameterObject) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.parameterObject = parameterObject;
    this.additionalParameters = new HashMap<String, Object>();
    this.metaParameters = configuration.newMetaObject(additionalParameters);
  }

  // 返回可执行SQL语句
  public String getSql() {
    return sql;
  }

  // 返回#{}解析对象
  public List<ParameterMapping> getParameterMappings() {
    return parameterMappings;
  }

  // 返回参数对象
  public Object getParameterObject() {
    return parameterObject;
  }

  // 判断additionalParameters是否存在某个键
  public boolean hasAdditionalParameter(String name) {
    String paramName = new PropertyTokenizer(name).getName();
    return additionalParameters.containsKey(paramName);
  }

  // 将Context中bings = Map对象中key-value键值对存入metaParameters对象中
  public void setAdditionalParameter(String name, Object value) {
    metaParameters.setValue(name, value);
  }

  // 从metaParameters中获取指定name值
  public Object getAdditionalParameter(String name) {
    return metaParameters.getValue(name);
  }
}

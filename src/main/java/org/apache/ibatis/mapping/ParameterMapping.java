/**
 *    Copyright 2009-2016 the original author or authors.
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

import java.sql.ResultSet;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
// 它是用于存储#{property,javaType=int}解析成的数据信息
public class ParameterMapping {

  private Configuration configuration;

  private String property;  // 传入进来的name
  
  private ParameterMode mode;// 输入参数还是输出参数
  
  private Class<?> javaType = Object.class; // 参数java类型
  
  private JdbcType jdbcType;// 参数jdbc类型
  
  private Integer numericScale;// 浮点参数的精度
  
  private TypeHandler<?> typeHandler;// 参数对应的TypeHandler对象
  
  private String resultMapId;// 参数对应的ResultMap的Id
  
  private String jdbcTypeName;// 参数的jdbcTypeName属性
  
  private String expression;// 目前不支持该属性

  private ParameterMapping() {
  }

  public static class Builder {
    private ParameterMapping parameterMapping = new ParameterMapping();

    // 创建Builder对象
    public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
      parameterMapping.configuration = configuration;
      parameterMapping.property = property;
      parameterMapping.typeHandler = typeHandler;
      parameterMapping.mode = ParameterMode.IN;// 默认为IN模式
    }

    // 创建Builder对象
    public Builder(Configuration configuration, String property, Class<?> javaType) {
      parameterMapping.configuration = configuration;
      parameterMapping.property = property;
      parameterMapping.javaType = javaType;
      parameterMapping.mode = ParameterMode.IN;// 默认为IN模式
    }

    // 设置参数模式
    public Builder mode(ParameterMode mode) {
      parameterMapping.mode = mode;
      return this;
    }

    // 设置java类型
    public Builder javaType(Class<?> javaType) {
      parameterMapping.javaType = javaType;
      return this;
    }

    // 设置jdbc类型
    public Builder jdbcType(JdbcType jdbcType) {
      parameterMapping.jdbcType = jdbcType;
      return this;
    }

    public Builder numericScale(Integer numericScale) {
      parameterMapping.numericScale = numericScale;
      return this;
    }

    // 设置嵌套查询ID
    public Builder resultMapId(String resultMapId) {
      parameterMapping.resultMapId = resultMapId;
      return this;
    }

    // 设置类型处理器
    public Builder typeHandler(TypeHandler<?> typeHandler) {
      parameterMapping.typeHandler = typeHandler;
      return this;
    }

    // 设置jdbcType名称
    public Builder jdbcTypeName(String jdbcTypeName) {
      parameterMapping.jdbcTypeName = jdbcTypeName;
      return this;
    }

    // 设置expression属性值
    public Builder expression(String expression) {
      parameterMapping.expression = expression;
      return this;
    }

    public ParameterMapping build() {
      resolveTypeHandler();
      validate();
      return parameterMapping;
    }

    // 检验合法性
    private void validate() {
      if (ResultSet.class.equals(parameterMapping.javaType)) {
        if (parameterMapping.resultMapId == null) { 
          throw new IllegalStateException("Missing resultmap in property '"  
              + parameterMapping.property + "'.  " 
              + "Parameters of type java.sql.ResultSet require a resultmap.");
        }            
      } else {
        if (parameterMapping.typeHandler == null) { 
          throw new IllegalStateException("Type handler was null on parameter mapping for property '"
            + parameterMapping.property + "'. It was either not specified and/or could not be found for the javaType ("
            + parameterMapping.javaType.getName() + ") : jdbcType (" + parameterMapping.jdbcType + ") combination.");
        }
      }
    }

    // 解析类型处理器(获取类型处理器)
    private void resolveTypeHandler() {
      if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
        Configuration configuration = parameterMapping.configuration;
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
      }
    }

  }

  // 获取property属性值
  public String getProperty() {
    return property;
  }

  /**
   * Used for handling output of callable statements
   * @return
   */
  // 获取参数模式
  public ParameterMode getMode() {
    return mode;
  }

  /**
   * Used for handling output of callable statements
   * @return
   */
  // 获取java类型
  public Class<?> getJavaType() {
    return javaType;
  }

  /**
   * Used in the UnknownTypeHandler in case there is no handler for the property type
   * @return
   */
  // 获取jdbc类型
  public JdbcType getJdbcType() {
    return jdbcType;
  }

  /**
   * Used for handling output of callable statements
   * @return
   */
  // 浮点数精度
  public Integer getNumericScale() {
    return numericScale;
  }

  /**
   * Used when setting parameters to the PreparedStatement
   * @return
   */
  // 获取类型处理器
  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  /**
   * Used for handling output of callable statements
   * @return
   */
  // 获取嵌套映射ID
  public String getResultMapId() {
    return resultMapId;
  }

  /**
   * Used for handling output of callable statements
   * @return
   */
  // 获取jdbcType名称
  public String getJdbcTypeName() {
    return jdbcTypeName;
  }

  /**
   * Not used
   * @return
   */
  // 获取expression属性值,现在不支持
  public String getExpression() {
    return expression;
  }

  // 返回String对象
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ParameterMapping{");
    //sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
    sb.append("property='").append(property).append('\'');
    sb.append(", mode=").append(mode);
    sb.append(", javaType=").append(javaType);
    sb.append(", jdbcType=").append(jdbcType);
    sb.append(", numericScale=").append(numericScale);
    //sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
    sb.append(", resultMapId='").append(resultMapId).append('\'');
    sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
    sb.append(", expression='").append(expression).append('\'');
    sb.append('}');
    return sb.toString();
  }
}

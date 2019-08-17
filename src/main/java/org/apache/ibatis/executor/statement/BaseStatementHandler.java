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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 */
// 模板模式(它是simple、Prepared、Callable的基类),完成基本方法的设计
public abstract class BaseStatementHandler implements StatementHandler {

  protected final Configuration configuration; // 全局配置对象
  
  protected final ObjectFactory objectFactory;
  // 类型处理器
  protected final TypeHandlerRegistry typeHandlerRegistry;
  
  // 记录使用的ResultSetHandler对象，它的主要功能是将结果集映射成结果对象
  protected final ResultSetHandler resultSetHandler;
  
  // 记录使用的ParameterHandler对象，ParameterHandler的主要功能是为SQL语句绑定实参，也是就是使用传入的参数
  // 替换SQL语句中的"?"占位符
  protected final ParameterHandler parameterHandler;

  // 记录SQL语句的Executor对象
  protected final Executor executor;
  
  // 记录SQL语句对应的MappedStatement和BoundSql对象
  protected final MappedStatement mappedStatement;
  
  // RowBounds记录用户设置的offset和limit,用于结果集中定位映射的起始位置和结束位置!
  protected final RowBounds rowBounds;

  // 存储可执行的SQL语句
  protected BoundSql boundSql;

  protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    this.configuration = mappedStatement.getConfiguration();
    this.executor = executor;
    this.mappedStatement = mappedStatement;
    this.rowBounds = rowBounds;

    this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    this.objectFactory = configuration.getObjectFactory();

    if (boundSql == null) { // issue #435, get the key before calculating the statement
      // 调用KeyGenerator.processBefore()方法获取主键
      generateKeys(parameterObject);
      boundSql = mappedStatement.getBoundSql(parameterObject);
    }

    // 存入可执行SQL语句
    this.boundSql = boundSql;

    // 初始化ParameterHandler对象
    this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
    // 初始化ResultSetHandler对象
    this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
  }

  @Override// 获取可执行Sql语句
  public BoundSql getBoundSql() {
    return boundSql;
  }

  @Override// 获取参数处理器对象
  public ParameterHandler getParameterHandler() {
    return parameterHandler;
  }

  @Override// 创建Statement对象
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
    ErrorContext.instance().sql(boundSql.getSql());
    Statement statement = null;
    try {
    	// 创建statement对象
      statement = instantiateStatement(connection);
      // 设置等待sql执行时间
      setStatementTimeout(statement, transactionTimeout);
      // 设置FetchSize属性
      setFetchSize(statement);
      return statement;
    } catch (SQLException e) {
      closeStatement(statement);
      throw e;
    } catch (Exception e) {
      closeStatement(statement);
      throw new ExecutorException("Error preparing statement.  Cause: " + e, e);
    }
  }

  // 由子类去实现创建Statement对象
  protected abstract Statement instantiateStatement(Connection connection) throws SQLException;

  protected void setStatementTimeout(Statement stmt, Integer transactionTimeout) throws SQLException {
    
	Integer queryTimeout = null;
    // Timeout属性在这里起作用
    if (mappedStatement.getTimeout() != null) {
      queryTimeout = mappedStatement.getTimeout();
    } else if (configuration.getDefaultStatementTimeout() != null) {
      queryTimeout = configuration.getDefaultStatementTimeout();
    }
    
    if (queryTimeout != null) {
      stmt.setQueryTimeout(queryTimeout);
    }
    
    // Statement设置QueryTimeout = timeToLiveOfQuery
    StatementUtil.applyTransactionTimeout(stmt, queryTimeout, transactionTimeout);
  }

  // FetchSize:获取结果集合的行数,该数是根据此Statement对象生成的 ResultSet对象的默认获取大小。
  protected void setFetchSize(Statement stmt) throws SQLException {
    Integer fetchSize = mappedStatement.getFetchSize();
    if (fetchSize != null) {
      // 设置fetchSize属性
      stmt.setFetchSize(fetchSize);
      return;
    }
    
    Integer defaultFetchSize = configuration.getDefaultFetchSize();
    if (defaultFetchSize != null) {
      stmt.setFetchSize(defaultFetchSize);
    }
  }

  // 关闭Statement对象
  protected void closeStatement(Statement statement) {
    try {
      if (statement != null) {
        statement.close();
      }
    } catch (SQLException e) {
      //ignore
    }
  }

  // 在正式执行SQL语句前执行主键查询
  protected void generateKeys(Object parameter) {
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    ErrorContext.instance().store();
    
    // 调用KeyGenerator.processBefore()方法获取主键
    keyGenerator.processBefore(executor, mappedStatement, null, parameter);
    ErrorContext.instance().recall();
  }

}

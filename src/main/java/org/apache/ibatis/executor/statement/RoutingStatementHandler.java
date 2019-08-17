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
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 */
// 它是委托模式,它是对具体执行对象的把控
public class RoutingStatementHandler implements StatementHandler {

	// 委托具体的实现者对象
  private final StatementHandler delegate; // 底层封装真正的StatementHandler对象

  public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {

	// RoutingStatementHandler的主要功能就是根据MappedStatement的配置，生成一个对应的
	//　StatementHandler对象，并设置到delegate字段中
    switch (ms.getStatementType()) {
      case STATEMENT:
        delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      case PREPARED:
        delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      case CALLABLE:
        delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        break;
      default:
        throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
    }

  }
  
  // RoutingStatementHandler中所有的方法，都是通过调用delegate对象的对应方法实现的
  @Override
  public Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException {
      
	  System.out.println("delegateType = " + delegate.getClass().getName());
	  return delegate.prepare(connection, transactionTimeout);
  }

  @Override// 设置参数
  public void parameterize(Statement statement) throws SQLException {
    delegate.parameterize(statement);
  }

  @Override// 跑批任务
  public void batch(Statement statement) throws SQLException {
    delegate.batch(statement);
  }

  @Override// 更新SQL语句
  public int update(Statement statement) throws SQLException {
    return delegate.update(statement);
  }

  @Override// 查询SQL语句
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    return delegate.<E>query(statement, resultHandler);
  }

  @Override// 查询游标对象
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    return delegate.queryCursor(statement);
  }

  @Override// 获取可执行SQL语句
  public BoundSql getBoundSql() {
    return delegate.getBoundSql();
  }

  @Override// 获取参数处理器
  public ParameterHandler getParameterHandler() {
    return delegate.getParameterHandler();
  }
}

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
package org.apache.ibatis.executor;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

/**
 * @author Clinton Begin
 */
// 它是最基本的SimpleExecutor执行器
public class SimpleExecutor extends BaseExecutor {

  // 创建SimpleExecutor对象
  public SimpleExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override// 更新SQL语句
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override// 查询SQL语句
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    Statement stmt = null;
    try {
      // MappedStatement会存放对Configuration配置的引用
      Configuration configuration = ms.getConfiguration(); // 获取配置对象
      
      // 创建StatementHandler对象，实际返回的是RoutingStatementHandler对象，
      // 其中根据MappedStatement.statementType选择具体的StatementHandler实现
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      
      // 完成Statement的创建与初始化，该方法首先会调用StatementHandler.prepare()方法创建Statement对象
      // ，然后调用StatementHandler.parameterize()方法处理占位符
      // 完成一些属性上的配置
      stmt = prepareStatement(handler, ms.getStatementLog());
      
      
      // 调用StatementHandler.query()方法，执行查询SQL语句，并通过ResultHandler完成结果集映射
      return handler.<E>query(stmt, resultHandler);
    } finally {
      closeStatement(stmt); // 关闭Statement对象
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    return handler.<E>queryCursor(stmt);
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
    return Collections.emptyList();
  }

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt; 
    // 获取数据库的连接对象
    System.out.println("statementLog = " + statementLog.toString());
    Connection connection = getConnection(statementLog);  
    System.out.println("StatementHandlerType = " + handler.getClass().getName());
    
    stmt = handler.prepare(connection, transaction.getTimeout()); // 创建Statement对象
    handler.parameterize(stmt); // 处理占位符
    return stmt; 
  }

}

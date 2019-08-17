/**
 *    Copyright 2009-2015 the original author or authors.
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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.keygen.SelectKeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 */
// simpleStatement对象,它用于调用select count(*) from user类型SQL语句
public class SimpleStatementHandler extends BaseStatementHandler {

  public SimpleStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
  }

  @Override// 返回因update操作而受影响的行数
  public int update(Statement statement) throws SQLException {
    
	String sql = boundSql.getSql(); // 获取SQL语句
    Object parameterObject = boundSql.getParameterObject(); // 获取用户传入的实参
    
    // 获取配置的KeyGenerator对象(SelectKeyGenerator对象)
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    
    int rows;
    if (keyGenerator instanceof Jdbc3KeyGenerator) {
      statement.execute(sql, Statement.RETURN_GENERATED_KEYS); // 执行SQL语句
      rows = statement.getUpdateCount(); // 获取受影响的行数
      // 将数据库生成的主键添加parameterObject中
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
    } else if (keyGenerator instanceof SelectKeyGenerator) {
      statement.execute(sql);// 执行SQL语句
      rows = statement.getUpdateCount();// 获取受影响的行数
      // 执行<selectKey>节点中配置的SQL语句获取数据库生成的主键，并添加到parameterObject中
      keyGenerator.processAfter(executor, mappedStatement, statement, parameterObject);
    } else {
      statement.execute(sql);
      rows = statement.getUpdateCount();
    }
    return rows;
  }

  @Override// 批量执行(将sql存储起来,特定时间在触发执行)
  public void batch(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    statement.addBatch(sql);
  }

  @Override
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    String sql = boundSql.getSql();  // 获取SQL语句
    statement.execute(sql);// 调用Statement.execute(sql)方法执行SQL语句
    return resultSetHandler.<E>handleResultSets(statement); // 映射结果集
  }

  @Override// 查询游标对象
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    String sql = boundSql.getSql();
    statement.execute(sql);
    return resultSetHandler.<E>handleCursorResultSets(statement);
  }

  @Override// 创建Statement对象
  protected Statement instantiateStatement(Connection connection) throws SQLException {
    // 设置结果集是否可以滚动及其游标是否可以上下移动，设置结果是否可更新
	if (mappedStatement.getResultSetType() != null) {
	  // ResultSet设置属性在这里起作用
      return connection.createStatement(mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
    } else {
      return connection.createStatement();
    }
  }

  @Override
  public void parameterize(Statement statement) throws SQLException {
    // N/A
  }

}

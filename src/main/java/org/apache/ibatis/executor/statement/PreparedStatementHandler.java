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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @author Clinton Begin
 */
// 它是Mybatis中默认的stateHandler处理器,完成对SQL('?')参数的设置
public class PreparedStatementHandler extends BaseStatementHandler {

  public PreparedStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
    // --> BaseStatementHandler
	super(executor, mappedStatement, parameter, rowBounds, resultHandler, boundSql);
  }

  
  @Override // 执行更新数据库操作
  public int update(Statement statement) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute();
    int rows = ps.getUpdateCount();
    // 获取调用MyBatis系统时的输入参数对象
    Object parameterObject = boundSql.getParameterObject();
    // 获取KeyGenerator(它通常是用于数据库主键的获取)
    KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
    
    // 自增主键的设计
    keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
    return rows;
  }

  @Override // 批量执行sql语句(即是先将SQL语句存起来,特定时间一起执行)
  public void batch(Statement statement) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.addBatch();
  }

  
  @Override// 查询Sql语句,并进行结果集
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute();
    System.out.println("=====================开始准备结果集映射===================================");
    return resultSetHandler.<E> handleResultSets(ps);
  }

  @Override// 查询游标对象,并进行游标结果集处理
  public <E> Cursor<E> queryCursor(Statement statement) throws SQLException {
    PreparedStatement ps = (PreparedStatement) statement;
    ps.execute();
    // 处理游标结果集
    return resultSetHandler.<E> handleCursorResultSets(ps);
  }

  // 它是根据KeyGenerator与ResultSets来创建Statement对象
  @Override// 根据数据库连接,去创建Statement对象执行SQL语句
  protected Statement instantiateStatement(Connection connection) throws SQLException {
    
	  String sql = boundSql.getSql();  // 获取待执行的SQL语句
    // 根据mappedStatement.getKeyGenerator()字段的值，创建PreparedStatement对象
    if (mappedStatement.getKeyGenerator() instanceof Jdbc3KeyGenerator) {
      String[] keyColumnNames = mappedStatement.getKeyColumns();
      if (keyColumnNames == null) {
    	// 返回数据库生成的主键
        return connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
      } else {
    	// 在insert语句执行完成之后，会将keyColumnNames指定的列返回
        return connection.prepareStatement(sql, keyColumnNames);
      }
      
    } else if (mappedStatement.getResultSetType() != null) {
      // 设置结果集是否可以滚动以及游标是否可以上下移动，设置结果集是否可更新
      return connection.prepareStatement(sql, mappedStatement.getResultSetType().getValue(), ResultSet.CONCUR_READ_ONLY);
    } else {
      System.out.println("正常sql语句执行到这里-------------------------------------");
      return connection.prepareStatement(sql); // 创建普通的Statement对象
    }
  }

  
  @Override// 为sql语句设置实参(设置可执行的Sql语句)
  public void parameterize(Statement statement) throws SQLException {
	  System.out.println("parameterHandler = " + parameterHandler.getClass().getName());
	  System.out.println("resultSetHandler = " + resultSetHandler.getClass().getName());
    parameterHandler.setParameters((PreparedStatement) statement);
  }

}

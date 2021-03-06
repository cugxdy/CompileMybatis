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
package org.apache.ibatis.session;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.reflection.ExceptionUtil;

/**
 * @author Larry Meadors
 */
// 为SqlSessionFactory创建代理对象
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

  private final SqlSessionFactory sqlSessionFactory; // 底层封装的SqlSessionFactory对象
  
  // localSqlSession中记录的SqlSession对象的代理对象，在SqlSessionManager初始化时，
  // 会使用JDK动态代理的方式为localSqlSession创建代理对象
  /*
   * SqlSessionManager通过localSqlSession这个ThreadLocal变量，记录与当前线程绑定的SqlSession，供当前
   * 线程循环使用，从而避免在同一线程多次创建SqlSession对象带来的性能损失
   */
  private final SqlSession sqlSessionProxy;// 

  // ThreadLocal变量，记录一个一个与当前线程绑定的SqlSession对象
  private final ThreadLocal<SqlSession> localSqlSession = new ThreadLocal<SqlSession>();

  private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    // 使用动态代理的方式生成sqlSession的代理对象
    this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSession.class},
        new SqlSessionInterceptor());
  }
  
  // 通过newInstance()方法创建SqlSessionManager对象
  public static SqlSessionManager newInstance(Reader reader) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, null));
  }

  public static SqlSessionManager newInstance(Reader reader, String environment) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, environment, null));
  }

  public static SqlSessionManager newInstance(Reader reader, Properties properties) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(reader, null, properties));
  }

  public static SqlSessionManager newInstance(InputStream inputStream) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, null));
  }

  public static SqlSessionManager newInstance(InputStream inputStream, String environment) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, environment, null));
  }

  public static SqlSessionManager newInstance(InputStream inputStream, Properties properties) {
    return new SqlSessionManager(new SqlSessionFactoryBuilder().build(inputStream, null, properties));
  }

  public static SqlSessionManager newInstance(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionManager(sqlSessionFactory);
  }

  public void startManagedSession() {
    this.localSqlSession.set(openSession());
  }

  public void startManagedSession(boolean autoCommit) {
    this.localSqlSession.set(openSession(autoCommit));
  }

  public void startManagedSession(Connection connection) {
    this.localSqlSession.set(openSession(connection));
  }

  public void startManagedSession(TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(level));
  }

  public void startManagedSession(ExecutorType execType) {
    this.localSqlSession.set(openSession(execType));
  }

  public void startManagedSession(ExecutorType execType, boolean autoCommit) {
    this.localSqlSession.set(openSession(execType, autoCommit));
  }

  public void startManagedSession(ExecutorType execType, TransactionIsolationLevel level) {
    this.localSqlSession.set(openSession(execType, level));
  }

  public void startManagedSession(ExecutorType execType, Connection connection) {
    this.localSqlSession.set(openSession(execType, connection));
  }

  public boolean isManagedSessionStarted() {
    return this.localSqlSession.get() != null;
  }

  public SqlSession openSession() {
    return sqlSessionFactory.openSession();
  }

  public SqlSession openSession(boolean autoCommit) {
    return sqlSessionFactory.openSession(autoCommit);
  }

  public SqlSession openSession(Connection connection) {
    return sqlSessionFactory.openSession(connection);
  }

  public SqlSession openSession(TransactionIsolationLevel level) {
    return sqlSessionFactory.openSession(level);
  }

  public SqlSession openSession(ExecutorType execType) {
    return sqlSessionFactory.openSession(execType);
  }

  public SqlSession openSession(ExecutorType execType, boolean autoCommit) {
    return sqlSessionFactory.openSession(execType, autoCommit);
  }

  public SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level) {
    return sqlSessionFactory.openSession(execType, level);
  }

  public SqlSession openSession(ExecutorType execType, Connection connection) {
    return sqlSessionFactory.openSession(execType, connection);
  }

  public Configuration getConfiguration() {
    return sqlSessionFactory.getConfiguration();
  }

  public <T> T selectOne(String statement) {
    return sqlSessionProxy.<T> selectOne(statement);
  }

  public <T> T selectOne(String statement, Object parameter) {
    return sqlSessionProxy.<T> selectOne(statement, parameter);
  }

  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return sqlSessionProxy.<K, V> selectMap(statement, mapKey);
  }

  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return sqlSessionProxy.<K, V> selectMap(statement, parameter, mapKey);
  }

  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    return sqlSessionProxy.<K, V> selectMap(statement, parameter, mapKey, rowBounds);
  }

  public <T> Cursor<T> selectCursor(String statement) {
    return sqlSessionProxy.selectCursor(statement);
  }

  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return sqlSessionProxy.selectCursor(statement, parameter);
  }

  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    return sqlSessionProxy.selectCursor(statement, parameter, rowBounds);
  }

  public <E> List<E> selectList(String statement) {
    return sqlSessionProxy.<E> selectList(statement);
  }

  public <E> List<E> selectList(String statement, Object parameter) {
    return sqlSessionProxy.<E> selectList(statement, parameter);
  }

  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    return sqlSessionProxy.<E> selectList(statement, parameter, rowBounds);
  }

  public void select(String statement, ResultHandler handler) {
    sqlSessionProxy.select(statement, handler);
  }

  public void select(String statement, Object parameter, ResultHandler handler) {
    sqlSessionProxy.select(statement, parameter, handler);
  }

  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    sqlSessionProxy.select(statement, parameter, rowBounds, handler);
  }

  public int insert(String statement) {
    return sqlSessionProxy.insert(statement);
  }

  public int insert(String statement, Object parameter) {
    return sqlSessionProxy.insert(statement, parameter);
  }

  public int update(String statement) {
    return sqlSessionProxy.update(statement);
  }

  public int update(String statement, Object parameter) {
    return sqlSessionProxy.update(statement, parameter);
  }

  public int delete(String statement) {
    return sqlSessionProxy.delete(statement);
  }

  public int delete(String statement, Object parameter) {
    return sqlSessionProxy.delete(statement, parameter);
  }

  public <T> T getMapper(Class<T> type) {
    return getConfiguration().getMapper(type, this);
  }

  public Connection getConnection() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot get connection.  No managed session is started.");
    }
    return sqlSession.getConnection();
  }

  public void clearCache() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot clear the cache.  No managed session is started.");
    }
    sqlSession.clearCache();
  }

  public void commit() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit();
  }

  public void commit(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot commit.  No managed session is started.");
    }
    sqlSession.commit(force);
  }

  public void rollback() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback();
  }

  public void rollback(boolean force) {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    sqlSession.rollback(force);
  }

  public List<BatchResult> flushStatements() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot rollback.  No managed session is started.");
    }
    return sqlSession.flushStatements();
  }

  public void close() {
    final SqlSession sqlSession = localSqlSession.get();
    if (sqlSession == null) {
      throw new SqlSessionException("Error:  Cannot close.  No managed session is started.");
    }
    try {
      sqlSession.close();
    } finally {
      localSqlSession.set(null);
    }
  }

  private class SqlSessionInterceptor implements InvocationHandler {
    public SqlSessionInterceptor() {
        // Prevent Synthetic Access
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      // 获取当前线程绑定的Sqlsession对象
      final SqlSession sqlSession = SqlSessionManager.this.localSqlSession.get();
      if (sqlSession != null) { // 第二种模式
        try {
          // 调用真正的SqlSession对象，完成数据库的相关操作
          return method.invoke(sqlSession, args);
        } catch (Throwable t) {
          throw ExceptionUtil.unwrapThrowable(t);
        }
      } else {
    	// 如果当前线程为绑定SqlSession对象，则创建新的SqlSession对象
        final SqlSession autoSqlSession = openSession();
        try {
          // 通过新建的SqlSession对象完成数据库操作
          final Object result = method.invoke(autoSqlSession, args);
          autoSqlSession.commit();// 提交事务
          return result;
        } catch (Throwable t) {
          autoSqlSession.rollback(); // 回滚事务
          throw ExceptionUtil.unwrapThrowable(t);
        } finally {
          autoSqlSession.close(); // 关闭上面创建的SqlSession对象
        }
      }
    }
  }

}

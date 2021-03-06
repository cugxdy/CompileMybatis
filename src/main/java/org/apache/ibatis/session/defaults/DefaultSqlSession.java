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
package org.apache.ibatis.session.defaults;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.exceptions.ExceptionFactory;
import org.apache.ibatis.exceptions.TooManyResultsException;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.result.DefaultMapResultHandler;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;

/**
 *
 * The default implementation for {@link SqlSession}.
 * Note that this class is not Thread-Safe.
 *
 * @author Clinton Begin
 */
public class DefaultSqlSession implements SqlSession {

  private final Configuration configuration;   // Configuration配置对象
  private final Executor executor; // 底层依赖的Executor对象

  private final boolean autoCommit;  // 是否自动提交事务
  
  private boolean dirty; // 当前缓存中是否有脏数据
  
  // 为防止用户忘记关闭打开的游标对象，会通过cursorList字段记录由该SqlSession对象生成的游标对象
  // 在DefaultSqlSession.close()方法中统一关闭这些游标对象
  private List<Cursor<?>> cursorList;

  // 创建DefaultSqlSession对象
  public DefaultSqlSession(Configuration configuration, Executor executor, boolean autoCommit) {
    this.configuration = configuration;
    this.executor = executor;
    this.dirty = false;
    // 判断是否执行自动提交
    this.autoCommit = autoCommit;
  }

  public DefaultSqlSession(Configuration configuration, Executor executor) {
    this(configuration, executor, false);
  }

  // 查询并处理第一个结果集行
  public <T> T selectOne(String statement) {
    return this.<T>selectOne(statement, null);
  }
  
/*  String resource = "org/mybatis/example/mybatis-config.xml";
   	InputStream inputStream = Resources.getResourceAsStream(resource);
  	sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
  	SqlSession session = sqlSessionFactory.openSession();
	try {
  		Blog blog = (Blog) session.selectOne("org.mybatis.example.BlogMapper.selectBlog", 101);
	} finally {
  		session.close();
	}
*/
  // 查询并处理第一个结果集行
  public <T> T selectOne(String statement, Object parameter) {
    // Popular vote was to return null on 0 results and throw exception on too many.
    List<T> list = this.<T>selectList(statement, parameter); // 调用重载方法执行查询数据
    if(list != null) {
        Iterator<T> itor = list.iterator();
        while(itor.hasNext()) {
        	T obj = itor.next();
        	System.out.println(obj);
        }
    }
    // 获取第一个就行了
    if (list.size() == 1) {
      return list.get(0);
    } else if (list.size() > 1) {
      throw new TooManyResultsException("Expected one result (or null) to be returned by selectOne(), but found: " + list.size());
    } else {
      return null;
    }
  }

  // 返回Map对象(不存在参数)
  public <K, V> Map<K, V> selectMap(String statement, String mapKey) {
    return this.selectMap(statement, null, mapKey, RowBounds.DEFAULT);
  }

  // 返回Map对象(存在parameter参数)
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey) {
    return this.selectMap(statement, parameter, mapKey, RowBounds.DEFAULT);
  }

  // 返回Map对象(并利用DefaultMapResultHandler对象去处理)
  public <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds) {
    
	final List<? extends V> list = selectList(statement, parameter, rowBounds);
    
	final DefaultMapResultHandler<K, V> mapResultHandler = new DefaultMapResultHandler<K, V>(mapKey,
        configuration.getObjectFactory(), configuration.getObjectWrapperFactory(), configuration.getReflectorFactory());
    
	final DefaultResultContext<V> context = new DefaultResultContext<V>();
    for (V o : list) {
      context.nextResultObject(o);
      mapResultHandler.handleResult(context);
    }
    return mapResultHandler.getMappedResults();
  }

  // 返回Cursor对象(不存在参数)
  public <T> Cursor<T> selectCursor(String statement) {
    return selectCursor(statement, null);
  }

  // 返回Cursor对象(存在parameter参数)
  public <T> Cursor<T> selectCursor(String statement, Object parameter) {
    return selectCursor(statement, parameter, RowBounds.DEFAULT);
  }

  // 查询Cursor对象并进行注册
  public <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      Cursor<T> cursor = executor.queryCursor(ms, wrapCollection(parameter), rowBounds);
      registerCursor(cursor);
      return cursor;
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  // 返回List对象(不存在参数)
  public <E> List<E> selectList(String statement) {
    return this.selectList(statement, null);
  }

  // 返回List对象(存在parameter参数)
  public <E> List<E> selectList(String statement, Object parameter) {
    return this.selectList(statement, parameter, RowBounds.DEFAULT);
  }

  // 返回List对象
  public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
    	
      // 获取Mapper接口对应的MappedStatement对象，其中包含SQL节点
      MappedStatement ms = configuration.getMappedStatement(statement);
      // executor.query(selectBlog, 101, rowBounds, null);
      System.out.println("executor = " + executor.getClass().getName());
      return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
      // 抛出异常
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  public void select(String statement, Object parameter, ResultHandler handler) {
    select(statement, parameter, RowBounds.DEFAULT, handler);
  }

  public void select(String statement, ResultHandler handler) {
    select(statement, null, RowBounds.DEFAULT, handler);
  }

  public void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
    try {
      MappedStatement ms = configuration.getMappedStatement(statement);
      executor.query(ms, wrapCollection(parameter), rowBounds, handler);
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  // 向数据库中插入数据
  public int insert(String statement) {
    return insert(statement, null);
  }

  // 向数据库中插入数据parameter
  public int insert(String statement, Object parameter) {
    return update(statement, parameter);
  }

  // 向数据库中更新数据
  public int update(String statement) {
    return update(statement, null);
  }

  // 向数据库中更新数据parameter
  public int update(String statement, Object parameter) {
    try {
      // 标识为脏数据
      dirty = true;
      // 获取SQL语句对象
      MappedStatement ms = configuration.getMappedStatement(statement);
      // 由具体的执行器执行Sql语句
      return executor.update(ms, wrapCollection(parameter));
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  // 向数据库中删除数据
  public int delete(String statement) {
    return update(statement, null);
  }

  // 向数据库中删除数据parameter
  public int delete(String statement, Object parameter) {
    return update(statement, parameter);
  }

  // 执行事务提交
  public void commit() {
    commit(false);
  }

  // 执行事务提交
  public void commit(boolean force) {
    try {
      executor.commit(isCommitOrRollbackRequired(force));
      dirty = false;
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error committing transaction.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  // 执行事务回滚
  public void rollback() {
    rollback(false);
  }

  // 执行事务回滚
  public void rollback(boolean force) {
    try {
      executor.rollback(isCommitOrRollbackRequired(force));
      dirty = false;
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error rolling back transaction.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  // 返回批处理结果
  public List<BatchResult> flushStatements() {
    try {
      return executor.flushStatements();
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error flushing statements.  Cause: " + e, e);
    } finally {
      ErrorContext.instance().reset();
    }
  }

  // 关闭sqlsession相对应的资源
  public void close() {
    try {
      executor.close(isCommitOrRollbackRequired(false));
      closeCursors();
      dirty = false;
    } finally {
      ErrorContext.instance().reset();
    }
  }

  // 清理相关游标对象
  private void closeCursors() {
    if (cursorList != null && cursorList.size() != 0) {
      for (Cursor<?> cursor : cursorList) {
        try {
          cursor.close();
        } catch (IOException e) {
          throw ExceptionFactory.wrapException("Error closing cursor.  Cause: " + e, e);
        }
      }
      cursorList.clear();
    }
  }

  // 获取Configuration对象
  public Configuration getConfiguration() {
    return configuration;
  }

  public <T> T getMapper(Class<T> type) {  
     return configuration.<T>getMapper(type, this);
  }

  // 获取数据库连接对象Connection对象
  public Connection getConnection() {
    try {
      return executor.getTransaction().getConnection();
    } catch (SQLException e) {
      throw ExceptionFactory.wrapException("Error getting a new connection.  Cause: " + e, e);
    }
  }

  // 清理SqlSession中的缓存对象
  public void clearCache() {
    executor.clearLocalCache();
  }

  // 注册游标对象
  private <T> void registerCursor(Cursor<T> cursor) {
    if (cursorList == null) {
      cursorList = new ArrayList<Cursor<?>>();
    }
    cursorList.add(cursor);
  }

  // 当force = true返回true
  // 当force = false时:autoCommit = false 和 dirty = true时,才返回True
  private boolean isCommitOrRollbackRequired(boolean force) {
    return (!autoCommit && dirty) || force;
  }

  // 将list、set、array结构数据转换成map形式
  // Collection(list,set) :  (collection = object);
  // Collection(list) :  (list = object);
  // Array : (array = object)
  // Object : (object)
  private Object wrapCollection(final Object object) {
	  // Map不属于Collection接口
    if (object instanceof Collection) {
      StrictMap<Object> map = new StrictMap<Object>();
      map.put("collection", object); // 对set/list的处理
      if (object instanceof List) {
        map.put("list", object); // 对list的处理
      }
      return map;
    } else if (object != null && object.getClass().isArray()) {
      StrictMap<Object> map = new StrictMap<Object>();
      map.put("array", object); // 对数组的处理
      return map;
    }
    return object;
  }

  // StrictMap继承了HashMap对象修改了get方法,就是不含指定键的时候,就抛出异常！
  public static class StrictMap<V> extends HashMap<String, V> {

    private static final long serialVersionUID = -5741767162221585340L;

    @Override
    public V get(Object key) {
      if (!super.containsKey(key)) {
        throw new BindingException("Parameter '" + key + "' not found. Available parameters are " + this.keySet());
      }
      return super.get(key);
    }

  }

}

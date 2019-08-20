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
package org.apache.ibatis.executor;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.cache.TransactionalCacheManager;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.StatementType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
// 即Mybatis处理中的二级缓存实现对象,它是委托模式的实现
public class CachingExecutor implements Executor {

  private final Executor delegate;
  private final TransactionalCacheManager tcm = new TransactionalCacheManager();

  public CachingExecutor(Executor delegate) {
    this.delegate = delegate;
    delegate.setExecutorWrapper(this);
  }

  @Override // 获取Transaction对象
  public Transaction getTransaction() {
    return delegate.getTransaction();
  }

  @Override // 依据forceRollback进行回滚或者提交操作
  public void close(boolean forceRollback) {
    try {
      //issues #499, #524 and #573
      if (forceRollback) { 
        tcm.rollback(); // 回滚
      } else { 
        tcm.commit(); // 提交
      }
    } finally {
      // 关闭Executor对象
      delegate.close(forceRollback);
    }
  }

  @Override // 判断是否已经处于关闭状态中
  public boolean isClosed() {
    return delegate.isClosed();
  }

  @Override // 更新MappedStatement对象中的缓存对象
  public int update(MappedStatement ms, Object parameterObject) throws SQLException {
    flushCacheIfRequired(ms);
    return delegate.update(ms, parameterObject);
  }

  @SuppressWarnings("rawtypes") // 执行SQL查询语句
  @Override  // executor.query(selectBlog, 101, rowBounds, null);
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException {
    System.out.println("CachingExecutor --------------------执行中");
	// 步骤一:获取BoundSql对象，解析BoundSql
	BoundSql boundSql = ms.getBoundSql(parameterObject);
	// 创建CacheKey对象
    CacheKey key = createCacheKey(ms, parameterObject, rowBounds, boundSql);
    
    return query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override
  public <E> Cursor<E> queryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds) throws SQLException {
    flushCacheIfRequired(ms);
    return delegate.queryCursor(ms, parameter, rowBounds);
  }

  @Override // 查询SQL语句
  public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {
	  
    Cache cache = ms.getCache(); // 获取查询语句所在的命名空间对应的二级缓存
    
    if (cache != null) { // 步骤二:是否开启了二级缓存功能
    	
      flushCacheIfRequired(ms); // 根据<select>节点的配置，决定是否需要清空二级缓存
      
      // 检测SQL节点的useCache配置以及是否使用了resultHandler配置
      if (ms.isUseCache() && resultHandler == null) {
    	
    	// 步骤三:二级缓存不能保存输出类型的参数，如果查询操作调用了包含输出参数的存储过程，则报错
        ensureNoOutParams(ms, boundSql);
        
        @SuppressWarnings("unchecked") // 从缓存中获取缓存结果对象
        List<E> list = (List<E>) tcm.getObject(cache, key);
        if (list == null) {
        	
          // 步骤五:二级缓存中没有相应的结果对象，调用封装的Executor对象的query()方法
          list = delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
          // 将查询结果保存到TransactionCache.entriesToAddOnCommit集合中
          tcm.putObject(cache, key, list); // issue #578 and #116
        }
        return list;
      }
    }
    
    // 没有启动二级缓存，直接调用底层Executor执行数据库查询操作
    return delegate.<E> query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
  }

  @Override // 触发批量执行方法
  public List<BatchResult> flushStatements() throws SQLException {
    return delegate.flushStatements();
  }

  @Override // 事务提交
  public void commit(boolean required) throws SQLException {
    delegate.commit(required); // 调用底层的Executor提交事务
    tcm.commit();// 遍历所有相关的TransactionCache对象执行commit方法
  }

  @Override // 事务回滚
  public void rollback(boolean required) throws SQLException {
    try {
      delegate.rollback(required); // 调用底层的Executor回滚事务
    } finally {
      if (required) {
        tcm.rollback();// 遍历所有相关的TransactionCache对象执行rollback方法
      }
    }
  }

  // 在调用存储过程中,不允许使用OUT参数
  private void ensureNoOutParams(MappedStatement ms, BoundSql boundSql) {
	// 当StatementType = CALLABLE(存储过程)
    if (ms.getStatementType() == StatementType.CALLABLE) {
      for (ParameterMapping parameterMapping : boundSql.getParameterMappings()) {
    	// 不为输入参数即会报错
        if (parameterMapping.getMode() != ParameterMode.IN) {
          throw new ExecutorException("Caching stored procedures with OUT params is not supported.  Please configure useCache=false in " + ms.getId() + " statement.");
        }
      }
    }
  }

  @Override // 创建CacheKey对象
  public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
    return delegate.createCacheKey(ms, parameterObject, rowBounds, boundSql);
  }

  @Override // 判断是否已经缓存key对象
  public boolean isCached(MappedStatement ms, CacheKey key) {
    return delegate.isCached(ms, key);
  }

  @Override // 延迟加载器
  public void deferLoad(MappedStatement ms, MetaObject resultObject, String property, CacheKey key, Class<?> targetType) {
    delegate.deferLoad(ms, resultObject, property, key, targetType);
  }

  @Override // 清空一个缓存器
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  // 根据节点属性flushCacheRequired判断是否进行去刷新缓存
  private void flushCacheIfRequired(MappedStatement ms) {
    Cache cache = ms.getCache();
    // 节点属性flushCache属性的实现
    if (cache != null && ms.isFlushCacheRequired()) {      
      tcm.clear(cache);
    }
  }

  @Override // 抛出UnsupportedOperationException异常
  public void setExecutorWrapper(Executor executor) {
    throw new UnsupportedOperationException("This method should not be called");
  }

}

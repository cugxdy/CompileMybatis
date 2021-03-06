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
package org.apache.ibatis.cache;

import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.cache.decorators.TransactionalCache;

/**
 * @author Clinton Begin
 */
// 它用来管理CacheingExcutor使用的二级缓存对象，其中定义了一个transactionalCaches(HashMap<Cache, TransactionalCache>())字段
// 它的key是对应的CachingExctor使用的二级缓存对象，value是相应的TransactionalCache对象，
// 在该TransactionalCache中封装了对应的二级缓存对象，也就是这里的key
public class TransactionalCacheManager {

  // 管理缓存对象(TransactionalCache中封装了相应的Cache对象[即对应的key值])
  private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<Cache, TransactionalCache>();

  // 清空指定Cache下的缓存对象
  public void clear(Cache cache) {
	// 调用相应的TransactionalCache对象清除缓存数据
    getTransactionalCache(cache).clear();
  }

  // 从缓存中获取结果对象
  public Object getObject(Cache cache, CacheKey key) {
	// 调用相应的TransactionalCache对象从缓存中取数据
    return getTransactionalCache(cache).getObject(key);
  }
  
  // 向缓存中存入结果对象
  public void putObject(Cache cache, CacheKey key, Object value) {
	// 调用相应的TransactionalCache对象向缓存中放入数据
    getTransactionalCache(cache).putObject(key, value);
  }

  // 遍历transactionalCaches的值对象并执行提交操作
  public void commit() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      // 调用相应的TransactionalCache对象提交方法
      txCache.commit();
    }
  }

  // 遍历transactionalCaches的值对象并执行回滚操作
  public void rollback() {
    for (TransactionalCache txCache : transactionalCaches.values()) {
      // 调用相应的TransactionalCache对象回滚方法
      txCache.rollback();
    }
  }

  // 获取TransactionalCache对象
  // 对二级缓存对象添加装饰器TransactionalCache对象,并放入transactionalCaches对象中
  private TransactionalCache getTransactionalCache(Cache cache) {
    TransactionalCache txCache = transactionalCaches.get(cache);
    
    if (txCache == null) {  // txCache为空时
    	
      // 创建TransactionalCache对象,装饰模式
      txCache = new TransactionalCache(cache); 
      
      transactionalCaches.put(cache, txCache);// 添加到TransactionalCaches集合
    }
    return txCache;
  }

}

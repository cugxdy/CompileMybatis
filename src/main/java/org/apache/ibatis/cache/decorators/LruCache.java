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
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator
 *
 * @author Clinton Begin
 */
// 它使用最近最少使用来删除缓存中的数据
public class LruCache implements Cache {

  private final Cache delegate; // 被装饰的Cache对象
  // LinkedHashMap<Object,Object>类型对象，它是一个有序的HashMap，用于记录key最近的使用的情况
  private Map<Object, Object> keyMap;
  
  private Object eldestKey; // 记录最少使用的缓存项的key值

  public LruCache(Cache delegate) {
    this.delegate = delegate;
    setSize(1024);
  }

  @Override // 获取缓存ID
  public String getId() {
    return delegate.getId();
  }

  @Override // 获取缓存大小
  public int getSize() {
    return delegate.getSize();
  }

  public void setSize(final int size) {  // 重新设置缓存大小时，会重置keyMap字段
	
	// 注意LinkedHashMap构造函数的第三个参数，true表示LinkedHashMap记录的顺序是
	// access-order，也就是说LinkedHashMap.get()方法会改变其记录的顺序
    keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
      private static final long serialVersionUID = 4267176411845948333L;

      @Override // 调用LinkedHashMap.put()方法，会调用该方法
      protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
        boolean tooBig = size() > size;
        if (tooBig) {
          // 获取Key值
          eldestKey = eldest.getKey(); // 如果已达到缓存上限，则更新eldestKey字段，后面会删除该项
        }
        return tooBig;
      }
    };
  }

  @Override // 存放对象
  public void putObject(Object key, Object value) {
    delegate.putObject(key, value);  // 添加缓存项
    cycleKeyList(key);// 删除最久未使用的缓存项
  }

  @Override // 获取对象
  public Object getObject(Object key) {
    keyMap.get(key); //touch  // 修改LinkedHashMap中记录的顺序
    return delegate.getObject(key);
  }

  @Override // 删除对象
  public Object removeObject(Object key) {
    return delegate.removeObject(key);
  }

  @Override // 清空缓存
  public void clear() {
    delegate.clear();
    keyMap.clear();
  }

  @Override
  public ReadWriteLock getReadWriteLock() {
    return null;
  }

  // 删除Map中最近最少使用的对象
  private void cycleKeyList(Object key) {
    keyMap.put(key, key);
    if (eldestKey != null) {  // eldestKey不为空，表示已经达到缓存上限
      delegate.removeObject(eldestKey);// 删除最久未使用的缓存项
      eldestKey = null;
    }
  }

}

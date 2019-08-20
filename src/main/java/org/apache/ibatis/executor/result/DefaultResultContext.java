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
package org.apache.ibatis.executor.result;

import org.apache.ibatis.session.ResultContext;

/**
 * @author Clinton Begin
 */
// 它暂存记录单行记录解析的结果集
public class DefaultResultContext<T> implements ResultContext<T> {

  // 暂存映射后的结果对象，之后会将该对象放入DefaultResultHandler.list中
  private T resultObject;
  
  private int resultCount; // 记录经过DefaultResultContext暂存的对象个数
  
  private boolean stopped;// 控制是否停止映射

  public DefaultResultContext() {
    resultObject = null;
    resultCount = 0;
    stopped = false;
  }

  @Override
  public T getResultObject() {
    return resultObject;
  }

  @Override
  public int getResultCount() {
    return resultCount;
  }

  @Override// 判断该对象是否已经为STOP=true
  public boolean isStopped() {
    return stopped;
  }

  public void nextResultObject(T resultObject) {
    resultCount++; // 递增结果对象计数 
    // 设置结果对象
    this.resultObject = resultObject;
  }

  @Override // 设置为True
  public void stop() {
    this.stopped = true;
  }

}

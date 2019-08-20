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

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

/**
 * @author Clinton Begin
 */
// 它实现对结果集每行记录进行解析存储的List对象中
public class DefaultResultHandler implements ResultHandler<Object> {

  // 存储结果对象的List对象
  private final List<Object> list;

  public DefaultResultHandler() {
	// 为ArrayList对象
    list = new ArrayList<Object>();
  }

  @SuppressWarnings("unchecked")
  public DefaultResultHandler(ObjectFactory objectFactory) {
    list = objectFactory.create(List.class); // 创建List对象
  }

  @Override// 向List对象中加入单行结果集解析对象
  public void handleResult(ResultContext<? extends Object> context) {
    list.add(context.getResultObject()); // 将ResultContext中保存着的结果对象保存到到list集合中
  }

  // 获取结果集对象
  public List<Object> getResultList() {
    return list;
  }

}

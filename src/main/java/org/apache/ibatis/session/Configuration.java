package org.apache.ibatis.session;
 
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.ibatis.binding.MapperRegistry;
import org.apache.ibatis.builder.CacheRefResolver;
import org.apache.ibatis.builder.ResultMapResolver;
import org.apache.ibatis.builder.annotation.MethodResolver;
import org.apache.ibatis.builder.xml.XMLStatementBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.decorators.FifoCache;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.decorators.SoftCache;
import org.apache.ibatis.cache.decorators.WeakCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.datasource.jndi.JndiDataSourceFactory;
import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSourceFactory;
import org.apache.ibatis.executor.BatchExecutor;
import org.apache.ibatis.executor.CachingExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.ReuseExecutor;
import org.apache.ibatis.executor.SimpleExecutor;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.executor.loader.cglib.CglibProxyFactory;
import org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.executor.resultset.DefaultResultSetHandler;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.logging.commons.JakartaCommonsLoggingImpl;
import org.apache.ibatis.logging.jdk14.Jdk14LoggingImpl;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.logging.log4j2.Log4j2Impl;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.InterceptorChain;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.DefaultObjectFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.scripting.LanguageDriverRegistry;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.transaction.Transaction;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

public class Configuration
{
	// 配置Tranction对象
	protected Environment environment;
	protected boolean safeRowBoundsEnabled;
	protected boolean safeResultHandlerEnabled = true;
   protected boolean mapUnderscoreToCamelCase;
   protected boolean aggressiveLazyLoading;
/* 105 */   protected boolean multipleResultSetsEnabled = true;
   protected boolean useGeneratedKeys;
			// 使用允许使用列别名
/* 107 */   protected boolean useColumnLabel = true;
/* 108 */   protected boolean cacheEnabled = true;
   protected boolean callSettersOnNulls;
/* 110 */   protected boolean useActualParamName = true;
   
   protected boolean returnInstanceForEmptyRow;
   protected String logPrefix;
   protected Class<? extends Log> logImpl;
   protected Class<? extends VFS> vfsImpl;
/* 116 */   protected LocalCacheScope localCacheScope = LocalCacheScope.SESSION;
/* 117 */   protected JdbcType jdbcTypeForNull = JdbcType.OTHER;
/* 118 */   protected Set<String> lazyLoadTriggerMethods = new HashSet(Arrays.asList(new String[] { "equals", "clone", "hashCode", "toString" }));
			// 获取驱动程序等待 Statement 对象执行的秒数。
   protected Integer defaultStatementTimeout;

   protected Integer defaultFetchSize;
/* 121 */   protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
/* 122 */   protected AutoMappingBehavior autoMappingBehavior = AutoMappingBehavior.PARTIAL;
/* 123 */   protected AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior = AutoMappingUnknownColumnBehavior.NONE;
   
/* 125 */   protected Properties variables = new Properties();
/* 126 */   protected ReflectorFactory reflectorFactory = new DefaultReflectorFactory();
/* 127 */   protected ObjectFactory objectFactory = new DefaultObjectFactory();
/* 128 */   protected ObjectWrapperFactory objectWrapperFactory = new DefaultObjectWrapperFactory();
   
/* 130 */   protected boolean lazyLoadingEnabled = false;
/* 131 */   protected ProxyFactory proxyFactory = new JavassistProxyFactory();
   
 
 
   protected String databaseId;
   
 
 
   protected Class<?> configurationFactory;
   
 
/* 142 */   protected final MapperRegistry mapperRegistry = new MapperRegistry(this);
/* 143 */   protected final InterceptorChain interceptorChain = new InterceptorChain();
/* 144 */   protected final TypeHandlerRegistry typeHandlerRegistry = new TypeHandlerRegistry();
/* 145 */   protected final TypeAliasRegistry typeAliasRegistry = new TypeAliasRegistry();
/* 146 */   protected final LanguageDriverRegistry languageRegistry = new LanguageDriverRegistry();
   
/* 148 */   protected final Map<String, MappedStatement> mappedStatements = new StrictMap("Mapped Statements collection");
/* 149 */   protected final Map<String, Cache> caches = new StrictMap("Caches collection");
/* 150 */   protected final Map<String, ResultMap> resultMaps = new StrictMap("Result Maps collection");
/* 151 */   protected final Map<String, ParameterMap> parameterMaps = new StrictMap("Parameter Maps collection");
/* 152 */   protected final Map<String, KeyGenerator> keyGenerators = new StrictMap("Key Generators collection");
   
/* 154 */   protected final Set<String> loadedResources = new HashSet();
/* 155 */   protected final Map<String, XNode> sqlFragments = new StrictMap("XML fragments parsed from previous mappers");
   
/* 157 */   protected final Collection<XMLStatementBuilder> incompleteStatements = new LinkedList();
/* 158 */   protected final Collection<CacheRefResolver> incompleteCacheRefs = new LinkedList();
/* 159 */   protected final Collection<ResultMapResolver> incompleteResultMaps = new LinkedList();
/* 160 */   protected final Collection<MethodResolver> incompleteMethods = new LinkedList();
   
 
 
 
 
 
/* 167 */   protected final Map<String, String> cacheRefMap = new HashMap();
   
   public Configuration(Environment environment) {
/* 170 */     this();
/* 171 */     this.environment = environment;
   }
   
   public Configuration() {
/* 175 */     typeAliasRegistry.registerAlias("JDBC", JdbcTransactionFactory.class);
/* 176 */     typeAliasRegistry.registerAlias("MANAGED", ManagedTransactionFactory.class);
     
/* 178 */     typeAliasRegistry.registerAlias("JNDI", JndiDataSourceFactory.class);
/* 179 */     typeAliasRegistry.registerAlias("POOLED", PooledDataSourceFactory.class);
/* 180 */     typeAliasRegistry.registerAlias("UNPOOLED", UnpooledDataSourceFactory.class);
     
/* 182 */     typeAliasRegistry.registerAlias("PERPETUAL", PerpetualCache.class);
/* 183 */     typeAliasRegistry.registerAlias("FIFO", FifoCache.class);
/* 184 */     typeAliasRegistry.registerAlias("LRU", LruCache.class);
/* 185 */     typeAliasRegistry.registerAlias("SOFT", SoftCache.class);
/* 186 */     typeAliasRegistry.registerAlias("WEAK", WeakCache.class);
     
/* 188 */     typeAliasRegistry.registerAlias("DB_VENDOR", VendorDatabaseIdProvider.class);
     
/* 190 */     typeAliasRegistry.registerAlias("XML", XMLLanguageDriver.class);
/* 191 */     typeAliasRegistry.registerAlias("RAW", RawLanguageDriver.class);
     
/* 193 */     typeAliasRegistry.registerAlias("SLF4J", Slf4jImpl.class);
/* 194 */     typeAliasRegistry.registerAlias("COMMONS_LOGGING", JakartaCommonsLoggingImpl.class);
/* 195 */     typeAliasRegistry.registerAlias("LOG4J", Log4jImpl.class);
/* 196 */     typeAliasRegistry.registerAlias("LOG4J2", Log4j2Impl.class);
/* 197 */     typeAliasRegistry.registerAlias("JDK_LOGGING", Jdk14LoggingImpl.class);
/* 198 */     typeAliasRegistry.registerAlias("STDOUT_LOGGING", StdOutImpl.class);
/* 199 */     typeAliasRegistry.registerAlias("NO_LOGGING", NoLoggingImpl.class);
     
/* 201 */     typeAliasRegistry.registerAlias("CGLIB", CglibProxyFactory.class);
/* 202 */     typeAliasRegistry.registerAlias("JAVASSIST", JavassistProxyFactory.class);
     
/* 204 */     languageRegistry.setDefaultDriverClass(XMLLanguageDriver.class);
/* 205 */     languageRegistry.register(RawLanguageDriver.class);
   }
   
   public String getLogPrefix() {
/* 209 */     return logPrefix;
   }
   
   public void setLogPrefix(String logPrefix) {
/* 213 */     this.logPrefix = logPrefix;
   }
   
   public Class<? extends Log> getLogImpl() {
/* 217 */     return logImpl;
   }
   
   public void setLogImpl(Class<? extends Log> logImpl) {
/* 221 */     if (logImpl != null) {
/* 222 */       this.logImpl = logImpl;
/* 223 */       LogFactory.useCustomLogging(this.logImpl);
     }
   }
   
   public Class<? extends VFS> getVfsImpl() {
/* 228 */     return vfsImpl;
   }
   
   public void setVfsImpl(Class<? extends VFS> vfsImpl) {
/* 232 */     if (vfsImpl != null) {
/* 233 */       this.vfsImpl = vfsImpl;
/* 234 */       VFS.addImplClass(this.vfsImpl);
     }
   }
   
   public boolean isCallSettersOnNulls() {
/* 239 */     return callSettersOnNulls;
   }
   
   public void setCallSettersOnNulls(boolean callSettersOnNulls) {
/* 243 */     this.callSettersOnNulls = callSettersOnNulls;
   }
   
   public boolean isUseActualParamName() {
/* 247 */     return useActualParamName;
   }
   
   public void setUseActualParamName(boolean useActualParamName) {
/* 251 */     this.useActualParamName = useActualParamName;
   }
   
   public boolean isReturnInstanceForEmptyRow() {
/* 255 */     return returnInstanceForEmptyRow;
   }
   
   public void setReturnInstanceForEmptyRow(boolean returnEmptyInstance) {
/* 259 */     returnInstanceForEmptyRow = returnEmptyInstance;
   }
   
   public String getDatabaseId() {
/* 263 */     return databaseId;
   }
   
   public void setDatabaseId(String databaseId) {
/* 267 */     this.databaseId = databaseId;
   }
   
   public Class<?> getConfigurationFactory() {
/* 271 */     return configurationFactory;
   }
   
   public void setConfigurationFactory(Class<?> configurationFactory) {
/* 275 */     this.configurationFactory = configurationFactory;
   }
   
   public boolean isSafeResultHandlerEnabled() {
/* 279 */     return safeResultHandlerEnabled;
   }
   
   public void setSafeResultHandlerEnabled(boolean safeResultHandlerEnabled) {
/* 283 */     this.safeResultHandlerEnabled = safeResultHandlerEnabled;
   }
   
   public boolean isSafeRowBoundsEnabled() {
/* 287 */     return safeRowBoundsEnabled;
   }
   
   public void setSafeRowBoundsEnabled(boolean safeRowBoundsEnabled) {
/* 291 */     this.safeRowBoundsEnabled = safeRowBoundsEnabled;
   }
   
   public boolean isMapUnderscoreToCamelCase() {
/* 295 */     return mapUnderscoreToCamelCase;
   }
   
   public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
/* 299 */     this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
   }
   
   public void addLoadedResource(String resource) {
/* 303 */     loadedResources.add(resource);
   }
   
   public boolean isResourceLoaded(String resource) {
/* 307 */     return loadedResources.contains(resource);
   }
   
   public Environment getEnvironment() {
/* 311 */     return environment;
   }
   
   public void setEnvironment(Environment environment) {
/* 315 */     this.environment = environment;
   }
   
   public AutoMappingBehavior getAutoMappingBehavior() {
/* 319 */     return autoMappingBehavior;
   }
   
   public void setAutoMappingBehavior(AutoMappingBehavior autoMappingBehavior) {
/* 323 */     this.autoMappingBehavior = autoMappingBehavior;
   }
   
 
 
   public AutoMappingUnknownColumnBehavior getAutoMappingUnknownColumnBehavior()
   {
/* 330 */     return autoMappingUnknownColumnBehavior;
   }
   
 
 
   public void setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior autoMappingUnknownColumnBehavior)
   {
/* 337 */     this.autoMappingUnknownColumnBehavior = autoMappingUnknownColumnBehavior;
   }
   
   public boolean isLazyLoadingEnabled() {
/* 341 */     return lazyLoadingEnabled;
   }
   
   public void setLazyLoadingEnabled(boolean lazyLoadingEnabled) {
/* 345 */     this.lazyLoadingEnabled = lazyLoadingEnabled;
   }
   
   public ProxyFactory getProxyFactory() {
/* 349 */     return proxyFactory;
   }
   
   public void setProxyFactory(ProxyFactory proxyFactory) {
/* 353 */     if (proxyFactory == null) {
/* 354 */       proxyFactory = new JavassistProxyFactory();
     }
/* 356 */     this.proxyFactory = proxyFactory;
   }
   
   public boolean isAggressiveLazyLoading() {
/* 360 */     return aggressiveLazyLoading;
   }
   
   public void setAggressiveLazyLoading(boolean aggressiveLazyLoading) {
/* 364 */     this.aggressiveLazyLoading = aggressiveLazyLoading;
   }
   
   public boolean isMultipleResultSetsEnabled() {
/* 368 */     return multipleResultSetsEnabled;
   }
   
   public void setMultipleResultSetsEnabled(boolean multipleResultSetsEnabled) {
/* 372 */     this.multipleResultSetsEnabled = multipleResultSetsEnabled;
   }
   
   public Set<String> getLazyLoadTriggerMethods() {
/* 376 */     return lazyLoadTriggerMethods;
   }
   
   public void setLazyLoadTriggerMethods(Set<String> lazyLoadTriggerMethods) {
/* 380 */     this.lazyLoadTriggerMethods = lazyLoadTriggerMethods;
   }
   
   public boolean isUseGeneratedKeys() {
/* 384 */     return useGeneratedKeys;
   }
   
   public void setUseGeneratedKeys(boolean useGeneratedKeys) {
/* 388 */     this.useGeneratedKeys = useGeneratedKeys;
   }
   
   public ExecutorType getDefaultExecutorType() {
/* 392 */     return defaultExecutorType;
   }
   
   public void setDefaultExecutorType(ExecutorType defaultExecutorType) {
/* 396 */     this.defaultExecutorType = defaultExecutorType;
   }
   
   public boolean isCacheEnabled() {
/* 400 */     return cacheEnabled;
   }
   
   public void setCacheEnabled(boolean cacheEnabled) {
/* 404 */     this.cacheEnabled = cacheEnabled;
   }
   
   public Integer getDefaultStatementTimeout() {
/* 408 */     return defaultStatementTimeout;
   }
   
   public void setDefaultStatementTimeout(Integer defaultStatementTimeout) {
/* 412 */     this.defaultStatementTimeout = defaultStatementTimeout;
   }
   
 
 
   public Integer getDefaultFetchSize()
   {
/* 419 */     return defaultFetchSize;
   }
   
 
 
   public void setDefaultFetchSize(Integer defaultFetchSize)
   {
/* 426 */     this.defaultFetchSize = defaultFetchSize;
   }
   
   public boolean isUseColumnLabel() {
/* 430 */     return useColumnLabel;
   }
   
   public void setUseColumnLabel(boolean useColumnLabel) {
/* 434 */     this.useColumnLabel = useColumnLabel;
   }
   
   public LocalCacheScope getLocalCacheScope() {
/* 438 */     return localCacheScope;
   }
   
   public void setLocalCacheScope(LocalCacheScope localCacheScope) {
/* 442 */     this.localCacheScope = localCacheScope;
   }
   
   public JdbcType getJdbcTypeForNull() {
/* 446 */     return jdbcTypeForNull;
   }
   
   public void setJdbcTypeForNull(JdbcType jdbcTypeForNull) {
/* 450 */     this.jdbcTypeForNull = jdbcTypeForNull;
   }
   
   public Properties getVariables() {
/* 454 */     return variables;
   }
   
   public void setVariables(Properties variables) {
/* 458 */     this.variables = variables;
   }
   
   public TypeHandlerRegistry getTypeHandlerRegistry() {
/* 462 */     return typeHandlerRegistry;
   }
   
 
 
 
 
 
   public void setDefaultEnumTypeHandler(Class<? extends TypeHandler> typeHandler)
   {
/* 472 */     if (typeHandler != null) {
/* 473 */       getTypeHandlerRegistry().setDefaultEnumTypeHandler(typeHandler);
     }
   }
   
   public TypeAliasRegistry getTypeAliasRegistry() {
/* 478 */     return typeAliasRegistry;
   }
   
 
 
   public MapperRegistry getMapperRegistry()
   {
/* 485 */     return mapperRegistry;
   }
   
   public ReflectorFactory getReflectorFactory() {
/* 489 */     return reflectorFactory;
   }
   
   public void setReflectorFactory(ReflectorFactory reflectorFactory) {
/* 493 */     this.reflectorFactory = reflectorFactory;
   }
   
   public ObjectFactory getObjectFactory() {
/* 497 */     return objectFactory;
   }
   
   public void setObjectFactory(ObjectFactory objectFactory) {
/* 501 */     this.objectFactory = objectFactory;
   }
   
   public ObjectWrapperFactory getObjectWrapperFactory() {
/* 505 */     return objectWrapperFactory;
   }
   
   public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
/* 509 */     this.objectWrapperFactory = objectWrapperFactory;
   }
   
 
 
   public List<Interceptor> getInterceptors()
   {
/* 516 */     return interceptorChain.getInterceptors();
   }
   
   public LanguageDriverRegistry getLanguageRegistry() {
/* 520 */     return languageRegistry;
   }
   
   public void setDefaultScriptingLanguage(Class<?> driver) {
/* 524 */     if (driver == null) {
/* 525 */       driver = XMLLanguageDriver.class;
     }
/* 527 */     getLanguageRegistry().setDefaultDriverClass(driver);
   }
   
   public LanguageDriver getDefaultScriptingLanguageInstance() {
/* 531 */     return languageRegistry.getDefaultDriver();
   }
   
   @Deprecated
   public LanguageDriver getDefaultScriptingLanuageInstance()
   {
/* 537 */     return getDefaultScriptingLanguageInstance();
   }
   
   public MetaObject newMetaObject(Object object) {
/* 541 */     return MetaObject.forObject(object, objectFactory, objectWrapperFactory, reflectorFactory);
   }
   
   public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
/* 545 */     ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
/* 546 */     parameterHandler = (ParameterHandler)interceptorChain.pluginAll(parameterHandler);
/* 547 */     return parameterHandler;
   }
   
   public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler, ResultHandler resultHandler, BoundSql boundSql)
   {
/* 552 */     ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
/* 553 */     resultSetHandler = (ResultSetHandler)interceptorChain.pluginAll(resultSetHandler);
/* 554 */     return resultSetHandler;
   }
   
   public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
/* 558 */     StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
/* 559 */     statementHandler = (StatementHandler)interceptorChain.pluginAll(statementHandler);
/* 560 */     return statementHandler;
   }

   // 创建执行器对象
   public Executor newExecutor(Transaction transaction) {
/* 564 */     return newExecutor(transaction, defaultExecutorType);
   }
   
   public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
/* 568 */     // 获取执行器类型
				executorType = executorType == null ? defaultExecutorType : executorType;
/* 569 */     executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
     Executor executor;

/* 571 */     if (ExecutorType.BATCH == executorType) {
/* 572 */       executor = new BatchExecutor(this, transaction); 
			  } else { 
/* 573 */           if (ExecutorType.REUSE == executorType) {
/* 574 */         		executor = new ReuseExecutor(this, transaction);
       	} else
/* 576 */         	executor = new SimpleExecutor(this, transaction);
     }
				// 是否开启缓存功能。
/* 578 */     if (cacheEnabled) {
				// 创建CachingExecutor对象
/* 579 */       executor = new CachingExecutor(executor);
     }

/* 581 */     executor = (Executor)interceptorChain.pluginAll(executor);
/* 582 */     return executor;
   }
   
   public void addKeyGenerator(String id, KeyGenerator keyGenerator) {
/* 586 */     keyGenerators.put(id, keyGenerator);
   }
   
   public Collection<String> getKeyGeneratorNames() {
/* 590 */     return keyGenerators.keySet();
   }
   
   public Collection<KeyGenerator> getKeyGenerators() {
/* 594 */     return keyGenerators.values();
   }
   
   public KeyGenerator getKeyGenerator(String id) {
/* 598 */     return (KeyGenerator)keyGenerators.get(id);
   }
   
   public boolean hasKeyGenerator(String id) {
/* 602 */     return keyGenerators.containsKey(id);
   }
   
   public void addCache(Cache cache) {
/* 606 */     caches.put(cache.getId(), cache);
   }
   
   public Collection<String> getCacheNames() {
/* 610 */     return caches.keySet();
   }
   
   public Collection<Cache> getCaches() {
/* 614 */     return caches.values();
   }
   
   public Cache getCache(String id) {
/* 618 */     return (Cache)caches.get(id);
   }
   
   public boolean hasCache(String id) {
/* 622 */     return caches.containsKey(id);
   }
   
   public void addResultMap(ResultMap rm) {
/* 626 */     resultMaps.put(rm.getId(), rm);
/* 627 */     checkLocallyForDiscriminatedNestedResultMaps(rm);
/* 628 */     checkGloballyForDiscriminatedNestedResultMaps(rm);
   }
   
   public Collection<String> getResultMapNames() {
/* 632 */     return resultMaps.keySet();
   }
   
   public Collection<ResultMap> getResultMaps() {
/* 636 */     return resultMaps.values();
   }
   
   public ResultMap getResultMap(String id) {
/* 640 */     return (ResultMap)resultMaps.get(id);
   }
   
   public boolean hasResultMap(String id) {
/* 644 */     return resultMaps.containsKey(id);
   }
   
   public void addParameterMap(ParameterMap pm) {
/* 648 */     parameterMaps.put(pm.getId(), pm);
   }
   
   public Collection<String> getParameterMapNames() {
/* 652 */     return parameterMaps.keySet();
   }
   
   public Collection<ParameterMap> getParameterMaps() {
/* 656 */     return parameterMaps.values();
   }
   
   public ParameterMap getParameterMap(String id) {
/* 660 */     return (ParameterMap)parameterMaps.get(id);
   }
   
   public boolean hasParameterMap(String id) {
/* 664 */     return parameterMaps.containsKey(id);
   }
   
   public void addMappedStatement(MappedStatement ms) {
/* 668 */     mappedStatements.put(ms.getId(), ms);
   }
   
   public Collection<String> getMappedStatementNames() {
/* 672 */     buildAllStatements();
/* 673 */     return mappedStatements.keySet();
   }
   
   public Collection<MappedStatement> getMappedStatements() {
/* 677 */     buildAllStatements();
/* 678 */     return mappedStatements.values();
   }
   
   public Collection<XMLStatementBuilder> getIncompleteStatements() {
/* 682 */     return incompleteStatements;
   }
   
   public void addIncompleteStatement(XMLStatementBuilder incompleteStatement) {
/* 686 */     incompleteStatements.add(incompleteStatement);
   }
   
   public Collection<CacheRefResolver> getIncompleteCacheRefs() {
/* 690 */     return incompleteCacheRefs;
   }
   
   public void addIncompleteCacheRef(CacheRefResolver incompleteCacheRef) {
/* 694 */     incompleteCacheRefs.add(incompleteCacheRef);
   }
   
   public Collection<ResultMapResolver> getIncompleteResultMaps() {
/* 698 */     return incompleteResultMaps;
   }
   
   public void addIncompleteResultMap(ResultMapResolver resultMapResolver) {
/* 702 */     incompleteResultMaps.add(resultMapResolver);
   }
   
   public void addIncompleteMethod(MethodResolver builder) {
/* 706 */     incompleteMethods.add(builder);
   }
   
   public Collection<MethodResolver> getIncompleteMethods() {
/* 710 */     return incompleteMethods;
   }
   
   public MappedStatement getMappedStatement(String id) {
/* 714 */     return getMappedStatement(id, true);
   }
   
   public MappedStatement getMappedStatement(String id, boolean validateIncompleteStatements) {
/* 718 */     if (validateIncompleteStatements) {
/* 719 */       buildAllStatements();
     }
/* 721 */     return (MappedStatement)mappedStatements.get(id);
   }
   
   public Map<String, XNode> getSqlFragments() {
/* 725 */     return sqlFragments;
   }
   
   public void addInterceptor(Interceptor interceptor) {
/* 729 */     interceptorChain.addInterceptor(interceptor);
   }
   
   public void addMappers(String packageName, Class<?> superType) {
/* 733 */     mapperRegistry.addMappers(packageName, superType);
   }
   
   public void addMappers(String packageName) {
/* 737 */     mapperRegistry.addMappers(packageName);
   }
   
   public <T> void addMapper(Class<T> type) {
/* 741 */     mapperRegistry.addMapper(type);
   }
   
   public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
/* 745 */     return (T)mapperRegistry.getMapper(type, sqlSession);
   }
   
   public boolean hasMapper(Class<?> type) {
/* 749 */     return mapperRegistry.hasMapper(type);
   }
   
   public boolean hasStatement(String statementName) {
/* 753 */     return hasStatement(statementName, true);
   }
   
   public boolean hasStatement(String statementName, boolean validateIncompleteStatements) {
/* 757 */     if (validateIncompleteStatements) {
/* 758 */       buildAllStatements();
     }
/* 760 */     return mappedStatements.containsKey(statementName);
   }
   
   public void addCacheRef(String namespace, String referencedNamespace) {
/* 764 */     cacheRefMap.put(namespace, referencedNamespace);
   }
   
 
 
 
 
   protected void buildAllStatements()
   {
/* 773 */     if (!incompleteResultMaps.isEmpty()) {
/* 774 */       synchronized (incompleteResultMaps)
       {
/* 776 */         ((ResultMapResolver)incompleteResultMaps.iterator().next()).resolve();
       }
     }
/* 779 */     if (!incompleteCacheRefs.isEmpty()) {
/* 780 */       synchronized (incompleteCacheRefs)
       {
/* 782 */         ((CacheRefResolver)incompleteCacheRefs.iterator().next()).resolveCacheRef();
       }
     }
/* 785 */     if (!incompleteStatements.isEmpty()) {
/* 786 */       synchronized (incompleteStatements)
       {
/* 788 */         ((XMLStatementBuilder)incompleteStatements.iterator().next()).parseStatementNode();
       }
     }
/* 791 */     if (!incompleteMethods.isEmpty()) {
/* 792 */       synchronized (incompleteMethods)
       {
/* 794 */         ((MethodResolver)incompleteMethods.iterator().next()).resolve();
       }
     }
   }
   
 
 
 
 
 
   protected String extractNamespace(String statementId)
   {
/* 806 */     int lastPeriod = statementId.lastIndexOf('.');
/* 807 */     return lastPeriod > 0 ? statementId.substring(0, lastPeriod) : null;
   }
   
   protected void checkGloballyForDiscriminatedNestedResultMaps(ResultMap rm)
   {
/* 812 */     if (rm.hasNestedResultMaps()) {
/* 813 */       for (Map.Entry<String, ResultMap> entry : resultMaps.entrySet()) {
/* 814 */         Object value = entry.getValue();
/* 815 */         if ((value instanceof ResultMap)) {
/* 816 */           ResultMap entryResultMap = (ResultMap)value;
/* 817 */           if ((!entryResultMap.hasNestedResultMaps()) && (entryResultMap.getDiscriminator() != null)) {
/* 818 */             Collection<String> discriminatedResultMapNames = entryResultMap.getDiscriminator().getDiscriminatorMap().values();
/* 819 */             if (discriminatedResultMapNames.contains(rm.getId())) {
/* 820 */               entryResultMap.forceNestedResultMaps();
             }
           }
         }
       }
     }
   }
   
   protected void checkLocallyForDiscriminatedNestedResultMaps(ResultMap rm)
   {
/* 830 */     if ((!rm.hasNestedResultMaps()) && (rm.getDiscriminator() != null)) {
/* 831 */       for (Map.Entry<String, String> entry : rm.getDiscriminator().getDiscriminatorMap().entrySet()) {
/* 832 */         String discriminatedResultMapName = (String)entry.getValue();
/* 833 */         if (hasResultMap(discriminatedResultMapName)) {
/* 834 */           ResultMap discriminatedResultMap = (ResultMap)resultMaps.get(discriminatedResultMapName);
/* 835 */           if (discriminatedResultMap.hasNestedResultMaps()) {
/* 836 */             rm.forceNestedResultMaps();
/* 837 */             break;
           }
         }
       }
     }
   }
   
   protected static class StrictMap<V> extends HashMap<String, V>
   {
     private static final long serialVersionUID = -4950446264854982944L;
     private final String name;
     
     public StrictMap(String name, int initialCapacity, int loadFactor) {
/* 850 */       super(loadFactor);
/* 851 */       this.name = name;
     }
     
     public StrictMap(String name, int initialCapacity) {
/* 855 */       super();
/* 856 */       this.name = name;
     }
     
     public StrictMap(String name)
     {
/* 861 */       this.name = name;
     }
     
     public StrictMap(String name, Map<String, ? extends V> m) {
/* 865 */       super();
/* 866 */       this.name = name;
     }
     
     public V put(String key, V value)
     {
/* 871 */       if (containsKey(key)) {
/* 872 */         throw new IllegalArgumentException(name + " already contains value for " + key);
       }
/* 874 */       if (key.contains(".")) {
/* 875 */         String shortKey = getShortName(key);
/* 876 */         if (super.get(shortKey) == null) {
/* 877 */           super.put(shortKey, value);
         } else {
/* 879 */           super.put(shortKey, (V) new Ambiguity(shortKey));
         }
       }
/* 882 */       return (V)super.put(key, value);
     }
     
     public V get(Object key) {
				System.out.println("name = " + name + " key = " + key);
/* 886 */       V value = super.get(key);
/* 887 */       if (value == null) {
/* 888 */         throw new IllegalArgumentException(name + " does not contain value for " + key);
       }
/* 890 */       if ((value instanceof Ambiguity)) {
/* 891 */         throw new IllegalArgumentException(((Ambiguity)value).getSubject() + " is ambiguous in " + name + " (try using the full name including the namespace, or rename one of the entries)");
       }
       
/* 894 */       return value;
     }
     
     private String getShortName(String key) {
/* 898 */       String[] keyParts = key.split("\\.");
/* 899 */       return keyParts[(keyParts.length - 1)];
     }
     
     protected static class Ambiguity {
       private final String subject;
       
       public Ambiguity(String subject) {
/* 906 */         this.subject = subject;
       }
       
       public String getSubject() {
/* 910 */         return subject;
       }
     }
   }
 }

package org.apache.ibatis.session.defaults;

public class DefaultSqlSession implements SqlSession {
    private Configuration cfg;
    private Connection conn;
    
    public DefaultSqlSession(Configuration cfg) {
        this.cfg = cfg;
        this.conn = DataSourceUtil.getConnection(cfg);
    }
    
    @Override
    public <T> T getMapper(Class<T> daoInterfaceClass) {
        Map<String,Mapper> mappers = cfg.getMappers();
        return (T) Proxy.newProxyInstance(daoInterfaceClass.getClassLoader(), new Class[]{daoInterfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throw Throwable {
                //（1）获取方法名
                String methodName = method.getName();
                //（2）获取方法所在接口的名称
                String className = method.getDeclaringClass().getName();
                //（3）组合key
                String key = className + "." + methodName;;
                //（4）获取mappers中的mapper对象
                Mapper mapper = mappers.get(key);
                //（5）判断是否有mapper
                if(mapper == null) {
                    throw new IllegalArgumentException("传入的参数有误");
                }
                //（6）调用工具类执行查询所有
                return new Executor().selectList(mapper,conn);
            }
        });
    }
    
    @Override
    public void close() {
        if(conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

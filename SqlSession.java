package org.apache.ibatis.session;

// 自定义Mybatis中和数据库交互的核心类，里面用来创建XxxDAO接口的代理对象
public interface SqlSession extends Claseable {
    /**
     * 根据参数创建一个代理对象
     * @param daoInterfaceClass -> XxxDAO的接口字节码对象
     */
    <T> T getMapper(Class<T> daoInterfaceClass);
    
    /**
     * 释放资源
     */
    void close();
    
    /**
     * 无需创建代理对象，自己创建接口实现XXXDAOImpl
     */
    public <E> List<E> selectList(string statement);
    public <E> List<E> selectList(String statement,Object parameter);
    public <E> List<E> selectList(String statement,Object parameter,RowBounds rowBounds);
}

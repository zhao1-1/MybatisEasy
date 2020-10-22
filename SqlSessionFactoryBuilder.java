package org.apache.ibatis.session;

// 利用建造者模式，创建SqlSessionFactory对象
public class SqlSessionFactoryBuilder {
    /**
     * 根据参数的字节输入流来构建一个SqlSessionFactory工厂对象
     */
    public SqlSessionFactory build(InputStream in) {
        Configuration cfg = XMLConfigBuilder.loadConfiguration(in);
        return new DefaultSqlSessionFactory(cfg);
    }
}

package org.apache.ibatis.session.defaults;

public class DefaultSqlSessionFactory implements SqlSessionFactory{
    private Configuration cfg;
    
    public DefaultSqlSessionFactory(Configuration cfg) {
        this.cfg = cfg;
    }
    
    @Override
    public SqlSession openSession() {
        return new DefaultSqlSession(cfg);
    }
}

package org.apache.ibatis.cfg;

// 自定义mybatis的配置类
public class Configuration {
    private String driver;
    private String url;
    private String username;
    private String password;
    
    private Map<String,Mapper> mappers;
    
    // mappers的set方法特殊：
    public void setMappers(Map<String,Mapper> mappers) {
        this.mappers.putAll(mappers);		// 使用追加的方式定义set方法
    }
    
    // 省略get、set方法
}

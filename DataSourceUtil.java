package org.apache.ibatis.utils;

// 用于创建数据源的工具类
public class DataSourceUtil {
    /**
     * 用来获取一个连接
     */
    public static Connection getConnection(Configuration cfg) {
        try {
            Class.forName(cfg.getDriver());
        	return DriverManager.getConnection(cfg.getUrl(),cfg.getUsername(),cfg.getPassword());
        } catch (Exception e) {
            throw new RutimeException(e);
        }
    }
}

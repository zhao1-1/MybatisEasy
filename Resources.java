package org.apache.ibatis.io;

// 使用类加载器读取配置文件的类
public class Resources {
    /**
     * 根据传入的参数，获取一个字节输入流
     * @param filePath
     * @return
     */
    public static InputStream getResourceAsStream(String filePath) {
        return Resources.class.getClassLoader().getResourceAsStream(filePath);
    }
}

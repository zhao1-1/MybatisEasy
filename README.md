# MybatisEasy
写一个简易版的mybatis框架，让大家缕清mybatis的运行原理，和关键实现

包结构：
+ org.apache.ibatis.cfg
+ org.apache.ibatis.utils
+ org.apache.ibatis.io
+ org.apache.ibatis.session
+ org.apache.ibatis.session.defaults


完整版：

```java
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
```

```java
package org.apache.ibatis.cfg;

// 用于封装执行的SQL语句和结果类型的全限定类名
public class Mapper {
    private String queryString;		// SQL语句
    private String resultType;		// 结果封装实体类的全限定类名
    
    // 省略get、set方法
}
```

```java
package org.apache.ibatis.utils;

// 目的就是为了解析xml文件
// 用到的技术是（后面需要Maven导入这两个依赖）：dom4j + xpath
public class XMLConfigBuilder {
    
    publi static Configuration loadConfiguration(InputStream in) {
        try {
            Configuration cfg = new Configuration();
            //（1）根据字节输入流获取Document对象
            Document document = new SAXReader().read(in);
            //（2）获取根节点
            Element root = document.getRootElement();
            //（3）使用xpath中选择指定节点的方式，获取<dataSource>节点下的所有<property>标签
            List<Element> propertyElements = root.selectNodes("//dataSource/property");
            //（3-0）遍历节点
            for (Element propertyElement : propertyElements) {
                //（3-1）获取<property name="...">标签中name属性的值
                String name = propertyElement.attributeValue("name");
                //（3-2）判断name属性值是（driver、url、username、password）它们中的哪一个
                if("driver".equals(name)) {
                    String driver = propertyElement.attributeValue("value");
                    cfg.setDriver(driver);
                }
                if("url".equals(name)) {
                    String url = propertyElement.attributeValue("value");
                    cfg.setUrl(url);
                }
                if("username".equals(name)) {
                    String username = propertyElement.attributeValue("value");
                    cfg.setDriver(username);
                }
                if("password".equals(name)) {
                    String password = propertyElement.attributeValue("value");
                    cfg.setDriver(password);
                }
            }
            //（4）使用xpath中选择指定节点的方式，获取<mappers>节点下的所有<mapper>标签
            List<Element> mapperElements = root.selectNodes("//mappers/mapper");
            //（4-0）遍历节点
            for (Element mapperElement : mapperElements) {
                //（4-1）使用的是XML配置（<mapper>标签内有resource属性）
                if (mapperElement.attribute("resource") !=null) {
                    String mapperPath = mapperElement.attributeValue("resource");
                    Map<String,Mapper> mappers = loadMapperConfiguration(mapperPath);
                    cfg.setMappers(mappers);
                }
                //（4-2）使用的是注解配置
                if (mapperElement.attribute("class") !=null) {
                    String daoClassPath = mapperElement.attributeValue("class");
                    Map<String,Mapper> mappers = loadMapperAnnotation(daoClassPath);
                    cfg.setMappers(mappers);
                }
            }
            return cfg;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 根据传入的参数，解析XML，并且封装到Map中
     * @param mapperPath -> 映射配置文件的位置（mapperPath="com/woodbox/dao/EmpDAOMapper.xml"）
     * return map中包含了：
     			key    ->  获取的唯一标识：由XxxDAO的全限定类名和方法名组成
     			value  ->  执行所需的必要信息：一个Mapper对象，里面存放的是执行SQL语句和要封装的实体类全限定类名
     */
    private static Map<String,Mapper> loadMapperConfiguration(String mapperPath) throws IOException {
        InputStream in = null;
        try {
            //（0）定义返回值对象
            Map<String, Mapper> mappers = new HashMap<String, Mapper>();
            //（1）根据路径获取字节输入流
            in = Resources.getResourceAsStream(mapperPath);
            //（2）根据字节输入流获取Document对象
            Document document = new SAXReader().read(in);
            //（3）获取根节点<mapper namespace=".....">
            Element root = document.getRootElement();
            //（4）获取根节点的namespace属性值
            String namespace = root.attributeValue("namespace");
            //（5）获取所有的<select>节点
            List<Element> selectElements = root.selectNode("//select");
            //（6）遍历<select>节点集合
            for(Element selectElement : selectElements) {
                //（6-1）取出<select id="...">中id属性的值，和namespace一起组成Map中key的部分
                String key = namespace + "." + selectElement.attributeValue("id");
                //（6-2.0）定义一个Mapper对象，作为Map中value的部分
                Mapper mapper = new Mapper();
                //（6-2.1）取出<select resultType="...">中resultType属性的值，放入Mapper对象
                String resultType = selectElement.attributeValue("resultType");
                mapper.setResultType(resultType);
                //（6-2.2）取出<select>标签下的文本信息，放入Mapper对象
                String queryString = selectElement.getText();
                mapper.setQueryString(queryString);
                //（6-3）把第一步得到的key、第二步得到的value存入mappers中
                mappers.put(key,mapper);
            }
            //（7）返回
            return mappers;
        } catch (Exception e) {
            
        }
    }
    
    
    /**
     * 根据传入的参数，得到dao中所有被@Select注解标注的方法。
     * 根据方法名称和类名，以及方法上@Select(value=".sql.")注解的value属性的值，组成Mapper的必要信息。
     */
    public static Map<String,Mapper> loadMapperAnnotation(String daoClassPath) {
        //（0）定义返回值对象
        Map<String,Mapper> mappers = new HashMap<String,Mapper>();
        //（1）得到DAO接口的字节码对象
        Class daoClazz = Class.forName(daoClassPath);
        //（2）根据字节码最新，得到DAO接口中的方法数组
        Method[] methods = daoClazz.getMethods();
        //（3）遍历Method[]数组
        for (Method method : methods) {
            //（3-0）准备好mapper对象和key
            String key = daoClassPath + "." + method.getName();
            Mapper mapper = new Mapper();
            //（3-1）method上存在@Select注解，获得mapper值
            if (method.isAnnotationPresent(Select.class)) {
                String queryString = method.getAnnotation(Select.class).value();
                mapper.setQueryString(queryString);
                // 获取当前方法的返回值，还要求必须带有泛型信息
                Type type = method.getGenericReturnType();		// List<Emp>
                // 判断type是不是参数化类型
                if(type instanceof ParameterizedType) {
                    ParameterizedType ptype = (ParameterizedType)type;
                    Type[] types = ptype.getActualTypeArguments();			// 得到参数化类型中的实际类型参数
                    Class domainClazz = (Class)types[0];
                    String resultType = domainClazz.getName();
                    mapper.setResultType(resultType);
                }
            }
            //（3-2）method上存在@Update注解，获得mapper值
            if (method.isAnnotationPresent(Update.class)) {
                。。。。。。。
            }
            //（3-3）method上存在@Delete注解，获得mapper值
            if (method.isAnnotationPresent(Delete.class)) {
                。。。。。。
            }
            //（3-4）method上存在@Insert注解，获得mapper值
            if (method.isAnnotationPresent(Insert.class)) {
                。。。。。。
            }
            mappers.put(key,mapper);
        }
        return mappers;
    }
    
}
```

```java
package org.apache.ibatis.utils;

public class Executor {
    public <E> List<E> selectList(Mapper mapper, Connection conn) {
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            //（1）取出mapper中的数据
            String queryString = mapper.getQueryString();		// select * from t_Emp
            String resultType = mapper.getResultType();			// com.woodbox.entity.Emp
            Class domainClazz = Class.forName(resultType);
            //（2）获取PreparedStatement预处理对象，传入SQL语句
            pstm = conn.preparedStatement(queryString);
            //（3）执行SQL语句，获取结果集
            rs = pstm.executeQuery();
            //（4）封装结果集
            List<E> list = new ArrayList<E>();
            while (rs.next()) {
                //（4-1）实例化要封装的实体类对象
                E obj = (E)domainClazz.newInstance();
                //（4-2）取出结果集的元信息：ResultSetMetaData
                ResultSetMetaData rsmd = rs.getMetaData();
                //（4-3）取出总列数并遍历
                int columnCount = rsmd.getColumnCount();
                for (int i =1; i<=columnCount; i++) {
                    String columnName = rsmd.getColumnName(i);
                    Object columnValue = rs.getObject(columnName);
                    PropertyDescriptor pd = new PropertyDescriptor(columnName,domainClazz);
                    Method writeMethod = pd.getWriteMethod();
                    writeMethod.invoke(obj,columnValue);
                }
                list.add(obj);
            }
            retutn list;
        } catch (Exception e) {
            throw new RuntimeExceptin(e);
        } finally {
            release(pstm,rs);
        }
    }
    
    // 方法重载，用于自己实现XxxDAOImpl实现类时候调用
    public <E> List<E> selectList(String mappersKey) {
        // .......
    }
    
    private void release(PreparedStatement pstm, ResultSet rs) {
        if (rs!=null) {
            try {
                rs.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (pstm!=null) {
            try {
                pstm.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

```java
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
```

```xml
<!-- Maven需要导入dom4j、jaxen的坐标 -->
<dependency>
    <groupId>dom4j</groupId>
    <artifactId>dom4j</artifactId>
    <version>1.6.1</version>
</dependency>
<dependency>
    <groupId>jaxen</groupId>
    <artifactId>jaxen</artifactId>
    <version>1.1.6</version>
</dependency>
```

```java
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
```

```java
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
```

```java
package org.apache.ibatis.session;

public interface SqlSessionFactory {
    /**
     * 用于打开一个新的SqlSession对象
     * @return
     */
    SqlSession openSession();
}
```

```java
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
```

```java
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
```

```java
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
```

```java
public class Test {
    public static void main(String[] args) throws Exception {
        InputStream in = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(in);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        EmpDAO empDAO = sqlSession.getMapper(EmpDAO.class);
        List<Emp> emps = empDAO.findAll();
        for (Emp emp : emps) {
            System.out.println(emp);
        }
        sqlSession.close();
        in.close();
    }
}
```


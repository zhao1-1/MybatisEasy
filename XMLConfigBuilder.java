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

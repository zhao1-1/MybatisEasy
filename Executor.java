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

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

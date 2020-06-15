package me.zyee.hibatis.bytecode;

import me.zyee.hibatis.config.HiBatisConfig;
import me.zyee.hibatis.template.HiBatisTemplate;
import me.zyee.hibatis.template.factory.TemplateFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;

import java.io.File;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/11
 */
public class DaoGeneratorTest {

    /**
     * 使用方法：
     * <p>
     * 1 构造原生hibernate 的Configuration
     * <p>
     * 2 构造HiBatisConfig
     * <p>
     * 3 设置xml的查找路径
     * <p>
     * 4 构建TemplateFactory
     * <p>
     * 5 构建HiBatisTemplate
     * <p>
     * 6 执行对应的方法
     * <p>
     * 用例生成的Dao代码：
     * <p>
     * <pre>
     * package me.zyee.dynamic.hql.bytecode.$gen.impl;
     *
     * import java.util.HashMap;
     * import java.util.List;
     * import java.util.Map;
     * import me.zyee.dynamic.hql.bytecode.Test;
     * import me.zyee.dynamic.hql.bytecode.TestDao;
     * import me.zyee.dynamic.hql.bytecode.TestEntity;
     * import org.apache.commons.lang3.reflect.FieldUtils;
     * import org.hibernate.Session;
     * import org.hibernate.query.NativeQuery;
     * import org.hibernate.query.Query;
     * import org.hibernate.transform.Transformers;
     *
     * public final class TestDaoImpl$1591936845881 implements TestDao {
     *     private Session session;
     *     private final Class entityClass;
     *
     *     private TestDaoImpl$1591936845881(Session session) {
     *         this.session = session;
     *         this.entityClass = TestEntity.class;
     *     }
     *
     *     public int insert(String id, String name) {
     *         NativeQuery query = this.session.createSQLQuery("insert into test values (:id, :name)");
     *         Map temp_0 = new HashMap(2);
     *         temp_0.put("id", id);
     *         temp_0.put("name", name);
     *         query.setProperties(temp_0);
     *         return query.executeUpdate();
     *     }
     *
     *     public TestEntity findById(Test var_arg0) {
     *         Query query = this.session.createQuery("from TestEntity where id = :id");
     *         Map temp_0 = new HashMap(1);
     *         temp_0.put("id", FieldUtils.readField(var_arg0, "id", true));
     *         query.setProperties(temp_0);
     *         return (TestEntity)query.getSingleResult();
     *     }
     *
     *     public List findNativeAll() {
     *         NativeQuery query = this.session.createSQLQuery("select * from test");
     *         query.setResultTransformer(Transformers.aliasToBean(this.entityClass));
     *         return query.getResultList();
     *     }
     *
     *     public List findAll() {
     *         Query query = this.session.createQuery("from TestEntity");
     *         return query.getResultList();
     *     }
     *
     *     public static TestDaoImpl$1591936845881 newInstance(Session session) {
     *         return new TestDaoImpl$1591936845881(session);
     *     }
     * }
     * </pre>
     *
     * @throws Exception
     */
    @Test
    public void generate() throws Exception {
        // 1 构造原生hibernate 的Configuration
        final Configuration configuration = getConfiguration();
        // 2 构造HiBatisConfig
        final HiBatisConfig hiBatisConfig = new HiBatisConfig();
        hiBatisConfig.setConfiguration(configuration);
        // 3 设置xml的查找路径
        hiBatisConfig.setDaoXmlScanPath("");
        // 4 构建TemplateFactory
        final TemplateFactory templateFactory = hiBatisConfig.buildTemplateFactory();
        // 5 构建HiBatisTemplate
        final HiBatisTemplate template = templateFactory.createTemplate();
        // 6 执行对应的方法
        final Object testEntities = template.runTx(TestDao.class, ((session, testDao) -> testDao.findAll()));
        System.out.println(testEntities);
//        final InputStream resourceAsStream = Class.class.getResourceAsStream("/TestDao.xml");
//        final DaoInfo parse = DomParser.parse(resourceAsStream);
//        final Class<?> generate = DaoGenerator.generate(parse, Paths.get("/Users/yee/work/tmp"));
    }

    private Configuration getConfiguration() {
        Configuration configuration = new Configuration();
        File confFile = new File("hibernate.cfg.xml");
        if (confFile.exists()) {
            configuration.configure(confFile);
        } else {
            configuration.configure("hibernate.cfg.xml");
        }

        configuration.addAnnotatedClass(TestEntity.class);
        return configuration;
    }

    public static final class Test2q1 {

    }
}
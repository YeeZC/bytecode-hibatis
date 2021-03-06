package me.zyee.hibatis.bytecode;

import io.airlift.bytecode.ClassDefinition;
import io.airlift.bytecode.ClassGenerator;
import io.airlift.bytecode.DynamicClassLoader;
import me.zyee.hibatis.bytecode.compiler.dao.DaoCompiler;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.parser.DomParser;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/11
 */
public class HibatisGeneratorTest {

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
        final InputStream resourceAsStream = Class.class.getResourceAsStream("/dao/TestDao.xml");
        final DaoInfo parse = DomParser.parse(resourceAsStream);
//        final DaoRegistry daoRegistry = new DaoRegistry();
////        daoRegistry.addDao(parse);
        final ClassDefinition compile = new DaoCompiler().compile(parse);
        final DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(HibatisGenerator.class.getClassLoader()
                , Collections.emptyMap());
        // 生成方法
        ClassGenerator.classGenerator(dynamicClassLoader)
                .dumpClassFilesTo(Paths.get("/Users/yee/work/tmp1")).defineClass(compile, parse.getId());
//        final TestDao newDao = daoRegistry.getNewDao(TestDao.class, null);
//        final Class<?> generate = DaoGenerator.generate(parse, Paths.get("/Users/yee/work/tmp"));
//        Assert.assertNotNull(generate);
    }

}
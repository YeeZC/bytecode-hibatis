package me.zyee.hibatis.example;

import com.mysql.jdbc.Driver;
import me.zyee.hibatis.bytecode.TestBean;
import me.zyee.hibatis.bytecode.TestDao;
import me.zyee.hibatis.bytecode.TestEntity;
import me.zyee.hibatis.config.HiBatisConfig;
import me.zyee.hibatis.exception.HibatisException;
import me.zyee.hibatis.query.page.PageHelper;
import me.zyee.hibatis.query.page.PageInfo;
import me.zyee.hibatis.template.HiBatisTemplate;
import me.zyee.hibatis.template.factory.TemplateFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class Example {
    public static void main(String[] args) throws HibatisException, IOException {
        final HiBatisConfig hiBatisConfig = new HiBatisConfig();
        hiBatisConfig.setXmlPattern("*Dao.xml");
        hiBatisConfig.setDialect("org.hibernate.dialect.MySQL57Dialect");
        hiBatisConfig.setUsername("admin");
        hiBatisConfig.setPassword("admin");
        hiBatisConfig.setDriverClass(Driver.class.getName());
        hiBatisConfig.setUrl("jdbc:mysql://localhost:3306/test_hibernate");
        hiBatisConfig.addEntity(TestEntity.class);
        final TemplateFactory templateFactory = hiBatisConfig.buildTemplateFactory();
        final HiBatisTemplate template = templateFactory.createTemplate();
        final Object count = template.runTx(TestDao.class, ((dao, session) -> {
            PageHelper.startPage(0, 2);
            final List<TestBean> allNative = dao.findAllNative();
            return PageInfo.of(allNative);
        }));

        System.out.println("find count " + count);
    }


}

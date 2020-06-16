package me.zyee.hibatis.example;

import me.zyee.hibatis.bytecode.TestDao;
import me.zyee.hibatis.bytecode.TestEntity;
import me.zyee.hibatis.config.HiBatisConfig;
import me.zyee.hibatis.template.HiBatisTemplate;
import me.zyee.hibatis.template.factory.TemplateFactory;
import org.hibernate.cfg.Configuration;

import java.io.File;
import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class Example {
    public static void main(String[] args) {
        final HiBatisConfig hiBatisConfig = new HiBatisConfig();
        hiBatisConfig.setXmlPattern("*Dao.xml");
        hiBatisConfig.setConfiguration(getConfiguration());
        final TemplateFactory templateFactory = hiBatisConfig.buildTemplateFactory();
        final HiBatisTemplate template = templateFactory.createTemplate();
        final List list = template.runNonTx(TestDao.class, ((dao, session) -> dao.findNativeAll()));
        System.out.println(list);
    }


    private static Configuration getConfiguration() {
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
}

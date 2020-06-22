package me.zyee.hibatis.template.factory.impl;

import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.datasource.DataSource;
import me.zyee.hibatis.template.HiBatisTemplate;
import me.zyee.hibatis.template.factory.TemplateFactory;
import me.zyee.hibatis.template.impl.BaseHiBatisTemplate;
import org.hibernate.SessionFactory;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class DefaultTemplateFactory implements TemplateFactory {
    //    private final Configuration configuration;
    private final DataSource dataSource;
    private final DaoRegistry registry;
    private SessionFactory sessionFactory;

    public DefaultTemplateFactory(DataSource configuration, DaoRegistry registry) {
        this.dataSource = configuration;
        this.registry = registry;
    }

    @Override
    public HiBatisTemplate createTemplate() {
        return new BaseHiBatisTemplate(dataSource, registry);
    }

//    synchronized
//    public SessionFactory getFactory() {
//        if (null == sessionFactory) {
//            sessionFactory = initSessionFactory();
//        }
//        return sessionFactory;
//    }
//
//    private SessionFactory initSessionFactory() {
//        ServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(
//                configuration.getProperties()).build();
//        return configuration.buildSessionFactory(registry);
//    }
}

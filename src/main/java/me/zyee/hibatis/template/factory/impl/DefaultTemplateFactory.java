package me.zyee.hibatis.template.factory.impl;

import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.template.HiBatisTemplate;
import me.zyee.hibatis.template.factory.TemplateFactory;
import me.zyee.hibatis.template.impl.BaseHiBatisTemplate;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/15
 */
public class DefaultTemplateFactory implements TemplateFactory {
    private final Configuration configuration;
    private final DaoRegistry registry;
    private SessionFactory sessionFactory;

    public DefaultTemplateFactory(Configuration configuration, DaoRegistry registry) {
        this.configuration = configuration;
        this.registry = registry;
    }

    @Override
    public HiBatisTemplate createTemplate() {
        return new BaseHiBatisTemplate(getFactory(), registry);
    }

    synchronized
    public SessionFactory getFactory() {
        if (null == sessionFactory) {
            sessionFactory = initSessionFactory();
        }
        return sessionFactory;
    }

    private SessionFactory initSessionFactory() {
        ServiceRegistry registry = new StandardServiceRegistryBuilder().applySettings(
                configuration.getProperties()).build();
        return configuration.buildSessionFactory(registry);
    }
}

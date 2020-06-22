package me.zyee.hibatis.datasource.impl;

import me.zyee.hibatis.common.LazyGet;
import me.zyee.hibatis.common.SupplierLazyGet;
import me.zyee.hibatis.config.HiBatisConfig;
import me.zyee.hibatis.datasource.DataSource;
import me.zyee.hibatis.datasource.annotation.ConfigProperty;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/22
 */
public class DefaultDataSource implements DataSource {
    private final SupplierLazyGet<SessionFactory> factoryHolder;
    private final HiBatisConfig config;

    public DefaultDataSource(HiBatisConfig config) {
        this.config = config;
        this.factoryHolder = LazyGet.of(() -> {
            final BootstrapServiceRegistryBuilder builder = new BootstrapServiceRegistryBuilder();
            final Configuration configuration = new Configuration(builder.build())
                    .addProperties(config.toProperties());
            Optional<Properties> properties = Optional.ofNullable(toProperties());
            properties.ifPresent(configuration::addProperties);
            final Set<Class<?>> entityClasses = config.getEntityClasses();
            for (Class<?> entityClass : entityClasses) {
                configuration.addAnnotatedClass(entityClass);
            }
            return configuration.buildSessionFactory();
        });
    }

    @Override
    public Session createSession() {
        return factoryHolder.get().openSession();
    }

    @Override
    public String driverClass() {
        return config.getDriverClass();
    }

    @Override
    public String username() {
        return config.getUsername();
    }

    @Override
    public String password() {
        return config.getPassword();
    }

    @Override
    public String url() {
        return config.getUrl();
    }

    @Override
    public Properties toProperties() {
        final Properties properties = new Properties();
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(getClass(), ConfigProperty.class);
        for (Field field : fields) {
            final ConfigProperty conf = field.getAnnotation(ConfigProperty.class);
            try {
                final Object o = Optional.ofNullable(
                        FieldUtils.readField(field, this, true))
                        .orElse(conf.defaultValue());
                String property = StringUtils.isEmpty(conf.value()) ? field.getName() : conf.value();
                properties.setProperty("hibernate." + property, o.toString());
                properties.setProperty(property, o.toString());
            } catch (IllegalAccessException ignore) {
            }
        }
        return properties;
    }
}

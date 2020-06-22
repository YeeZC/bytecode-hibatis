package me.zyee.hibatis.config;

import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.dao.scaner.DaoScanner;
import me.zyee.hibatis.datasource.DataSource;
import me.zyee.hibatis.datasource.annotation.ConfigProperty;
import me.zyee.hibatis.datasource.impl.DefaultDataSource;
import me.zyee.hibatis.template.factory.TemplateFactory;
import me.zyee.hibatis.template.factory.impl.DefaultTemplateFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class HiBatisConfig {
    @ConfigProperty
    private String username;
    @ConfigProperty
    private String password;
    @ConfigProperty
    private String dialect;
    @ConfigProperty
    private String url;
    @ConfigProperty
    private String driverClass;
    private boolean showSql;
    private final Set<Class<?>> entityClasses = new HashSet<>();
    private final Properties properties = new Properties();
    private String cfgPath;
    private Class<? extends DataSource> dataSource;
    /**
     * dao xml的扫描路径
     */
    private String scanPath;

    private String xmlPattern;

    public String getScanPath() {
        return scanPath;
    }

    public HiBatisConfig scanPath(String scanPath) {
        this.scanPath = scanPath;
        return this;
    }

    public String getXmlPattern() {
        return xmlPattern;
    }

    public HiBatisConfig xmlPattern(String xmlPattern) {
        this.xmlPattern = xmlPattern;
        return this;
    }

    /**
     * 1 搜索dao.xml
     * 2 初始化registry
     * 3 初始化TemplateFactory
     *
     * @return
     */
    public TemplateFactory buildTemplateFactory() {
        final DaoRegistry daoRegistry = new DaoRegistry();
        if (null == scanPath) {
            scanPath = StringUtils.EMPTY;
        }
        DaoScanner.scan(scanPath, xmlPattern).forEach(daoRegistry::addDao);
        if (null == dataSource) {
            dataSource = DefaultDataSource.class;
        }
        final Constructor<? extends DataSource> constructor =
                ConstructorUtils.getAccessibleConstructor(dataSource, HiBatisConfig.class);
        try {
            return new DefaultTemplateFactory(constructor.newInstance(this), daoRegistry);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            return new DefaultTemplateFactory(new DefaultDataSource(this), daoRegistry);
        }
    }

    public Properties toProperties() {
        properties.setProperty("hibernate.connection.autocommit", Boolean.toString(false));
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        properties.setProperty("hibernate.dialect", dialect);
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("format_sql", "true");
        properties.setProperty("hibernate.show_sql", Boolean.toString(showSql));
        properties.setProperty("show_sql", Boolean.toString(showSql));
        properties.setProperty("hibernate.connection.url", url);
        properties.setProperty("hibernate.connection.driver_class", driverClass);
        properties.setProperty("hibernate.connection.username", username);
        properties.setProperty("hibernate.connection.password", password);
        properties.setProperty("hibernate.connection.isolation", "4");
        properties.setProperty("hibernate.current_session_context_class", "thread");

        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(HiBatisConfig.class, ConfigProperty.class);
        for (Field field : fields) {
            final ConfigProperty conf = field.getAnnotation(ConfigProperty.class);
            try {
                final Object o = Optional.ofNullable(
                        FieldUtils.readField(field, this, true))
                        .orElse(conf.defaultValue());
                String value = StringUtils.isEmpty(conf.value()) ? field.getName() : conf.value();
                properties.setProperty("hibernate." + value, o.toString());
                properties.setProperty(value, o.toString());
            } catch (IllegalAccessException ignore) {
            }
        }
        return properties;
    }

    public Set<Class<?>> getEntityClasses() {
        return entityClasses;
    }

    public String getUsername() {
        return username;
    }

    public HiBatisConfig username(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public HiBatisConfig password(String password) {
        this.password = password;
        return this;
    }

    public String getDialect() {
        return dialect;
    }

    public HiBatisConfig dialect(String dialect) {
        this.dialect = dialect;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public HiBatisConfig url(String url) {
        this.url = url;
        return this;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public HiBatisConfig driverClass(String driverClass) {
        this.driverClass = driverClass;
        return this;
    }

    public HiBatisConfig addEntity(Class<?> entityClasses) {
        this.entityClasses.add(entityClasses);
        return this;
    }

    public boolean getShowSql() {
        return showSql;
    }

    public HiBatisConfig showSql(boolean showSql) {
        this.showSql = showSql;
        return this;
    }

    public String getCfgPath() {
        return cfgPath;
    }

    public HiBatisConfig cfgPath(String cfgPath) {
        this.cfgPath = cfgPath;
        return this;
    }

    public HiBatisConfig dataSource(Class<? extends DataSource> dataSource) {
        this.dataSource = dataSource;
        return this;
    }
}

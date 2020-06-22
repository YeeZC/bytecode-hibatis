package me.zyee.hibatis.config;

import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.dao.scaner.DaoScanner;
import me.zyee.hibatis.datasource.annotation.ConfigProperty;
import me.zyee.hibatis.datasource.impl.DruidDataSource;
import me.zyee.hibatis.template.factory.TemplateFactory;
import me.zyee.hibatis.template.factory.impl.DefaultTemplateFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.cfg.Configuration;

import java.lang.reflect.Field;
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

    /**
     * dao xml的扫描路径
     */
    private String scanPath;

    private String xmlPattern;

    public void setConfiguration(Configuration configuration) {
    }

    public String getScanPath() {
        return scanPath;
    }

    public void setScanPath(String scanPath) {
        this.scanPath = scanPath;
    }

    public String getXmlPattern() {
        return xmlPattern;
    }

    public void setXmlPattern(String xmlPattern) {
        this.xmlPattern = xmlPattern;
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
        return new DefaultTemplateFactory(new DruidDataSource(this), daoRegistry);
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

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDialect() {
        return dialect;
    }

    public void setDialect(String dialect) {
        this.dialect = dialect;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public HiBatisConfig addEntity(Class<?> entityClasses) {
        this.entityClasses.add(entityClasses);
        return this;
    }

    public boolean getShowSql() {
        return showSql;
    }

    public void setShowSql(boolean showSql) {
        this.showSql = showSql;
    }
}

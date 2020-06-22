package me.zyee.hibatis.datasource.impl;

import me.zyee.hibatis.config.HiBatisConfig;
import me.zyee.hibatis.datasource.annotation.ConfigProperty;

import java.util.Properties;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/22
 */
public class HikariDataSource extends DefaultDataSource {

    @ConfigProperty(value = "hikari.connectionTimeout", defaultValue = "30000")
    private Long connectTimeout;
    @ConfigProperty(value = "hikari.minimumIdle", defaultValue = "5")
    private Integer minIdle;
    @ConfigProperty(value = "hikari.maximumPoolSize", defaultValue = "20")
    private Integer maxPoolSize;
    @ConfigProperty(value = "hikari.autoCommit", defaultValue = "false")
    private Boolean autoCommit;
    @ConfigProperty(value = "hikari.idleTimeout", defaultValue = "600000")
    private Long idleTimeout;
    @ConfigProperty(value = "hikari.poolName", defaultValue = "hibatis-hikari")
    private String poolName;
    @ConfigProperty(value = "hikari.maxLifetime", defaultValue = "1800000")
    private Long maxLifeTime;
    @ConfigProperty(value = "hikari.connectionTestQuery", defaultValue = "select 1")
    private String testQuery;

    public HikariDataSource(HiBatisConfig config) {
        super(config);

    }

    @Override
    public Properties toProperties() {
        final Properties properties = super.toProperties();
        properties.setProperty("hibernate.connection.provider_class", "com.zaxxer.hikari.hibernate.HikariConnectionProvider");
        return properties;
    }

    public Long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(Integer minIdle) {
        this.minIdle = minIdle;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public Long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(Long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public Long getMaxLifeTime() {
        return maxLifeTime;
    }

    public void setMaxLifeTime(Long maxLifeTime) {
        this.maxLifeTime = maxLifeTime;
    }

    public String getTestQuery() {
        return testQuery;
    }

    public void setTestQuery(String testQuery) {
        this.testQuery = testQuery;
    }
}

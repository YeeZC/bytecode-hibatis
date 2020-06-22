package me.zyee.hibatis.datasource.impl;

import me.zyee.hibatis.config.HiBatisConfig;
import me.zyee.hibatis.datasource.annotation.ConfigProperty;

import java.util.Properties;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/22
 */
public class DruidDataSource extends DefaultDataSource {
    @ConfigProperty(defaultValue = "5")
    private Integer initialSize;
    @ConfigProperty(defaultValue = "100")
    private Integer maxActive;
    @ConfigProperty(defaultValue = "60000")
    private Integer maxWait;
    @ConfigProperty(defaultValue = "60000")
    private Long timeBetweenEvictionRunsMillis;
    @ConfigProperty(defaultValue = "300000")
    private Long minEvictableIdleTimeMillis;
    @ConfigProperty(defaultValue = "select 1")
    private String validationQuery;
    @ConfigProperty(defaultValue = "true")
    private Boolean testWhileIdle;
    @ConfigProperty(defaultValue = "false")
    private Boolean testOnBorrow;
    @ConfigProperty(defaultValue = "false")
    private Boolean testOnReturn;
    @ConfigProperty(defaultValue = "false")
    private Boolean poolPreparedStatements;
    @ConfigProperty(defaultValue = "200")
    private Integer maxPoolPreparedStatementPerConnectionSize;

    public DruidDataSource(HiBatisConfig config) {
        super(config);
    }

    @Override
    public Properties toProperties() {
        final Properties properties = super.toProperties();
        properties.setProperty("hibernate.connection.provider_class", "com.alibaba.druid.support.hibernate.DruidConnectionProvider");

        properties.setProperty("hibernate.driverClassName", driverClass());
        properties.setProperty("hibernate.filter", "stat,log4j");

        return properties;
    }

    public Integer getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(Integer initialSize) {
        this.initialSize = initialSize;
    }

    public Integer getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(Integer maxActive) {
        this.maxActive = maxActive;
    }

    public Integer getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(Integer maxWait) {
        this.maxWait = maxWait;
    }

    public Long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(Long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public Long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(Long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public Boolean getTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(Boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public Boolean getTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(Boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public Boolean getTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(Boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public Boolean getPoolPreparedStatements() {
        return poolPreparedStatements;
    }

    public void setPoolPreparedStatements(Boolean poolPreparedStatements) {
        this.poolPreparedStatements = poolPreparedStatements;
    }

    public Integer getMaxPoolPreparedStatementPerConnectionSize() {
        return maxPoolPreparedStatementPerConnectionSize;
    }

    public void setMaxPoolPreparedStatementPerConnectionSize(Integer maxPoolPreparedStatementPerConnectionSize) {
        this.maxPoolPreparedStatementPerConnectionSize = maxPoolPreparedStatementPerConnectionSize;
    }
}

package me.zyee.hibatis.config;

import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.dao.scaner.DaoScanner;
import me.zyee.hibatis.template.factory.TemplateFactory;
import me.zyee.hibatis.template.factory.impl.DefaultTemplateFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.cfg.Configuration;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class HiBatisConfig {
    /**
     * hibernate原生config
     */
    private Configuration configuration;
    /**
     * dao xml的扫描路径
     */
    private String scanPath;

    private String xmlPattern;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
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
        if (null == configuration) {
            throw new RuntimeException("Configuration must be null");
        }
        final DaoRegistry daoRegistry = new DaoRegistry();
        if (null == scanPath) {
            scanPath = StringUtils.EMPTY;
        }
        DaoScanner.scan(scanPath, xmlPattern).forEach(daoRegistry::addDao);
        return new DefaultTemplateFactory(configuration, daoRegistry);
    }
}

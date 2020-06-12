package me.zyee.hibatis.config;

import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.registry.DaoRegistry;
import me.zyee.hibatis.dao.scaner.DaoScanner;
import me.zyee.hibatis.template.factory.TemplateFactory;
import me.zyee.hibatis.template.factory.impl.DefaultTemplateFactory;
import org.hibernate.cfg.Configuration;

import java.util.List;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class HiBatisConfig {
    private Configuration configuration;
    private String daoXmlScanPath;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getDaoXmlScanPath() {
        return daoXmlScanPath;
    }

    public void setDaoXmlScanPath(String daoXmlScanPath) {
        this.daoXmlScanPath = daoXmlScanPath;
    }

    /**
     * 1 搜索dao.xml
     * 2 初始化registry
     * 3 初始化TemplateFactory
     * @return
     */
    public TemplateFactory buildTemplateFactory() {
        if (null == configuration) {
            throw new RuntimeException("Configuration must be null");
        }
        final DaoRegistry daoRegistry = new DaoRegistry();
        if (null != daoXmlScanPath) {
            final List<DaoInfo> scan = DaoScanner.scan(daoXmlScanPath);
            scan.forEach(daoRegistry::addDao);
        }
        return new DefaultTemplateFactory(configuration, daoRegistry);
    }
}

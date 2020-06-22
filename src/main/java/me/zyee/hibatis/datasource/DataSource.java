package me.zyee.hibatis.datasource;

import org.hibernate.Session;

import java.util.Properties;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/22
 */
public interface DataSource {
    /**
     * 创建Session
     *
     * @return
     */
    Session createSession();

    String driverClass();

    String username();

    String password();

    String url();

    Properties toProperties();
}

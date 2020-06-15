package me.zyee.hibatis.bytecode;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/16
 */
public class TestBean {
    private String hello;
    private String gogogo;

    public String getHello() {
        return hello;
    }

    public void setHello(String hello) {
        this.hello = hello;
    }

    public String getGogogo() {
        return gogogo;
    }

    public void setGogogo(String gogogo) {
        this.gogogo = gogogo;
    }

    @Override
    public String toString() {
        return "TestBean{" +
                "hello='" + hello + '\'' +
                ", gogogo='" + gogogo + '\'' +
                '}';
    }
}

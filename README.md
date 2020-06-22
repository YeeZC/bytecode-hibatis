# ByteCode Hibatis

基于动态字节码和hibernate，参考mybatis实现的简单xml的`sql/hql`访问数据库的小组件。

## Example
* 实体类

```java
package me.zyee.hibatis.bytecode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/11
 */
@Entity
@Table(name = "test")
public class TestEntity {
    @Id
    String id;
    @Column
    String name;

    private TestEntity() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}

```

* Dao接口

```java
package me.zyee.hibatis.bytecode;

import me.zyee.hibatis.dao.annotation.Param;

import java.util.List;

/**
 * @author yee
 * @version 1.0
 * Created by yee on 2020/6/11
 */
public interface TestDao {
    List<TestBean> findAll();

    int getAllCount();

    TestEntity findById(@Param("id") String id);

    int insert(@Param("id") String id, @Param("name") String name);
}

```

* Dao对应的Dao.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE hibatis SYSTEM "hibatis.dtd">

<hibatis id="me.zyee.hibatis.bytecode.TestDao" entity="me.zyee.hibatis.bytecode.TestEntity">
    <map id="all" class="me.zyee.hibatis.bytecode.TestBean">
        <column column="id" field="hello" javaType="java.lang.String"/>
        <column column="name" field="gogogo" javaType="java.lang.String"/>
    </map>
    <select id="findAll" resultMap="all">
        select id, name from TestEntity
    </select>
    <select id="getAllCount" native="true">
        select count(*) from test
    </select>
    <select id="findById">
        from TestEntity where id = :id
    </select>

    <insert id="insert">
        insert into test values (:id, :name)
    </insert>
</hibatis>
```

* 使用方法

```java
package me.zyee.hibatis.example;

import me.zyee.hibatis.bytecode.TestDao;
import me.zyee.hibatis.bytecode.TestEntity;
import me.zyee.hibatis.config.HiBatisConfig;
import me.zyee.hibatis.template.HiBatisTemplate;
import me.zyee.hibatis.template.factory.TemplateFactory;

/**
 * @author yee
 * @version 1.0
 * Create by yee on 2020/6/16
 */
public class Example {
    public static void main(String[] args) {
        final HiBatisConfig hiBatisConfig = new HiBatisConfig();
        hiBatisConfig.xmlPattern("*Dao.xml");
        hiBatisConfig.dialect("org.hibernate.dialect.MySQL57Dialect");
        hiBatisConfig.username("admin");
        hiBatisConfig.password("admin");
        hiBatisConfig.driverClass(Driver.class.getName());
        hiBatisConfig.url("jdbc:mysql://localhost:3306/test_hibernate");
        hiBatisConfig.addEntity(TestEntity.class);
        final TemplateFactory templateFactory = hiBatisConfig.buildTemplateFactory();
        final HiBatisTemplate template = templateFactory.createTemplate();
        final Object count = template.runTx(TestDao.class, ((dao, session) -> {
            PageHelper.startPage(0, 2);
            final List<TestBean> allNative = dao.findAllNative();
            return PageInfo.of(allNative);
        }));
        System.out.println("find count " + count);
    }
}

```



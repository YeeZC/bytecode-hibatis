package me.zyee.hibatis.parser;

import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMapInfo;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.DaoProperty;
import me.zyee.hibatis.dao.MethodType;
import me.zyee.hibatis.dao.annotation.Attr;
import me.zyee.hibatis.dao.annotation.Children;
import me.zyee.hibatis.dao.annotation.Content;
import me.zyee.hibatis.exception.HibatisAttrAbsentException;
import me.zyee.hibatis.exception.HibatisXmlIllegalException;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yee
 * Created by yee on 2020/6/11
 **/
public class DomParser {
    private static final String DAO = "hibatis";
    private static final String SELECT = "select";
    private static final String UPDATE = "update";
    private static final String INSERT = "insert";
    private static final String DELETE = "delete";
    private static final String MAP = "map";

    /**
     * 解析xml成DaoInfo
     *
     * @param dom xml的inputstream
     * @return 返回解析后的DaoInfo
     * @throws Exception 解析异常
     */
    public static DaoInfo parse(InputStream dom) {
        try (InputStream is = dom) {
            SAXReader reader = new SAXReader();
            Document document = reader.read(is);
            Element root = document.getRootElement();
            // 根节点
            if (DAO.equals(root.getName())) {
                List<Element> elements = root.elements();
                DaoInfo daoInfo = readAttributes(root, new DaoInfo());
                daoInfo.setMethodInfos(new ArrayList<>());
                daoInfo.setMaps(new ArrayList<>());
                for (Element element : elements) {
                    String name = element.getName();
                    switch (name) {
                        case SELECT:
                            // 解析查询
                            parseMethod(daoInfo, element, MethodType.SELECT, false);
                            break;
                        case UPDATE:
                        case DELETE:
                            // 解析delete 和 update 因为执行方法是一样的，就当成一类
                            parseMethod(daoInfo, element, MethodType.MODIFY, false);
                            break;
                        case INSERT:
                            // 解析insert 因为 hibernate 通过 hql 没办法插入，只能通过原生sql插入
                            parseMethod(daoInfo, element, MethodType.MODIFY, true);
                            break;
                        case MAP: {
                            DaoMapInfo mapInfo = readAttributes(element, new DaoMapInfo());
                            List<DaoProperty> properties = new ArrayList<>();
                            for (Element e : element.elements()) {
                                properties.add(readAttributes(e, new DaoProperty()));
                            }
                            mapInfo.setProperties(properties);
                            daoInfo.getMaps().add(mapInfo);
                            break;
                        }
                        default:
                    }
                }
                check(daoInfo);
                return daoInfo;
            } else {
                throw new HibatisXmlIllegalException();
            }

        } catch (IOException | DocumentException e) {
            throw new HibatisXmlIllegalException(e);
        }
    }


    private static void parseMethod(DaoInfo dao, Element element, MethodType type, boolean nativeSql) {

        final DaoMethodInfo method = readAttributes(element, new DaoMethodInfo());
        method.setType(type);
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(DaoMethodInfo.class, Content.class);
        if (fields.size() != 1) {
            throw new HibatisXmlIllegalException(element.asXML());
        }
        try {
            FieldUtils.writeField(fields.get(0), method, element.getStringValue(), true);
            if (null == method.isNativeSql()) {
                method.setNativeSql(nativeSql);
            }
        } catch (IllegalAccessException e) {
            throw new HibatisXmlIllegalException(element.asXML(), e);
        }
        check(method);
        dao.getMethodInfos().add(method);
    }

    /**
     * 根据实体类属性上打的注解解析
     *
     * @param child
     * @param property
     * @param <T>
     * @return
     * @throws Exception
     */
    private static <T> T readAttributes(Element child, T property) {
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(property.getClass(), Attr.class);
        for (Field field : fields) {
            final Attr attr = field.getAnnotation(Attr.class);
            final Attribute attribute = child.attribute(attr.value());
            if (null == attribute) {
                if (attr.require()) {
                    throw new HibatisAttrAbsentException(attr.value());
                }
            } else {
                final Class<?> type = field.getType();
                try {
                    if (ClassUtils.isAssignable(int.class, type)) {
                        FieldUtils.writeField(field, property, Integer.parseInt(attribute.getStringValue()), true);
                    } else if (ClassUtils.isAssignable(long.class, type)) {
                        FieldUtils.writeField(field, property, Long.parseLong(attribute.getStringValue()), true);
                    } else if (ClassUtils.isAssignable(double.class, type)) {
                        FieldUtils.writeField(field, property, Double.parseDouble(attribute.getStringValue()), true);
                    } else if (ClassUtils.isAssignable(boolean.class, type)) {
                        FieldUtils.writeField(field, property, Boolean.parseBoolean(attribute.getStringValue()), true);
                    } else if (ClassUtils.isAssignable(String.class, type)) {
                        FieldUtils.writeField(field, property, attribute.getStringValue(), true);
                    } else if (ClassUtils.isAssignable(Class.class, type)) {
                        FieldUtils.writeField(field, property, ClassUtils.getClass(attribute.getStringValue()), true);
                    } else {
                        FieldUtils.writeField(field, property, attribute.getStringValue(), true);
                    }
                } catch (IllegalAccessException | ClassNotFoundException ignore) {
                }
            }
        }
        return property;
    }

    private static void check(Object dao) {
        final Class<?> aClass = dao.getClass();
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(aClass, Attr.class);
        try {
            for (Field field : fields) {
                final Attr attr = field.getAnnotation(Attr.class);
                if (attr.require()) {
                    final Object o = FieldUtils.readField(field, dao, true);
                    if (o == null) {
                        throw new HibatisAttrAbsentException(attr.value());
                    }
                }
            }
            final List<Field> contents = FieldUtils.getFieldsListWithAnnotation(aClass, Content.class);
            checkSingleField(dao, contents);
            final List<Field> children = FieldUtils.getFieldsListWithAnnotation(aClass, Children.class);
            checkSingleField(dao, children);
        } catch (IllegalAccessException e) {
            throw new HibatisXmlIllegalException(e);
        }
    }

    private static void checkSingleField(Object dao, List<Field> contents) throws IllegalAccessException {
        if (contents.size() > 1) {
            throw new HibatisXmlIllegalException();
        }
        if (!contents.isEmpty()) {
            final Object o = FieldUtils.readField(contents.get(0), dao, true);
            if (o == null) {
                throw new HibatisXmlIllegalException();
            }
        }
    }
}

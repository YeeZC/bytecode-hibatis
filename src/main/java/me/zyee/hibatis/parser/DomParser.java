package me.zyee.hibatis.parser;

import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.dao.DaoMethodInfo;
import me.zyee.hibatis.dao.MethodType;
import me.zyee.hibatis.dao.annotation.Attr;
import me.zyee.hibatis.dao.annotation.Children;
import me.zyee.hibatis.dao.annotation.Content;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

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

    public static DaoInfo parse(InputStream dom) throws Exception {
        try (InputStream is = dom) {
            SAXReader reader = new SAXReader();
            Document document = reader.read(is);
            Element root = document.getRootElement();
            if (DAO.equals(root.getName())) {
                List<Element> elements = root.elements();
                DaoInfo daoInfo = readAttributes(root, new DaoInfo());
                daoInfo.setMethodInfos(new ArrayList<>());
                for (Element element : elements) {
                    String name = element.getName();
                    switch (name) {
                        case SELECT:
                            parseMethod(daoInfo, element, MethodType.SELECT, false);
                            break;
                        case UPDATE:
                            parseMethod(daoInfo, element, MethodType.MODIFY, false);
                            break;
                        case INSERT:
                            parseMethod(daoInfo, element, MethodType.MODIFY, true);
                            break;
                        default:
                    }
                }
                check(daoInfo);
                return daoInfo;
            } else {
                throw new Exception();
            }

        }
    }


    private static void parseMethod(DaoInfo dao, Element element, MethodType type, boolean nativeSql) throws Exception {

        final DaoMethodInfo method = readAttributes(element, new DaoMethodInfo());
        method.setType(type);
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(DaoMethodInfo.class, Content.class);
        if (fields.size() != 1) {
            throw new Exception();
        }
        FieldUtils.writeField(fields.get(0), method, element.getStringValue(), true);
        if (null == method.isNativeSql()) {
            method.setNativeSql(nativeSql);
        }
        check(method);
        dao.getMethodInfos().add(method);
    }

    private static <T> T readAttributes(Element child, T property) throws Exception {
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(property.getClass(), Attr.class);
        for (Field field : fields) {
            final Attr attr = field.getAnnotation(Attr.class);
            final Attribute attribute = child.attribute(attr.value());
            if (null == attribute) {
                if (attr.require()) {
                    throw new Exception("Attribute " + attr.value() + " is require");
                }
            } else {
                final Class<?> type = field.getType();
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
                } else {
                    FieldUtils.writeField(field, property, attribute.getStringValue(), true);
                }

            }
        }
        return property;
    }

    private static void check(Object dao) throws Exception {
        final Class<?> aClass = dao.getClass();
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(aClass, Attr.class);
        for (Field field : fields) {
            final Attr attr = field.getAnnotation(Attr.class);
            if (attr.require()) {
                final Object o = FieldUtils.readField(field, dao, true);
                if (o == null) {
                    throw new Exception("Property " + attr.value() + " is require");
                }
            }
        }
        final List<Field> contents = FieldUtils.getFieldsListWithAnnotation(aClass, Content.class);
        if (contents.size() > 1) {
            throw new Exception("Content more then one");
        }
        if (!contents.isEmpty()) {
            final Object o = FieldUtils.readField(contents.get(0), dao, true);
            if (o == null) {
                throw new Exception("Content is require");
            }
        }
        final List<Field> children = FieldUtils.getFieldsListWithAnnotation(aClass, Children.class);
        if (children.size() > 1) {
            throw new Exception("Children more then one");
        }
        if (!children.isEmpty()) {
            final Object o = FieldUtils.readField(children.get(0), dao, true);
            if (o == null) {
                throw new Exception("Children is require");
            }
        }
    }
}

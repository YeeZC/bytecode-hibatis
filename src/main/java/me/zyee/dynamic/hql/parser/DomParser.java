package me.zyee.dynamic.hql.parser;

import me.zyee.dynamic.hql.config.DaoInfo;
import me.zyee.dynamic.hql.config.DaoMapInfo;
import me.zyee.dynamic.hql.config.DaoMethodInfo;
import me.zyee.dynamic.hql.config.DaoProperty;
import me.zyee.dynamic.hql.config.MethodType;
import me.zyee.dynamic.hql.config.annotation.Attr;
import me.zyee.dynamic.hql.config.annotation.Children;
import me.zyee.dynamic.hql.config.annotation.Content;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yeezc
 * Created by yeezc on 2020/6/11
 **/
public class DomParser {
    private static final String DAO = "dao";
    private static final String DAO_MAP = "map";
    private static final String DAO_MAP_PROPERTY = "property";
    private static final String SELECT = "select";
    private static final String UPDATE = "update";

    public static DaoInfo parse(InputStream dom) throws Exception {
        try (InputStream is = dom) {
            SAXReader reader = new SAXReader();
            Document document = reader.read(is);
            Element root = document.getRootElement();
            List<Element> elements = root.elements();
            DaoInfo daoInfo = null;
            Map<String, DaoMapInfo> maps = new HashMap<>();
            for (Element element : elements) {
                String name = element.getName();
                switch (name) {
                    case DAO:
                        daoInfo = readAttributes(element, new DaoInfo());
                        daoInfo.setMethodInfos(new ArrayList<>());
                        break;
                    case DAO_MAP:
                        parseMap(maps, element);
                        break;
                    case SELECT:
                        parseMethod(daoInfo, element, maps, MethodType.SELECT);
                        break;
                    case UPDATE:
                        parseMethod(daoInfo, element, maps, MethodType.UPDATE);
                        break;
                    default:
                }
            }
            check(daoInfo);
            return daoInfo;
        }
    }


    private static void parseMethod(DaoInfo dao, Element element, Map<String, DaoMapInfo> maps, MethodType type) throws Exception {

        final DaoMethodInfo method = readAttributes(element, new DaoMethodInfo());
        method.setType(type);
        final List<Field> fields = FieldUtils.getFieldsListWithAnnotation(DaoMethodInfo.class, Content.class);
        if (fields.size() != 1) {
            throw new Exception();
        }
        FieldUtils.writeField(fields.get(0), method, element.getStringValue(), true);
        check(method);
        dao.getMethodInfos().add(method);
    }

    private static void parseMap(Map<String, DaoMapInfo> maps, Element element) throws Exception {
        final DaoMapInfo daoMapInfo = readAttributes(element, new DaoMapInfo());
        final List<Field> children = FieldUtils.getFieldsListWithAnnotation(DaoMapInfo.class, Children.class);
        if (children.size() != 1) {
            throw new Exception();
        }
        List<DaoProperty> properties = new ArrayList<>();
        for (Element child : element.elements()) {
            properties.add(readAttributes(child, new DaoProperty()));
        }
        daoMapInfo.setProperties(properties);
        check(daoMapInfo);
        maps.put(daoMapInfo.getMapId(), daoMapInfo);
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
                FieldUtils.writeField(field, property, attribute.getStringValue(), true);
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

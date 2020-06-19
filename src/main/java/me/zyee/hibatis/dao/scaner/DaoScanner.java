package me.zyee.hibatis.dao.scaner;

import me.zyee.hibatis.bytecode.HibatisGenerator;
import me.zyee.hibatis.dao.DaoInfo;
import me.zyee.hibatis.parser.DomParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author yee
 * Created by yee on 2020/6/12
 **/
public class DaoScanner {
    public static List<DaoInfo> scan(String path, String filePattern) {
        ClassLoader loader = HibatisGenerator.getDefaultClassLoader();
        URL resource;
        if (null != loader) {
            resource = loader.getResource(path);
        } else {
            resource = ClassLoader.getSystemResource(path);
        }
        if (null == resource) {
            return Collections.emptyList();
        }
        Predicate<String> predicate = NamePredicate.of(filePattern);
        // 如果是jar包则从jar包中查找，否则从文件中查找
        if (resource.getPath().contains(".jar")) {
            return scanDaoFromJar(new URL[]{resource}, "", true, predicate);
        } else {
            List<DaoInfo> result = new ArrayList<>();
            final File dir = new File(resource.getFile());

            try {
                final List<File> matches = Files.walk(dir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toString)
                        .filter(predicate)
                        .map(File::new).collect(Collectors.toList());
                if (matches.isEmpty()) {
                    return Collections.emptyList();
                }
                for (File file : matches) {
                    try {
                        result.add(DomParser.parse(new FileInputStream(file)));
                    } catch (Exception ignore) {
                    }
                }
                return result;
            } catch (IOException e) {
                return Collections.emptyList();
            }

        }
    }

    /**
     * 从jar获取某包下所有类
     *
     * @param jarPath      jar文件路径
     * @param childPackage 是否遍历子包
     * @param predicate
     * @return 类的完整名称
     */
    private static List<DaoInfo> getClassNameByJar(String jarPath,
                                                   boolean childPackage, Predicate<String> predicate) {
        String[] jarInfo = jarPath.split("!");
        String jarFilePath = jarInfo[0].substring(jarInfo[0].indexOf("/"));
        String packagePath = jarInfo[1].substring(1);
        List<DaoInfo> result = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jarFilePath)) {
            Enumeration<JarEntry> entrys = jarFile.entries();
            while (entrys.hasMoreElements()) {
                JarEntry jarEntry = entrys.nextElement();
                String entryName = jarEntry.getName();
                if (predicate.test(entryName)) {
                    if (childPackage) {
                        if (entryName.startsWith(packagePath)) {
                            result.add(DomParser.parse(jarFile.getInputStream(jarEntry)));
                        }
                    } else {
                        int index = entryName.lastIndexOf("/");
                        String myPackagePath;
                        if (index != -1) {
                            myPackagePath = entryName.substring(0, index);
                        } else {
                            myPackagePath = entryName;
                        }
                        if (myPackagePath.equals(packagePath)) {
                            result.add(DomParser.parse(jarFile.getInputStream(jarEntry)));
                        }
                    }
                }
            }
        } catch (Exception ignore) {
        }

        return result;
    }

    /**
     * 从所有jar中搜索该包，并获取该包下所有类
     *
     * @param urls         URL集合
     * @param packagePath  包路径
     * @param childPackage 是否遍历子包
     * @param predicate
     * @return 类的完整名称
     */
    public static List<DaoInfo> scanDaoFromJar(URL[] urls,
                                               String packagePath, boolean childPackage, Predicate<String> predicate) {
        if (urls != null) {
            return Stream.of(urls).parallel().map(url -> {
                String urlPath = url.getPath();
                // 不必搜索classes文件夹
                if (!urlPath.endsWith("classes/")) {
                    String jarPath = urlPath + "!/" + packagePath;
                    return getClassNameByJar(jarPath, childPackage, predicate);
                }
                return new ArrayList<DaoInfo>();
            }).reduce((list1, list2) -> {
                list1.addAll(list2);
                return list1;
            }).orElse(Collections.emptyList());
        }
        return Collections.emptyList();
    }
}

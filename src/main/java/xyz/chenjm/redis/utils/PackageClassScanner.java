package xyz.chenjm.redis.utils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PackageClassScanner {

    public interface Filter {
        boolean filter(String str, Class<?> clazz);
    }

    public interface ResourceHandler {
        boolean supports(URL url);
        List<String> handle(URL url, String basePackage);
    }

    static class JarResourceHandler implements ResourceHandler{

        @Override
        public boolean supports(URL url) {
            return "jar".equals(url.getProtocol());
        }

        @Override
        public List<String> handle(URL url, String basePackage) {
            List<String> result = new ArrayList<>();
            try {
                JarURLConnection conn = (JarURLConnection) url.openConnection();
                JarFile jarFile = conn.getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                basePackage = conn.getEntryName();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (!name.startsWith(basePackage) || !name.endsWith(".class")) {
                        continue;
                    }
                    name = name.replace("/", ".");
                    result.add(name.substring(0, name.length()-6));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return result;
        }
    }

    static class FileResourceHandler implements ResourceHandler {

        @Override
        public boolean supports(URL url) {
            return "file".equals(url.getProtocol());
        }

        @Override
        public List<String> handle(URL url, String basePackage) {
            List<String> res = new ArrayList<>();
            File rootFile = new File(url.getFile());
            basePackage = basePackage.replace(".", File.separator);
            findClass(rootFile, basePackage, res);
            return res;
        }

        /**
         * 递归的方式查找class文件
         * @param rootFile 当前文件
         * @param basePackage 包名
         * @param res 结果集
         */
        private void findClass(File rootFile, String basePackage, List<String> res) {
            if (rootFile.isDirectory()) {
                File[] files = rootFile.listFiles();
                if (files == null) return;
                for (File file : files) {
                    findClass(file, basePackage, res);
                }
            }
            String path = rootFile.getAbsolutePath();
            if (rootFile.isFile() && path.endsWith(".class")) {
                // /aa/bb/*.class -> bb.*
                int i = path.lastIndexOf(basePackage);
                path = path.substring(i, path.length() - 6);
                path = path.replace(File.separator, ".");
                res.add(path);
            }
        }
    }

    public static class AnnotationTypeFilter implements Filter {
        private final Class<? extends Annotation> annotationType;
        public AnnotationTypeFilter(Class<? extends Annotation> ann) {
            annotationType = ann;
        }
        @Override
        public boolean filter(String str, Class<?> clazz) {
            return clazz != null && clazz.getAnnotation(annotationType) != null;
        }
    }

    public static class TypeFilter implements Filter {
        private final Class<?> type;
        public TypeFilter(Class<?> type) {
            this.type = type;
        }

        @Override
        public boolean filter(String str, Class<?> clazz) {
            return clazz != null && type.isAssignableFrom(clazz);
        }
    }

    public Set<Class<?>> getScans() {
        return scans;
    }

    final Set<Class<?>> scans = new HashSet<>();

    private final List<Filter> filterChain = new ArrayList<>();
    private final List<ResourceHandler> handlerChain = new ArrayList<>();

    private ClassLoader classLoader = getClass().getClassLoader();

    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void addFilter(Filter filter) {
        filterChain.add(filter);
    }

    public void addHandler(ResourceHandler handler) {
        handlerChain.add(handler);
    }


    public PackageClassScanner() {
        addHandler(new FileResourceHandler());
        addHandler(new JarResourceHandler());
    }

    public void scan(String... basePackages) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        for (String basePackage : basePackages) {
            doScan(basePackage, classLoader);
        }
    }

    private void doScan(String basePackage, ClassLoader classLoader) {
        String basePath = basePackage.replace(".", "/") + "/";
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(basePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            for (ResourceHandler handler : handlerChain) {
                if (handler.supports(url)) {
                    List<String> classes = handler.handle(url, basePackage);
                    for (String aClass : classes) {
                        filter(aClass);
                    }
                }
            }
        }
    }

    private void filter(String className) {
        Class<?> aClass;
        try {
            aClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        for (Filter filter: filterChain) {
            if (!filter.filter(className, aClass)){
                return;
            }
        }
        scans.add(aClass);
    }

}
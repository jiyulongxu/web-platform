package tech.mlsql.serviceframework.platform.plugin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import tech.mlsql.serviceframework.platform.PackageNames;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 30/1/2020 WilliamZhu(allwefantasy@gmail.com)
 */
public class RuntimePluginLoader extends URLClassLoader {

    private Cache<String, Class<?>> classCache = CacheBuilder.newBuilder()
            .maximumSize(1000000)
            .removalListener(new RemovalListener<String, Class<?>>() {
                @Override
                public void onRemoval(RemovalNotification<String, Class<?>> notification) {

                }
            })
            .build();


    public RuntimePluginLoader(URL[] urls) {
        super(urls);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> c = null;
        synchronized (getClassLoadingLock(name)) {

            c = findLoadedClass(name);
            if (c != null) return c;

            if (c == null) {
                ClassLoader parent = getParent();
                while (parent != null && c == null) {
                    c = (Class<?>) ReflectUtils.method(parent, "findLoadedClass", name, false);
                    parent = parent.getParent();
                }
                if (c == null) {
                    c = (Class<?>) ReflectUtils.method(this, "findBootstrapClassOrNull", name);
                }
                if (c != null) {
                    return c;
                }
            }

            boolean loadInParent = false;

            if (name.startsWith("java.")
                    || name.startsWith("javax.")
                    || name.startsWith("scala.")) {
                loadInParent = true;
            }

            if (!loadInParent) {
                for (String item : PackageNames.names) {
                    if (name.startsWith(item)) {
                        loadInParent = true;
                        break;
                    }
                }
            }

            if (loadInParent) {
                c = super.loadClass(name, false);
            } else {
                c = classCache.getIfPresent(name);
                if (c == null) {
                    try {
                        long t1 = System.nanoTime();
                        c = findClass(name);
                        sun.misc.PerfCounter.getFindClassTime().addElapsedTimeFrom(t1);
                        sun.misc.PerfCounter.getFindClasses().increment();
                        resolveClass(c);
                        classCache.put(name, c);
                    } catch (Exception e) {
                        c = null;
                    }
                }

                if (c == null) {
                    c = super.loadClass(name, false);
                }
            }
        }
        return c;
    }
}

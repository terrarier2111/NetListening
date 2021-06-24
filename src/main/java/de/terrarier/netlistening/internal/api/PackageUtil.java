package de.terrarier.netlistening.internal.api;

import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.ApiStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public final class PackageUtil {

    // Credit: https://www.baeldung.com/java-find-all-classes-in-package

    private PackageUtil() {
        throw new UnsupportedOperationException("This class may not be instantiated!");
    }

    @AssumeNotNull
    public static List<Class<?>> findAllClassesUsingClassLoader(@AssumeNotNull String packageName) {
        InputStream stream = ClassLoader.getSystemClassLoader()
                .getResourceAsStream(packageName.replaceAll("[.]", "/"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        final List<Class<?>> ret = new ArrayList<>();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                if(line.endsWith(".class")) {
                    ret.add(getClass(line, packageName));
                }
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @AssumeNotNull
    private static Class<?> getClass(@AssumeNotNull String className, @AssumeNotNull String packageName) {
        try {
            return Class.forName(packageName + '.'
                    + className.substring(0, className.lastIndexOf('.')));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

}

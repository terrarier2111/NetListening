package de.terrarier.netlistening.internal.api;

import de.terrarier.netlistening.Application;
import de.terrarier.netlistening.api.event.ListenerType;
import de.terrarier.netlistening.impl.ApplicationImpl;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public final class EventManager {

    private final Map<ListenerType, Method> listenerMapping = new HashMap<>();
    private final Set<PrepListener> listeners = new HashSet<>();
    private final ApplicationImpl application;

    public EventManager(ApplicationImpl application) {
        this.application = application;
    }

    public static long[] registerListeners(Application application, String pack) {
        final List<Class<?>> classes = PackageUtil.findAllClassesUsingClassLoader(pack);
        int size = classes.size();
        for(int i = 0; i < size; i++) {
            final Class<?> clazz = classes.get(i);
            if(clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                classes.remove(clazz);
            }
        }
        size = classes.size();
        final long[] ret = new long[size];
        for(int i = 0; i < size; i++) {
            final Class<?> clazz = classes.get(i);
            // ret[i] = ((ApplicationImpl) application).getEventManager().registerListener(clazz);
        }
        return ret;
    }

    public long registerListener(Class<?> clazz) {
        return 0L;
    }

    public long registerListener(Listener listener) {
        return 0L;
    }

}

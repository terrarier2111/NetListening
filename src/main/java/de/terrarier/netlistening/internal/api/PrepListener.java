package de.terrarier.netlistening.internal.api;

import de.terrarier.netlistening.api.event.Event;
import de.terrarier.netlistening.internal.AssumeNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class PrepListener {

    private static final Method[] EMPTY_METHODS = new Method[0];
    private final Listener back;
    private final ListenerScope.Scope scope;
    private final ListenerPriority.Priority priority;
    private final Method[] listenerMethods;

    public PrepListener(@AssumeNotNull Listener back) {
        this.back = back;
        scope = ListenerScope.Scope.resolve(back.getClass());
        priority = ListenerPriority.Priority.resolve(back.getClass());
        final List<Method> methods = new ArrayList<>();
        for (Method method : back.getClass().getDeclaredMethods()) {
            if (!method.isSynthetic() &&
                    !method.isBridge() &&
                    !method.isVarArgs() &&
                    (method.getReturnType() == Void.class || method.getReturnType() == boolean.class)
                    && method.getParameterCount() == 1
                    //&& hasInterface(method.getParameterTypes()[0], Listener.class)
                    && Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
                method.setAccessible(true);
                methods.add(method);
            }
        }
        listenerMethods = methods.toArray(EMPTY_METHODS);
    }

    @AssumeNotNull
    public ListenerScope.Scope getScope() {
        return scope;
    }

    @AssumeNotNull
    public ListenerPriority.Priority getPriority() {
        return priority;
    }

    private static boolean hasInterface(@AssumeNotNull Class<?> clazz, @AssumeNotNull Class<?> interfaceType) {
        if(clazz.equals(Object.class)) {
            return false;
        }
        final Class<?>[] interfaces = clazz.getInterfaces();
        if(interfaces.length == 0) {
            return false;
        }
        for(Class<?> inter : interfaces) {
            if(inter.equals(interfaceType) || hasInterface(inter, interfaceType)) {
                return true;
            }
        }
        return hasInterface(clazz.getSuperclass(), interfaceType);
    }

    @AssumeNotNull
    public Method[] getListenerMethods() {
        return listenerMethods;
    }

}

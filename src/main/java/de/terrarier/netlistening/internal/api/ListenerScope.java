package de.terrarier.netlistening.internal.api;

import de.terrarier.netlistening.internal.AssumeNotNull;

import java.lang.reflect.Method;

public @interface ListenerScope {

    Scope scope() default Scope.METHOD;

    enum Scope {

        CLASS, METHOD;

        @AssumeNotNull
        static ListenerScope.Scope resolve(@AssumeNotNull Class<?> clazz) {
            return resolve(clazz.getDeclaredAnnotation(ListenerScope.class));
        }

        @AssumeNotNull
        static ListenerScope.Scope resolve(@AssumeNotNull Method method) {
            return resolve(method.getDeclaredAnnotation(ListenerScope.class));
        }

        @AssumeNotNull
        private static ListenerScope.Scope resolve(ListenerScope scope) {
            if(scope == null) {
                return Scope.METHOD;
            }
            return scope.scope();
        }

    }

}

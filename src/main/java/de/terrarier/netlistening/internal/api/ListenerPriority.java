package de.terrarier.netlistening.internal.api;

import de.terrarier.netlistening.internal.AssumeNotNull;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * @author Terrarier2111
 * @since 1.12
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ListenerPriority {

    /**
     * @return the priority which defines in which order the listener is to be called.
     */
    @NotNull
    Priority priority() default Priority.MEDIUM;

    enum Priority {

        LOWEST, LOW, MEDIUM, HIGH, HIGHEST;

        @AssumeNotNull
        static Priority resolve(@AssumeNotNull Class<?> clazz) {
            return resolve(clazz.getDeclaredAnnotation(ListenerPriority.class));
        }

        @AssumeNotNull
        static Priority resolve(@AssumeNotNull Method method) {
            return resolve(method.getDeclaredAnnotation(ListenerPriority.class));
        }

        @AssumeNotNull
        private static Priority resolve(ListenerPriority priority) {
            if(priority == null) {
                return Priority.MEDIUM;
            }
            return priority.priority();
        }

    }



}

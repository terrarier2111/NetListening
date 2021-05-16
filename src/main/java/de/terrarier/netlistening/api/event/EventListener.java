package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Terrarier2111
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener {

    /**
     * @return the priority which defines in which order the listener is to be called.
     */
    @NotNull
    Priority priority() default Priority.MEDIUM;

    enum Priority {

        LOWEST, LOW, MEDIUM, HIGH, HIGHEST

    }

}

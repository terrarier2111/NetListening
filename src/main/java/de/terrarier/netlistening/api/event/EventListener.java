package de.terrarier.netlistening.api.event;

import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @since 1.0
 * @author Terrarier2111
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventListener {

    @NotNull
    Priority priority() default Priority.MEDIUM;

    enum Priority {

        LOWEST, LOW, MEDIUM, HIGH, HIGHEST

    }

}

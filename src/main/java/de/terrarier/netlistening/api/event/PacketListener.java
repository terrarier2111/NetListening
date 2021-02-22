package de.terrarier.netlistening.api.event;

import de.terrarier.netlistening.api.Type;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation which can be used to annotate the trigger
 * method of a decode listener.
 *
 * @since 1.0
 * @author Terrarier2111
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PacketListener {

	/**
	 * @return the data types which should be the content of a packet in order to trigger the listener,
	 * when no data types were specified, just call the listener without checking.
	 */
	@NotNull
	Type[] dataTypes() default {};

}

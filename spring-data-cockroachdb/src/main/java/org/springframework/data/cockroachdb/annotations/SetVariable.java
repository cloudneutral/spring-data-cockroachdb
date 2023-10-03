package org.springframework.data.cockroachdb.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation for modifying arbitrary variables.
 *
 * @see Variable
 */
@Target({})
@Retention(RUNTIME)
public @interface SetVariable {
    /**
     * The session variable to set.
     */
    Variable variable();

    /**
     * Scope of the variable (default is local).
     */
    enum Scope {
        local,
        session
    }

    Scope scope() default Scope.local;

    /**
     * Value of the variable for setting it to a constant.
     */
    String value() default "";

    int intValue() default -1;
}

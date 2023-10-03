package org.springframework.data.cockroachdb.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Annotation denoting the CockroachDB version when introduced.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {FIELD})
@interface Since {
    String value() default "(unknown)";
}

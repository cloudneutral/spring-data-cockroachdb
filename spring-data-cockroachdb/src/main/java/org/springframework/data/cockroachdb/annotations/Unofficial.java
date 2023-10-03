package org.springframework.data.cockroachdb.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation denoting an undocumented variable.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {java.lang.annotation.ElementType.FIELD})
@interface Unofficial {
}

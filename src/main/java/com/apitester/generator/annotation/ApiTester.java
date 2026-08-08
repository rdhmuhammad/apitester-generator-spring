package com.apitester.generator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiTester {

    String[] folders() default {};

    Class<?>[] ignoreParams() default {};

    Headers[] globalHeaders() default {};

    @interface Headers {
        String key() default "";
        String value() default "";
        String description() default "";
    }
}

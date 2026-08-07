package com.apitester.generator.annotation;

import com.apitester.generator.config.ApiTesterAutoConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(ApiTesterAutoConfiguration.class)
public @interface EnableApiTester {
}

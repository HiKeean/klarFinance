package com.api.klarfinance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.api.klarfinance.annotation.AdminAnnotation;
import com.api.klarfinance.annotation.InternalAnnotation;
import com.api.klarfinance.annotation.NasabahAnnotation;

import java.util.function.Predicate;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                "/api/v1/nasabah",
                HandlerTypePredicate.forAnnotation(NasabahAnnotation.class)
        );
        configurer.addPathPrefix(
                "/api/v1/internal",
                HandlerTypePredicate.forAnnotation(InternalAnnotation.class)
        );

        configurer.addPathPrefix(
                "/api/v1/admin",
                HandlerTypePredicate.forAnnotation(AdminAnnotation.class)
        );

        Predicate<Class<?>> isRestController = HandlerTypePredicate.forAnnotation(RestController.class);
        Predicate<Class<?>> isAppPackage = HandlerTypePredicate.forBasePackage("com.api.klarfinance");
        Predicate<Class<?>> isNotNasabah = HandlerTypePredicate.forAnnotation(NasabahAnnotation.class).negate();
        Predicate<Class<?>> isNotInternal = HandlerTypePredicate.forAnnotation(InternalAnnotation.class).negate();
        Predicate<Class<?>> isNotAdmin = HandlerTypePredicate.forAnnotation(AdminAnnotation.class).negate();

        configurer.addPathPrefix(
                "/api/v1",
                isAppPackage.and(isRestController).and(isNotNasabah).and(isNotInternal).and(isNotAdmin)
        );
    }
}
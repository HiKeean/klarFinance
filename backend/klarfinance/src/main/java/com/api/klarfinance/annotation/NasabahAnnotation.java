package com.api.klarfinance.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RestController;
import java.lang.annotation.*;

@Target(ElementType.TYPE) // Hanya bisa ditaruh di atas class
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RestController
public @interface NasabahAnnotation {

    @AliasFor(annotation = RestController.class)
    String value() default "";

}
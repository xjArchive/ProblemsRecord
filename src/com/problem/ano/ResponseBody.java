package com.problem.ano;

 
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value={ ElementType.METHOD })
public @interface ResponseBody {
    String charset() default "UTF-8";
}

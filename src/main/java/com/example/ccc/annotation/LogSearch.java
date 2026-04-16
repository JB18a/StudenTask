package com.example.ccc.annotation;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogSearch {
    String value() default "执行了搜索操作";
}
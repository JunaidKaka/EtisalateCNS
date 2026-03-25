package com.etisalat.utils;
import java.lang.annotation.*;
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoWrap {}
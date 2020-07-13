package core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Copyright (c) 2017 hadlinks, All Rights Reserved.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LogAnnotation {

    public String value() default "";

}

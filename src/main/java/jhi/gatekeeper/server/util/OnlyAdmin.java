package jhi.gatekeeper.server.util;

import java.lang.annotation.*;

/**
 * @author Sebastian Raubach
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface OnlyAdmin
{
}

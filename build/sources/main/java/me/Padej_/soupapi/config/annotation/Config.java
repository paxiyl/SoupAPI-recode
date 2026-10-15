package me.Padej_.soupapi.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Names the config file, replacing Cloth Config's {@code me.shedaniel.autoconfig.annotation.Config}.
 *
 * <p>Cloth Config has no 1.8.9 build, so the port carries its own minimal equivalent. The
 * annotation shape is kept identical to Cloth's so the config class itself only needed its imports
 * changed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Config {
    String name();
}

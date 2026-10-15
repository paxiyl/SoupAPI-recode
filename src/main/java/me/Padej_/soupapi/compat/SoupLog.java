package me.Padej_.soupapi.compat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Single log channel for the mod. 1.8.9 ships log4j 2, so this matches vanilla's logging. */
public final class SoupLog {
    private static final Logger LOGGER = LogManager.getLogger("SoupAPI");

    private SoupLog() {
    }

    public static void info(String msg) {
        LOGGER.info(msg);
    }

    public static void warn(String msg) {
        LOGGER.warn(msg);
    }

    public static void warn(String msg, Throwable t) {
        LOGGER.warn(msg, t);
    }

    public static void error(String msg, Throwable t) {
        LOGGER.error(msg, t);
    }
}

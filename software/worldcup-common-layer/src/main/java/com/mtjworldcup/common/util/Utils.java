package com.mtjworldcup.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    private Utils() {
    }

    public static <T> T safeGet(Supplier<T> supplier, T defaultValue) {
        try {
            return supplier.get();
        } catch (NullPointerException e) {
            log.warn("Null Pointer Exception caught. Returning default value. Cause: {}", e.getMessage());
            return defaultValue;
        } catch (IndexOutOfBoundsException e) {
            log.warn("Index Out Of Bounds Exception caught. Returning default value. Cause: {}", e.getMessage());
            return defaultValue;
        }
    }
}

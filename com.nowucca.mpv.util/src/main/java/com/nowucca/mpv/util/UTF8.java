/**
 * Copyright (c) 2012-2014, Steven Atkinson. All rights reserved.
 */
package com.nowucca.mpv.util;

import java.nio.charset.Charset;

import static java.lang.String.format;

public final class UTF8 {

    private UTF8() {
    }

    private static final String UTF_8 = "UTF-8";

    public static Charset charset() {
        if (!Charset.isSupported("UTF-8")) {
            throw new RuntimeException(format("Charset %s is not supported.", UTF_8));
        }
        return Charset.forName("UTF-8");
    }

    public static String asString() {
        return UTF_8;
    }
}

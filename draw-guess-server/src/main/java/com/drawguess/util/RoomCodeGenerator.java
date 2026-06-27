package com.drawguess.util;

import java.util.Random;

public class RoomCodeGenerator {

    private static final Random RANDOM = new Random();

    public static String generate() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }
}

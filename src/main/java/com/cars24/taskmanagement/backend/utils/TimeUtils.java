package com.cars24.taskmanagement.backend.utils;

public class TimeUtils {

    private TimeUtils() {

    }

    public static long convertFormattedTimeToMillis(String time) {
        if (time == null || time.trim().isEmpty()) {
            return 0;
        }

        String[] parts = time.trim().split(" ");
        long totalMillis = 0;

        try {
            for (int i = 0; i < parts.length - 1; i += 2) {
                long num = Long.parseLong(parts[i]);
                switch (parts[i + 1]) {
                    case "days" -> totalMillis += num * 24 * 3600 * 1000;
                    case "hrs" -> totalMillis += num * 3600 * 1000;
                    case "min" -> totalMillis += num * 60 * 1000;
                    case "sec" -> totalMillis += num * 1000;
                    default -> {

                    }
                }
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {

            return 0;
        }

        return totalMillis;
    }
}

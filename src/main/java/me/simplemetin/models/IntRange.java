package me.simplemetin.models;

import java.util.Random;

/** Inclusive integer range written in config as a number ("3") or a range ("1-3"). */
public record IntRange(int min, int max) {

    public IntRange {
        if (max < min) {
            int tmp = min;
            min = max;
            max = tmp;
        }
    }

    public static IntRange of(int value) {
        return new IntRange(value, value);
    }

    /** Parses "5", "1-3" or a number object; falls back to the default on invalid input. */
    public static IntRange parse(Object value, IntRange fallback) {
        if (value == null) return fallback;
        if (value instanceof Number number) return of(number.intValue());

        var text = value.toString().trim();
        try {
            int dash = text.indexOf('-', 1); // allows a leading minus sign
            if (dash > 0) {
                return new IntRange(Integer.parseInt(text.substring(0, dash).trim()),
                        Integer.parseInt(text.substring(dash + 1).trim()));
            }
            return of(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public int roll(Random random) {
        return min == max ? min : min + random.nextInt(max - min + 1);
    }

    @Override
    public String toString() {
        return min == max ? String.valueOf(min) : min + "-" + max;
    }
}

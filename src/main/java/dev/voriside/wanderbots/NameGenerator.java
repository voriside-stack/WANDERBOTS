package dev.voriside.wanderbots;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

final class NameGenerator {
    private static final String[] A = {
            "Amber", "Ash", "Blue", "Bright", "Calm", "Copper", "Crimson", "Dusty", "Ember", "Frost",
            "Golden", "Gray", "Jolly", "Lucky", "Misty", "Night", "Quiet", "Rapid", "Silver", "Soft",
            "Storm", "Swift", "Velvet", "Wild", "Winter", "Warm", "Clever", "Cosmic", "Hidden", "Neon"
    };
    private static final String[] B = {
            "Badger", "Bear", "Cat", "Cobra", "Crow", "Deer", "Eagle", "Falcon", "Fox", "Frog",
            "Goat", "Hare", "Hawk", "Lynx", "Otter", "Owl", "Panda", "Rabbit", "Raven", "Shark",
            "Sheep", "Swan", "Tiger", "Turtle", "Wolf", "Wren", "Bee", "Moth", "Koala", "Moose"
    };

    private NameGenerator() {}

    static String next(Set<String> used) {
        for (int attempt = 0; attempt < 200; attempt++) {
            String name = A[ThreadLocalRandom.current().nextInt(A.length)]
                    + B[ThreadLocalRandom.current().nextInt(B.length)];
            if (ThreadLocalRandom.current().nextDouble() < 0.35) {
                name += ThreadLocalRandom.current().nextInt(10, 100);
            }
            if (name.length() > 16) {
                name = name.substring(0, 16);
            }
            if (used.add(name)) {
                return name;
            }
        }

        int i = 1;
        while (!used.add("Bot" + i)) {
            i++;
        }
        return "Bot" + i;
    }

    static Set<String> newUsedSet() {
        return new HashSet<>();
    }
}

package com.geendutchman.lhf_mudv2.entities.creatures;

public enum ResourcePoolSize {
    EMPTY(0), CRITICAL(1 / 8), LOW(2 / 8), SOME(3 / 8), HALF(4 / 8), DECENT(5 / 8), MOST(6 / 8), UNDERFULL(7 / 8),
    FULL(1);

    final double fraction;

    private ResourcePoolSize(double frac) {
        this.fraction = frac;
    }

    public static ResourcePoolSize fromInts(int current, int maximum) {
        return ResourcePoolSize.fromDouble((double) current / (double) maximum);
    }

    public static ResourcePoolSize fromDouble(final double value) {
        for (ResourcePoolSize size : ResourcePoolSize.values()) {
            if (value > size.fraction) {
                continue;
            }
            return size;
        }
        return ResourcePoolSize.HALF;
    }

}
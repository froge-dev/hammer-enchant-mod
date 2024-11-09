package com.frogedev.hammer_enchant.config;

import com.frogedev.hammer_enchant.HammerEnchantMod;

import java.util.List;

public enum MiningSpeedMode {
    FOCUSED_BLOCK {
        @Override
        public float computeDestroyTime(float primaryDestroyTime, List<Float> allDestroyTimes) {
            return primaryDestroyTime;
        }
    },
    SUM {
        @Override
        public float computeDestroyTime(float primaryDestroyTime, List<Float> allDestroyTimes) {
            return allDestroyTimes.stream().reduce(0f, Float::sum);
        }
    },
    SUM_OVER_SQRT_N {
        @Override
        public float computeDestroyTime(float primaryDestroyTime, List<Float> allDestroyTimes) {
            return (float) (allDestroyTimes.stream().reduce(0f, Float::sum) / Math.sqrt(allDestroyTimes.size()));
        }
    },
    AVG {
        @Override
        public float computeDestroyTime(float primaryDestroyTime, List<Float> allDestroyTimes) {
            return allDestroyTimes.stream().reduce(0f, Float::sum) / allDestroyTimes.size();
        }
    },
    MAX {
        @Override
        public float computeDestroyTime(float primaryDestroyTime, List<Float> allDestroyTimes) {
            return allDestroyTimes.stream().reduce(0f, Float::max);
        }
    };

    public static final MiningSpeedMode DEFAULT = SUM_OVER_SQRT_N;

    public static MiningSpeedMode fromString(String str) {
        try {
            return MiningSpeedMode.valueOf(str.toUpperCase());
        } catch (IllegalArgumentException e) {
            HammerEnchantMod.LOGGER.warn("Invalid value for config 'MiningSpeedMode': '{}', assuming default value '{}'", str, DEFAULT);
            return DEFAULT;
        }
    }

    abstract public float computeDestroyTime(float primaryDestroyTime, List<Float> allDestroyTimes);
}

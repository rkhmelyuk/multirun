package com.khmelyuk.multirun;

import com.intellij.execution.configurations.RunConfiguration;

public class RunConfigurationHelper {
    /** This to avoid problems with one multirun configuration A contains multirun configuration B, which itself contains A. */
    public static boolean containsLoopies(MultirunRunConfiguration configuration, MultirunRunConfiguration target) {
        if (configuration.equals(target)) {
            return true;
        }
        for (RunConfiguration each : configuration.getRunConfigurations()) {
            if (each.equals(target)) {
                return true;
            }
            if (each instanceof MultirunRunConfiguration) {
                return containsLoopies((MultirunRunConfiguration) each, target);
            }
        }
        return false;
    }
}

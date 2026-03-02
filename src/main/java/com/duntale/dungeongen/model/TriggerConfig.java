package com.duntale.dungeongen.model;

import javax.annotation.Nonnull;

/**
 * Trigger parameters for a spawner.
 *
 * @param type               how the spawner activates
 * @param activationRadius   for PROXIMITY: radius in blocks
 * @param deactivationRadius for PROXIMITY: deactivation radius (0 = no deactivation)
 * @param delaySec           for TIMED: delay in seconds (future)
 * @since 1.1.0
 */
public record TriggerConfig(
    @Nonnull TriggerType type,
    double activationRadius,
    double deactivationRadius,
    double delaySec
) {
    /**
     * Create a proximity trigger with explicit activation radius.
     *
     * @param activationRadius radius in blocks at which the spawner activates
     * @return a new proximity trigger config
     */
    @Nonnull
    public static TriggerConfig proximity(double activationRadius) {
        return new TriggerConfig(TriggerType.PROXIMITY, activationRadius, 0, 0);
    }
}

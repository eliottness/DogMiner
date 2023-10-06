package com.softalys.metrics.listeners;

import org.bukkit.event.Listener;

/**
 * Base interface for listener metrics.
 */
public interface MetricListener extends Listener
{
    public boolean shouldRegister();
}

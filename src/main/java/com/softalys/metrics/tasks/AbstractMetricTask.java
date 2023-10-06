package com.softalys.metrics.tasks;

import com.softalys.DogMinerPlugin;
import com.timgroup.statsd.StatsDClient;

/**
 * Base metric class.
 */
public abstract class AbstractMetricTask implements MetricCollector
{
    protected StatsDClient getClient()
    {
        return DogMinerPlugin.getInstance().getStatsd();
    }
}

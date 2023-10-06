package com.softalys.metrics.listeners;

import com.timgroup.statsd.NonBlockingStatsDClient;
import com.softalys.DogMinerPlugin;
import com.timgroup.statsd.StatsDClient;

/**
 * Base class for metric listeners.
 */
public abstract class AbstractMetricListener implements MetricListener
{
    protected String getInstanceName()
    {
        return DogMinerPlugin.getInstance().getInstanceName();
    }
    
    protected StatsDClient getClient()
    {
        return DogMinerPlugin.getInstance().getStatsd();
    }
    
    protected NonBlockingStatsDClient getEventClient()
    {
        return DogMinerPlugin.getInstance().getEventsd();
    }
    
    public boolean shouldRegister()
    {
        return true;
    }
}

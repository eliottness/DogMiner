package com.softalys.metrics.listeners;

import com.softalys.DogMinerPlugin;
import com.timgroup.statsd.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

/**
 * Sends events when worlds are loaded/unloaded.
 */
public class WorldListener extends AbstractMetricListener
{
    @EventHandler( priority = EventPriority.MONITOR, 
                   ignoreCancelled = true )
    public void onWorldLoad( WorldLoadEvent event )
    {
        if( !DogMinerPlugin.getInstance().getConfig().getBoolean( "events.worlds", true ) )
        {
            return;
        }
        
        String message = "[Loaded] World " + event.getWorld().getName() + " has been loaded on " + this.getInstanceName();
        this.getEventClient().recordEvent(Event.builder()
                                                  .withTitle(message)
                                                  .withPriority(Event.Priority.LOW)
                                               .build());
    }
    
    @EventHandler( priority = EventPriority.MONITOR, 
                   ignoreCancelled = true )
    public void onWorldUnload( WorldUnloadEvent event )
    {
        if( !DogMinerPlugin.getInstance().getConfig().getBoolean( "events.worlds", true ) )
        {
            return;
        }
        
        String message = "[Unloaded] World " + event.getWorld().getName() + " has been unloaded on " + this.getInstanceName();
        this.getEventClient().recordEvent(Event.builder()
                                               .withTitle(message)
                                               .withPriority(Event.Priority.LOW)
                                               .build());
    }
}

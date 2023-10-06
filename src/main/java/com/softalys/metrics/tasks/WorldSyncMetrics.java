package com.softalys.metrics.tasks;

import com.softalys.DogMinerPlugin;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

/**
 * Sends (synchronously collected) world metrics.
 */
public class WorldSyncMetrics extends AbstractMetricTask
{
    @Override
    public void collect()
    {
        long startTime = System.nanoTime();
        
        for( World world : Bukkit.getServer().getWorlds() )
        {
            try
            {
                int tiles = 0;
                
                for( Chunk chunk : world.getLoadedChunks() )
                {
                    tiles += chunk.getTileEntities().length;
                }
                
                this.getClient().gauge( "world.tiles", tiles, "world:" + world.getName() );
            }
            catch( ClassCastException ex )
            {
                DogMinerPlugin.getInstance().getLogger().log( Level.SEVERE, "Corrupted chunk data on world " + world, ex );
            }
            catch( IllegalStateException ex )
            {
                DogMinerPlugin.getInstance().getLogger().log( Level.INFO, "Entity added asynchronously while iterating tiles." );
            }
        }
        
        if( DogMinerPlugin.getInstance().isDebug() )
        {
            DogMinerPlugin.getInstance().getLogger().log( Level.INFO, "WorldSyncMetrics collected in {0}ns.", System.nanoTime() - startTime );
        }
    }
}

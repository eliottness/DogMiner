package com.softalys.metrics.tasks;

import com.softalys.DogMinerPlugin;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Sends server performance metrics.
 */
@Getter( AccessLevel.PRIVATE )
@Setter( AccessLevel.PRIVATE )
public class ServerMetrics extends AbstractMetricTask
{
    private String versionString;
    
    private Field recentTpsField;
    
    private Method getServerMethod;
    
    private Object serverObject;
    
    private boolean reflectionSetup;
    
    public ServerMetrics()
    {
        this.setReflectionSetup( false );
        try
        {
            this.setVersionString( Bukkit.getServer().getClass().getPackage().getName().split( "\\." )[ 3 ] );
            this.setRecentTpsField( Class.forName( "net.minecraft.server." + this.versionString + ".MinecraftServer" ).getField( "recentTps" ) );
            this.setGetServerMethod( Class.forName( "org.bukkit.craftbukkit." + this.versionString + ".CraftServer" ).getMethod( "getServer" ) );
            this.setServerObject( getServerMethod.invoke( Bukkit.getServer() ) );
            this.setReflectionSetup( true );
        }
        catch( ArrayIndexOutOfBoundsException | ClassNotFoundException | NoSuchFieldException | SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex )
        {
            if( DogMinerPlugin.getInstance().isDebug() )
            {
                DogMinerPlugin.getInstance().getLogger().log( Level.WARNING, "Couldn't set up reflection, TPS metrics will not be reported.", ex );
            }
            else
            {
                DogMinerPlugin.getInstance().getLogger().log( Level.WARNING, "Couldn't set up reflection, TPS metrics will not be reported." );
            }
        }
    }
    
    @Override
    public void collect()
    {
        long startTime = System.nanoTime();
        
        if( this.isReflectionSetup() )
        {
            try
            {
                this.getClient().gauge( "tps", ( (double[]) this.getRecentTpsField().get( serverObject ) )[ 0 ] );
            }
            catch( IllegalArgumentException | IllegalAccessException ex )
            {
                if( DogMinerPlugin.getInstance().isDebug() )
                {
                    DogMinerPlugin.getInstance().getLogger().log( Level.WARNING, "Couldn't report TPS.", ex );
                }
            }
        }
        
        this.getClient().gauge( "servers", 1, DogMinerPlugin.getInstance().getInstanceName() );
        
        this.getClient().gauge( "tasks.running", Bukkit.getServer().getScheduler().getActiveWorkers().size() );
        this.getClient().gauge( "tasks.pending", Bukkit.getServer().getScheduler().getPendingTasks().size() );
        
        int pluginsEnabled = 0;
        for( Plugin plugin : Bukkit.getServer().getPluginManager().getPlugins() )
        {
            if( plugin.isEnabled() )
            {
                pluginsEnabled++;
            }
        }
        this.getClient().gauge( "plugins.loaded", Bukkit.getServer().getPluginManager().getPlugins().length );
        this.getClient().gauge( "plugins.enabled", pluginsEnabled );
        
        this.getClient().gauge( "memory.maximum", Runtime.getRuntime().maxMemory() );
        this.getClient().gauge( "memory.allocated", Runtime.getRuntime().totalMemory() );
        this.getClient().gauge( "memory.used", ( Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() ) );
        
        this.getClient().gauge( "players.maximum", Bukkit.getServer().getMaxPlayers() );
        
        if( DogMinerPlugin.getInstance().isDebug() )
        {
            DogMinerPlugin.getInstance().getLogger().log( Level.INFO, "ServerMetrics collected in {0}ns.", System.nanoTime() - startTime );
        }
    }
}

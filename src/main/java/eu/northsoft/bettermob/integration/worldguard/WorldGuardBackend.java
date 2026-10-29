package eu.northsoft.bettermob.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import eu.northsoft.bettermob.integration.Regions;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class WorldGuardBackend implements Regions.Backend {
    public static final String SPAWN_FLAG = "bettermob-spawn";

    private static volatile StateFlag spawnFlag;

    public static void registerFlags() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            StateFlag flag = new StateFlag(SPAWN_FLAG, true);
            registry.register(flag);
            spawnFlag = flag;
        } catch (FlagConflictException exception) {
            Flag<?> existing = registry.get(SPAWN_FLAG);
            if (existing instanceof StateFlag state) spawnFlag = state;
        }
    }

    private final RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();

    @Override
    public Set<String> at(Location location) {
        Set<String> names = new HashSet<>();
        for (ProtectedRegion region : query.getApplicableRegions(BukkitAdapter.adapt(location)).getRegions()) {
            names.add(region.getId().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    @Override
    public boolean spawnAllowed(Location location) {
        StateFlag flag = spawnFlag;
        if (flag == null) return true;
        return query.queryState(BukkitAdapter.adapt(location), null, flag) != StateFlag.State.DENY;
    }
}

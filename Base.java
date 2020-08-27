package me.Bestem0r.pastebase;

import com.google.common.base.Verify;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.MCEditSchematicFormat;
import com.sk89q.worldedit.schematic.SchematicFormat;
import me.Bestem0r.pastebase.utilities.ColorBuilder;
import me.Bestem0r.pastebase.utilities.WorldEditPaster;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;

public class Base {

    private final Player player;
    private String factionName;

    private final PBPlugin plugin;
    private final DataManager dataManager;

    private final Location location;
    private final Chunk chunk;

    public Base(Player player, String factionName, PBPlugin plugin) {
        this.player = player;
        this.factionName = factionName;

        this.plugin = plugin;
        this.dataManager = PBPlugin.getInstance().getDataManager();

        int quePosition = dataManager.getPasteBases().size() + 1;
        double remaining_time = (dataManager.getPasteBases().size() + 1) * plugin.getConfig().getDouble("bases_delay");
        String prefix = plugin.getPrefix();

        player.sendTitle(ColorBuilder.color("create_base.title"), ColorBuilder.color("create_base.sub_title"));
        player.sendMessage(prefix + ColorBuilder.color("messages.add_to_que"));
        player.sendMessage(prefix + ColorBuilder.colorReplaceTwo("messages.estimated_time", "%time%", String.valueOf(remaining_time), "%position%", String.valueOf(quePosition)));
        player.playSound(player.getLocation(), plugin.getConfig().getString("sounds.create_base"), 1, 1);

        this.location = findLocation();
        this.chunk = location.getChunk();
    }

    private Location findLocation() {

        int min = -plugin.getConfig().getInt("location.random_radius");
        int max = plugin.getConfig().getInt("location.random_radius");

        Location origin = player.getLocation().clone();
        Location randomLocation;

        while (true) {
            int randomX = ThreadLocalRandom.current().nextInt(min, max + 1);
            int randomZ = ThreadLocalRandom.current().nextInt(min, max + 1);
            randomLocation = origin.clone().add(randomX, 0, randomZ);

            randomLocation.setY(255);

            Block searchBlock;
            for (int i = 255; i > 0; i--) {
                searchBlock = randomLocation.getBlock();
                if (searchBlock.getType().isSolid()) break;
                randomLocation.subtract(0, 1, 0);
            }
            randomLocation.add(0, 1, 0);

            Location teleportLocation = randomLocation.clone();

            boolean isFlat = true;
            for (int i = 1; i <= plugin.getConfig().getInt("location.flat_search_diameter"); i ++) {
                Location south = randomLocation.clone().add(0, 0, i);
                Location east = randomLocation.clone().add(i, 0, 0);

                if (south.getBlock().getType() != Material.AIR) isFlat = false;
                if (east.getBlock().getType() != Material.AIR) isFlat = false;

                if (!south.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) isFlat = false;
                if (!east.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) isFlat = false;

            }
            if (isFlat) {
                teleportLocation.getChunk().load();
                return teleportLocation;
            }
        }
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void sendNotification() {
        int quePosition = dataManager.getPasteBases().size();
        double remaining_time = dataManager.getPasteBases().size() * plugin.getConfig().getDouble("bases_delay");
        String prefix = plugin.getPrefix();
        player.sendMessage(prefix + ColorBuilder.colorReplaceTwo("messages.estimated_time", "%time%", String.valueOf(remaining_time), "%position%", String.valueOf(quePosition)));
        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("sounds.notification")), 1, 1);
    }

    public void paste() {
        player.sendMessage(plugin.getPrefix() + ColorBuilder.color("messages.base_created"));
        player.teleport(location);

        File folder = new File(Bukkit.getServer().getPluginManager().getPlugin("PasteBase").getDataFolder() + "/schematics/");
        if (!folder.exists()) return;
        if (!folder.isDirectory()) return;
        int numberOfSchematics = folder.listFiles().length;

        int random = ThreadLocalRandom.current().nextInt(0, numberOfSchematics);
        File dir = folder.listFiles()[random];

        WorldEditPaster worldEditPaster = new WorldEditPaster();
        worldEditPaster.pasteSchematic(dir, location);

    }
}

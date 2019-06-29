package site.modicum.rgrp;

/**

AUTHOR: IGGYSAUR
DATE: 26/10/2018
FOR: RETRIBUTION GAMING - WILD WEST FRONTIER
VERSION 1.5

 */
import
import com.pablo67340.SQLiteLib.Database.Database;
import com.pablo67340.SQLiteLib.Main.SQLiteLib;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class RGRP extends JavaPlugin implements Listener, CommandExecutor {

    private double costPerRail = getConfig().getDouble("costPerRail");
    private String currency = getConfig().getString("currency");
    private List<Integer> blockList;
    private Database railsDB;

    private static RGRP INSTANCE;

    @Override
    public void onEnable()
    {
        //singleton instance reference
        INSTANCE = this;
        //register commands and events
        getCommand("rgrp").setExecutor(new cmdRGRP());
        getServer().getPluginManager().registerEvents(INSTANCE, INSTANCE);
        //initialize config
        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        //initialize DB
        SQLiteLib sqlLib = SQLiteLib.hookSQLiteLib(INSTANCE);
        sqlLib.initializeDatabase("protectedRails", "CREATE TABLE IF NOT EXISTS rails(id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, rail BLOB, block BLOB, town BLOB, player BLOB)");
        railsDB = sqlLib.getDatabase("protectedRails");
    }

    @EventHandler
    private void onClickWithWandOrRail(PlayerInteractEvent event)
    {
        if (event.getAction().equals(Action.LEFT_CLICK_AIR)
                || event.getAction().equals(Action.LEFT_CLICK_BLOCK)
                || !event.hasBlock()
                || !event.hasItem())
        {return;}

        int itemInHand = event.getItem().getTypeId();
        Block clickedBlock = event.getClickedBlock();

        if(TownyUniverse.isWilderness(clickedBlock)) // if clicked block is in wilderness
        {
            try
            {
                Resident res = TownyUniverse.getDataSource().getResident(event.getPlayer().getName());

                if (res.hasTown())
                {

                    Town town = res.getTown();

                    if (isWand(itemInHand) && isProtectedBlock(clickedBlock.getTypeId())) //if clicked block is on protections list and wand is in the hand
                    {
                        Location rLoc = clickedBlock.getLocation(); //rail location is block clicked with wand
                        Location loc = clickedBlock.getRelative(BlockFace.DOWN).getLocation(); //block under rail

                        // if claimRail is false, cancel event
                        event.setCancelled(!claimRail(event.getPlayer(), town, rLoc, loc));
                    }
                    else if (isProtectedBlock(itemInHand)) //if player is attempting to place block with blockID on protections list
                    {
                        Location rLoc = clickedBlock.getRelative(BlockFace.UP).getLocation(); //rail is placed above the block clicked with rail
                        Location loc = clickedBlock.getLocation(); //the block under the rail is the clicked block

                        // if claimRail is false, cancel event
                        event.setCancelled(!claimRail(event.getPlayer(), town, rLoc, loc));
                    }
                    else if (isProtectedMulti(itemInHand)) //if player is attempting to place block with blockID on multiblock protections list
                    {
                        List<Location> toClaim = new ArrayList<>();
                        Location rLoc = clickedBlock.getRelative(BlockFace.UP).getLocation();

                        for (int x = 0; x < 7; x++) {
                            for (int y = 0; y < 3; y++) {
                                for (int z = 0; z < 7; z++) {

                                    Location railScan = rLoc.add(x,y,z);

                                    if (isProtectedMulti(railScan.getBlock().getTypeId()))
                                    {
                                        toClaim.add(railScan);
                                    }
                                }
                            }
                        }

                        boolean isRank = (res.hasTownRank("assistant") || res.hasTownRank("helper") || res.isMayor());
                        double multiCost = toClaim.size() * costPerRail;

                        if (isRank && town.canPayFromHoldings(multiCost))
                        {
                            for (Location rail : toClaim)
                            {
                                Location loc = rail.getBlock().getRelative(BlockFace.DOWN).getLocation();
                                event.setCancelled(!claimRail(event.getPlayer(), town, rail, loc));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //identifying ownership with empty hand right click
    @EventHandler
    public void tellMeWhoOwnsThis(PlayerInteractEvent event)
    {
        if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.hasBlock() && !event.hasItem()) {
            if (isOwned(event.getClickedBlock().getLocation())) {
                informPlayerOfOwnership(event.getPlayer(), event.getClickedBlock().getLocation());
            }
        }
    }

    //says no to fluids and dragon eggs
    @EventHandler
    public void sayNoToFluidsAndDragonEggs(BlockFromToEvent event){
        event.setCancelled(isProtectedBlock(event.getToBlock().getTypeId()));
    }

    @EventHandler
    public void onRailBreak(BlockBreakEvent event) {

        Location brokenBlock = event.getBlock().getLocation();
        Player pl = event.getPlayer();

        try {

            if (isOwned(brokenBlock)) // is on list of *ALL* rail locations in config
            {
                Resident res = TownyUniverse.getDataSource().getResident(pl.getName()); // get player as <towny resident>

                if(res.hasTown()) {

                    Town town = res.getTown();

                    if (isOwnedByPlayersTown(brokenBlock, town))
                    {
                        if (res.hasTownRank("assistant") || res.hasTownRank("helper") || res.isMayor())
                        {
                            event.getPlayer().sendMessage("");
                            event.getPlayer().sendMessage("§a[RGRP] §fBroke a rail owned by §6" + town.getName());
                            removeRailsFromDatabase(brokenBlock);
                            return;
                        }
                    }
                }
                event.setCancelled(true); // if rail is owned but not by player's town or by player's town but player isn't mayor or assistant, cancel event

                event.getPlayer().sendMessage("");
                event.getPlayer().sendMessage("§a[RGRP] §fOnly §eTown Mayors§f, §eHelpers§f, and §eAssistants");
                event.getPlayer().sendMessage("         §fcan break rails that belong to a town.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onTownDelete(DeleteTownEvent event)
    {
        removeTownFromDatabase(event.getTownName());
    }

    protected boolean isProtectedBlock(int itemInHand)
    {
        return getConfig().getIntegerList("blockIDtoProtect").contains(itemInHand);
    }

    protected boolean isProtectedMulti(int itemInHand)
    {
        return getConfig().getIntegerList("multiblockIDs").contains(itemInHand);
    }

    protected boolean isProtected(int itemInHand)
    {
        return (isProtectedBlock(itemInHand)) || isProtectedMulti(itemInHand))
    }

    protected boolean addToBlockList(int blockID)
    {
        if(!isProtectedBlock(blockID)){
        blockList = getConfig().getIntegerList("blockIDtoProtect");
        blockList.add(blockID);
        getConfig().set("blockIDtoProtect", blockList);
        saveConfig();
        return true;
        }else return false;
    }

    protected boolean addToMultiList(int blockID)
    {
        if(!isProtectedMulti(blockID)){
            blockList = getConfig().getIntegerList("multiblockIDs");
            blockList.add(blockID);
            getConfig().set("multiblockIDs", blockList);
            saveConfig();
            return true;
        }else return false;
    }

    protected boolean removeFromBlockList(int blockID)
    {
        if(isProtectedBlock(blockID)){
            blockList = getConfig().getIntegerList("blockIDtoProtect");
            blockList.remove(new Integer((blockID)));
            getConfig().set("blockIDtoProtect", blockList);
            saveConfig();
            return true;
        }else return false;
    }

    protected boolean removeFromMultiList(int blockID)
    {
        if(isProtectedMulti(blockID)){
            blockList = getConfig().getIntegerList("multiblockIDs");
            blockList.remove(new Integer((blockID)));
            getConfig().set("multiblockIDs", blockList);
            saveConfig();
            return true;
        }else return false;
    }


    private boolean isOwned(Location location)
    {
        List<Object> rResults = railsDB.queryRow("SELECT * FROM rails WHERE rail = '" + location + "';", "rail");
        List<Object> bResults = railsDB.queryRow("SELECT * FROM rails WHERE block = '" + location + "';", "block");
        return rResults.contains(location.toString()) || bResults.contains(location.toString());
    }

    private boolean isOwnedByPlayersTown(Location location, Town town)
    {
        List<Object> rResults = railsDB.queryRow("SELECT * FROM rails WHERE town = '" + town.getName() + "';", "rail");
        List<Object> bResults = railsDB.queryRow("SELECT * FROM rails WHERE town = '" + town.getName() + "';", "block");
        return rResults.contains(location.toString()) || bResults.contains(location.toString());
    }

    private void informPlayerOfOwnership(Player player, Location location){

        String townName = "nobody";
        if(isOwned(location))
        {
             townName = (String) railsDB.queryValue("SELECT * FROM rails WHERE rail = '" + location + "' OR block = '" + location + "';", "town");
        }
        player.sendMessage("");
        player.sendMessage("§a[RGRP] §fRail owned by: §6" + townName);
    }

    private boolean isWand(int itemInHand)
    {
        return (itemInHand == getWand());
    }

    protected void setWand(int itemInHand)
    {
        getConfig().set("wand", itemInHand);
        saveConfig();
    }

    protected int getWand(){
        return getConfig().getInt("wand");
    }

    private boolean claimRail(Player player, Town town, Location rLoc, Location loc) throws Exception
    {

        Resident res = TownyUniverse.getDataSource().getResident(player.getName());
        boolean isRank = (res.hasTownRank("assistant") || res.hasTownRank("helper") || res.isMayor());

        if (isRank && town.canPayFromHoldings(costPerRail)) // if the town can afford to pay x land deeds per rail and player has rank
        {
            //pay x deeds from towny bank to place rail
            town.pay(costPerRail, "Rail purchased in unclaimed territory for " + costPerRail + " " + currency + " by " + res.getName());
            player.sendMessage("");
            player.sendMessage("§a[RGRP] §fBought a rail for §6" + town.getName() + " §fat a cost of §e" + (int) costPerRail + " " + currency);
            player.sendMessage("");
            player.sendMessage("§a[RGRP] §6" + town.getName() + "§f's balance§3: §e" + (int) (town.getHoldingBalance()) + " " + currency);

            addRailsToDatabase(rLoc, loc, town, player);
            return true;        // if rails successfully added to DB, return true
        }
        else
        {
            if (!isRank)
            {
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fOnly §eTown Mayors§f, §eHelpers§f, and §eAssistants");
                player.sendMessage("         §fcan claim rails in the wilderness.");
            }
            if (!town.canPayFromHoldings(costPerRail))
            {
                player.sendMessage(" ");
                player.sendMessage("§a[RGRP] §fEach rail costs the town §e" + (int) costPerRail + " " + currency);
            }
            return false;       // if rails were not added to DB, return false
        }
    }

    private void addRailsToDatabase(Location rLoc, Location loc, Town town, Player pl)
    {
        String statement = "INSERT INTO `rails`(`rail`,`block`,`town`,'player') VALUES ('" + rLoc + "','" + loc + "','" + town + "','" + pl.getUniqueId() +"');";
        railsDB.executeStatement(statement);
    }

    private void removeRailsFromDatabase(Location rLoc)
    {
        railsDB.executeStatement(deleteStatement("rail", rLoc));
    }

    private void removeTownFromDatabase(String town)
    {
        railsDB.executeStatement(deleteStatement("town", town));
    }

    private String deleteStatement(String column, Object queryToDelete)
    {
        return "DELETE FROM `rails` WHERE " + column + " = '" + queryToDelete + "';";
    }

    protected static RGRP getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void onDisable(){}
}



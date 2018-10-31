package site.modicum.rgrp;

/*

AUTHOR: IGGYSAUR
DATE: 26/10/2018
FOR: RETRIBUTION GAMING - WILD WEST FRONTIER
VERSION 1.1

 */

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class RGRP extends JavaPlugin implements Listener, CommandExecutor {

    private double costPerRail = getConfig().getDouble("costPerRail");
    private String currency = getConfig().getString("currency");
    private SQLiteLib sqlLib;
    private List<Integer> blockList;
    private Database railsDB;

    private static RGRP INSTANCE;

    @Override
    public void onEnable()
    {
        INSTANCE = this;
        sqlLib = SQLiteLib.hookSQLiteLib(INSTANCE);
        sqlLib.initializeDatabase("protectedRails", "CREATE TABLE IF NOT EXISTS rails(id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, rail BLOB, block BLOB, town BLOB, player BLOB)");
        railsDB = sqlLib.getDatabase("protectedRails");

        getCommand("rgrp").setExecutor(new cmdRGRP());


        getConfig().options().copyDefaults(true);
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(INSTANCE, INSTANCE);
    }

    @EventHandler
    private void onClickWithRail(PlayerInteractEvent event)
    {
        if (event.getAction().equals(Action.LEFT_CLICK_AIR)
                || event.getAction().equals(Action.LEFT_CLICK_BLOCK)
                || !event.hasBlock()
                || !event.hasItem())
        {return;}
        Block block = event.getClickedBlock();
        int itemInHand = event.getItem().getTypeId();

        if(isProtectedBlock(itemInHand)) // if item in hand is on protections list
        {
            try
            {
                Resident res = TownyUniverse.getDataSource().getResident(event.getPlayer().getName());

                if (TownyUniverse.isWilderness(block)) //if location of rail placement isn't owned by a town then check
                {
                    if (res.hasTown()) // if player who placed rail has a town
                    {
                        Town town = res.getTown();
                        Location rLoc = block.getRelative(BlockFace.UP).getLocation();
                        Location loc = block.getLocation();
                        boolean isRank = (res.hasTownRank("assistant") || res.hasTownRank("helper") || res.isMayor());

                        if (isRank && town.canPayFromHoldings(costPerRail)) // if the town can afford to pay x land deeds per rail and player has rank
                        {
                            //pay x deeds from towny bank to place rail
                            town.pay(costPerRail, "Rail placed in unclaimed territory for " + costPerRail + " " + currency + " by " + res.getName());
                            event.getPlayer().sendMessage("");
                            event.getPlayer().sendMessage("§a[RGRP] §fBought a rail for §6" + town.getName() + " §fat a cost of §e" + (int) costPerRail + " " + currency);
                            event.getPlayer().sendMessage("");
                            event.getPlayer().sendMessage("§a[RGRP] §6" + town.getName() + "§f's balance§3: §e" + (int) (town.getHoldingBalance()) + " " + currency);

                            addRailsToDatabase(rLoc, loc, town, event.getPlayer());
                        }
                        else
                        {
                            event.setCancelled(true); // if player is not inside of (does not have a town) or player is part of a town but player isn't mayor or assistant, cancel event.

                            if (!isRank)
                            {
                                event.getPlayer().sendMessage("");
                                event.getPlayer().sendMessage("§a[RGRP] §fOnly §eTown Mayors§f, §eHelpers§f, and §eAssistants");
                                event.getPlayer().sendMessage("         §fcan place rails in the wilderness.");
                            }
                            else if (!town.canPayFromHoldings(costPerRail))
                            {
                                event.getPlayer().sendMessage(" ");
                                event.getPlayer().sendMessage("§a[RGRP] §fEach rail costs the town §e" + (int) costPerRail + " " + currency);
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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



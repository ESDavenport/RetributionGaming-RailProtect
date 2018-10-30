package site.modicum.rgrp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class cmdRGRP implements CommandExecutor {

    private RGRP plugin;

    protected cmdRGRP(RGRP plg) {
        plugin = plg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!cmd.getName().equalsIgnoreCase("rgrp") || args.length != 1 || !(sender instanceof Player) || !sender.hasPermission("rgrp.admin")){ return false; }

        Player player = (Player) sender;
        int blockID = player.getItemInHand().getTypeId();

        if (args[0].toLowerCase().charAt(0) == 'a') //add
        {
            if (!plugin.isProtectedBlock(blockID)) {
                plugin.addToBlockList(blockID);
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fBlockID: §3" + blockID + "§2 added");
                player.sendMessage("§e       - Config list updated.");
            } else {
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fBlockID: §3" + blockID + "§2 is already registered §fon the list.");
            }
        }
        else if (args[0].toLowerCase().charAt(0) == 'r') // remove
        {
            if (plugin.isProtectedBlock(blockID)) {
                plugin.removeFromBlockList(blockID);
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fBlockID: §3" + blockID + "§4 removed");
                player.sendMessage("§e       - Config list updated.");
            } else {
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fBlockID: §3" + blockID + "§4 is *NOT* registered §fon the list.");
            }
        }
        else if (args[0].toLowerCase().charAt(0) == 'c') // check
        {
            if (plugin.isProtectedBlock(blockID)) {
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fBlockID: §3" + blockID + "§2 is registered §fon the list.");
            } else {
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fBlockID: §3" + blockID + "§4 is *NOT* registered §fon the list.");
            }
        } else {return false;}

        return true;
    }
}

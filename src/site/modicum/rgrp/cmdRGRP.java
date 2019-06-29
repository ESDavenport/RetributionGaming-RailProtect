package site.modicum.rgrp;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class cmdRGRP implements CommandExecutor {

    private RGRP plugin = RGRP.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length != 1 || !(sender instanceof Player) || !sender.hasPermission("rgbf.admin")) { return false; }

        Player player = (Player) sender;
        int itemInHand = player.getItemInHand().getTypeId();
        char arg = args[0].toLowerCase().charAt(0);

        switch (arg) {

            default: return false;

            case 'a':
                if (plugin.addToBlockList(itemInHand)) {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§2 added");
                    player.sendMessage("§e       - Config list updated.");
                } else {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§2 is already registered §fon the list.");
                }
                break;

            case 'm':
                if (plugin.addToMultiList(itemInHand)) {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§2 added to multi list");
                    player.sendMessage("§e       - Config list updated.");
                } else {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§2 is already registered §fon the multi list.");
                }
                break;

            case 'r':
                if (plugin.removeFromBlockList(itemInHand)) {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§4 removed");
                    player.sendMessage("§e       - Config list updated.");
                }
                else if (plugin.removeFromMultiList(itemInHand)) {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§4 removed from the multi list");
                    player.sendMessage("§e       - Config list updated.");
                }
                else {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§4 is *NOT* registered §fon the list.");
                }
                break;

            case 'c':
                if (plugin.isProtectedBlock(itemInHand)) {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§2 is registered §fon the list.");
                } else {
                    player.sendMessage("");
                    player.sendMessage("§a[RGRP] §fBlockID: §3" + itemInHand + "§4 is *NOT* registered §fon the list.");
                }
                break;

            case 'g':
                player.getInventory().addItem(new ItemStack(plugin.getWand()));
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fRailProtection Wand §2 added");
                break;

            case 's':
                if (itemInHand != plugin.getWand()){plugin.setWand(itemInHand);}
                player.sendMessage("");
                player.sendMessage("§a[RGRP] §fRailProtection Wand §2 set: " + itemInHand);
                break;
        }

        return true;
    }
}

package com.NguyenDevs.physicEnderPearl;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class PEPCommand implements CommandExecutor, TabCompleter {
    private final PhysicEnderPearl base;
    private FileConfiguration messages;

    public PEPCommand(PhysicEnderPearl base) {
        this.base = base;
        loadMessages();
    }

    // Tải file messages.yml
    private void loadMessages() {
        File messagesFile = new File(base.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            base.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("physicenderpearl.reload")) {
                sender.sendMessage(messages.getString("messages.no-permission", "§cYou don't have permission to do this!"));
                return true;
            }
            base.reload();
            sender.sendMessage(messages.getString("messages.reload-success", "§aPhysicEnderPearl reload successfully!"));
            return true;
        }
        sender.sendMessage(String.format(messages.getString("messages.usage", "§cUse: /%s reload"), label));
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("physicenderpearl.reload")) {
            return List.of("reload");
        }
        return List.of();
    }
}
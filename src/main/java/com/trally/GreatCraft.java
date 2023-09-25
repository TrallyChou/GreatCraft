/*
 *  Copyright (c) 2023. Trally Chou (Zhou jiale, in Chinese)
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.trally;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GreatCraft extends JavaPlugin {

    public static GreatCraft plugin;

    public static File fileFold;


    @Override
    public void onEnable() {
        plugin = this;
        fileFold = this.getDataFolder();
        log("GreatCraft已加载");
        reloadRecipes();
        Bukkit.getPluginManager().registerEvents(new CraftListener(), this);
    }

    @Override
    public void onDisable() {
        log("GreatCraft已停用");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (args.length == 2) {
                if (p.hasPermission("greatcraft.create") && args[0].equals("new")) {
                    switch (args[1]) {
                        case "common":
                            CraftListener.opInSetting.put(p.getName(), 1);
                            Inventory workbench = Bukkit.createInventory(null, InventoryType.WORKBENCH, "请放置配方");
                            p.openInventory(workbench);
                            break;
                        case "furnace":
                            CraftListener.opInSetting.put(p.getName(), 2);
                            //p.sendMessage("请输入烹饪时间，单位：tick (1tick=0.05s)");
                            Inventory furnace = Bukkit.createInventory(null, InventoryType.FURNACE, "请放置被烹饪物和结果");
                            p.openInventory(furnace);
                            break;
                        case "special":
                            CraftListener.opInSetting.put(p.getName(), 3);
                            Inventory workbench_s = Bukkit.createInventory(null, InventoryType.WORKBENCH, "请放置配方，支持特殊物品");
                            p.openInventory(workbench_s);
                            break;
                    }
                    return true;
                }
                return true;
            }
            TextComponent[] showT = {new TextComponent("点击输入")};
            TextComponent msg1 = new TextComponent("§c/greatcraft new common 创建普通合成表");
            msg1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, showT));
            msg1.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/gct new common"));


            TextComponent msg2 = new TextComponent("§c/greatcraft new furnace 创建熔炉合成表");
            msg2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, showT));
            msg2.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/gct new furnace"));

            p.sendMessage("§c-----------伟大合成-----------");
            p.spigot().sendMessage(msg1);
            p.spigot().sendMessage(msg2);
            p.sendMessage("§c-----------伟大合成-----------");
        }


        return true;
    }

    static public void log(String s) {
        Bukkit.getConsoleSender().sendMessage("§b" + s);
    }

    static public void log(Object s) {
        System.out.println(s);
    }

    static public void reloadRecipes() {
        reloadCommonRecipes();
        reloadFurnaceRecipes();
        reloadSpecialRecipes();
    }

    public static void reloadCommonRecipes() {
        File commonFile = new File(GreatCraft.fileFold, "common.yml");
        YamlConfiguration commonYml = YamlConfiguration.loadConfiguration(commonFile);
        int commonSize = commonYml.getInt("size", 0);
        for (int i = 0; i < commonSize; i++) {
            if (commonYml.getItemStack(i + ".result") != null) {
                ShapedRecipe recipe = new ShapedRecipe(commonYml.getItemStack(i + ".result"));
                recipe.shape("123", "456", "789");
                for (int j = 1; j < 10; j++) {
                    if (commonYml.getString(i + "." + j) != null) {
                        recipe.setIngredient(Integer.toString(j).charAt(0), Material.getMaterial(commonYml.getString(i + "." + j)));
                    }


                }
                Bukkit.addRecipe(recipe);
            }
        }
    }

    public static void reloadFurnaceRecipes() {
        File furnaceFile = new File(GreatCraft.fileFold, "furnace.yml");
        YamlConfiguration furnaceYml = YamlConfiguration.loadConfiguration(furnaceFile);
        for (String str : furnaceYml.getKeys(false)) {
            FurnaceRecipe recipe = new FurnaceRecipe(furnaceYml.getItemStack(str + ".result"), Material.getMaterial(str));
            Bukkit.addRecipe(recipe);
            List<Object> tmp = new ArrayList<>();
            tmp.add(furnaceYml.getItemStack(str + ".result"));
            tmp.add((short) furnaceYml.getInt(str + ".cookingTime"));
            tmp.add(furnaceYml.getInt(str + ".exp"));
            CraftListener.furnace.put(str, tmp);
        }
    }

    public static void reloadSpecialRecipes() {
        File specialFile = new File(GreatCraft.fileFold, "special.yml");
        YamlConfiguration specialYml = YamlConfiguration.loadConfiguration(specialFile);
        int commonSize = specialYml.getInt("size", 0);
        List<String> specials = specialYml.getStringList("specials");
        for (int i = 0; i < specials.size(); i++) {
            CraftListener.specialItemsId.put(specials.get(i), String.valueOf(i));
        }
        Set<String> conditions;
        if (specialYml.getConfigurationSection("conditions") != null) {
            conditions = specialYml.getConfigurationSection("conditions").getKeys(false);
        } else {
            conditions = new HashSet<>();
        }

        for (String id : conditions) {
            CraftListener.specialCraftCondition.put(id, (List<Object>) specialYml.getList("conditions." + id));
        }

        for (int i = 0; i < commonSize; i++) {
            if (specialYml.getItemStack("common." + i + ".result") != null) {
                ShapedRecipe recipe = new ShapedRecipe(specialYml.getItemStack("common." + i + ".result"));
                recipe.shape("123", "456", "789");
                for (int j = 1; j < 10; j++) {
                    if (specialYml.getString("common." + i + "." + j) != null) {
                        recipe.setIngredient(Integer.toString(j).charAt(0), Material.getMaterial(specialYml.getString("common." + i + "." + j)));
                    }
                }
                Bukkit.addRecipe(recipe);
            }
        }

    }
}

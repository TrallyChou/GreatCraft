/*
 *  Copyright (c) 2023. Trally Chou (Zhou jiale, in Chinese)
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.trally;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CraftListener implements Listener {


    /*
     * 0/null: 无
     * 1: common
     * 2: furnace
     * 3: special
     */
    public static HashMap<String, Integer> opInSetting = new HashMap<>();

    public static HashMap<String, List<Object>> opOptions = new HashMap<>();

    public static HashMap<String, List<Object>> furnace = new HashMap<>();

    public static HashMap<String, Integer> expPerFur = new HashMap<>();

    public static HashMap<String, Integer> smeltTimesPerFur = new HashMap<>();

    public static HashMap<String, String> specialItemsId = new HashMap<>();

    public static HashMap<String, List<Object>> specialCraftCondition = new HashMap<>();
    //0:true/false 1-9:格子对应物品的消耗数量

    public static HashMap<String, String> playerCrafting = new HashMap<>();

    public static HashMap<String, String> playerAddingItemsForCrafting = new HashMap<>();


    @EventHandler
    public void invClicked(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (p.hasPermission("greatcraft.create") && opInSetting.containsKey(p.getName()) && opInSetting.get(p.getName()) != 0) {

            if (e.getClickedInventory() != null) {
                if (e.getClickedInventory().getType() == InventoryType.WORKBENCH) {
                    if (e.getSlot() == 0) {
                        e.setCancelled(true);
                        e.getInventory().setItem(0, p.getItemOnCursor());
                    }
                }

                if (e.getClickedInventory() != null) {
                    if (e.getClickedInventory().getType() == InventoryType.FURNACE) {
                        if (e.getSlot() == 2) {
                            e.setCancelled(true);
                            e.getInventory().setItem(2, p.getItemOnCursor());
                        }
                    }
                }

            }
            return;
        }

        if (e.getClickedInventory() != null && e.getClickedInventory().getType() == InventoryType.FURNACE) {
            if (e.getSlot() == 0) {
                ItemStack item = e.getCursor();
                Furnace fur = (Furnace) e.getInventory().getHolder();
                Bukkit.getScheduler().runTask(GreatCraft.plugin, () -> resetFurTimes(item, fur));

            }
        }


        if (playerAddingItemsForCrafting.containsKey(p.getName())) {
            if (e.getClickedInventory() != null && e.getClickedInventory().getType() == InventoryType.WORKBENCH) {
                //无奖竞猜：这段代码干了什么？
                if (e.getCursor().isSimilar(e.getCurrentItem())) {
                    e.setCancelled(true);
                    int amount = e.getCurrentItem().getAmount() + e.getCursor().getAmount();
                    ItemStack tmp = e.getCurrentItem();
                    e.getClickedInventory().setItem(e.getSlot(), new ItemStack(Material.AIR));
                    tmp.setAmount(amount);
                    e.getView().setCursor(tmp);
                }
                playerAddingItemsForCrafting.remove(p.getName());

            }
        }
    }

    @EventHandler
    public void invItemMove(InventoryMoveItemEvent e) {
        Inventory from = e.getSource();
        Inventory to = e.getDestination();
        ItemStack item = e.getItem();
        if (to != null && to.getType() == InventoryType.FURNACE) {
            resetFurTimes(item, (Furnace) to.getHolder());
        }
    }


    @EventHandler
    public void furStartBurning(FurnaceBurnEvent e) {
        if (e.getBlock() != null) {
            Furnace fur = (Furnace) e.getBlock().getState();
            ItemStack item = fur.getInventory().getSmelting();
            resetFurTimes(item, fur);
        }
    }


    @EventHandler
    public void invExtract(FurnaceExtractEvent e) {
        Furnace fur = (Furnace) e.getBlock().getState();
        if (expPerFur.get(fur.getLocation().toString()) != null) {
            e.setExpToDrop(expPerFur.get(fur.getLocation().toString()) * e.getItemAmount());
        }
    }

    @EventHandler
    public void furSmelt(FurnaceSmeltEvent e) {
        Furnace fur = (Furnace) e.getBlock().getState();
        Integer times = smeltTimesPerFur.get(fur.getLocation().toString());
        ItemStack item = fur.getInventory().getItem(0);
        if (item != null) {
            if (times != null) {
                if (times > 0) {
                    e.setCancelled(true);
                    smeltTimesPerFur.put(fur.getLocation().toString(), times - 1);
                } else {
                    smeltTimesPerFur.remove(fur.getLocation().toString());
                    resetFurTimes(item, fur);
                }

            } else {
                resetFurTimes(item, fur);
            }
        }
    }


    @EventHandler
    public void invClosed(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (p.hasPermission("greatcraft.create") && opInSetting.containsKey(p.getName()) && opInSetting.get(p.getName()) != 0) {
            Inventory inv = e.getInventory();
            switch (opInSetting.get(p.getName())) {
                case 1:
                    File commonFile = new File(GreatCraft.fileFold, "common.yml");
                    YamlConfiguration commonYml = YamlConfiguration.loadConfiguration(commonFile);
                    if (inv.getItem(0) != null) {
                        int index = commonYml.getInt("size", 0);

                        commonYml.set(index + ".result", inv.getItem(0));
                        for (int i = 1; i < 10; i++) {
                            if (inv.getItem(i) != null) {
                                commonYml.set(index + "." + i, inv.getItem(i).getType().name());
                            }
                        }

                        commonYml.set("size", index + 1);
                        try {
                            commonYml.save(commonFile);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        GreatCraft.reloadCommonRecipes();
                        p.sendMessage("成功创建");
                    } else {
                        p.sendMessage("取消创建");
                    }
                    opInSetting.remove(p.getName());
                    break;
                case 2:
                    File furnaceFile = new File(GreatCraft.fileFold, "furnace.yml");
                    YamlConfiguration furnaceYml = YamlConfiguration.loadConfiguration(furnaceFile);
                    if (inv.getItem(0) != null) {
                        furnaceYml.set(inv.getItem(0).getType().name() + ".result", inv.getItem(2));
                        try {
                            furnaceYml.save(furnaceFile);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        List<Object> options = new ArrayList<>();
                        options.add(inv.getItem(0).getType().name());
                        opOptions.put(p.getName(), options);
                        Bukkit.getPluginManager().registerEvents(new ChatListener(), GreatCraft.plugin);
                        p.sendMessage("请输入烹饪时间");
                    } else {
                        p.sendMessage("取消创建");
                    }
                    break;
                case 3:
                    File specialFile = new File(GreatCraft.fileFold, "special.yml");
                    YamlConfiguration specialYml = YamlConfiguration.loadConfiguration(specialFile);
                    if (inv.getItem(0) != null) {
                        int index = specialYml.getInt("size", 0);
                        List<String> specials = specialYml.getStringList("specials");  //为特殊物品赋予id
                        if (specials == null) specials = new ArrayList<>();
                        StringBuilder resultId = new StringBuilder();          //为制作结果赋予id
                        List<Object> specialsForThis = new ArrayList<>();      //为制作结果赋予制作需求

                        //§
                        ItemStack tmpItem = inv.getItem(0);
                        ItemMeta tmpMeta = tmpItem.getItemMeta();
                        tmpMeta.setDisplayName(tmpMeta.getDisplayName() + "§s§r");
                        tmpItem.setItemMeta(tmpMeta);

                        specialYml.set("common." + index + ".result", tmpItem);
                        specialsForThis.add(true);
                        for (int i = 1; i < 10; i++) {
                            p.sendMessage("3");
                            int specialId;
                            if (inv.getItem(i) != null) {
                                specialYml.set("common." + index + "." + i, inv.getItem(i).getType().name());
                                if (specials.contains(inv.getItem(i).toString())) {
                                    specialId = specials.indexOf(inv.getItem(i).toString());
                                } else {
                                    specialId = specials.size();
                                    ItemStack tmpItem2 = inv.getItem(i).clone();
                                    tmpItem2.setAmount(1);
                                    specials.add(tmpItem2.toString());
                                }
                                resultId.append(specialId).append("-");
                                specialsForThis.add(inv.getItem(i).getAmount());
                            } else {
                                resultId.append("-");
                                specialsForThis.add(0);
                            }
                        }

                        specialYml.set("size", index + 1);
                        specialYml.set("specials", specials);
                        specialYml.set("conditions." + resultId.toString(), specialsForThis);
                        try {
                            specialYml.save(specialFile);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        GreatCraft.reloadSpecialRecipes();
                        p.sendMessage("成功创建");
                    } else {
                        p.sendMessage("取消创建");
                    }
                    opInSetting.remove(p.getName());
                    break;
            }
        }

        playerAddingItemsForCrafting.remove(p.getName());
    }


//    @EventHandler
//    void onBurning(Furnace e) {
//
//    }


    @EventHandler
    public void preShowCraft(PrepareItemCraftEvent e) {

        //注意，拿走东西会触发，但是放下东西不触发，所以需要额外处理放下东西


        Inventory inv = e.getInventory();
        Player p = (Player) e.getView().getPlayer();
        ItemStack tmpItem = inv.getItem(0);
        ItemMeta tmpMeta = tmpItem.getItemMeta();
        if (tmpMeta.getDisplayName() != null && tmpMeta.getDisplayName().endsWith("§s§r")) {

            StringBuilder tmpId = new StringBuilder();
            GreatCraft.log(2);
            boolean success = true;
            for (int i = 1; i < 10; i++) {
                ItemStack nowItem = inv.getItem(i);
                if (nowItem != null) {
                    ItemStack tmpNowItem = nowItem.clone();
                    tmpNowItem.setAmount(1);
                    String tmpStr = specialItemsId.get(tmpNowItem.toString());
                    if (tmpStr == null) {
                        success = false;
                        break;
                    }
                    tmpId.append(tmpStr).append("-");
                } else {
                    tmpId.append("-");
                }
            }
            if (!success) {
                inv.setItem(0, new ItemStack(Material.AIR));
                playerCrafting.remove(p.getName());
                GreatCraft.log(3);
            } else {
                List<Object> condition = specialCraftCondition.get(tmpId.toString());
                if (condition == null) {
                    inv.setItem(0, new ItemStack(Material.AIR));
                    playerCrafting.remove(p.getName());
                    GreatCraft.log(6);
                } else {
                    for (int i = 1; i < 10; i++) {
                        if (inv.getItem(i) != null) {
                            if ((Integer) condition.get(i) > inv.getItem(i).getAmount()) {
                                inv.setItem(0, new ItemStack(Material.AIR));
                                List<Object> tmpList = new ArrayList<>();
                                playerAddingItemsForCrafting.put(p.getName(), tmpId.toString());
                                playerCrafting.put(p.getName(), tmpId.toString());
                                return;
                            }
                        }

                    }
                    playerCrafting.put(p.getName(), tmpId.toString());
                    String newName = tmpMeta.getDisplayName().substring(0, tmpMeta.getDisplayName().length() - 4);
                    if (newName.equals("null")) newName = null;
                    tmpMeta.setDisplayName(newName);
                    tmpItem.setItemMeta(tmpMeta);
                }
            }

        } else {
            playerCrafting.remove(p.getName());
        }
    }


    @EventHandler
    public void onCraft(CraftItemEvent e) {
        Player p = (Player) e.getWhoClicked();
        String craftingId = playerCrafting.get(p.getName());
        if (craftingId != null) {
            e.setCancelled(false);
            List<Object> conditions = specialCraftCondition.get(craftingId);
            Inventory inv = e.getClickedInventory();
            for (int i = 1; i < 10; i++) {
                ItemStack item = inv.getItem(i);
                if (item != null) item.setAmount(item.getAmount() - (Integer) conditions.get(i) + 1);

            }

            playerCrafting.remove(p.getName());
        }


    }


    public static void createAFurnaceRecipe(Player p) {
        File furnaceFile = new File(GreatCraft.fileFold, "furnace.yml");
        YamlConfiguration furnaceYml = YamlConfiguration.loadConfiguration(furnaceFile);
        List<Object> options = opOptions.get(p.getName());
        String name = (String) options.get(0);
        int cookingTime = (int) options.get(1);
        int exp = (int) options.get(2);
        furnaceYml.set(name + ".cookingTime", cookingTime);
        furnaceYml.set(name + ".exp", exp);
        try {
            furnaceYml.save(furnaceFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        opInSetting.remove(p.getName());
        GreatCraft.reloadFurnaceRecipes();
    }


    public static void resetFurTimes(ItemStack item, Furnace fur) {
        if (item != null) {
            String tmpName = item.getType().name();
            List<Object> l = furnace.get(tmpName);
            if (l != null) {
                int cookTime = (short) l.get(1);
                if (cookTime <= 200) {
                    fur.setCookTime((short) (200 - cookTime));
                } else {
                    int times = cookTime / 200;
                    fur.setCookTime((short) (200 - (cookTime % 200)));
                    smeltTimesPerFur.put(fur.getLocation().toString(), times);

                }
                expPerFur.put(fur.getLocation().toString(), (int) l.get(2));
            }

        } else {
            fur.setCookTime((short) 0);
            expPerFur.remove(fur.getLocation().toString());
            smeltTimesPerFur.remove(fur.getLocation().toString());
        }
    }


}

package com.trally;

import org.bukkit.Bukkit;
import org.bukkit.block.Furnace;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CraftListener implements Listener {


    /*
     * 0/null: 无
     * 1: common
     */
    public static HashMap<String, Integer> opInSetting = new HashMap<>();

    public static HashMap<String, List<Object>> opOptions = new HashMap<>();

    public static HashMap<String, List<Object>> furnace = new HashMap<>();

    public static HashMap<String, Integer> expPerFur = new HashMap<>();

    public static HashMap<String, Integer> smeltTimesPerFur = new HashMap<>();

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


//    @EventHandler
//    public void prepareCraft(PrepareItemCraftEvent e){
//
//
//    }


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
                    }


                    break;
            }
        }


    }


//    @EventHandler
//    void onBurning(Furnace e) {
//
//    }


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

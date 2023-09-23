package com.trally;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatListener implements Listener {

    @EventHandler
    public void input(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();

        if (CraftListener.opInSetting.get(p.getName()) == 2) {
            List<Object> tmp = CraftListener.opOptions.get(p.getName());
            e.setCancelled(true);
            if (tmp != null) {
                if (tmp.size() == 1) {
                    tmp.add(Integer.parseInt(e.getMessage()));
                    p.sendMessage("请输入成功后给予玩家的经验");
                    return;
                }

                if (tmp.size() == 2) {
                    tmp.add(Integer.parseInt(e.getMessage()));
                    AsyncPlayerChatEvent.getHandlerList().unregister(this);
                }
            }
            p.sendMessage("成功创建");
            CraftListener.opOptions.put(p.getName(), tmp);
            CraftListener.createAFurnaceRecipe(p);
        }
    }
}

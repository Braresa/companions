package me.astero.companions.listener.companions;

import me.astero.companions.CompanionsPlugin;
import me.astero.companions.companiondata.PlayerData;
import me.astero.companions.database.DataUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

// NOT REGISTERED YET

public class PlayerDataHandler implements Listener {
    CompanionsPlugin main;

    public PlayerDataHandler(CompanionsPlugin _main) {
        this.main = _main;
    }

    @EventHandler
    public void PlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        DataUtils dataUtils = main.getDataUtils();
        PlayerData firstPlayerData = PlayerData.instanceOf(player);

        if(main.isSQLDataBase()) {
            dataUtils.loadSQLSavedDataIntoPlayer(player, firstPlayerData);
        } else {
            dataUtils.loadYMLSavedDataIntoPlayer(player);
        }
    }

    @EventHandler
    public void PlayerLeft(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = PlayerData.instanceOf(player);
        DataUtils dataUtils = main.getDataUtils();

            dataUtils.saveOwnedCompanions(player, playerData);
            dataUtils.saveActiveCompanionName(player, playerData);
    }
}

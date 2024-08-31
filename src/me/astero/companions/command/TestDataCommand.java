package me.astero.companions.command;

import me.astero.companions.CompanionsPlugin;
import me.astero.companions.companiondata.PlayerData;
import me.astero.companions.database.DataUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TestDataCommand implements CommandExecutor {
    CompanionsPlugin main;
    public TestDataCommand(CompanionsPlugin _main) { this.main = _main;  }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = (Player) sender;
        PlayerData playerData = PlayerData.instanceOf(player);
        DataUtils dataUtils = main.getDataUtils();

        if(args[0].equals("sql")) {
            dataUtils.saveActiveCompanionName(player, playerData);
            dataUtils.saveOwnedCompanions(player, playerData);
            return true;
        } else if (args[0].equals("sql2")) {
            dataUtils.loadSQLSavedDataIntoPlayer(player, PlayerData.instanceOf(player));
        } else if (args[0].equals("pets")) {
            for(String companionName : playerData.getOwnedCompanions().keySet()) {
                player.sendMessage(companionName);
            }
        }
        return false;
    }
}

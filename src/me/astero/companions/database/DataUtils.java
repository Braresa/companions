package me.astero.companions.database;

import me.astero.companions.CompanionsPlugin;
import me.astero.companions.companiondata.CustomCompanion;
import me.astero.companions.companiondata.PlayerCache;
import me.astero.companions.companiondata.PlayerData;
import me.astero.companions.util.ItemBuilderUtil;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DataUtils {

    CompanionsPlugin main;

    public DataUtils(CompanionsPlugin main) {
        this.main = main;
    }

    private Runnable storeActiveCompanionNameSQL(Player player, String companionName) {
        return () -> {
            try(Connection connection = main.getDatabase().getHikari().getConnection()) {
                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO "+ main.getDatabase().getTablePrefix()
                        +"active (UUID,name,companion) VALUES (?,?,?)" +
                        "  ON DUPLICATE KEY UPDATE companion=?");
                preparedStatement.setString(1, player.getUniqueId().toString());
                preparedStatement.setString(2, player.getName());
                preparedStatement.setString(3, companionName.toUpperCase());
                preparedStatement.setString(4, companionName.toUpperCase());

            } catch (SQLException exception) {
                exception.printStackTrace();
            }
        };
    }

    private Runnable storeOwnedCompanionsSQL(Player player, Map<String, CustomCompanion> ownedCompanionsMap) {
        return () -> {
          try(Connection connection = main.getDatabase().getHikari().getConnection()) {
              for (Map.Entry<String, CustomCompanion> entry : ownedCompanionsMap.entrySet()) {
                  String companionName = entry.getKey();
                  CustomCompanion companion = entry.getValue();
                  String customWeapon;

                  if(companionName == null) {
                      main.getLogger().warning("[WARN] This is not supposed to happen, companionName is null");
                      continue;
                  }

                  if (companion.getCustomWeapon() == null) {
                      customWeapon = "NONE";
                  } else {
                      customWeapon = companion.getCustomWeapon().getType().toString();
                  }

                  String statement = String.format("INSERT INTO %s" + "owned(UUID, name, companion, customWeapon, customName, nameVisible, abilityLevel) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?)", main.getDatabase().getTablePrefix());
                  PreparedStatement preparedStatement = connection.prepareStatement(statement);
                  preparedStatement.setString(1, player.getUniqueId().toString());
                  preparedStatement.setString(2, player.getName());
                  preparedStatement.setString(3, companionName.toUpperCase());
                  preparedStatement.setString(4, customWeapon);
                  preparedStatement.setString(5, companion.getCustomName());
                  preparedStatement.setBoolean(6, companion.isNameVisible());
                  preparedStatement.setInt(7, companion.getAbilityLevel());
                  preparedStatement.execute();

              }
          } catch (SQLException exception) {
              exception.printStackTrace();
          }
        };
    }

    private void storeOwnedCompanionsYML(Player player, Map<String, CustomCompanion> ownedCompanionsMap) {
        UUID playerUUID = player.getUniqueId();
        for(Map.Entry<String, CustomCompanion> entry : ownedCompanionsMap.entrySet()) {
            String companionName = entry.getKey();
            CustomCompanion companion = entry.getValue();

            YamlConfiguration dataFile = main.getFileManager().getCompanionsData();

            dataFile.set(String.format("companions.%s.owned.%s.customWeapon", playerUUID, companionName),
                    companion.getCustomWeapon().getType().toString());
            dataFile.set(String.format("companions.%s.owned.%s.customName", playerUUID, companionName),
                    companion.getCustomName());
            dataFile.set(String.format("companions.%s.owned.%s.nameVisible", playerUUID, companionName),
                    companion.isNameVisible());
            dataFile.set(String.format("companions.%s.owned.%s.abilityLevel", playerUUID, companionName),
                    companion.getAbilityLevel());
        }
    }

    private void storeActiveCompanionNameYML (Player player, String companionName) {
        main.getFileManager().getCompanionsData().set("companions." + player.getUniqueId() + ".active", companionName.toUpperCase());
    }

    private CustomCompanion registerNewCompanion(String companionName, String customName, String customWeapon, Boolean isNameVisible, Integer abilityLevel) {
        CustomCompanion companion = new CustomCompanion();

        if(!(customWeapon == null) || !customWeapon.equals("NONE")) {
            try
            {
                companion.setCustomWeapon(new ItemBuilderUtil(
                        Material.valueOf(customWeapon),
                        companionName.toUpperCase() + "'S WEAPON",
                        1).build());
            }
            catch(IllegalArgumentException itemNotFound)
            {
                companion.setCustomWeapon(new ItemBuilderUtil(
                        Material.STONE,
                        companionName.toUpperCase() + "'S WEAPON",
                        1).build());

                System.out.println(ChatColor.GOLD + "COMPANIONS → " + ChatColor.GRAY + companionName + "'s Weapon failed to load. - "
                        + "Please check if the material name is for the correct Minecraft server version.");
            }
        }

        companion.setCustomName(customName);
        companion.setNameVisible(isNameVisible);
        companion.setAbilityLevel(abilityLevel);

        return companion;
    }

    public void loadSQLSavedDataIntoPlayer (Player player, PlayerData playerData) {
        UUID playerUUID = player.getUniqueId();
        try(Connection connection = main.getDatabase().getHikari().getConnection()) {

            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + main.getDatabase().getTablePrefix() +
                    "owned WHERE UUID = ?");
            preparedStatement.setString(1, playerUUID.toString());

            ResultSet resultSet = preparedStatement.executeQuery();

            Map<String, CustomCompanion> companionMap = new HashMap<>();

            // Companions owned
            while(resultSet.next()) {
                String companionName = resultSet.getString("companion");
                CustomCompanion companion = registerNewCompanion(companionName, resultSet.getString("customName"),
                        resultSet.getString("customWeapon"), resultSet.getBoolean("nameVisible"),
                        resultSet.getInt("abilityLevel"));
                companionMap.put(companionName, companion);
            }

            playerData.setOwnedCompanions(companionMap);

            preparedStatement = connection.prepareStatement("SELECT * FROM " + main.getDatabase().getTablePrefix() + "active WHERE UUID = ?");
            preparedStatement.setString(1, playerUUID.toString());
            resultSet = preparedStatement.executeQuery();

            // Active companion name
            if(resultSet.next()) {
                String companionName = resultSet.getString("companion");
                if(main.getCompanionUtil().isCompanionNameValid(companionName)) {
                    playerData.setActiveCompanionName(companionName);
                } else {
                    playerData.setActiveCompanionName("NONE");
                    main.getLogger().warning(String.format("%s saved SQL active pet name is not correct!", player.getName()));
                }
            }

            main.getDatabase().close(connection, preparedStatement, resultSet);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    public void loadYMLSavedDataIntoPlayer(Player player) {
        PlayerData playerData = PlayerData.instanceOf(player);
        YamlConfiguration dataFile = main.getFileManager().getCompanionsData();
        UUID playerUUID = player.getUniqueId();


        // Owned companions
        for(String companionName : dataFile.getConfigurationSection(String.format("companions.%s.owned", playerUUID)).getKeys(false)) {
            String customName;
            String customWeaponString;
            boolean isNameVisible;
            int abilityLevel;

            customName = dataFile.getString(String.format("companions.owned.%s.%s.customName", playerUUID, companionName));
            customWeaponString = dataFile.getString(String.format("companions.owned.%s.%s.customWeapon", playerUUID, companionName));
            isNameVisible = dataFile.getBoolean(String.format("companions.owned.%s.%s.nameVisible", playerUUID, companionName));
            abilityLevel = dataFile.getInt(String.format("companions.owned.%s.%s.abilityLevel", playerUUID, companionName));

            playerData.getOwnedCompanions().put(companionName, registerNewCompanion(companionName, customName, customWeaponString, isNameVisible, abilityLevel));
        }

        // Active companion
        String activeCompanionName = dataFile.getString(String.format("companions.%s.active", playerUUID));
        if(activeCompanionName != null && main.getCompanionUtil().isCompanionNameValid(activeCompanionName)) {
            playerData.setActiveCompanionName(activeCompanionName);
        } else {
            playerData.setActiveCompanionName("NONE");
            main.getLogger().warning(String.format("%s saved YML active pet name is not correct!", player.getName()));
        }
    }

    public void saveActiveCompanionName(Player player, PlayerData playerData) {
            if(main.isSQLDataBase()) {
                Bukkit.getScheduler().runTaskAsynchronously(main, storeActiveCompanionNameSQL(player, playerData.getActiveCompanionName()));
                main.getLogger().info("Saving ActiveCompanionName SQL!");
            } else {
                storeActiveCompanionNameYML(player, playerData.getActiveCompanionName());
                main.getLogger().info("Saving ActiveCompanionName YML!");
            }
    }

    public void saveOwnedCompanions(Player player, PlayerData playerData) {
        playerData.setOwnedCompanions(PlayerCache.instanceOf(player.getUniqueId()).getOwnedCache());
        if(main.isSQLDataBase()) {
            Bukkit.getScheduler().runTaskAsynchronously(main, storeOwnedCompanionsSQL(player, playerData.getOwnedCompanions()));
            main.getLogger().info("Saving OwnedCompanions SQL!");
        } else {
            storeOwnedCompanionsYML(player, playerData.getOwnedCompanions());
            main.getLogger().info("Saving OwnedCompanions YML!");
        }
    }
}

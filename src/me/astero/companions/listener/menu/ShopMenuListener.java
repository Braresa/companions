package me.astero.companions.listener.menu;

import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import me.astero.companions.CompanionsPlugin;
import me.astero.companions.companiondata.PlayerData;
import me.astero.companions.economy.EconomyHandler;
import me.astero.companions.gui.ShopMenu;

import java.util.UUID;

public class ShopMenuListener implements Listener {

	private CompanionsPlugin main;
	
	public ShopMenuListener(CompanionsPlugin main)
	{
		this.main = main;
	}
	
	@EventHandler
	public void onClick (InventoryClickEvent e)
	{
		Player player = (Player) e.getWhoClicked();
		
		try
		{
			
			boolean shopMenu = ChatColor.translateAlternateColorCodes('&', 
					e.getView().getTitle()).equals(ChatColor.translateAlternateColorCodes('&', 
							main.getFileHandler().getCompanionShopTitle()));
			
			
			if(shopMenu)
			{
				if(e.getCurrentItem() != null)
				{
					e.setCancelled(true);		
					
					String getCurrent = e.getCurrentItem().getItemMeta().getDisplayName();
					
					
					if(getCurrent.equals(ChatColor.translateAlternateColorCodes('&', main.getFileHandler().getGoBackName()))) // goback button
					{
						PlayerData.instanceOf(player).setPageNumber(PlayerData.instanceOf(player).getPageNumber() - 1);
						
						if(PlayerData.instanceOf(player).getPageNumber() == 0)
						{
							Bukkit.dispatchCommand(player, main.getFileHandler().getCompanionsShopGoBackCommand());
						}
						else
						{
							Bukkit.dispatchCommand(player, "companions shop"); // So it resets to the first page too.
						}
					}
					else if(getCurrent.equals(ChatColor.translateAlternateColorCodes('&', main.getFileHandler().getNextPageName()))) //next page button
					{
						PlayerData.instanceOf(player).setPageNumber(PlayerData.instanceOf(player).getPageNumber() + 1);
						new ShopMenu(main, player);
					}
					
					for(String getCompanionName : main.getFileHandler().getCompanionDetails().keySet()) // Iterate an ArrayList that contains all the companion names.
					{
						if(ChatColor.translateAlternateColorCodes('&', main.getFileHandler().getCompanionDetails().get(getCompanionName).getItemName()).equals(getCurrent))
						{
							
							if(main.getFileHandler().getCompanionDetails().get(getCompanionName).getRawPrice().contains("C")) // Playerpoints
							{
								PlayerPointsAPI ppApi = main.getPlayerPointsAPI();
								UUID playerUUID = player.getUniqueId();

								if(ppApi == null) {
									main.getLogger().warning("PlayerPointsAPI is null!");
									return;
								}

								int petPrice = (int) main.getFileHandler().getCompanionDetails().get(getCompanionName).getItemPrice();

								if(ppApi.look(playerUUID) < petPrice) {
									player.sendMessage(ChatColor.translateAlternateColorCodes('&', main.getCompanionUtil().getPrefix() + main.getFileHandler().getNotEnoughMoneyMessage()
											.replace("%price%", petPrice + " &6CASH")));
									return;
								} else if (ppApi.look(playerUUID) >= petPrice) {
									ppApi.take(playerUUID, petPrice);
								}
							} else if (main.getFileHandler().getCompanionDetails().get(getCompanionName).getRawPrice().contains("M")) { // Money
								if(main.getFileHandler().isVault())
								{
									String formattedPrice = main.getFileHandler().getCompanionDetails().get(getCompanionName).getFormatedPrice();
									if(EconomyHandler.getEconomy().has(player, main.getFileHandler().getCompanionDetails().get(getCompanionName).getItemPrice()))
									{
										EconomyHandler.getEconomy().withdrawPlayer(player, main.getFileHandler().getCompanionDetails().get(getCompanionName).getItemPrice());
									}
									else if(!EconomyHandler.getEconomy().has(player, main.getFileHandler().getCompanionDetails().get(getCompanionName).getItemPrice()))
									{
										player.sendMessage(ChatColor.translateAlternateColorCodes('&', main.getCompanionUtil().getPrefix() + main.getFileHandler().getNotEnoughMoneyMessage()
												.replace("%price%", formattedPrice)));
										return;
									}
								}
							} else
							{
								return;
							}
							// Comprado com sucesso
								main.getCompanionUtil().storeNewYML(getCompanionName, player);

								main.getCompanionUtil().updateCache(player.getUniqueId(), getCompanionName,
										main.getFileHandler().getCompanionDetails().get(getCompanionName).getName(), 
										main.getFileHandler().getCompanionDetails().get(getCompanionName).getWeapon(),
										main.getFileHandler().getCompanionDetails().get(getCompanionName).isNameVisible(),
										main.getFileHandler().getCompanionDetails().get(getCompanionName).getAbilityLevel());
								
								
								player.sendMessage(ChatColor.translateAlternateColorCodes('&', main.getCompanionUtil().getPrefix() + main.getFileHandler().getItemBoughtMessage()
										.replace("%companion%", getCompanionName.toUpperCase())
										.replace("%companion_l%", getCompanionName.substring(0,1).toUpperCase() + getCompanionName.substring(1).toLowerCase())));
								
								new ShopMenu(main, player);


						}
					}
				}
			}
				
		}
		catch(NullPointerException e1) {}
	}
	


}

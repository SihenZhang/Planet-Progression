package com.mjr.planetprogression.command;

import java.util.List;
import java.util.UUID;

import micdoodle8.mods.galacticraft.core.util.EnumColor;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import com.mjr.mjrlegendslib.util.PlayerUtilties;
import com.mjr.planetprogression.data.SatelliteData;
import com.mjr.planetprogression.handlers.capabilities.CapabilityStatsHandler;
import com.mjr.planetprogression.handlers.capabilities.IStatsCapability;
import com.mojang.authlib.GameProfile;

public class CommandAddSatellite extends CommandBase {

	@Override
	public String getUsage(ICommandSender var1) {
		return "/" + this.getName() + " <player>";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 3;
	}

	@Override
	public String getName() {
		return "addNewSatellite";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		String var3 = null;
		EntityPlayerMP playerBase = null;
		if (args.length > 0) {
			var3 = args[0];
			GameProfile gameprofile = server.getPlayerProfileCache().getGameProfileForUsername(var3);

			EntityPlayerMP playerToAddFor = PlayerUtilties.getPlayerFromUUID(gameprofile.getId());
			try {
				playerBase = PlayerUtil.getPlayerBaseServerFromPlayerUsername(sender.getName(), true);
				IStatsCapability stats = null;
				if (playerToAddFor != null) {
					stats = playerToAddFor.getCapability(CapabilityStatsHandler.PP_STATS_CAPABILITY, null);
				}
				String id = UUID.randomUUID().toString();
				stats.addSatellites(new SatelliteData(0, id, 0, null));
				playerToAddFor.sendMessage(new TextComponentString(EnumColor.RED + "Satellite: " + id + " has been launched in to space!"));
				playerBase.sendMessage(new TextComponentString(EnumColor.AQUA + "You have launched a satellite in to space! for: " + gameprofile.getName() + " with id: " + id));
			} catch (final Exception var6) {
				throw new CommandException(var6.getMessage(), new Object[0]);
			}
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {
		return args.length == 1 ? getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames()) : null;
	}

	@Override
	public boolean isUsernameIndex(String[] par1ArrayOfStr, int par2) {
		return par2 == 0;
	}
}
package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class BlockBreakTask extends LoopedPrinterTask {

	private BlockPos pos;
	private EnumFacing side;

	private static Minecraft minecraft;

	public BlockBreakTask(BlockPos realPos, EnumFacing fromSide) {
		super();
		pos = realPos;
		side = fromSide;
		minecraft = Minecraft.getMinecraft();
	}

	private static ItemStack findPick() {
		for (int i = 0; i < minecraft.player.inventory.mainInventory.size(); i++) {
			if (minecraft.player.inventory.mainInventory.get(i).getItem() instanceof ItemPickaxe) {
				return minecraft.player.inventory.mainInventory.get(i);
			}
		}
		return null;
	}

	@Override
	public void onUpdate() {
		minecraft.playerController.onPlayerDamageBlock(pos, EnumFacing.DOWN);
		if (minecraft.world.isAirBlock(pos)) {
			SchematicPrinter.INSTANCE.setTimeout(pos, ConfigurationHandler.timeout);
			endTask();
		}

	}

	public void queueWithFacing() {
		if (true) {
			new FaceBlockSideTask(pos, side).queue();
		}
		this.queue();
	}
	
	public void start() {
		ItemStack pick = findPick();
		if (pick != null) {
			SchematicPrinter.INSTANCE.swapToItem(minecraft.player.inventory, pick);
		}
	}

}

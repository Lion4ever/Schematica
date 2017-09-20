package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class StackUpTask extends LoopedPrinterTask {

	private boolean firstUpdate = true;
	private BlockPos pos;

	private static Minecraft minecraft = Minecraft.getMinecraft();
	private static EntityPlayerSP player = minecraft.player;

	public StackUpTask(BlockPos pos) {
		super();
		this.pos = pos;
	}

	@Override
	public void onUpdate() {
		if (pos.getY() == player.posY - 1) {
			player.jump();
			firstUpdate = false;
		}
		if (player.posY > pos.getY() + 2d) {
			if (nextTask.execute() == EnumActionResult.SUCCESS) {
				endTask(); // BlockPlaceTask ended before this but that does not
							// matter
			} else {
				SchematicPrinter.INSTANCE.currentTask = this; // Dont let this
																// end yet
			}
		}

	}
}

package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class SimpleMoveTask extends LoopedPrinterTask {
	
	private Vec3d pos;
	private boolean sneak;
	private static EntityPlayerSP player;
	
	private Vec3d motion;
	private boolean firstUpdate = true;

	public SimpleMoveTask(Vec3d pos, boolean sneak) {
		super();
		this.pos = pos;
		this.sneak = sneak;
		
		player = Minecraft.getMinecraft().player;
		motion = pos.subtract(player.getPositionVector()).normalize().scale(0.1d);
	}

	public SimpleMoveTask(BlockPos pos, boolean sneak) {
		this(new Vec3d(pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d), sneak);
	}

	@Override
	public void onUpdate() {
		if (firstUpdate) {
			SchematicPrinter.INSTANCE.syncSneaking(player, sneak);
			firstUpdate = false;
		}
		player.motionX = motion.x;
		player.motionY = motion.y;
		player.motionZ = motion.z;
		if (player.getPositionVector().subtract(pos).lengthSquared() < 0.5d) {
			SchematicPrinter.INSTANCE.syncSneaking(player, false);
			endTask();
		}
	}

}

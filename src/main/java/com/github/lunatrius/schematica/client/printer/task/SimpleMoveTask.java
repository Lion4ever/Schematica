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

	public SimpleMoveTask(Vec3d pos, boolean sneak) {
		super();
		this.pos = pos;
		this.sneak = sneak;
		
		player = Minecraft.getMinecraft().player;
	}

	public SimpleMoveTask(BlockPos pos, boolean sneak) {
		this(new Vec3d(pos.getX() + 0.5d, pos.getY(), pos.getZ() + 0.5d), sneak);
	}

	@Override
	public void onUpdate() {
		Vec3d motion = pos.subtract(player.getPositionVector()).normalize().scale(0.1d);
		player.motionX = motion.x;
		//player.motionY = motion.y;
		player.motionZ = motion.z;
		if (player.getPositionVector().subtract(pos).lengthSquared() < 0.25d) {
			SchematicPrinter.INSTANCE.syncSneaking(player, false);
			endTask();
		}
	}
	
	public void start() {
		SchematicPrinter.INSTANCE.syncSneaking(player, sneak);
	}

}

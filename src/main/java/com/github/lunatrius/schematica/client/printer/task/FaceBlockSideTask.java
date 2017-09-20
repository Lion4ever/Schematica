package com.github.lunatrius.schematica.client.printer.task;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FaceBlockSideTask extends LoopedPrinterTask {

	private Vec3d clickPos;
	private static EntityPlayerSP player;
	
	public FaceBlockSideTask(BlockPos pos, EnumFacing side) {
		super();
		clickPos = getClickPosition(pos, side);
		player = Minecraft.getMinecraft().player;
	}
	
	public FaceBlockSideTask(Vec3d directPosition) {
		super();
		clickPos = directPosition;
	}
	
	@Override
	public void onUpdate() {
		if (lookAtPosition(player, clickPos)) {
			endTask();
		}

	}
	
	public static boolean lookAtPosition(final EntityPlayerSP player, Vec3d pos) {
		// Analog to faceEntity in EntityLiving
		double xDiff = pos.x - player.posX;
		double yDiff = pos.y - player.getPositionEyes(1F).y;
		double zDiff = pos.z - player.posZ;
		double distance = (double) MathHelper.sqrt(xDiff * xDiff + zDiff * zDiff);

		float targetYaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(zDiff, xDiff) * (180D / Math.PI)) - 90.0F);
		float targetPitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(yDiff, distance) * (180D / Math.PI))));
		player.rotationYaw = updateRotation(player.rotationYaw, targetYaw, 30F);
		player.rotationPitch = updateRotation(player.rotationPitch, targetPitch, 30F);

		float yawDiff = player.rotationYaw - player.prevRotationYaw;
		if (yawDiff > 180F) {
			player.prevRotationYaw += 360;
		} else if (yawDiff < -180F) {
			player.prevRotationYaw -= 360;
		}

		return Math.abs(player.rotationYaw - targetYaw) + Math.abs(player.rotationPitch - targetPitch) < 1;
	}

	// Stolen from EntityLiving :(
	public static float updateRotation(float angle, float targetAngle, float maxIncrease) {
		float f = MathHelper.wrapDegrees(targetAngle - angle);

		if (f > maxIncrease) {
			f = maxIncrease;
		}

		if (f < -maxIncrease) {
			f = -maxIncrease;
		}

		return MathHelper.wrapDegrees(angle + f);
	}
	
	public static Vec3d getClickPosition(final BlockPos pos, EnumFacing side) {
		// Calculate the middle of the side of the block something is placed
		// against
		double blockSideX = pos.getX() + 0.5d + side.getFrontOffsetX() / 2D;
		double blockSideY = pos.getY() + 0.5d + side.getFrontOffsetY() / 2D;
		double blockSideZ = pos.getZ() + 0.5d + side.getFrontOffsetZ() / 2D;

		return new Vec3d(blockSideX, blockSideY, blockSideZ);
	}

}

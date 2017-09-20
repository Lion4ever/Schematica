package com.github.lunatrius.schematica.client.printer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class HiddenPathEntity extends EntityLiving {
	
	private static EntityPlayerSP player = Minecraft.getMinecraft().player;

	public HiddenPathEntity(World welt) {
		super(welt);
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public void setLocationAndAngles(double arg0, double arg1, double arg2, float arg3, float arg4) {
		player.setLocationAndAngles(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public void setPosition(double arg0, double arg1, double arg2) {
		player.setPosition(arg0, arg1, arg2);
	}

	@Override
	public void setPositionAndRotation(double arg0, double arg1, double arg2, float arg3, float arg4) {
		player.setPositionAndRotation(arg0, arg1, arg2, arg3, arg4);
	}

	@Override
	public void setPositionAndUpdate(double arg0, double arg1, double arg2) {
		player.setPositionAndUpdate(arg0, arg1, arg2);
	}

	@Override
	public double getDistance(double arg0, double arg1, double arg2) {
		return player.getDistance(arg0, arg1, arg2);
	}

	@Override
	public double getDistanceSq(BlockPos p_getDistanceSq_1_) {
		return player.getDistanceSq(p_getDistanceSq_1_);
	}

	@Override
	public double getDistanceSq(double arg0, double arg1, double arg2) {
		return player.getDistanceSq(arg0, arg1, arg2);
	}

	@Override
	public double getDistanceSqToCenter(BlockPos p_getDistanceSqToCenter_1_) {
		return player.getDistanceSqToCenter(p_getDistanceSqToCenter_1_);
	}

	@Override
	public double getDistanceSqToEntity(Entity p_getDistanceSqToEntity_1_) {
		return player.getDistanceSqToEntity(p_getDistanceSqToEntity_1_);
	}

	@Override
	public float getDistanceToEntity(Entity p_getDistanceToEntity_1_) {
		return player.getDistanceToEntity(p_getDistanceToEntity_1_);
	}

	@Override
	public Vec3d getLookVec() {
		return player.getLookVec();
	}

	@Override
	public Vec2f getPitchYaw() {
		return player.getPitchYaw();
	}

	@Override
	public BlockPos getPosition() {
		return player.getPosition();
	}

	@Override
	public Vec3d getPositionEyes(float p_getPositionEyes_1_) {
		return player.getPositionEyes(p_getPositionEyes_1_);
	}

	@Override
	public Vec3d getPositionVector() {
		return player.getPositionVector();
	}

	@Override
	public float getRotatedYaw(Rotation p_getRotatedYaw_1_) {
		return player.getRotatedYaw(p_getRotatedYaw_1_);
	}

	@Override
	public double getYOffset() {
		return player.getYOffset();
	}

	@Override
	public void turn(float p_turn_1_, float p_turn_2_) {
		player.turn(p_turn_1_, p_turn_2_);
	}
	

}
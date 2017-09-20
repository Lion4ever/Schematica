package com.github.lunatrius.schematica.client.printer.task;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class LeanOverBlockTask extends SimpleMoveTask {

	public LeanOverBlockTask(BlockPos pos, EnumFacing side) {
		//TODO Improve me: Sneaking isnt working and player can fall down and die
		super(new Vec3d(pos.getX() + 0.5d, pos.getY() + 1d, pos.getZ() + 0.5d).add(new Vec3d(side.getDirectionVec()).scale(0.8)), true);
	}

}

package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;

import io.netty.handler.codec.redis.LastBulkStringRedisContent;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

public class BridgeOverTask extends PrinterTask {

	BlockPos pos;

	private static ItemStack dirtStack = new ItemStack(Block.getBlockFromName("minecraft:dirt"));
	private static Minecraft minecraft = Minecraft.getMinecraft();

	public BridgeOverTask(BlockPos pos) {
		this.pos = pos;
	}

	@Override
	public EnumActionResult execute() {
		PrinterTask latest = PrinterTask.latestTask;
		BlockPos pPos = new BlockPos(minecraft.player.getPositionVector());
		BlockPos below = pPos.offset(EnumFacing.DOWN);
		if (minecraft.player.posY < pos.getY()) {
			clearPos(pPos.offset(EnumFacing.UP, 2));
			clearPos(pPos);
			Vec3d hitVec = new Vec3d(pPos.getX() + 0.5d, pPos.getY() + 0.5d, pPos.getZ() + 0.5d);
			new StackUpTask(below).queue();
			new BlockPlaceTask(below, EnumFacing.UP, hitVec, EnumHand.MAIN_HAND, dirtStack).queue();

		} else {
			Vec3i rest = pos.subtract(pPos);
			EnumFacing tDir = EnumFacing.getFacingFromVector(rest.getX(), 0, rest.getZ());
			BlockPos tPos = pPos.offset(tDir);
			if (pos.getX() != tPos.getX() || pos.getZ() != tPos.getZ()) {
				clearPos(tPos.offset(EnumFacing.UP));
				clearPos(tPos);
				if (!SchematicPrinter.isSolid(minecraft.world, pPos.offset(tDir), EnumFacing.DOWN)) {
					clearPos(below.offset(tDir));
					new LeanOverBlockTask(below, tDir).queue();
					Vec3d hitVec = new Vec3d(below.getX() + 0.5d, below.getY() + 0.5d, below.getZ() + 0.5d);
					new BlockPlaceTask(below, tDir, hitVec, EnumHand.MAIN_HAND, dirtStack).queueWithFacing();
				}
				new SimpleMoveTask(pPos.offset(tDir), false).queue();

				if (!SchematicPrinter.INSTANCE.isBlockCorrect(below)) {
					new BlockBreakTask(below, EnumFacing.UP).queue();
				}

			} else {
				SchematicPrinter.INSTANCE.setTimeout(below, ConfigurationHandler.timeout);
				SchematicPrinter.INSTANCE.currentTask = null;
				clearPos(tPos.offset(EnumFacing.UP));
				clearPos(tPos);
				if (nextTask != null) {
					nextTask.queue();
				}
				return EnumActionResult.SUCCESS;
			}
		}
		
		SchematicPrinter.INSTANCE.currentTask = latest.nextTask;
		PrinterTask.latestTask.nextTask = this;
		PrinterTask.latestTask = latest;
		latest.nextTask = null;

		return null;
	}

	public static void clearPos(BlockPos pos) {
		if (!minecraft.world.isAirBlock(pos)) {
			new BlockBreakTask(pos, EnumFacing.DOWN).queueWithFacing();
		}
	}

}

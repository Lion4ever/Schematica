package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;

import it.unimi.dsi.fastutil.shorts.Short2CharArrayMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.client.FMLClientHandler;

public class BlockPlaceTask extends PrinterTask {

	static Minecraft minecraft = Minecraft.getMinecraft();
	BlockPos pos;
	EnumFacing side;
	Vec3d hitVec;
	EnumHand hand;
	private ItemStack itemStack;

	public BlockPlaceTask(BlockPos pos, EnumFacing side, Vec3d hitVec, EnumHand hand, ItemStack itemStack) {
		super();
		this.pos = new BlockPos(pos);
		this.side = side;
		this.hitVec = hitVec.scale(1);
		this.hand = hand;
		this.itemStack = itemStack;
	}

	public EnumActionResult execute() {

		if (!SchematicPrinter.INSTANCE.swapToItem(minecraft.player.inventory, itemStack)) {
			return EnumActionResult.FAIL;
		}
		// FIXME: when an adjacent block is not required the blocks should be
		// placed 1 block away from the actual position (because air is
		// replaceable)
		final BlockPos actualPos = ConfigurationHandler.placeAdjacent ? pos : pos.offset(side);
		SchematicPrinter.INSTANCE.setTimeout(pos.offset(side), ConfigurationHandler.timeout);
		endTask();
		return minecraft.playerController.processRightClickBlock(minecraft.player, minecraft.world, actualPos, side,
				hitVec, hand);
	}

	public EnumFacing getSide() {
		return side;
	}

	public BlockPos getPos() {
		return pos;
	}

}

package com.github.lunatrius.schematica.client.printer;

import com.github.lunatrius.schematica.client.printer.task.LoopedPrinterTask;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;

public class PathFindTask extends LoopedPrinterTask {
	
	private BlockPos pos;
	
	private EntityPlayerSP player;
	private HiddenPathEntity guide;

	public PathFindTask(BlockPos pos) {
		super();
		this.pos = pos;
		Minecraft m = Minecraft.getMinecraft();
		this.player = m.player;
		this.guide = new HiddenPathEntity(m.world);
		guide.onLivingUpdate();
	}

	@Override
	public void onUpdate() {
		// TODO Auto-generated method stub

	}

}

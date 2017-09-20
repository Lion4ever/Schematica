package com.github.lunatrius.schematica.client.printer.task;

import net.minecraft.util.EnumActionResult;

public abstract class LoopedPrinterTask extends PrinterTask {

	public EnumActionResult execute() {
		return null;
	}

	public LoopedPrinterTask() {
		needsUpdates = true;
	}

	public abstract void onUpdate();

}

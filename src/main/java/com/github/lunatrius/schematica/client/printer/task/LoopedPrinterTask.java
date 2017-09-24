package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;

import net.minecraft.util.EnumActionResult;

public abstract class LoopedPrinterTask extends PrinterTask {

	public EnumActionResult execute() {
		return null;
	}

	public abstract void onUpdate();
	
	public void start() {}

	@Override
	public void queue() {
		if (SchematicPrinter.INSTANCE.currentTask == null) {
			start();
		}
		super.queue();
	}
	
	

}

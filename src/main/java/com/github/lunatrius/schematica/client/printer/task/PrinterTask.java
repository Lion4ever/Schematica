package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;

import net.minecraft.item.EnumAction;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;

public abstract class PrinterTask {
	
	public PrinterTask nextTask;
	public static PrinterTask latestTask;
	
	public void queue() {
		if (SchematicPrinter.INSTANCE.currentTask == null) {
			SchematicPrinter.INSTANCE.currentTask = this;
		} else {
			latestTask.nextTask = this;
		}
		latestTask = this;
	}
	
	public void endTask() {
		SchematicPrinter.INSTANCE.currentTask = nextTask;
		if (nextTask != null) {
			//Called in print instead
			//nextTask.execute();
			if (nextTask instanceof LoopedPrinterTask) {
				((LoopedPrinterTask) nextTask).start();
			}
		}
	}
	
	public abstract EnumActionResult execute();
}

package com.github.lunatrius.schematica.client.printer.task;

import com.github.lunatrius.schematica.client.printer.SchematicPrinter;

import net.minecraft.item.EnumAction;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;

public abstract class PrinterTask {
	
	public PrinterTask nextTask;
	public boolean needsUpdates = false;
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
		//Called in print instead
		/*if (nextTask != null) {
			nextTask.execute();
		}*/
	}
	
	public abstract EnumActionResult execute();
}

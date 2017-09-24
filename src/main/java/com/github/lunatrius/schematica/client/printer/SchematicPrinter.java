package com.github.lunatrius.schematica.client.printer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.github.lunatrius.core.util.math.BlockPosHelper;
import com.github.lunatrius.core.util.math.MBlockPos;
import com.github.lunatrius.schematica.block.state.BlockStateHelper;
import com.github.lunatrius.schematica.client.printer.nbtsync.NBTSync;
import com.github.lunatrius.schematica.client.printer.nbtsync.SyncRegistry;
import com.github.lunatrius.schematica.client.printer.registry.PlacementData;
import com.github.lunatrius.schematica.client.printer.registry.PlacementRegistry;
import com.github.lunatrius.schematica.client.printer.task.BlockBreakTask;
import com.github.lunatrius.schematica.client.printer.task.BlockPlaceTask;
import com.github.lunatrius.schematica.client.printer.task.BridgeOverTask;
import com.github.lunatrius.schematica.client.printer.task.FaceBlockSideTask;
import com.github.lunatrius.schematica.client.printer.task.LeanOverBlockTask;
import com.github.lunatrius.schematica.client.printer.task.LoopedPrinterTask;
import com.github.lunatrius.schematica.client.printer.task.PrinterTask;
import com.github.lunatrius.schematica.client.printer.task.StackUpTask;
import com.github.lunatrius.schematica.client.renderer.RenderSchematic;
import com.github.lunatrius.schematica.client.util.BlockStateToItemStack;
import com.github.lunatrius.schematica.client.world.SchematicWorld;
import com.github.lunatrius.schematica.handler.ConfigurationHandler;
import com.github.lunatrius.schematica.proxy.ClientProxy;
import com.github.lunatrius.schematica.reference.Constants;
import com.github.lunatrius.schematica.reference.Reference;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fluids.BlockFluidBase;

public class SchematicPrinter {
	public static final SchematicPrinter INSTANCE = new SchematicPrinter();

	private final Minecraft minecraft = Minecraft.getMinecraft();

	private boolean isEnabled = true;
	private boolean isPrinting = false;

	private SchematicWorld schematic = null;
	private byte[][][] timeout = null;
	private HashMap<BlockPos, Integer> syncBlacklist = new HashMap<BlockPos, Integer>();

	public PrinterTask currentTask;
	private PrinterTask lastTask;

	public boolean isEnabled() {
		return this.isEnabled;
	}

	public void setEnabled(final boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	public boolean togglePrinting() {
		this.isPrinting = !this.isPrinting && this.schematic != null;

		if (!this.isPrinting) {
			currentTask = null;
			for (int i = 0; i < timeout.length; i++) {
				for (int j = 0; j < timeout[i].length; j++) {
					for (int k = 0; k < timeout[i][j].length; k++) {
						if (timeout[i][j][k] == -TimeoutState.HAS_TASK.ordinal()) {
							timeout[i][j][j] = 0;
						}
					}
				}
			}
		}
		return this.isPrinting;
	}

	public boolean isPrinting() {
		return this.isPrinting;
	}

	public void setPrinting(final boolean isPrinting) {
		this.isPrinting = isPrinting;
	}

	public SchematicWorld getSchematic() {
		return this.schematic;
	}

	public void setSchematic(final SchematicWorld schematic) {
		this.isPrinting = false;
		this.schematic = schematic;
		refresh();
	}

	public void refresh() {
		if (this.schematic != null) {
			this.timeout = new byte[this.schematic.getWidth()][this.schematic.getHeight()][this.schematic.getLength()];
		} else {
			this.timeout = null;
		}
		currentTask = null;
		lastTask = null;
		this.syncBlacklist.clear();
	}

	public boolean faceTask() {
		if (currentTask != null) {
			if (currentTask instanceof LoopedPrinterTask) {

				((LoopedPrinterTask) currentTask).onUpdate();
				return true;
			}
		}
		return false;
	}

	public boolean print(final WorldClient world, final EntityPlayerSP player) {
		final double dX = ClientProxy.playerPosition.x - this.schematic.position.x;
		final double dY = ClientProxy.playerPosition.y - this.schematic.position.y;
		final double dZ = ClientProxy.playerPosition.z - this.schematic.position.z;
		final int x = (int) Math.floor(dX);
		final int y = (int) Math.floor(dY);
		final int z = (int) Math.floor(dZ);
		final int range = ConfigurationHandler.placeDistance;

		final int minX = 0; // Need the full schematic for walking
		final int maxX = this.schematic.getWidth() - 1;
		int minY = 0;
		int maxY = this.schematic.getHeight() - 1;
		final int minZ = 0;
		final int maxZ = this.schematic.getLength() - 1;

		if (currentTask != null) {
			currentTask.execute();
		}

		if (minX > maxX || minY > maxY || minZ > maxZ) {
			return false;
		}

		final int slot = player.inventory.currentItem;
		final boolean isSneaking = player.isSneaking();

		final boolean isRenderingLayer = this.schematic.isRenderingLayer;
		final int renderingLayer = this.schematic.renderingLayer;

		if (isRenderingLayer) {
			if (renderingLayer > maxY || renderingLayer < minY) {
				return false;
			}

			minY = maxY = renderingLayer;
		}

		syncSneaking(player, true);

		final double blockReachDistance = this.minecraft.playerController.getBlockReachDistance() - 0.1;
		final double blockReachDistanceSq = blockReachDistance * blockReachDistance;

		BlockPos closestMismatch = null;
		double minDistance = Double.MAX_VALUE;
		boolean hasSideBlock = false;

		for (final MBlockPos pos : BlockPosHelper.getAllInBoxXZY(minX, minY, minZ, maxX, maxY, maxZ)) {

			final int wx = this.schematic.position.x + pos.getX();
			final int wy = this.schematic.position.y + pos.getY();
			final int wz = this.schematic.position.z + pos.getZ();
			final BlockPos realPos = new BlockPos(wx, wy, wz);

			final IBlockState blockState = this.schematic.getBlockState(pos);
			final IBlockState realBlockState = world.getBlockState(realPos);
			final Block realBlock = realBlockState.getBlock();

			double currentSqDis = pos.distanceSqToCenter(dX, dY, dZ);

			if (!BlockStateHelper.areBlockStatesEqual(blockState, realBlockState)) {
				List<EnumFacing> sides = getSolidSides(world, realPos);
				sides.remove(EnumFacing.UP);
				sides.remove(EnumFacing.DOWN);
				boolean hasSideBlockNow = sides.size() > 0;
				if (((currentSqDis < minDistance) || (hasSideBlockNow && !hasSideBlock))
						&& (ConfigurationHandler.destroyBlocks || world.isAirBlock(realPos))
						&& this.timeout[pos.getX()][pos.getY()][pos.getZ()] != -TimeoutState.ALLREADY_PLACED
								.ordinal()) {
					closestMismatch = realPos;
					minDistance = currentSqDis;
					hasSideBlock = hasSideBlockNow;
				}
			}

			if (currentSqDis > blockReachDistanceSq) {
				continue;
			}

			try {
				if (placeBlock(world, player, pos)) {
					return syncSlotAndSneaking(player, slot, isSneaking, true);
				}
			} catch (final Exception e) {
				Reference.logger.error("Could not place block!", e);
				return syncSlotAndSneaking(player, slot, isSneaking, false);
			}
		}
		if (currentTask == null && lastTask == null) {
			if (closestMismatch == null) {
				final SchematicWorld schematic = ClientProxy.schematic;
				if (schematic != null && schematic.isRenderingLayer) {
					schematic.renderingLayer = MathHelper.clamp(schematic.renderingLayer + 1, 0,
							schematic.getHeight() - 1);
					RenderSchematic.INSTANCE.refresh();
					SchematicPrinter.INSTANCE.refresh();

				}
			} else {
				List<EnumFacing> possibleSides;
				IBlockState closestMState = schematic.getBlockState(closestMismatch.subtract(schematic.position));
				ItemStack stack = closestMState.getBlock().getItem(world, closestMismatch, closestMState);
				final PlacementData pd = PlacementRegistry.INSTANCE.getPlacementData(closestMState, stack);
				if (pd != null) {
					// float oldPitch = player.rotationPitch;
					float oldYaw = player.rotationYaw;
					possibleSides = new LinkedList<>();
					for (int i = -180; i < 180; i += 90) {
						player.rotationYaw = (float) i;
						if (pd.isValidPlayerFacing(closestMState, player, closestMismatch, world)) {
							possibleSides.add(player.getHorizontalFacing().getOpposite());
						}
					}
					player.rotationYaw = oldYaw;
				} else {
					possibleSides = new LinkedList<>();
					possibleSides.add(EnumFacing.NORTH);
					possibleSides.add(EnumFacing.EAST);
					possibleSides.add(EnumFacing.SOUTH);
					possibleSides.add(EnumFacing.WEST);
				}
				EnumFacing choosenSide;
				boolean pathFailed = false;
				List<EnumFacing> solidSides = getSolidSides(world, closestMismatch);
				solidSides.retainAll(possibleSides);
				if (solidSides.size() > 0 && solidSides.get(solidSides.size() - 1).ordinal() >= 2) {
					choosenSide = getClosestSide(closestMismatch, solidSides);

					// new
					// PathFindTask(closestMismatch.offset(choosenSide).offset(EnumFacing.UP)).queue();
				}

				if (solidSides.size() == 0 || solidSides.get(solidSides.size() - 1).ordinal() < 2 || pathFailed
						|| true) {
					choosenSide = getClosestSide(closestMismatch, possibleSides);
					new BridgeOverTask(closestMismatch, choosenSide).queue();
				}
				if (!isSolid(world, closestMismatch, EnumFacing.DOWN)) {
					new LeanOverBlockTask(closestMismatch.offset(choosenSide), choosenSide.getOpposite()).queue();
				}
				if (possibleSides.size() == 1) {
					new FaceBlockSideTask(closestMismatch, EnumFacing.DOWN).queue();
				}
			}
		}
		return syncSlotAndSneaking(player, slot, isSneaking, true);
	}

	private EnumFacing getClosestSide(BlockPos pos, List<EnumFacing> sides) {
		EnumFacing result = EnumFacing.NORTH;
		double minDistance = Double.MAX_VALUE;
		for (EnumFacing side : sides) {
			Vec3d blockMiddle = new Vec3d(pos.offset(side)).addVector(0.5, 1, 0.5);
			double currentDistance = minecraft.player.getPositionVector().subtract(blockMiddle).lengthSquared();
			if (currentDistance < minDistance) {
				minDistance = currentDistance;
				result = side;
			}
		}
		return result;
	}

	private boolean syncSlotAndSneaking(final EntityPlayerSP player, final int slot, final boolean isSneaking,
			final boolean success) {
		player.inventory.currentItem = slot;
		syncSneaking(player, isSneaking);
		return success;
	}

	private boolean placeBlock(final WorldClient world, final EntityPlayerSP player, final BlockPos pos) {
		final int x = pos.getX();
		final int y = pos.getY();
		final int z = pos.getZ();
		if (this.timeout[x][y][z] > 0) {
			this.timeout[x][y][z]--;
			return false;
		}
		if (this.timeout[x][y][z] == -TimeoutState.HAS_TASK.ordinal()) {
			return true;
		}

		if (this.timeout[x][y][z] == -TimeoutState.ALLREADY_PLACED.ordinal()) {
			return false;
		}

		final int wx = this.schematic.position.x + x;
		final int wy = this.schematic.position.y + y;
		final int wz = this.schematic.position.z + z;
		final BlockPos realPos = new BlockPos(wx, wy, wz);

		final IBlockState blockState = this.schematic.getBlockState(pos);
		final IBlockState realBlockState = world.getBlockState(realPos);
		final Block realBlock = realBlockState.getBlock();

		if (BlockStateHelper.areBlockStatesEqual(blockState, realBlockState)) {
			// TODO: clean up this mess
			final NBTSync handler = SyncRegistry.INSTANCE.getHandler(realBlock);
			if (handler != null) {
				this.timeout[x][y][z] = (byte) ConfigurationHandler.timeout;

				Integer tries = this.syncBlacklist.get(realPos);
				if (tries == null) {
					tries = 0;
				} else if (tries >= 10) {
					return false;
				}

				Reference.logger.trace("Trying to sync block at {} {}", realPos, tries);
				final boolean success = handler.execute(player, this.schematic, pos, world, realPos);
				if (success) {
					this.syncBlacklist.put(realPos, tries + 1);
				}

				return success;
			}
			return false;
		}

		if (ConfigurationHandler.destroyBlocks && !world.isAirBlock(realPos)) {
			if (this.minecraft.playerController.isInCreativeMode()) {
				this.minecraft.playerController.clickBlock(realPos, EnumFacing.DOWN);

				this.timeout[x][y][z] = (byte) ConfigurationHandler.timeout;

				return !ConfigurationHandler.destroyInstantly;
			} else {
				EnumFacing choice = null;
				for (EnumFacing baseBlock : EnumFacing.VALUES) {
					Vec3d clickPos = FaceBlockSideTask.getClickPosition(realPos, baseBlock);
					RayTraceResult raytraceresult = world.rayTraceBlocks(player.getPositionEyes(1F), clickPos);
					// boolean canSeeBlock = raytraceresult != null &&
					// raytraceresult.getBlockPos().equals(realBlock);
					if (raytraceresult == null) {
						choice = baseBlock;
						break;
					}
				}
				if (choice == null) {
					return false;
				}

				new BlockBreakTask(realPos, choice).queueWithFacing();
				this.timeout[x][y][z] = (byte) -TimeoutState.HAS_TASK.ordinal();
			}
		}

		if (this.schematic.isAirBlock(pos)) {
			return false;
		}

		if (!realBlock.isReplaceable(world, realPos)) {
			return false;
		}

		final ItemStack itemStack = BlockStateToItemStack.getItemStack(blockState, new RayTraceResult(player),
				this.schematic, pos, player);
		if (itemStack.isEmpty()) {
			Reference.logger.debug("{} is missing a mapping!", blockState);
			return false;
		}
		if (placeBlock(world, player, realPos, blockState, itemStack)) {

			this.timeout[x][y][z] = (byte) -TimeoutState.HAS_TASK.ordinal();
			if (!ConfigurationHandler.placeInstantly) {
				return true;
			}
		}

		return false;
	}

	public void setTimeout(BlockPos pos, int timeout) {
		BlockPos p = pos.subtract(this.schematic.position);
		if (p.getX() >= 0 && p.getX() < this.schematic.getWidth() && p.getY() >= 0
				&& p.getY() < this.schematic.getHeight() && p.getZ() >= 0 && p.getZ() < this.schematic.getLength()) {
			this.timeout[p.getX()][p.getY()][p.getZ()] = (byte) timeout;
		}
	}

	public boolean isBlockCorrect(BlockPos pos) {
		final IBlockState blockState = this.schematic.getBlockState(pos.subtract(this.schematic.position));
		final IBlockState realBlockState = minecraft.world.getBlockState(pos);
		return BlockStateHelper.areBlockStatesEqual(blockState, realBlockState);
	}

	public static boolean isSolid(final World world, final BlockPos pos, final EnumFacing side) {
		final BlockPos offset = pos.offset(side);

		final IBlockState blockState = world.getBlockState(offset);
		final Block block = blockState.getBlock();

		if (block == null) {
			return false;
		}

		if (block.isAir(blockState, world, offset)) {
			return false;
		}

		if (block instanceof BlockFluidBase) {
			return false;
		}

		if (block.isReplaceable(world, offset)) {
			return false;
		}

		return true;
	}

	private List<EnumFacing> getSolidSides(final World world, final BlockPos pos) {
		if (!ConfigurationHandler.placeAdjacent) {
			return Arrays.asList(EnumFacing.VALUES);
		}

		final List<EnumFacing> list = new ArrayList<EnumFacing>();

		for (final EnumFacing side : EnumFacing.VALUES) {
			if (isSolid(world, pos, side)) {
				list.add(side);
			}
		}

		return list;
	}

	private boolean placeBlock(final WorldClient world, final EntityPlayerSP player, final BlockPos pos,
			final IBlockState blockState, final ItemStack itemStack) {
		if (itemStack.getItem() instanceof ItemBucket) {
			return false;
		}

		final PlacementData data = PlacementRegistry.INSTANCE.getPlacementData(blockState, itemStack);
		if (data != null && !data.isValidPlayerFacing(blockState, player, pos, world)) {
			return false;
		}

		final List<EnumFacing> solidSides = getSolidSides(world, pos);

		if (solidSides.size() == 0) {
			return false;
		}

		final List<EnumFacing> directions;
		final float offsetX;
		final float offsetY;
		final float offsetZ;
		final int extraClicks;

		if (data != null) {
			final List<EnumFacing> validDirections = data.getValidBlockFacings(solidSides, blockState);
			if (validDirections.size() == 0) {
				return false;
			}

			directions = validDirections;
			offsetX = data.getOffsetX(blockState);
			offsetY = data.getOffsetY(blockState);
			offsetZ = data.getOffsetZ(blockState);
			extraClicks = data.getExtraClicks(blockState);
		} else {
			directions = solidSides;
			offsetX = 0.5f;
			offsetY = 0.5f;
			offsetZ = 0.5f;
			extraClicks = 0;
		}

		EnumFacing choice = null;
		for (EnumFacing baseBlock : directions) {
			Vec3d clickPos = FaceBlockSideTask.getClickPosition(pos, baseBlock);
			RayTraceResult raytraceresult = world.rayTraceBlocks(player.getPositionEyes(1F), clickPos, false, false,
					true);
			boolean canSeeBlock = raytraceresult != null && raytraceresult.getBlockPos().equals(pos);
			if (canSeeBlock) {
				choice = baseBlock;
				break;
			}
		}
		if (choice == null) {
			return false;
		}

		return placeBlock(world, player, pos, choice, offsetX, offsetY, offsetZ, extraClicks, itemStack);
	}

	private boolean placeBlock(final WorldClient world, final EntityPlayerSP player, final BlockPos pos,
			final EnumFacing direction, final float offsetX, final float offsetY, final float offsetZ,
			final int extraClicks, ItemStack blockToPlace) {
		final EnumHand hand = EnumHand.MAIN_HAND;
		final ItemStack itemStack = player.getHeldItem(hand);
		boolean success = false;

		if (!this.minecraft.playerController.isInCreativeMode() && !itemStack.isEmpty()
				&& itemStack.getCount() <= extraClicks) {
			return false;
		}

		final BlockPos offset = pos.offset(direction);
		final EnumFacing side = direction.getOpposite();
		final Vec3d hitVec = new Vec3d(offset.getX() + offsetX, offset.getY() + offsetY, offset.getZ() + offsetZ);

		success = placeBlock(world, player, itemStack, offset, side, hitVec, hand, blockToPlace);
		for (int i = 0; success && i < extraClicks; i++) {
			success = placeBlock(world, player, itemStack, offset, side, hitVec, hand, blockToPlace);
		}

		if (itemStack.getCount() == 0 && success) {
			player.inventory.mainInventory.set(player.inventory.currentItem, ItemStack.EMPTY);
		}

		return success;
	}

	private boolean placeBlock(final WorldClient world, final EntityPlayerSP player, final ItemStack itemStack,
			final BlockPos pos, final EnumFacing side, final Vec3d hitVec, final EnumHand hand,
			ItemStack blockToPlace) {
		// FIXME: where did this event go?
		/*
		 * if (ForgeEventFactory.onPlayerInteract(player,
		 * Action.RIGHT_CLICK_BLOCK, world, pos, side, hitVec).isCanceled()) {
		 * return false; }
		 */

		EnumActionResult result = null;

		if (currentTask instanceof BlockPlaceTask) {
			result = currentTask.execute();
			if ((result == EnumActionResult.SUCCESS)) {
				player.swingArm(hand);
				lastTask = currentTask;
				currentTask = null;
				// Vec3i relaP = (Vec3i) ((BlockPlaceTask)
				// currentTask).getPos().subtract((Vec3i)
				// this.schematic.position);
				// this.timeout[relaP.getX()][relaP.getY()][relaP.getZ()] =
				// (byte) ConfigurationHandler.timeout;
			}
		}
		lastTask = currentTask;
		if (currentTask == null) {
			AxisAlignedBB a = player.getEntityBoundingBox();
			AxisAlignedBB b = new AxisAlignedBB(pos.offset(EnumFacing.UP));

			if (a.intersects(b) && side == EnumFacing.UP) {
				new StackUpTask(pos).queueWithFacing();
				new BlockPlaceTask(pos, side, hitVec, hand, blockToPlace).queue();
			} else {
				new BlockPlaceTask(pos, side, hitVec, hand, blockToPlace).queueWithFacing();
			}
		}

		return lastTask == null;
	}

	public void syncSneaking(final EntityPlayerSP player, final boolean isSneaking) {
		/*player.setSneaking(isSneaking);
		player.connection.sendPacket(new CPacketEntityAction(player,
				isSneaking ? CPacketEntityAction.Action.START_SNEAKING : CPacketEntityAction.Action.STOP_SNEAKING));
	*/}

	public boolean swapToItem(final InventoryPlayer inventory, final ItemStack itemStack) {
		return swapToItem(inventory, itemStack, true);
	}

	private boolean swapToItem(final InventoryPlayer inventory, final ItemStack itemStack, final boolean swapSlots) {
		final int slot = getInventorySlotWithItem(inventory, itemStack);

		if (this.minecraft.playerController.isInCreativeMode()
				&& (slot < Constants.Inventory.InventoryOffset.HOTBAR
						|| slot >= Constants.Inventory.InventoryOffset.HOTBAR + Constants.Inventory.Size.HOTBAR)
				&& ConfigurationHandler.swapSlotsQueue.size() > 0) {
			inventory.currentItem = getNextSlot();
			inventory.setInventorySlotContents(inventory.currentItem, itemStack.copy());
			this.minecraft.playerController.sendSlotPacket(inventory.getStackInSlot(inventory.currentItem),
					Constants.Inventory.SlotOffset.HOTBAR + inventory.currentItem);
			return true;
		}

		if (slot >= Constants.Inventory.InventoryOffset.HOTBAR
				&& slot < Constants.Inventory.InventoryOffset.HOTBAR + Constants.Inventory.Size.HOTBAR) {
			inventory.currentItem = slot;
			return true;
		} else if (swapSlots && slot >= Constants.Inventory.InventoryOffset.INVENTORY
				&& slot < Constants.Inventory.InventoryOffset.INVENTORY + Constants.Inventory.Size.INVENTORY) {
			if (swapSlots(inventory, slot)) {
				return swapToItem(inventory, itemStack, false);
			}
		}

		return false;
	}

	private int getInventorySlotWithItem(final InventoryPlayer inventory, final ItemStack itemStack) {
		for (int i = 0; i < inventory.mainInventory.size(); i++) {
			if (inventory.mainInventory.get(i).isItemEqual(itemStack)) {
				return i;
			}
		}
		return -1;
	}

	private boolean swapSlots(final InventoryPlayer inventory, final int from) {
		if (ConfigurationHandler.swapSlotsQueue.size() > 0) {
			final int slot = getNextSlot();

			swapSlots(from, slot);
			return true;
		}

		return false;
	}

	private int getNextSlot() {
		final int slot = ConfigurationHandler.swapSlotsQueue.poll() % Constants.Inventory.Size.HOTBAR;
		ConfigurationHandler.swapSlotsQueue.offer(slot);
		return slot;
	}

	private boolean swapSlots(final int from, final int to) {
		return this.minecraft.playerController.windowClick(this.minecraft.player.inventoryContainer.windowId, from, to,
				ClickType.SWAP, this.minecraft.player) == ItemStack.EMPTY;
	}
}

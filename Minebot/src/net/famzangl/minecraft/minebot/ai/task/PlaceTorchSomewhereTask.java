package net.famzangl.minecraft.minebot.ai.task;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import net.famzangl.minecraft.minebot.Pos;
import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.BlockItemFilter;
import net.famzangl.minecraft.minebot.ai.strategy.TaskOperations;
import net.famzangl.minecraft.minebot.ai.task.error.SelectTaskError;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Place a torch on one of the given positions. Attempts to place it somewhere
 * 
 * @author michael
 * 
 */
@SkipWhenSearchingPrefetch
public class PlaceTorchSomewhereTask extends AITask {
	private LinkedList<PosAndDir> attemptOnPositions;
	private final List<Pos> places;
	private final ForgeDirection[] preferedDirection;
	private Pos lastAttempt;

	private static class PosAndDir {
		public final Pos place;
		public final ForgeDirection dir;
		public int attemptsLeft = 10;

		public PosAndDir(Pos place, ForgeDirection dir) {
			super();
			this.place = place;
			this.dir = dir;
		}

		public Pos getPlaceOn() {
			return place.add(Pos.fromDir(dir));
		}

		@Override
		public String toString() {
			return "PosAndDir [place=" + place + ", dir=" + dir
					+ ", attemptsLeft=" + attemptsLeft + "]";
		}
	}

	/**
	 * Create a new task-
	 * 
	 * @param positions
	 *            The positions on which the torch should be placed.
	 * @param preferedDirectionThe
	 *            direction in which the stick of the torch should be mounted.
	 */
	public PlaceTorchSomewhereTask(List<Pos> positions,
			ForgeDirection... preferedDirection) {
		super();
		this.places = positions;
		this.preferedDirection = preferedDirection;
	}

	@Override
	public boolean isFinished(AIHelper h) {
		final PosAndDir place = getNextPlace(h);
		return place == null || lastAttempt != null
				&& Block.isEqualTo(h.getBlock(lastAttempt), Blocks.torch);
	}

	private PosAndDir getNextPlace(AIHelper h) {
		if (attemptOnPositions == null) {
			attemptOnPositions = new LinkedList<PlaceTorchSomewhereTask.PosAndDir>();
			for (final Pos p : places) {
				for (final ForgeDirection d : preferedDirection) {
					final PosAndDir current = new PosAndDir(p, d);
					final Pos placeOn = current.getPlaceOn();
					if (!h.isAirBlock(placeOn.x, placeOn.y, placeOn.z)) {
						attemptOnPositions.add(current);
					}
				}
			}
			System.out.println("Placing torch somewhere there: "
					+ attemptOnPositions);
		}

		while (!attemptOnPositions.isEmpty()
				&& attemptOnPositions.peekFirst().attemptsLeft <= 0) {
			attemptOnPositions.removeFirst();
		}

		return attemptOnPositions.peekFirst();
	}

	@Override
	public void runTick(AIHelper h, TaskOperations o) {
		final BlockItemFilter f = new BlockItemFilter(Blocks.torch);
		if (!h.selectCurrentItem(f)) {
			o.desync(new SelectTaskError(f));
		}

		final PosAndDir next = getNextPlace(h);
		final Pos placeOn = next.getPlaceOn();
		h.faceSideOf(placeOn.x, placeOn.y, placeOn.z, next.dir.getOpposite());
		if (h.isFacingBlock(placeOn.x, placeOn.y, placeOn.z,
				next.dir.getOpposite())) {
			h.overrideUseItem();
		}
		next.attemptsLeft--;
		lastAttempt = next.place;
	}

	@Override
	public int getGameTickTimeout() {
		return super.getGameTickTimeout() * 3;
	}

	@Override
	public String toString() {
		return "PlaceTorchSomewhereTask [places=" + places
				+ ", preferedDirection=" + Arrays.toString(preferedDirection)
				+ "]";
	}

}
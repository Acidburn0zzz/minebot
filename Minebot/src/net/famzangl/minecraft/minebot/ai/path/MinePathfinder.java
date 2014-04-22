package net.famzangl.minecraft.minebot.ai.path;

import java.util.Random;

import net.famzangl.minecraft.minebot.MinebotSettings;
import net.famzangl.minecraft.minebot.Pos;
import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.task.MineBlockTask;
import net.minecraft.block.Block;

public abstract class MinePathfinder extends MovePathFinder {
	protected static final float MIN_FACTOR = 0.1f;
	protected static final int MAX_FACTOR = 10;
	protected static final float MAX_POINTS = 50;
	private static final int MAX_BLOCK_IDS = 4096;
	protected MinebotSettings settings;
	private Random rand = new Random();
	protected float maxDistancePoints = 0;
	protected float maxDistanceFactor = 1;

	private final FloatBlockCache points;
	private final FloatBlockCache factors;

	protected static interface ISettingsProvider {
		float getFloat(String name);
	}

	private class FloatBlockCache {
		private float[] cached = new float[MAX_BLOCK_IDS];
		private boolean[] isCached = new boolean[MAX_BLOCK_IDS];
		private ISettingsProvider settingsProvider;

		public FloatBlockCache(ISettingsProvider settingsProvider) {
			super();
			this.settingsProvider = settingsProvider;
		}

		public float getForBlock(int x, int y, int z) {
			int id = helper.getBlockId(x, y, z);

			if (!isCached[id]) {
				String name = Block.blockRegistry.getNameForObject(
						Block.blockRegistry.getObjectById(id)).replace(
						"minecraft:", "");
				cached[id] = settingsProvider.getFloat(name);
				isCached[id] = true;
			}
			return cached[id];
		}
	}

	public MinePathfinder(AIHelper helper) {
		super(helper);
		points = new FloatBlockCache(getPointsProvider());
		factors = new FloatBlockCache(getFactorProvider());
		settings = new MinebotSettings();
		maxDistancePoints = 0;
		maxDistanceFactor = MIN_FACTOR;
		for (String s : settings.getKeys()) {
			if (s.matches("mine_points_.*")) {
				float p = settings.getFloat(s, 1, 0, MAX_POINTS);
				maxDistancePoints = Math.max(p, maxDistancePoints);
			} else if (s.matches("mine_factor_.*")) {
				float p = settings.getFloat(s, 1, 0, MAX_FACTOR);
				if (p > 0) {
					maxDistanceFactor = Math.max(p, maxDistanceFactor);
				}
			}
		}
	}

	protected abstract ISettingsProvider getFactorProvider();

	protected abstract ISettingsProvider getPointsProvider();

	@Override
	protected float rateDestination(int distance, int x, int y, int z) {
		float r1 = rateOreBlockDistance(distance, x, y + 1, z);
		float r2 = rateOreBlockDistance(distance, x, y, z);

		float addForDoubleMine = 0;

		if (r1 != Float.POSITIVE_INFINITY && r2 != Float.POSITIVE_INFINITY) {
			addForDoubleMine = settings
					.getFloat("mine_double_add", 1, 0.1f, 10);
		}

		float rating = Math.min(r1, r2);

		if (rating == Float.POSITIVE_INFINITY) {
			return -1;
		} else {
			return makeRandom(rating + addForDoubleMine);
		}
	}

	private float makeRandom(float f) {
		float r = settings.getFloat("mine_randomness", 0, 0, 1)
				* rand.nextFloat();
		return f * (1 - r);
	}

	private float rateOreBlockDistance(int distance, int x, int y, int z) {
		// Block block = helper.getBlock(x, y, z);
		float point = points.getForBlock(x, y, z);

		float factor = factors.getForBlock(x, y, z);
		return factor == 0 ? Float.POSITIVE_INFINITY : distance / factor
				* maxDistanceFactor + maxDistancePoints - point;
	}

	private float weightDouble(float f1, float f2) {
		float maxRating = Math.max(f1, f2);
		float smallerRating = Math.min(f1, f2);

		float doubleRatingFactor = settings.getFloat("mine_double_factor", 1,
				0.1f, 10);
		return maxRating + (doubleRatingFactor - 1) * smallerRating;
	}

	private int layerMalus(int y) {
		if (y < 5) {
			return 1;
		} else if (y > 15) {
			return (y - 15) / 5;
		} else {
			return 0;
		}
	}

	private boolean isOreBlock(int x, int y, int z) {
		return (factors.getForBlock(x, y, z) > 0);
	}

	@Override
	protected void addTasksForTarget(Pos currentPos) {
		for (int i = 2; i < 5; i++) {
			if (!helper.hasSafeSides(currentPos.x, currentPos.y + i,
					currentPos.z)
					|| !helper.isSafeHeadBlock(currentPos.x, currentPos.y + i
							+ 1, currentPos.z)) {
				break;
			}
			if (isOreBlock(currentPos.x, currentPos.y + i, currentPos.z)) {
				helper.addTask(new MineBlockTask(currentPos.x,
						currentPos.y + i, currentPos.z));
			} else {
				break;
			}
		}
	}

	@Override
	protected int materialDistance(int x, int y, int z, boolean onFloor) {
		return isOreBlock(x, y, z) ? 0 : super.materialDistance(x, y, z,
				onFloor);
	}
}
package net.famzangl.minecraft.minebot.build.blockbuild;

import net.famzangl.minecraft.minebot.Pos;
import net.famzangl.minecraft.minebot.ai.ColoredBlockItemFilter;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class ColoredCubeBuildTask extends CubeBuildTask {

	public static final Block[] BLOCKS = new Block[] { Blocks.wool,
			Blocks.stained_glass, Blocks.stained_hardened_clay };

	protected ColoredCubeBuildTask(Pos forPosition, Block blockType,
			String colorArg) {
		super(forPosition, new ColoredBlockItemFilter(blockType, colorArg));
	}

	@Override
	public String toString() {
		return "ColoredCubeBuildTask [blockFilter=" + blockFilter
				+ ", forPosition=" + forPosition + "]";
	}

}

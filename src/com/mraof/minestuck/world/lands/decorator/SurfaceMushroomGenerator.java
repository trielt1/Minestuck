package com.mraof.minestuck.world.lands.decorator;

import java.util.Random;

import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.world.lands.decorator.ILandDecorator;
import com.mraof.minestuck.world.lands.gen.ChunkProviderLands;

public class SurfaceMushroomGenerator implements ILandDecorator
{
	
	@Override
	public BlockPos generate(World world, Random random, int chunkX, int chunkZ, ChunkProviderLands provider)
	{
		BlockPos pos = world.getTopSolidOrLiquidBlock(new BlockPos((chunkX << 4) + 8 + random.nextInt(16), 0, (chunkZ << 4) + 8 +random.nextInt(16)));
		for (int i = 0; i < 64; ++i)
		{
			BlockPos pos1 = pos.add(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
			if (world.isAirBlock(pos1) && Minestuck.glowingMushroom.canSpread(world, pos1, Minestuck.glowingMushroom.getDefaultState()))
				world.setBlockState(pos1, Minestuck.glowingMushroom.getDefaultState(), 2);
		}
		
		return null;
	}
	
	@Override
	public float getPriority()
	{
		return 0.5F;
	}
}
package com.mraof.minestuck.tileentity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldServer;

import com.mraof.minestuck.util.Debug;
import com.mraof.minestuck.util.ITeleporter;
import com.mraof.minestuck.util.Location;
import com.mraof.minestuck.util.Teleport;

public class TileEntityTransportalizer extends TileEntity implements ITeleporter
{
	public static HashMap<String, Location> transportalizers = new HashMap<String, Location>();
	private static Random rand = new Random();
	String id = "";
	private String destId = "";
	
	@Override
	public void validate()
	{
		super.validate();
		if(!worldObj.isRemote)
		{
			if(id.isEmpty())
				id = getUnusedId();
			put(id, new Location(this.pos, worldObj.provider.getDimensionId()));
		}
	}
	
	@Override
	public void invalidate()
	{
		super.invalidate();
		if(!worldObj.isRemote)
		{
			Location location = transportalizers.get(id);
			if(location.equals(new Location(this.pos, this.worldObj.provider.getDimensionId())))
				transportalizers.remove(id);
		}
	}
	
	public String getUnusedId()
	{
		String unusedId = "";
		do
		{
			for(int i = 0; i < 4; i++)
			{
				unusedId += (char) (rand.nextInt(26) + 'A');
			}
		} 
		while(transportalizers.containsKey(unusedId));

		return unusedId;
	}

	public static void put(String key, Location location)
	{
		transportalizers.put(key, location);
	}
	
	public void teleportTo(Entity entity, Location location)
	{
		double x = location.pos.getX() + 0.5;
		double y = location.pos.getY() + 0.6;
		double z = location.pos.getZ() + 0.5;
		if(entity instanceof EntityPlayerMP)
		{
			((EntityPlayerMP) entity).setPositionAndUpdate(x, y, z);
		}
		else
		{
			entity.setPosition(x, y, z);
		}
	}

	public void teleport(Entity entity)
	{
		Location location = transportalizers.get(this.destId);
		if(location != null && location.pos.getY() != -1)
		{
			WorldServer world = MinecraftServer.getServer().worldServerForDimension(location.dim);
			TileEntityTransportalizer destTransportalizer = (TileEntityTransportalizer) world.getTileEntity(location.pos);
			if(destTransportalizer == null)
			{
				Debug.print("Invalid transportalizer in map: " + this.destId + " at " + location);
				transportalizers.remove(this.destId);
				this.destId = "";
				return;
			}
			entity.timeUntilPortal = 60;
			
			IBlockState block0 = world.getBlockState(location.pos.up());
			IBlockState block1 = world.getBlockState(location.pos.up(2));
			if(block0.getBlock().getMaterial().blocksMovement() || block1.getBlock().getMaterial().blocksMovement())
			{
				if(entity instanceof EntityPlayerMP)
					((EntityPlayerMP) entity).addChatMessage(new ChatComponentTranslation("message.destinationBlocked"));
				return;
			}
			
			if(location.dim != entity.dimension)
				Teleport.teleportEntity(entity, location.dim, destTransportalizer);
			else
				teleportTo(entity, transportalizers.get(this.destId));
		}
	}

	public static void saveTransportalizers(NBTTagCompound tagCompound)
	{
		NBTTagCompound transportalizerTagCompound = new NBTTagCompound();
		Iterator<Map.Entry<String, Location>> it = transportalizers.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry<String, Location> entry = it.next();
			Location location = entry.getValue();
			NBTTagCompound locationTag = new NBTTagCompound();
			locationTag.setInteger("x", location.pos.getX());
			locationTag.setInteger("y", location.pos.getY());
			locationTag.setInteger("z", location.pos.getZ());
			locationTag.setInteger("dim", location.dim);
			transportalizerTagCompound.setTag(entry.getKey(), locationTag);
		}
		tagCompound.setTag("transportalizers", transportalizerTagCompound);
	}

	public static void loadTransportalizers(NBTTagCompound tagCompound)
	{
		for(Object id : tagCompound.getKeySet())
		{
			NBTTagCompound locationTag = tagCompound.getCompoundTag((String)id);
			put((String)id, new Location(locationTag.getInteger("x"), locationTag.getInteger("y"), locationTag.getInteger("z"), locationTag.getInteger("dim")));
		}
	}

	public String getId()
	{
		return id;
	}

	public String getDestId()
	{
		return destId;
	}

	public void setDestId(String destId)
	{
		this.destId = destId;
		worldObj.markBlockForUpdate(pos);
	}

	@Override
	public void readFromNBT(NBTTagCompound tagCompound)
	{
		super.readFromNBT(tagCompound);
		this.destId = tagCompound.getString("destId");
		this.id = tagCompound.getString("idString");
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCompound)
	{
		super.writeToNBT(tagCompound);
		
		tagCompound.setString("idString", id);
		tagCompound.setString("destId", destId);
	}

	@Override
	public Packet getDescriptionPacket() 
	{
		NBTTagCompound tagCompound = new NBTTagCompound();
		this.writeToNBT(tagCompound);
		return new S35PacketUpdateTileEntity(this.pos, 2, tagCompound);
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) 
	{
		this.readFromNBT(pkt.getNbtCompound());
	}

	@Override
	public void makeDestination(Entity entity, WorldServer worldserver, WorldServer worldserver1) 
	{
		entity.setLocationAndAngles(this.pos.getX() + 0.5, this.pos.getY() + 0.6, this.pos.getZ() + 0.5, entity.rotationYaw, entity.rotationPitch);
		entity.timeUntilPortal = 100;
	}
	
}


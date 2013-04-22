package mekanism.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.common.FMLCommonHandler;

import mekanism.api.GasTransmission;
import mekanism.api.IGasAcceptor;
import mekanism.api.IGasStorage;
import mekanism.api.IMechanicalPipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.liquids.ILiquidTank;
import net.minecraftforge.liquids.ITankContainer;
import net.minecraftforge.liquids.LiquidStack;
import net.minecraftforge.liquids.LiquidTank;

public class LiquidTransferProtocol
{
	/** List of iterated pipes, to prevent infinite loops. */
	public ArrayList<TileEntity> iteratedPipes = new ArrayList<TileEntity>();
	
	/** List of ITankContainers that can take in the type of liquid requested. */
	public ArrayList<ITankContainer> availableAcceptors = new ArrayList<ITankContainer>();
	
	/** Map of directions liquid is transferred to. */
	public Map<ITankContainer, ForgeDirection> acceptorDirections = new HashMap<ITankContainer, ForgeDirection>();
	
	/** Pointer pipe of this calculation */
	public TileEntity pointer;
	
	/** Original outputter Tile Entity. */
	public TileEntity original;
	
	/** Type of liquid to distribute */
	public LiquidStack liquidToSend;
	
	/**
	 * LiquidTransferProtocol -- a calculation used to distribute liquids through a pipe network.
	 * @param head - pointer tile entity
	 * @param orig - original outputter
	 * @param liquid - the LiquidStack to transfer
	 */
	public LiquidTransferProtocol(TileEntity head, TileEntity orig, LiquidStack liquid)
	{
		pointer = head;
		original = orig;
		liquidToSend = liquid;
	}
	
	/**
	 * Recursive loop that iterates through connected tubes and adds connected acceptors to an ArrayList.  Note that it will NOT add
	 * the original outputting tile into the availableAcceptors list, to prevent loops.
	 * @param tile - pointer tile entity
	 */
	public void loopThrough(TileEntity tile)
	{
		ITankContainer[] acceptors = PipeUtils.getConnectedAcceptors(tile);
		
		for(ITankContainer acceptor : acceptors)
		{
			if(acceptor != null)
			{
				ForgeDirection side = ForgeDirection.getOrientation(Arrays.asList(acceptors).indexOf(acceptor)).getOpposite();
				if(acceptor != original && !(acceptor instanceof IMechanicalPipe))
				{
					ILiquidTank[] tanks = acceptor.getTanks(side);
					boolean hasTank = false;
					
					for(ILiquidTank tank : tanks)
					{
						if(tank != null)
						{
							if(tank.getLiquid() == null)
							{
								hasTank = true;
								break;
							}
							else {
								if(tank.getLiquid().isLiquidEqual(liquidToSend))
								{
									if(tank.getCapacity()-tank.getLiquid().amount != 0)
									{
										hasTank = true;
										break;
									}
								}
							}
						}
					}
					
					if(!hasTank)
					{
						if(acceptor.getTank(side, liquidToSend) != null)
						{
							ILiquidTank tank = acceptor.getTank(side, liquidToSend);
							
							if(tank.getLiquid() == null)
							{
								hasTank = true;
								break;
							}
							else {
								if(tank.getLiquid().isLiquidEqual(liquidToSend))
								{
									if(tank.getCapacity()-tank.getLiquid().amount != 0)
									{
										hasTank = true;
										break;
									}
								}
							}
						}
					}
					
					if(hasTank)
					{
						availableAcceptors.add(acceptor);
						acceptorDirections.put(acceptor, ForgeDirection.getOrientation(Arrays.asList(acceptors).indexOf(acceptor)).getOpposite());
					}
				}
			}
		}
		
		iteratedPipes.add(tile);
		
		TileEntity[] pipes = PipeUtils.getConnectedPipes(tile);
		
		for(TileEntity pipe : pipes)
		{
			if(pipe != null)
			{
				if(!iteratedPipes.contains(pipe))
				{
					loopThrough(pipe);
				}
			}
		}
	}
	
	/**
	 * Runs the protocol and distributes the liquid.
	 * @return liquid transferred
	 */
	public int calculate()
	{
		loopThrough(pointer);
		
		Collections.shuffle(availableAcceptors);
		
		int liquidSent = 0;
		
		if(!availableAcceptors.isEmpty())
		{
			int divider = availableAcceptors.size();
			int remaining = liquidToSend.amount % divider;
			int sending = (liquidToSend.amount-remaining)/divider;
			
			for(ITankContainer acceptor : availableAcceptors)
			{
				int currentSending = sending;
				
				if(remaining > 0)
				{
					currentSending++;
					remaining--;
				}
				
				if(acceptor.getTanks(acceptorDirections.get(acceptor)).length != 0)
				{
					int tankDivider = acceptor.getTanks(acceptorDirections.get(acceptor)).length;
					int tankRemaining = currentSending % tankDivider;
					int tankSending = (currentSending-tankRemaining)/tankDivider;
					
					for(ILiquidTank tank : acceptor.getTanks(acceptorDirections.get(acceptor)))
					{
						int tankCurrentSending = tankSending;
						
						if(tankRemaining > 0)
						{
							tankCurrentSending++;
							tankRemaining--;
						}
						
						liquidSent += acceptor.fill(acceptorDirections.get(acceptor), new LiquidStack(liquidToSend.itemID, tankCurrentSending, liquidToSend.itemMeta), true);
					}
				}
				else {
					if(acceptor.getTank(acceptorDirections.get(acceptor), liquidToSend) != null)
					{
						ILiquidTank tank = acceptor.getTank(acceptorDirections.get(acceptor), liquidToSend);
						
						liquidSent += acceptor.fill(acceptorDirections.get(acceptor), new LiquidStack(liquidToSend.itemID, currentSending, liquidToSend.itemMeta), true);
					}
				}
			}
		}
		
		if(liquidSent > 0)
		{
			for(TileEntity tileEntity : iteratedPipes)
			{
				if(tileEntity instanceof IMechanicalPipe)
				{
					LiquidStack sendStack = liquidToSend.copy();
					sendStack.amount = liquidSent;
					((IMechanicalPipe)tileEntity).onTransfer(sendStack);
				}
			}
		}
		
		return liquidSent;
	}
}

package mekanism.client;

import mekanism.api.EnumColor;
import mekanism.api.util.StackUtils;
import mekanism.client.sound.SoundHandler;
import mekanism.common.Mekanism;
import mekanism.common.block.states.BlockStateMachine;
import mekanism.common.item.ItemBlockMachine;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.item.ItemConfigurator.ConfiguratorMode;
import mekanism.common.item.ItemElectricBow;
import mekanism.common.item.ItemFlamethrower;
import mekanism.common.item.ItemJetpack;
import mekanism.common.item.ItemJetpack.JetpackMode;
import mekanism.common.item.ItemScubaTank;
import mekanism.common.item.ItemWalkieTalkie;
import mekanism.common.network.PacketConfiguratorState.ConfiguratorStateMessage;
import mekanism.common.network.PacketElectricBowState.ElectricBowStateMessage;
import mekanism.common.network.PacketFlamethrowerData;
import mekanism.common.network.PacketJetpackData.JetpackDataMessage;
import mekanism.common.network.PacketJetpackData.JetpackPacket;
import mekanism.common.network.PacketPortableTankState.PortableTankStateMessage;
import mekanism.common.network.PacketScubaTankData.ScubaTankDataMessage;
import mekanism.common.network.PacketScubaTankData.ScubaTankPacket;
import mekanism.common.network.PacketWalkieTalkieState.WalkieTalkieStateMessage;
import mekanism.common.util.LangUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Keyboard;

@SideOnly(Side.CLIENT)
public class MekanismKeyHandler extends MekKeyHandler
{
	public static final String keybindCategory = "Mekanism";
	public static KeyBinding modeSwitchKey = new KeyBinding("Mekanism " + LangUtils.localize("key.mode"), Keyboard.KEY_M, keybindCategory);
	public static KeyBinding armorModeSwitchKey = new KeyBinding("Mekanism " + LangUtils.localize("key.armorMode"), Keyboard.KEY_F, keybindCategory);
	public static KeyBinding voiceKey = new KeyBinding("Mekanism " + LangUtils.localize("key.voice"), Keyboard.KEY_U, keybindCategory);
	public static KeyBinding sneakKey = Minecraft.getMinecraft().gameSettings.keyBindSneak;
	public static KeyBinding jumpKey = Minecraft.getMinecraft().gameSettings.keyBindJump;

	public MekanismKeyHandler()
	{
		super(new KeyBinding[] {modeSwitchKey, armorModeSwitchKey, voiceKey}, new boolean[] {false, false, true});
		
		ClientRegistry.registerKeyBinding(modeSwitchKey);
		ClientRegistry.registerKeyBinding(armorModeSwitchKey);
		ClientRegistry.registerKeyBinding(voiceKey);
		
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@SubscribeEvent
	public void onTick(InputEvent event)
	{
		keyTick();
	}

	@Override
	public void keyDown(KeyBinding kb, boolean isRepeat)
	{
		if(kb == modeSwitchKey)
		{
			EntityPlayer player = FMLClientHandler.instance().getClient().thePlayer;
			ItemStack toolStack = player.getCurrentEquippedItem();

			Item item = StackUtils.getItem(toolStack);
			
			if(player.isSneaking() && item instanceof ItemConfigurator)
			{
				ItemConfigurator configurator = (ItemConfigurator)item;

				int toSet = configurator.getState(toolStack).ordinal() < ConfiguratorMode.values().length-1 ? configurator.getState(toolStack).ordinal() + 1 : 0;
				configurator.setState(toolStack, ConfiguratorMode.values()[toSet]);
				Mekanism.packetHandler.sendToServer(new ConfiguratorStateMessage(configurator.getState(toolStack)));
				player.addChatMessage(new TextComponentString(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + LangUtils.localize("tooltip.configureState") + ": " + configurator.getColor(configurator.getState(toolStack)) + configurator.getStateDisplay(configurator.getState(toolStack))));
			}
			else if(player.isSneaking() && item instanceof ItemElectricBow)
			{
				ItemElectricBow bow = (ItemElectricBow)item;

				bow.setFireState(toolStack, !bow.getFireState(toolStack));
				Mekanism.packetHandler.sendToServer(new ElectricBowStateMessage(bow.getFireState(toolStack)));
				player.addChatMessage(new TextComponentString(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + LangUtils.localize("tooltip.fireMode") + ": " + (bow.getFireState(toolStack) ? EnumColor.DARK_GREEN : EnumColor.DARK_RED) + LangUtils.transOnOff(bow.getFireState(toolStack))));
			}
			else if(player.isSneaking() && item instanceof ItemBlockMachine)
			{
				ItemBlockMachine machine = (ItemBlockMachine)item;

				if(BlockStateMachine.MachineType.get(toolStack) == BlockStateMachine.MachineType.FLUID_TANK)
				{
					machine.setBucketMode(toolStack, !machine.getBucketMode(toolStack));
					Mekanism.packetHandler.sendToServer(new PortableTankStateMessage(machine.getBucketMode(toolStack)));
					player.addChatMessage(new TextComponentString(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + LangUtils.localize("tooltip.portableTank.bucketMode") + ": " + (machine.getBucketMode(toolStack) ? EnumColor.DARK_GREEN : EnumColor.DARK_RED) + LangUtils.transOnOff(machine.getBucketMode(toolStack))));
				}
			}
			else if(player.isSneaking() && item instanceof ItemWalkieTalkie)
			{
				ItemWalkieTalkie wt = (ItemWalkieTalkie)item;

				if(wt.getOn(toolStack))
				{
					int newChan = wt.getChannel(toolStack) < 9 ? wt.getChannel(toolStack) + 1 : 1;
					wt.setChannel(toolStack, newChan);
					Mekanism.packetHandler.sendToServer(new WalkieTalkieStateMessage(newChan));
					SoundHandler.playSound("mekanism:etc.Ding");
				}
			}
            else if(player.isSneaking() && item instanceof ItemFlamethrower)
            {
                ItemFlamethrower flamethrower = (ItemFlamethrower)item;

                flamethrower.incrementMode(toolStack);
                Mekanism.packetHandler.sendToServer(new PacketFlamethrowerData.FlamethrowerDataMessage(PacketFlamethrowerData.FlamethrowerPacket.MODE, null, false));
                player.addChatMessage(new TextComponentString(EnumColor.DARK_BLUE + "[Mekanism] " + EnumColor.GREY + LangUtils.localize("tooltip.flamethrower.modeBump") + ": " + flamethrower.getMode(toolStack).getName()));
            }
		}
		else if(kb == armorModeSwitchKey)
		{
			EntityPlayer player = FMLClientHandler.instance().getClient().thePlayer;
			ItemStack chestStack = player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
			Item chestItem = StackUtils.getItem(chestStack);

			if(chestItem instanceof ItemJetpack)
			{
				ItemJetpack jetpack = (ItemJetpack)chestItem;

				if(player.isSneaking())
				{
					jetpack.setMode(chestStack, JetpackMode.DISABLED);
				}
				else {
					jetpack.incrementMode(chestStack);
				}

				Mekanism.packetHandler.sendToServer(new JetpackDataMessage(JetpackPacket.MODE, null, player.isSneaking()));
				SoundHandler.playSound("mekanism:etc.Hydraulic");
			}
			else if(chestItem instanceof ItemScubaTank)
			{
				ItemScubaTank scubaTank = (ItemScubaTank)chestItem;

				scubaTank.toggleFlowing(chestStack);
				Mekanism.packetHandler.sendToServer(new ScubaTankDataMessage(ScubaTankPacket.MODE, null, false));
				SoundHandler.playSound("mekanism:etc.Hydraulic");
			}
		}
	}

	@Override
	public void keyUp(KeyBinding kb) {}
}

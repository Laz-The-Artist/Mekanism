package mekanism.client.gui;

import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public abstract class GuiConfigurableTile<TILE extends TileEntityMekanism, CONTAINER extends MekanismTileContainer<TILE>> extends GuiMekanismTile<TILE, CONTAINER> {

    private GuiSideConfigurationTab sideConfigTab;
    private GuiTransporterConfigTab transporterConfigTab;

    protected GuiConfigurableTile(CONTAINER container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
    }

    @Override
    public void func_231160_c_() {
        super.func_231160_c_();
        func_230480_a_(sideConfigTab = new GuiSideConfigurationTab(this, tile, () -> sideConfigTab));
        func_230480_a_(transporterConfigTab = new GuiTransporterConfigTab(this, tile, () -> transporterConfigTab));
    }
}
package mekanism.client.gui.qio;

import mekanism.client.gui.element.GuiScreenSwitch;
import mekanism.client.gui.element.tab.GuiQIOFrequencyTab;
import mekanism.client.gui.element.tab.GuiRedstoneControlTab;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.network.PacketGuiInteract;
import mekanism.common.network.PacketGuiInteract.GuiInteraction;
import mekanism.common.tile.qio.TileEntityQIOImporter;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class GuiQIOImporter extends GuiQIOFilterHandler<TileEntityQIOImporter> {

    public GuiQIOImporter(MekanismTileContainer<TileEntityQIOImporter> container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
    }

    @Override
    public void func_231160_c_() {
        super.func_231160_c_();
        func_230480_a_(new GuiQIOFrequencyTab(this, tile));
        func_230480_a_(new GuiSecurityTab<>(this, tile));
        func_230480_a_(new GuiRedstoneControlTab(this, tile));
        func_230480_a_(new GuiUpgradeTab(this, tile));
        func_230480_a_(new GuiScreenSwitch(this, 9, 122, xSize - 18, MekanismLang.QIO_IMPORT_WITHOUT_FILTER.translate(), tile::getImportWithoutFilter,
              () -> Mekanism.packetHandler.sendToServer(new PacketGuiInteract(GuiInteraction.QIO_TOGGLE_IMPORT_WITHOUT_FILTER, tile))));
    }
}
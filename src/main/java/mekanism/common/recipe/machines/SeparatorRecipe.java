package mekanism.common.recipe.machines;

import javax.annotation.Nonnull;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.common.recipe.inputs.FluidInput;
import mekanism.common.recipe.outputs.ChemicalPairOutput;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public class SeparatorRecipe extends MachineRecipe<FluidInput, ChemicalPairOutput, SeparatorRecipe> {

    public double energyUsage;

    public SeparatorRecipe(FluidInput input, double energy, ChemicalPairOutput output) {
        super(input, output);
        energyUsage = energy;
    }

    public SeparatorRecipe(FluidInput input, ChemicalPairOutput output, CompoundNBT extraNBT) {
        super(input, output);
        energyUsage = extraNBT.getDouble("energyUsage");
    }

    public SeparatorRecipe(@Nonnull FluidStack input, double energy, GasStack left, GasStack right) {
        this(new FluidInput(input), energy, new ChemicalPairOutput(left, right));
    }

    @Override
    public SeparatorRecipe copy() {
        return new SeparatorRecipe(getInput().copy(), energyUsage, getOutput().copy());
    }

    public boolean canOperate(FluidTank fluidTank, GasTank leftTank, GasTank rightTank) {
        return getInput().useFluid(fluidTank, FluidAction.SIMULATE, 1) && getOutput().applyOutputs(leftTank, rightTank, false, 1);
    }

    public void operate(FluidTank fluidTank, GasTank leftTank, GasTank rightTank, int scale) {
        if (getInput().useFluid(fluidTank, FluidAction.EXECUTE, scale)) {
            getOutput().applyOutputs(leftTank, rightTank, true, scale);
        }
    }
}
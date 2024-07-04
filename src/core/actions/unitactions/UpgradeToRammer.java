package core.actions.unitactions;

import core.TechnologyTree;
import core.Types;
import core.actions.Action;
import core.actors.Tribe;
import core.actors.units.Unit;
import core.game.GameState;

import static core.Types.UNIT.*;

public class UpgradeToRammer extends UnitAction
{
    public UpgradeToRammer(int unitId)
    {
        super(Types.ACTION.UPGRADE_TO_RAMMER);
        super.unitId = unitId;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        Unit unit = (Unit) gs.getActor(this.unitId);
        Tribe tribe = gs.getTribe(unit.getTribeId());
        TechnologyTree ttree = tribe.getTechTree();
        int stars = gs.getTribe(unit.getTribeId()).getStars();
        return ttree.isResearched(Types.TECHNOLOGY.AQUACULTURE) && stars >= RAMMER.getCost();
    }

    @Override
    public Action copy() {
        return new UpgradeToRammer(this.unitId);
    }

    public String toString() {
        return "UPGRADE TO RAMMER by unit " + this.unitId;
    }
}

package core.actions.unitactions;

import core.TechnologyTree;
import core.Types;
import core.actions.Action;
import core.actors.Tribe;
import core.actors.units.Unit;
import core.game.GameState;

import static core.Types.UNIT.*;

public class UpgradeToBomber extends UnitAction
{
    public UpgradeToBomber(int unitId)
    {
        super(Types.ACTION.UPGRADE_TO_BOMBER);
        super.unitId = unitId;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        Unit unit = (Unit) gs.getActor(this.unitId);
        Tribe tribe = gs.getTribe(unit.getTribeId());
        TechnologyTree ttree = tribe.getTechTree();
        int stars = gs.getTribe(unit.getTribeId()).getStars();
        return ttree.isResearched(Types.TECHNOLOGY.NAVIGATION) && stars >= BOMBER.getCost();
    }

    @Override
    public Action copy() {
        return new UpgradeToBomber(this.unitId);
    }

    public String toString() {
        return "UPGRADE TO BOMBER by unit " + this.unitId;
    }
}

package core.actions.unitactions;

import core.TechnologyTree;
import core.Types;
import core.actions.Action;
import core.actors.Tribe;
import core.game.GameState;
import core.actors.units.Unit;
import utils.Vector2d;

import static core.Types.UNIT.*;

public class UpgradeToScout extends UnitAction
{
    public UpgradeToScout(int unitId)
    {
        super(Types.ACTION.UPGRADE_TO_SCOUT);
        super.unitId = unitId;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        Unit unit = (Unit) gs.getActor(this.unitId);
        Tribe tribe = gs.getTribe(unit.getTribeId());
        TechnologyTree ttree = tribe.getTechTree();
        int stars = gs.getTribe(unit.getTribeId()).getStars();
        return ttree.isResearched(Types.TECHNOLOGY.SAILING) && stars >= SCOUT.getCost();
    }

    @Override
    public Action copy() {
        return new UpgradeToScout(this.unitId);
    }

    public String toString() {
        return "UPGRADE TO SCOUT by unit " + this.unitId;
    }
}

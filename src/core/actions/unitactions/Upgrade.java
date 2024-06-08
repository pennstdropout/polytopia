package core.actions.unitactions;

import core.TechnologyTree;
import core.Types;
import core.actions.Action;
import core.actors.Tribe;
import core.game.GameState;
import core.actors.units.Unit;

import static core.Types.UNIT.*;

public class Upgrade extends UnitAction
{
    public Upgrade(Types.ACTION actionType, int unitId)
    {
        super(actionType);
        super.unitId = unitId;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        Unit unit = (Unit) gs.getActor(this.unitId);
        Tribe tribe = gs.getTribe(unit.getTribeId());
        TechnologyTree ttree = tribe.getTechTree();

        int stars = gs.getTribe(unit.getTribeId()).getStars();
        return ((unit.getType() == RAFT && ttree.isResearched(Types.TECHNOLOGY.AQUACULTURE) && stars >= RAMMER.getCost()) ||
                (unit.getType() == RAFT && ttree.isResearched(Types.TECHNOLOGY.SAILING) && stars >= SCOUT.getCost()) ||
                (unit.getType() == RAFT && ttree.isResearchable(Types.TECHNOLOGY.NAVIGATION) && stars >= BOMBER.getCost()));
    }

    @Override
    public Action copy() {
        return new Upgrade(this.actionType, this.unitId);
    }

    public String toString() {
        return "UPGRADE by unit " + this.unitId;
    }
}

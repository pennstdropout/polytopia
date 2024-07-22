package core.actions.unitactions;

import core.Types;
import core.actions.Action;
import core.actors.Tribe;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

import javax.xml.transform.stax.StAXResult;

public class GatherStar extends UnitAction
{
    public GatherStar(int unitId)
    {
        super(Types.ACTION.GATHER_STAR);
        super.unitId = unitId;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        Unit unit = (Unit) gs.getActor(this.unitId);
        Vector2d unitPos = unit.getPosition();
        Board b = gs.getBoard();
        Tribe t = gs.getTribe(unit.getTribeId());

        return t.getTechTree().isResearched(Types.TECHNOLOGY.NAVIGATION)
                && unit.getStatus() == Types.TURN_STATUS.FRESH
                && b.getResourceAt(unitPos.x, unitPos.y) == Types.RESOURCE.STAR;
    }

    @Override
    public Action copy() {
        return new GatherStar(this.unitId);
    }

    public String toString() {
        return "GATHER STAR by unit " + this.unitId;
    }

    public boolean equals(Object o) {
        if(!(o instanceof GatherStar))
            return false;
        return super.equals(o);
    }
}

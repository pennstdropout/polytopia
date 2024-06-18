package core.actions.unitactions;

import core.Types;
import core.actions.Action;
import core.actors.units.Unit;
import core.actors.City;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

public class Infiltrate extends UnitAction
{
    public int targetId;

    public Infiltrate(int unitId)
    {
        super(Types.ACTION.INFILTRATE);
        super.unitId = unitId;
    }

    @Override
    public boolean isFeasible(final GameState gs) {
        Board board = gs.getBoard();
        Unit unit = (Unit) gs.getActor(this.unitId);

        //This needs to be a cloak that can "attack"
        if(unit.getType() != Types.UNIT.CLOAK || !unit.canAttack())
            return false;

        //Feasible if this unit can attack this turn and if there is at least one unfriendly city adjacent.
        for(Vector2d tile : unit.getPosition().neighborhood(unit.RANGE, 0, board.getSize())){
            City c = board.getCityInBorders(tile.x, tile.y);
            if (tile == c.getPosition() && c.getTribeId() != gs.getActiveTribeID())
                targetId = c.getActorId();
                return true;
        }

        return false;
    }

    @Override
    public Action copy() {
        return new Infiltrate(this.unitId);
    }

    public String toString() {
        return "INVADE CITY by unit " + this.unitId;
    }
}

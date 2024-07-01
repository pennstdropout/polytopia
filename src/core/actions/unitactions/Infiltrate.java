package core.actions.unitactions;

import core.Types;
import core.actions.Action;
import core.actors.units.Unit;
import core.actors.City;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

import java.util.Vector;

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
        Unit unit = (Unit) gs.getActor(this.unitId);
        City target = (City) gs.getActor(this.targetId);

        //This needs to be a cloak that can "attack"
        return target != null && unit.getType() == Types.UNIT.CLOAK && unit.canAttack();
    }

    public void setTargetId(int targetId) {this.targetId = targetId;}
    public int getTargetId() {return targetId;}

    @Override
    public Action copy() {
        Infiltrate infiltrate = new Infiltrate(this.unitId);
        infiltrate.setTargetId(this.targetId);
        return infiltrate;
    }

    public String toString() {
        return "INFILTRATE city " + this.targetId +  " by unit " + this.unitId;
    }
}

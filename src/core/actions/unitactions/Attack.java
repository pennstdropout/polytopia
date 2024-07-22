package core.actions.unitactions;

import core.Types;
import core.actions.Action;
import core.actions.cityactions.CityAction;
import core.actors.units.Cloak;
import core.game.GameState;
import core.actors.units.Unit;

public class Attack extends UnitAction
{
    private int targetId;

    public Attack (int unitId)
    {
        super(Types.ACTION.ATTACK);
        super.unitId = unitId;
    }

    public void setTargetId(int targetId) {this.targetId = targetId;}
    public int getTargetId() {
        return targetId;
    }

    @Override
    public boolean isFeasible(final GameState gs)
    {
        Unit target = (Unit) gs.getActor(this.targetId);
        Unit attacker = (Unit) gs.getActor(this.unitId);

        boolean isVisible = target != null && target.isVisible();

        // Check if target is not null and that it can attack
        if(target == null || !attacker.canAttack() || !isVisible
                || attacker.getType() == Types.UNIT.MIND_BENDER
                || attacker.getType() == Types.UNIT.RAFT
                || attacker.getType() == Types.UNIT.CLOAK
                || attacker.getType() == Types.UNIT.DINGY)
            return false;

        return unitInRange(attacker, target, gs.getBoard());
    }

    @Override
    public Action copy() {
        Attack attack = new Attack(this.unitId);
        attack.setTargetId(this.targetId);
        return attack;
    }

    public String toString() { return "ATTACK by unit " + this.unitId + " to unit " + this.targetId;}

    public boolean equals(Object o) {
        if(!(o instanceof Attack))
            return false;
        Attack other = (Attack) o;

        return super.equals(other) && targetId == other.targetId;
    }
}

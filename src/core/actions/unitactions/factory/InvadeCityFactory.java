package core.actions.unitactions.factory;

import core.actions.Action;
import core.actions.ActionFactory;
import core.actions.unitactions.Infiltrate;
import core.actors.Actor;
import core.actors.units.Unit;
import core.game.GameState;

import java.util.LinkedList;

public class InvadeCityFactory implements ActionFactory {

    @Override
    public LinkedList<Action> computeActionVariants(final Actor actor, final GameState gs) {
        LinkedList<Action> actions = new LinkedList<>();
        Unit unit = (Unit) actor;

        //Only if the unit can 'attack'
        if(unit.canAttack())
        {
            Infiltrate action = new Infiltrate(unit.getActorId());
            if(action.isFeasible(gs)){
                actions.add(action);
            }
        }

        return actions;
    }

}

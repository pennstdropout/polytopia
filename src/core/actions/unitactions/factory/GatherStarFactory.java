package core.actions.unitactions.factory;

import core.actions.Action;
import core.actions.ActionFactory;
import core.actions.unitactions.GatherStar;
import core.actors.Actor;
import core.actors.units.Unit;
import core.game.GameState;

import java.util.LinkedList;

public class GatherStarFactory implements ActionFactory {

    @Override
    public LinkedList<Action> computeActionVariants(final Actor actor, final GameState gs) {
        LinkedList<Action> actions = new LinkedList<>();
        Unit unit = (Unit) actor;

        GatherStar ex = new GatherStar(unit.getActorId());
        if (ex.isFeasible(gs)) {
            actions.add(ex);
        }

        return actions;
    }

}

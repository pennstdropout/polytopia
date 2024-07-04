package core.actions.unitactions.factory;

import core.Types;
import core.actions.Action;
import core.actions.ActionFactory;
import core.actions.unitactions.Infiltrate;
import core.actors.Actor;
import core.actors.City;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

import java.util.LinkedList;

public class InfiltrateFactory implements ActionFactory {

    @Override
    public LinkedList<Action> computeActionVariants(final Actor actor, final GameState gs) {
        Unit unit = (Unit) actor;
        LinkedList<Action> actions = new LinkedList<>();

        //Only if the unit can 'attack'
        if((unit.getType() == Types.UNIT.CLOAK || unit.getType() == Types.UNIT.DINGY) && unit.canAttack())
        {
            Board b = gs.getBoard();
            Vector2d position = unit.getPosition();
            LinkedList<Vector2d> potentialTiles = position.neighborhood(unit.RANGE, 0, b.getSize()); //use neighbourhood for board limits
            for (Vector2d tile : potentialTiles) {
                City target = b.getCityInBorders(tile.x, tile.y);

                if(target != null && tile.equals(target.getPosition()) && target.getTribeId() != unit.getTribeId())
                {
                    // Check if there is actually a city there (and it's not mine)
                    Infiltrate c = new Infiltrate(unit.getActorId());
                    c.setTargetId(target.getActorId());
                    if(c.isFeasible(gs)){
                        actions.add(c);
                    }
                }
            }
        }

        return actions;
    }

}

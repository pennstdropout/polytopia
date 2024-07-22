package core.actions.unitactions.command;

import core.TechnologyTree;
import core.Types;
import core.actions.Action;
import core.actions.ActionCommand;
import core.actions.unitactions.Examine;
import core.actions.unitactions.GatherStar;
import core.actors.City;
import core.actors.Tribe;
import core.actors.units.Cloak;
import core.actors.units.Dingy;
import core.actors.units.Rammer;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

import java.util.Random;

import static core.Types.EXAMINE_BONUS.RESEARCH;
import static core.Types.EXAMINE_BONUS.VETERAN;

public class GatherStarCommand implements ActionCommand {

    @Override
    public boolean execute(Action a, GameState gs) {
        GatherStar action = (GatherStar) a;
        int unitId = action.getUnitId();

        if(action.isFeasible(gs)) {
            Unit unit = (Unit) gs.getActor(unitId);
            Vector2d pos = unit.getPosition();
            Tribe t = gs.getTribe(unit.getTribeId());
            Board b = gs.getBoard();

            if (unit.getType() == Types.UNIT.CLOAK) {
                ((Cloak) unit).setVisibility(true);
            } else if (unit.getType() == Types.UNIT.DINGY) {
                ((Dingy) unit).setVisibility(true);
            }
            b.setResourceAt(pos.x, pos.y, null);
            t.addStars(Types.RESOURCE.STAR.getBonus());
            unit.setStatus(Types.TURN_STATUS.FINISHED);
            return true;
        }
        return false;
    }
}

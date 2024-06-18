package core.actions.unitactions.command;

import core.Types;
import core.actions.Action;
import core.actions.ActionCommand;
import core.actions.unitactions.Infiltrate;
import core.actors.City;
import core.actors.Tribe;
import core.actors.units.Dagger;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

import java.util.LinkedList;

public class InfiltrateCommand implements ActionCommand {

    @Override
    public boolean execute(Action a, GameState gs) {
        Infiltrate action = (Infiltrate)a;
        int unitId = action.getUnitId();

        if (action.isFeasible(gs)) {
            Unit unit = (Unit) gs.getActor(unitId);
            Board b = gs.getBoard();

            City targetCity = (City) gs.getActor(action.targetId);
            Tribe targetTribe = (Tribe) gs.getActor(targetCity.getTribeId());
            int targetProd = targetCity.getProduction();
            targetTribe.subtractStars(targetProd);

            City homeCity = (City) b.getActor(unit.getCityId());
            Tribe homeTribe = gs.getTribe(unit.getTribeId());

            b.removeUnitFromBoard(unit);
            b.removeUnitFromCity(unit, homeCity, homeTribe);
            homeTribe.subtractScore(unit.getType().getPoints());
            homeTribe.addStars(targetProd);

            int numDaggers = Math.min(5, targetCity.getLevel());
            LinkedList<Vector2d> tiles = b.getCityTiles(action.targetId);

            while (numDaggers > 0) {
                int randomIndex = (int) (Math.random() * tiles.size());
                Vector2d tile = tiles.get(randomIndex);
                Unit dagger = Types.UNIT.createUnit(tile, 0, false, homeCity.getActorId(), homeCity.getTribeId(), Types.UNIT.DAGGER);
                homeTribe.addExtraUnit(dagger);
                numDaggers--;
                tiles.remove(randomIndex);
            }
            return true;
        }
        return false;
    }
}

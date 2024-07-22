package core.actions.unitactions.command;

import core.Types;
import core.actions.Action;
import core.actions.ActionCommand;
import core.actions.unitactions.Move;
import core.actions.unitactions.StepMove;
import core.actors.Tribe;
import core.actors.units.Cloak;
import core.actors.units.Dingy;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;
import utils.graph.PathNode;
import utils.graph.Pathfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

public class MoveCommand implements ActionCommand {

    @Override
    public boolean execute(Action a, GameState gs) {
        Move action = (Move)a;
        if(action.isFeasible(gs)) {
            int unitId = action.getUnitId();
            Vector2d destination = action.getDestination();
            Unit unit = (Unit) gs.getActor(unitId);
            Vector2d start = unit.copy(false).getPosition();
            Board board = gs.getBoard();
            Tribe tribe = gs.getTribe(unit.getTribeId());
            Types.TERRAIN destinationTerrain = board.getTerrainAt(destination.x, destination.y);
            boolean isEmbarking = false;

            Unit otherUnit = board.getUnitAt(destination.x, destination.y);
            if (otherUnit != null) {
                otherUnit.setVisible(true);
                return true;
            }

            board.moveUnit(unit, unit.getPosition().x, unit.getPosition().y, destination.x, destination.y, gs.getRandomGenerator());

            if(unit.getType().isWaterUnit()){
                if(destinationTerrain != Types.TERRAIN.SHALLOW_WATER && destinationTerrain != Types.TERRAIN.DEEP_WATER){
                    isEmbarking = true;
                    board.disembark(unit, tribe, destination.x, destination.y);
                }
            } else {
                if(board.getBuildingAt(destination.x, destination.y) == Types.BUILDING.PORT){
                    isEmbarking = true;
                    board.embark(unit, tribe, destination.x, destination.y);
                }
            }

            if ((isEmbarking && unit.getType() == Types.UNIT.SUPERUNIT) || unit.getType() == Types.UNIT.JUGGERNAUT) {
                for (Vector2d v : destination.neighborhood(1, 0, board.getSize())) {
                    Unit splashTarget = board.getUnitAt(v.x, v.y);
                    if (splashTarget != null && splashTarget.getTribeId() != unit.getTribeId()) {
                        int splashResult = Math.floorDiv(AttackCommand.getAttackResults(unit, splashTarget, gs).getFirst(), 2);
                        if (splashTarget.getCurrentHP() <= splashResult) {
                            unit.addKill();
                            unit.addKill();
                            gs.killUnit(splashTarget);
                        } else {
                            splashTarget.setCurrentHP(splashTarget.getCurrentHP() - splashResult);
                        }
                    }
                }
            }

            if (unit.getType() == Types.UNIT.CLOAK) {
                ((Cloak) unit).setVisibility(false);
            } else if (unit.getType() == Types.UNIT.DINGY) {
                ((Dingy) unit).setVisibility(false);
            }

            unit.transitionToStatus(Types.TURN_STATUS.MOVED);
            return true;
        }
        return false;
    }
}

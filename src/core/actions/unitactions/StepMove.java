package core.actions.unitactions;

import core.TechnologyTree;
import core.Types;
import core.actors.City;
import core.actors.units.Cloak;
import core.actors.units.Dingy;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;
import utils.graph.NeighbourHelper;
import utils.graph.PathNode;

import java.util.ArrayList;

import static core.Types.TERRAIN.CITY;

public class StepMove implements NeighbourHelper
{
    private GameState gs;
    private Unit unit;

    public StepMove(GameState curGameState, Unit movingUnit)
    {
        this.gs = curGameState;
        this.unit = movingUnit;
    }

    @Override
    //from: position from which we need neighbours
    //costFrom: is the total move cost computed up to "from"
    //Using this.gs, this.unit, from and costFrom, gets all the adjacent neighbours to tile in position "from"
    public ArrayList<PathNode> getNeighbours(Vector2d from, double costFrom) {
        ArrayList<PathNode> neighbours = new ArrayList<>();

        //Check if the unit has reached the limit of it's movement range
        if (costFrom == unit.MOV) {
            return neighbours;
        }

        Board board = gs.getBoard();
        boolean onRoad = false;

        //Check if unit is on a neutral or a friendly road, cities also count as roads.
        if(board.isRoad(from.x, from.y) || board.getTerrainAt(from.x, from.y) == CITY){
            int cityId = board.getCityIdAt(from.x, from.y);
            if(cityId == -1 || board.getTribe(unit.getTribeId()).controlsCity(cityId)) {
                onRoad = true;
            }
        }

        //Each one of the tree nodes added to "neighbours" must have a position (x,y) and also the cost of moving there from "from":
        //TreeNode tn = new TreeNode (vector2d pos, double stepCost)
        //We only add nodes to neighbours if costFrom+stepCost <= total move range of this.unit
        for(Vector2d tile : from.neighborhood(1, 0, board.getSize())) {
            Types.TERRAIN terrain = board.getTerrainAt(tile.x, tile.y);
            double stepCost = 0.0;
            boolean zoneOfControl = false;
            boolean isCloak = unit.getType() == Types.UNIT.CLOAK || unit.getType() == Types.UNIT.DINGY;

            //Can't move to tiles where there's a non-friendly unit
            Unit otherUnit = board.getUnitAt(tile.x, tile.y);
            boolean isNotNull = otherUnit != null;

            if (isNotNull && otherUnit.isVisible() && otherUnit.getTribeId() != unit.getTribeId()) {
                continue;
            }

            //Check if there is an enemy unit adjacent to the destination.
            for (Vector2d tileAdj : tile.neighborhood(1, 0, board.getSize())) {
                Unit u = board.getUnitAt(tileAdj.x, tileAdj.y);  // There might not be a unit there at all
                if (u != null && u.getTribeId() != unit.getTribeId()) {
                    zoneOfControl = u.isVisible();
                }
            }

            //Cannot move into tiles that have not been discovered yet.
            if (!gs.getTribe(unit.getTribeId()).isVisible(tile.x, tile.y)) {
                continue;
            }

            //Check if current research allows movement to this tile.
            if (!board.traversable(tile.x, tile.y, unit.getTribeId())) {
                continue;
            }

            //Mind benders and cloaks cannot move into an enemy city tile.
            if ((unit.getType() == Types.UNIT.MIND_BENDER || isCloak)
                    && board.getTerrainAt(tile.x, tile.y) == CITY) {
                City targetCity = (City) board.getActor(board.getCityIdAt(tile.x, tile.y));
                //The city belongs to the enemy.
                if (targetCity.getTribeId() != unit.getTribeId()) {
                    continue;
                }
            }

            //Unit is a water unit
            if (unit.getType().isWaterUnit()) {
                switch (terrain) {
                    case CITY:
                        if (isCloak) {
                            stepCost = unit.MOV + 1;
                            break;
                        }
                    case PLAIN:
                    case FOREST:
                    case VILLAGE:
                    case MOUNTAIN:
                        //Disembark takes a turn of movement.
                        stepCost = costFrom < unit.MOV ? (unit.MOV - costFrom) : unit.MOV; //as much cost as needed to finished step here
                        break;
                    case FOG:
                    case DEEP_WATER:
                    case SHALLOW_WATER:
                        stepCost = 1.0;
                        break;
                }
            } else //Ground unit
            {
                switch (terrain) {
                    case SHALLOW_WATER:
                    case DEEP_WATER:
                        //Embarking takes a turn of movement.
                        if (board.getBuildingAt(tile.x, tile.y) == Types.BUILDING.PORT
                                && board.getCityInBorders(tile.x, tile.y).getTribeId() == unit.getTribeId()) {
                            stepCost = costFrom < unit.MOV ? (unit.MOV - costFrom) : unit.MOV; //as much cost as needed to finished step here;
                        } else {
                            continue;
                        }
                        break;
                    case FOG:
                    case PLAIN:
                    case CITY:
                    case VILLAGE:
                        stepCost = 1.0;
                        break;
                    case FOREST:
                    case MOUNTAIN:
                        stepCost = costFrom < unit.MOV ? (unit.MOV - costFrom) : unit.MOV; //as much cost as needed to finished step here
                        stepCost = isCloak ? 1.0: stepCost;
                        break;
                }

                //If there is a friendly/neutral road connection between two tiles then the movement cost is halved.
                //This movement boost applies only to ground units.
                if (onRoad && (board.isRoad(tile.x, tile.y) || board.getTerrainAt(tile.x, tile.y) == CITY)) {
                    int cityId = board.getCityIdAt(from.x, from.y);
                    if (cityId == -1 || board.getTribe(unit.getTribeId()).controlsCity(cityId)) {
                        stepCost = Math.max(0.5, stepCost / 2.0);
                    }
                }
            }

            // Moving to zone of control is never a problem, but it consumes all the rest of the movement.
            if(zoneOfControl){
                stepCost = costFrom < unit.MOV ? (unit.MOV - costFrom) : unit.MOV;
                stepCost = isCloak ? 1.0: stepCost;
                if(costFrom + stepCost <= unit.MOV)
                    neighbours.add(new PathNode(tile, stepCost));

            //No zone of control, allow movement if part of MOV is still available.
            } else if(Math.floor(costFrom + stepCost) <= unit.MOV){
                neighbours.add(new PathNode(tile, stepCost));
            }


        }
        return neighbours;
    }

    @Override
    public void addJumpLink(Vector2d from, Vector2d to, boolean reverse) {
        //No jump links
    }
}
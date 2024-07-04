package core.actions.unitactions.command;

import core.Types;
import core.actions.Action;
import core.actions.ActionCommand;
import core.actions.unitactions.Infiltrate;
import core.actors.City;
import core.actors.Tribe;
import core.actors.units.Dagger;
import core.actors.units.Pirate;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Pair;
import utils.Vector2d;

import java.util.Collections;
import java.util.LinkedList;

import static core.TribesConfig.ATTACK_REPERCUSSION;

public class InfiltrateCommand implements ActionCommand {

    @Override
    public boolean execute(Action a, GameState gs) {
        Infiltrate action = (Infiltrate)a;
        int unitId = action.getUnitId();

        if (action.isFeasible(gs)) {
            Unit unit = (Unit) gs.getActor(unitId);
            Board b = gs.getBoard();

            City targetCity = (City) gs.getActor(action.targetId);
            Vector2d cityPos = targetCity.getPosition();
            Tribe targetTribe = gs.getTribe(targetCity.getTribeId());
            int targetProd = targetCity.getProduction();
            targetTribe.subtractStars(targetProd);

            City homeCity = (City) b.getActor(unit.getCityId());
            Tribe homeTribe = gs.getTribe(unit.getTribeId());

            Unit target = b.getUnitAt(cityPos.x, cityPos.y);
            if (target != null) {
                AttackCommand attackCommand = new AttackCommand();
                Pair<Integer, Integer> results = attackCommand.getAttackResults(unit, target, gs);
                int attackResult = results.getFirst();

                if (target.getCurrentHP() <= attackResult) {
                    homeTribe.addKill();
                    gs.killUnit(target);
                } else {
                    target.setCurrentHP(target.getCurrentHP() - attackResult);
                }
            }

            gs.killUnit(unit);
            homeTribe.addStars(targetProd);

            int numDaggers = Math.min(5, targetCity.getLevel());
            LinkedList<Vector2d> tiles = b.getCityTiles(action.targetId);
            LinkedList<Vector2d> orderedTiles = new LinkedList<>();
            LinkedList<Vector2d> cityTiles = new LinkedList<>();
            LinkedList<Vector2d> forestTiles = new LinkedList<>();
            LinkedList<Vector2d> mountainTiles = new LinkedList<>();
            LinkedList<Vector2d> otherTiles = new LinkedList<>();
            LinkedList<Vector2d> waterTiles = new LinkedList<>();

            boolean hasClimbing = homeTribe.getTechTree().isResearched(Types.TECHNOLOGY.CLIMBING);
            boolean hasArchery = homeTribe.getTechTree().isResearched(Types.TECHNOLOGY.ARCHERY);
            boolean hasFishing = homeTribe.getTechTree().isResearched(Types.TECHNOLOGY.FISHING);
            boolean hasSailing = homeTribe.getTechTree().isResearched(Types.TECHNOLOGY.SAILING);

            for (Vector2d tile : tiles) {
                boolean noUnitOnTile = b.getUnitAt(tile.x, tile.y) == null;
                Types.TERRAIN tileType = b.getTerrainAt(tile.x, tile.y);

                if (noUnitOnTile) {
                    if (tile.equals(cityPos)) {
                        cityTiles.add(tile);
                    } else if (hasArchery && tileType == Types.TERRAIN.FOREST) {
                        forestTiles.add(tile);
                    } else if (tileType == Types.TERRAIN.MOUNTAIN) {
                        if (hasClimbing) { mountainTiles.add(tile); }
                    } else if (tileType == Types.TERRAIN.SHALLOW_WATER) {
                        if (hasFishing) { waterTiles.add(tile); }
                    } else if (tileType == Types.TERRAIN.DEEP_WATER) {
                        if (hasSailing) { waterTiles.add(tile); }
                    } else {
                        otherTiles.add(tile);
                    }
                }
            }

            Collections.shuffle(mountainTiles);
            Collections.shuffle(forestTiles);
            Collections.shuffle(otherTiles);
            Collections.shuffle(waterTiles);
            orderedTiles.addAll(cityTiles);
            orderedTiles.addAll(mountainTiles);
            orderedTiles.addAll(forestTiles);
            orderedTiles.addAll(otherTiles);

            for (Vector2d tile : orderedTiles) {
                if (numDaggers <= 0) {
                    break;
                }
                Dagger dagger = (Dagger) Types.UNIT.createUnit(tile, 0, false, -1, homeCity.getTribeId(), Types.UNIT.DAGGER);
                b.addUnit(null, dagger);
                numDaggers--;
            }

            for (Vector2d tile : waterTiles) {
                if (numDaggers <= 0) {
                    break;
                }
                Pirate pirate = (Pirate) Types.UNIT.createUnit(tile, 0, false, -1, homeCity.getTribeId(), Types.UNIT.PIRATE);
                pirate.setBaseLandUnit(Types.UNIT.DAGGER);
                b.addUnit(null, pirate);
                numDaggers--;
            }

            return true;
        }
        return false;
    }

    private LinkedList<Vector2d> orderTiles(LinkedList<Vector2d> tiles, Tribe tribe, City targetCity) {


        return tiles;
    }
}

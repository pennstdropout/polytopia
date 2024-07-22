package players.portfolio.scripts.utils;

import core.Types;
import core.actions.Action;
import core.actions.cityactions.Build;
import core.actions.cityactions.CityAction;
import core.actions.cityactions.ClearForest;
import core.game.GameState;
import utils.Pair;
import utils.Utils;
import utils.Vector2d;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static core.Types.BUILDING.*;
import static core.Types.RESOURCE.*;
import static core.Types.TERRAIN.*;

public class BuildingFunc {

    //This function checks if neighPos is a good position as a neighbour of "Unique_building"
    public boolean goodNeighbourFor(GameState gs, Vector2d neighPos, Types.BUILDING unique_building)
    {
        Types.TERRAIN t = gs.getBoard().getTerrainAt(neighPos.x, neighPos.y);
        Types.BUILDING b = gs.getBoard().getBuildingAt(neighPos.x, neighPos.y);
        Types.RESOURCE r = gs.getBoard().getResourceAt(neighPos.x, neighPos.y);
        switch (unique_building)
        {
            case MARKET:
                return (b == WINDMILL || b == FORGE || b == SAWMILL);
            case WINDMILL:
                return (r == CROPS || b == FARM);
            case FORGE:
                return ((t == MOUNTAIN && r == ORE) || b == MINE);
            case SAWMILL:
                return t == FOREST || b == LUMBER_HUT;
            default:
                System.out.println("You're using this function wrong: " + unique_building);
        }
        return false;
    }


    //Evaluates if centrePos is a good position for a building of type "target".
    //Only for support buildings: custom-house, sawmill, windmill and forge.
    public double evalNeighSupportBuilding(Vector2d centrePos, GameState gs, Types.BUILDING target)
    {
        int goodNeigh = 0;
        LinkedList<Vector2d> neighs = centrePos.neighborhood(1, 0, gs.getBoard().getSize());
        for(Vector2d neighPos : neighs)
        {
            goodNeigh += goodNeighbourFor(gs, neighPos, target) ? 1 : 0;
        }
        return (double) goodNeigh / neighs.size();
    }

    public boolean validConstruction(GameState gs, Vector2d pos, Types.BUILDING buildingType, int cityId, boolean checkUniqueness)
    {
        //Terrain constraint
        if(!buildingType.getTerrainRequirements().contains(gs.getBoard().getTerrainAt(pos.x, pos.y)))
            return false;

        //Uniqueness constraint.
        if(checkUniqueness) for(Vector2d tile : gs.getBoard().getCityTiles(cityId)) {
            if(gs.getBoard().getBuildingAt(tile.x, tile.y) == buildingType)
                return false;
        }

        return true;
    }

    //For support buildings: custom-house, sawmill, windmill and forge.
    public Pair<Action,Double> buildSupportBuilding(Types.BUILDING target, GameState gs, ArrayList<Action> actions, Random rnd)
    {
        ArrayList<Action> candidate_actions = new ArrayList<>();

        double highestNeigh = 0;
        for(Action act : actions)
        {
            boolean typeCheck = (act instanceof Build && ((Build)act).getBuildingType() == target)  //if building, check we're building the correct target
                             || (act instanceof ClearForest);                                       //if clearing forest, all is good.

            if(typeCheck)
            {
                Vector2d targetPos = ((CityAction)act).getTargetPos();
                double goodNeigh = evalNeighSupportBuilding(targetPos, gs, target);

                if(goodNeigh > highestNeigh)
                {
                    candidate_actions = new ArrayList<>();
                    highestNeigh = goodNeigh;
                    candidate_actions.add(act);
                }else if (goodNeigh == highestNeigh)
                {
                    candidate_actions.add(act);
                }
            }
        }

        if(candidate_actions.size() > 0)
        {
            int nActions = candidate_actions.size();
            return new Pair<>(candidate_actions.get(rnd.nextInt(nActions)), highestNeigh);
        }

        return null;
    }

    //For base buildings: port, farm, mine, lumber huts.
    public Pair<Action, Double> buildBaseBuilding(Types.BUILDING toBuild, Types.BUILDING bonusNeighBuilding, GameState gs, ArrayList<Action> actions, Random rnd)
    {
        ArrayList<Action> candidate_actions = new ArrayList<>();
        double bestSupportedVal = -1;
        for(Action act : actions)
        {
            Build action = (Build)act;
            Vector2d targetPos = action.getTargetPos();

            if(action.getBuildingType() == toBuild) {
                LinkedList<Vector2d> neighs = targetPos.neighborhood(1, 0, gs.getBoard().getSize());

                double bestForBonus = 0;
                for (Vector2d neighPos : neighs) {
                    boolean valid = validConstruction(gs, neighPos, bonusNeighBuilding, action.getCityId(), true);
                    if (valid)
                    {
                        double goodForBonusBuilding = evalNeighSupportBuilding(neighPos, gs, bonusNeighBuilding);
                        if (bestForBonus < goodForBonusBuilding) {
                            bestForBonus = goodForBonusBuilding;
                        }
                    }
                }

                if(bestForBonus > bestSupportedVal)
                {
                    candidate_actions.add(act);
                    bestSupportedVal = bestForBonus;
                    candidate_actions = new ArrayList<>();
                }else if(bestForBonus == bestSupportedVal)
                {
                    candidate_actions.add(act);
                }
            }
        }

        int nActions = candidate_actions.size();
        if( nActions > 0) {
            return new Pair<>(candidate_actions.get(rnd.nextInt(nActions)), bestSupportedVal);
        }
        return null;
    }

    public double valueForSupportingBuilding(GameState gs, Vector2d position, Types.BUILDING[] targets, int cityId)
    {
        double maxValue = 0;
        for (Types.BUILDING target : targets) {
            //Check if the supporting building can be built here and if it has a good value.
            boolean valid = validConstruction(gs, position, target, cityId, true);
            if (valid) {
                double goodVal = evalNeighSupportBuilding(position, gs, target);
                if(goodVal > maxValue) maxValue = goodVal;
            }
        }

        return maxValue;
    }

    public double valueForBaseBuilding(GameState gs, Vector2d position, Types.BUILDING[] targets, int cityId)
    {
        double maxValue = 0;
        for (Types.BUILDING target : targets) {

            // a. Check if base building can be built here
            boolean valid = validConstruction(gs, position, target, cityId, false);
            if (valid) {
                // b. Check if this is a good place for a base building: would it neighbour any good place for a support building?
                LinkedList<Vector2d> neighs = position.neighborhood(1, 0, gs.getBoard().getSize());

                for (Vector2d neighPos : neighs) {
                    List<Types.BUILDING> supportBuildings = target.getMatchingBuildingWithMarket();
                    for (Types.BUILDING supportBuilding : supportBuildings) {
                        valid = validConstruction(gs, neighPos, supportBuilding, cityId, true);
                        if (valid) {
                            //Keep track of the best value for a neighbouring support building.
                            double nGoodN = evalNeighSupportBuilding(neighPos, gs, supportBuilding);
                            if(nGoodN > maxValue) maxValue = nGoodN;
                        }
                    }
                }
            }
        }

        return maxValue;
    }


    public Pair<Action, Double> buildInIdle(Types.BUILDING targetBuilding, GameState gs, ArrayList<Action> actions, Random rnd)
    {
        ArrayList<Action> candidate_actions = new ArrayList<>();
        double bestScore = 0;

        for(Action act : actions)
        {
            Build action = (Build)act;
            if(action.getBuildingType() == targetBuilding)
            {
                Vector2d targetPos = action.getTargetPos();
                //1. Check that this is not a good place for a supporting building.
                double goodForSupportVal = valueForSupportingBuilding(gs, targetPos, new Types.BUILDING[]{SAWMILL, MARKET, WINDMILL, FORGE}, action.getCityId());
                double goodForBaseVal = valueForBaseBuilding(gs, targetPos, new Types.BUILDING[]{LUMBER_HUT, PORT, FARM, MINE}, action.getCityId());
                //2. Check that this is not a good place for a base building.
                double score = goodForSupportVal - goodForBaseVal;

                if(score > bestScore)
                {
                    candidate_actions.clear();
                    bestScore = score;
                }
                if(score == bestScore)
                    candidate_actions.add(act);

            }
        }

        int nActions = candidate_actions.size();
        if( nActions > 0) {
            Action chosen = candidate_actions.get(rnd.nextInt(nActions));
            double value = Utils.normalise(bestScore, -1, 1);
            new Pair<>(chosen, value);
        }
        return null;
    }

}

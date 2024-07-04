package core.actions.cityactions.factory;

import core.Types;
import core.actions.Action;
import core.actions.ActionFactory;
import core.actions.cityactions.ResourceGathering;
import core.actors.Actor;
import core.actors.City;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Vector2d;

import java.util.LinkedList;

public class ResourceGatheringFactory implements ActionFactory {

    @Override
    public LinkedList<Action> computeActionVariants(final Actor actor, final GameState gs) {

        City city = (City) actor;
        Board b = gs.getBoard();
        LinkedList<Action> resourceActions = new LinkedList<>();
        int cityId = city.getActorId();

        // loop through bounds of city and add resource actions if they are feasible
        for(Vector2d pos : b.getCityTiles(cityId)) {

            Types.RESOURCE r = b.getResourceAt(pos.x, pos.y);
            if (r == null)
                continue;
            ResourceGathering resourceAction = new ResourceGathering(cityId);
            resourceAction.setResource(r);
            resourceAction.setTargetPos(new Vector2d(pos.x, pos.y));
            if (resourceAction.isFeasible(gs)) {
                resourceActions.add(resourceAction);
            }
        }

        // loop through resource locations and add gather star if feasible
        boolean hasNavigation = b.getTribe(city.getTribeId()).getTechTree().isResearched(Types.TECHNOLOGY.NAVIGATION);
        if (city.isCapital() && hasNavigation) {
            System.out.println();
            System.out.println("resources pre star:  " + resourceActions.size());

            Types.RESOURCE[][] resources = b.getResources();
            for (int i = 0; i < resources.length; i++) {
                for (int j = 0; j < resources.length; j++) {
                    Types.RESOURCE r = b.getResourceAt(i, j);
                    if (r == Types.RESOURCE.STAR) {
                        ResourceGathering resourceAction = new ResourceGathering(cityId);
                        resourceAction.setResource(r);
                        resourceAction.setTargetPos(new Vector2d(i, j));
                        if (resourceAction.isFeasible(gs)) {
                            resourceActions.add(resourceAction);
                        }
                    }
                }
            }
            System.out.println("resources post star:  " + resourceActions.size());
        }
        return resourceActions;
    }

}

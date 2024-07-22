package core.actions.unitactions.factory;

import core.actions.Action;
import core.actions.ActionFactory;
import core.actions.unitactions.UpgradeToBomber;
import core.actions.unitactions.UpgradeToRammer;
import core.actions.unitactions.UpgradeToScout;
import core.actors.Actor;
import core.actors.City;
import core.actors.units.Raft;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import core.Types;
import utils.Vector2d;

import java.util.LinkedList;

public class UpgradeFactory implements ActionFactory {

    @Override
    public LinkedList<Action> computeActionVariants(final Actor actor, final GameState gs) {
        Unit unit = (Unit) actor;
        Vector2d unitPos = unit.getPosition();

        City c = gs.getBoard().getCityInBorders(unitPos.x, unitPos.y);
        int locTribeId = c == null ? -1 : c.getTribeId();
        boolean friendlyTile = unit.getTribeId() == locTribeId;
        boolean isRaft = unit.getType() == Types.UNIT.RAFT;

        LinkedList<Action> upgradeActions = new LinkedList<>();
        if(friendlyTile && isRaft) {

            UpgradeToScout scout = new UpgradeToScout(unit.getActorId());
            if(scout.isFeasible(gs)){
                upgradeActions.add(scout);
            }
            UpgradeToRammer rammer = new UpgradeToRammer(unit.getActorId());
            if(rammer.isFeasible(gs)){
                upgradeActions.add(rammer);
            }
            UpgradeToBomber bomber = new UpgradeToBomber(unit.getActorId());
            if(bomber.isFeasible(gs)){
                upgradeActions.add(bomber);
            }
        }
        return upgradeActions;
    }

}

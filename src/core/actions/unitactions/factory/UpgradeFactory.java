package core.actions.unitactions.factory;

import core.actions.Action;
import core.actions.ActionFactory;
import core.actions.unitactions.Upgrade;
import core.actors.Actor;
import core.actors.units.Unit;
import core.game.GameState;
import core.Types;

import java.util.LinkedList;

public class UpgradeFactory implements ActionFactory {

    @Override
    public LinkedList<Action> computeActionVariants(final Actor actor, final GameState gs) {
        Unit unit = (Unit) actor;
        LinkedList<Action> upgradeActions = new LinkedList<>();

        if(unit.getType() == Types.UNIT.RAFT) {
            LinkedList<Types.ACTION> actionTypeList = new LinkedList<>();
            actionTypeList.add(Types.ACTION.UPGRADE_TO_RAMMER);
            actionTypeList.add(Types.ACTION.UPGRADE_TO_SCOUT);
            actionTypeList.add(Types.ACTION.UPGRADE_TO_BOMBER);

            for (Types.ACTION actionType : actionTypeList) {
                Upgrade action = new Upgrade(actionType, unit.getActorId());
                if(action.isFeasible(gs)){
                    upgradeActions.add(action);
                }
            }
        }
        return upgradeActions;
    }

}

package core.actions.unitactions.command;

import core.Types;
import core.actions.Action;
import core.actions.ActionCommand;
import core.actions.unitactions.UpgradeToBomber;
import core.actions.unitactions.UpgradeToRammer;
import core.actions.unitactions.UpgradeToScout;
import core.actors.City;
import core.actors.Tribe;
import core.actors.units.Raft;
import core.actors.units.Rammer;
import core.actors.units.Scout;
import core.actors.units.Bomber;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;

import static core.Types.UNIT.SCOUT;
import static core.Types.UNIT.RAMMER;
import static core.Types.UNIT.BOMBER;

public class UpgradeCommand implements ActionCommand {

    Types.UNIT target;

    public UpgradeCommand(Types.UNIT target) {this.target = target;}

    @Override
    public boolean execute(Action a, GameState gs) {

        if(a.isFeasible(gs)){
            int unitId;
            if (target == SCOUT) {
                UpgradeToScout action = (UpgradeToScout) a;
                unitId = action.getUnitId();
            } else if (target == RAMMER) {
                UpgradeToRammer action = (UpgradeToRammer) a;
                unitId = action.getUnitId();
            } else {
                UpgradeToBomber action = (UpgradeToBomber) a;
                unitId = action.getUnitId();
            }
            Unit unit = (Unit) gs.getActor(unitId);
            Tribe tribe = gs.getTribe(unit.getTribeId());
            Board board = gs.getBoard();
            City city = (City) board.getActor(unit.getCityId());

            //Create the new unit
            Unit newUnit = Types.UNIT.createUnit(unit.getPosition(), unit.getKills(), unit.isVeteran(), unit.getCityId(), unit.getTribeId(), target);
            newUnit.setCurrentHP(unit.getCurrentHP());
            newUnit.setMaxHP(unit.getMaxHP());
            if(target == RAMMER)
                ((Rammer)newUnit).setBaseLandUnit(((Raft)unit).getBaseLandUnit());
            else if(target == SCOUT)
                ((Scout)newUnit).setBaseLandUnit(((Raft)unit).getBaseLandUnit());
            else if (target == BOMBER)
                ((Bomber)newUnit).setBaseLandUnit(((Raft)unit).getBaseLandUnit());

            //adjustments in tribe and board.
            tribe.subtractStars(target.getCost());

            Types.TURN_STATUS turn_status = unit.getStatus();
            //We first remove the unit, so there's space for the new one to take its place.
            board.removeUnitFromBoard(unit);
            board.removeUnitFromCity(unit, city, tribe);
            board.addUnit(city, newUnit);
            newUnit.setStatus(turn_status);
            return true;
        }
        return false;
    }
}

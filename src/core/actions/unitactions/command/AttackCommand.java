package core.actions.unitactions.command;

import core.Diplomacy;
import core.TribesConfig;
import core.Types;
import core.actions.Action;
import core.actions.ActionCommand;
import core.actions.unitactions.Attack;
import core.actors.City;
import core.actors.Tribe;
import core.actors.units.Unit;
import core.game.Board;
import core.game.GameState;
import utils.Pair;
import utils.Vector2d;

import java.util.LinkedList;

import static core.TribesConfig.ATTACK_REPERCUSSION;
import static core.Types.TECHNOLOGY.*;
import static core.Types.TERRAIN.*;

public class AttackCommand implements ActionCommand {

    @Override
    public boolean execute(Action a, GameState gs) {
        Attack action = (Attack) a;
        Types.UNIT u = ((Unit) gs.getActor(action.getUnitId())).getType();

        //Check if action is feasible before execution
        if (action.isFeasible(gs)) {

            // Getting diplomacy to update values
            Diplomacy d = gs.getBoard().getDiplomacy();

            int unitId = action.getUnitId();
            int targetId = action.getTargetId();

            Unit attacker = (Unit) gs.getActor(unitId);
            Unit target = (Unit) gs.getActor(targetId);

            attacker.transitionToStatus(Types.TURN_STATUS.ATTACKED);
            Tribe attackerTribe = gs.getTribe(attacker.getTribeId());
            attackerTribe.resetPacifistCount();

            Pair<Integer, Integer> results = getAttackResults(action, gs);
            int attackResult = results.getFirst();
            int defenceResult = results.getSecond();

            // Updating relationship between tribes, deducting 5
            d.updateAllegiance(ATTACK_REPERCUSSION, attacker.getTribeId(), target.getTribeId());
            // Checks consequences of the update
            d.checkConsequences(ATTACK_REPERCUSSION, attacker.getTribeId(), target.getTribeId());

            if (target.getCurrentHP() <= attackResult) {

                // Updating relationship between tribes, deducting an additional 5 if the unit is killed
                d.updateAllegiance(ATTACK_REPERCUSSION, attacker.getTribeId(), target.getTribeId());
                // Checks consequences of the update
                d.checkConsequences(ATTACK_REPERCUSSION, attacker.getTribeId(), target.getTribeId());

                attacker.addKill();
                attackerTribe.addKill();

                //Actually kill the unit
                gs.killUnit(target);

                //After killing the unit, move to target position if unit is melee type
                switch (attacker.getType()) {
                    case DEFENDER:
                    case SWORDMAN:
                    case RIDER:
                    case WARRIOR:
                    case KNIGHT:
                    case SUPERUNIT:
                    case DAGGER:
                    case PIRATE:
                        gs.getBoard().tryPush(attackerTribe, attacker, attacker.getPosition().x, attacker.getPosition().y, target.getPosition().x, target.getPosition().y, gs.getRandomGenerator());
                        break;
                    case RAMMER:
                    case JUGGERNAUT:
                        Types.TERRAIN destinationTerrain = gs.getBoard().getTerrainAt(target.getPosition().x, target.getPosition().y);
                        if(destinationTerrain != Types.TERRAIN.SHALLOW_WATER && destinationTerrain != Types.TERRAIN.DEEP_WATER) {
                            gs.getBoard().disembark(attacker, attackerTribe, target.getPosition().x, target.getPosition().y);
                        } else {
                            gs.getBoard().tryPush(attackerTribe, attacker, attacker.getPosition().x, attacker.getPosition().y, target.getPosition().x, target.getPosition().y, gs.getRandomGenerator());
                        }
                }
            } else {

                target.setCurrentHP(target.getCurrentHP() - attackResult);
                Types.UNIT targetType = target.getType();

                //Check if this unit is in target's attacking range (we can use chebichev distance)
                double distance = Vector2d.chebychevDistance(attacker.getPosition(), target.getPosition());

                boolean inRange = distance <= target.RANGE;
                boolean stiff = targetType == Types.UNIT.CATAPULT
                        || targetType == Types.UNIT.RAFT
                        || targetType == Types.UNIT.BOMBER
                        || targetType == Types.UNIT.CLOAK
                        || targetType == Types.UNIT.DINGY
                        || targetType == Types.UNIT.JUGGERNAUT;
                boolean surprise = attacker.getType() == Types.UNIT.DAGGER || attacker.getType() == Types.UNIT.PIRATE;

                if (inRange && !stiff && !surprise) {
                    //Deal damage based on targets defence stat, regardless of this units defence stat
                    attacker.setCurrentHP(attacker.getCurrentHP() - defenceResult);
                    //Check if attack kills this unit, if it does add a kill to the target
                    if (attacker.getCurrentHP() <= 0) {
                        target.addKill();
                        gs.getTribe(target.getTribeId()).addKill();

                        //Actually kill the unit
                        gs.killUnit(attacker);
                    }
                }
            }

            if (attacker.getType() == Types.UNIT.BOMBER) { // splash damage
                LinkedList<Vector2d> tiles = target.getPosition().neighborhood(1, 0, gs.getBoard().getSize());

                for (Vector2d tile: tiles) {

                    Unit splashTarget = gs.getBoard().getUnitAt(tile.x, tile.y);
                    if (splashTarget != null && splashTarget.getTribeId() != attacker.getTribeId()) {

                        int splashResult = Math.floorDiv(getAttackResults(attacker, splashTarget, gs).getFirst(), 2);
                        if (splashTarget.getCurrentHP() <= splashResult) {
                            attacker.addKill();
                            attackerTribe.addKill();
                            gs.killUnit(splashTarget);
                        } else {
                            splashTarget.setCurrentHP(splashTarget.getCurrentHP() - splashResult);
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Calculates the damage dealt by the attacker and by the defender.
     *
     * @param gs - current game state
     * @return Pair, where first element is the attack power (attackResult) and second is defence power (defenceResult)
     */
    public static Pair<Integer, Integer> getAttackResults(Unit attacker, Unit target, GameState gs) {
        Vector2d targetPos = target.getPosition();
        Tribe targetTribe = gs.getTribe(target.getTribeId());

        double attackForce = attacker.ATK * ((double) attacker.getCurrentHP() / attacker.getMaxHP());
        double defenceForce = target.DEF * ((double) target.getCurrentHP() / target.getMaxHP());
        double accelerator = TribesConfig.ATTACK_MODIFIER;

        // Defence bonuses:
        //  - DefenceForce x TribesConfig.DEFENCE_IN_WALLS if defender within city walls.
        //  - DefenceForce x TribesConfig.DEFENCE_BONUS if defender:
        //     * in city tile, with no walls, and unit has the Fortify ability.
        //     * in water tile and Aquatism is researched.
        //     * in forest tile if Archery is researched.
        //     * in mountain tile if Meditation is researched.

        Types.TERRAIN targetTerrain = gs.getBoard().getTerrainAt(targetPos.x, targetPos.y);
        if (targetTerrain == CITY) {
            int cityID = gs.getBoard().getCityIdAt(targetPos.x, targetPos.y);
            if (targetTribe.controlsCity(cityID)) {
                City c = (City) gs.getActor(cityID);
                if (c.hasWalls())
                    defenceForce *= TribesConfig.DEFENCE_IN_WALLS;
                else if (target.getType().canFortify())
                    defenceForce *= TribesConfig.DEFENCE_BONUS;
            }
        } else if (targetTerrain == MOUNTAIN && targetTribe.getTechTree().isResearched(MEDITATION) ||
                (targetTerrain.isWater() && targetTribe.getTechTree().isResearched(AQUATISM)) ||
                (targetTerrain == FOREST && targetTribe.getTechTree().isResearched(ARCHERY))) {
            defenceForce *= TribesConfig.DEFENCE_BONUS;
        }

        double totalDamage = attackForce + defenceForce;

        int attackResult = (int) Math.round((attackForce / totalDamage) * attacker.ATK * accelerator);
        int defenceResult = (int) Math.round((defenceForce / totalDamage) * target.DEF * accelerator);

        return new Pair<>(attackResult, defenceResult);
    }

    public Pair<Integer, Integer> getAttackResults(Attack action, GameState gs) {
        Unit attacker = (Unit) gs.getActor(action.getUnitId());
        Unit target = (Unit) gs.getActor(action.getTargetId());
        return getAttackResults(attacker, target, gs);
    }

    public boolean isRetaliation(Attack action, GameState gs) {
        if (action.isFeasible(gs)) {
            Unit attacker = (Unit) gs.getActor(action.getUnitId());
            Unit target = (Unit) gs.getActor(action.getTargetId());
            int attackResult = getAttackResults(action, gs).getFirst();
            if (target.getCurrentHP() > attackResult) {
                double distance = Vector2d.chebychevDistance(attacker.getPosition(), target.getPosition());
                return distance <= target.RANGE;
            }
        }
        return false;
    }
}
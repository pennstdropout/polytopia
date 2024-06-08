package core.actors.units;

import core.Types;
import utils.Vector2d;

import static core.TribesConfig.*;

public class Scout extends Unit
{
    private Types.UNIT baseLandUnit;

    public Scout(Vector2d pos, int kills, boolean isVeteran, int cityId, int tribeId) {
        super(SCOUT_ATTACK, SCOUT_DEFENCE, SCOUT_MOVEMENT, -1, SCOUT_RANGE, SCOUT_COST, pos, kills, isVeteran, cityId, tribeId);
    }

    public Types.UNIT getBaseLandUnit() {
        return baseLandUnit;
    }

    public void setBaseLandUnit(Types.UNIT baseLandUnit) {
        this.baseLandUnit = baseLandUnit;
    }

    @Override
    public Types.UNIT getType() {
        return Types.UNIT.SCOUT;
    }

    @Override
    public Scout copy(boolean hideInfo) {
        Scout c = new Scout(getPosition(), getKills(), isVeteran(), getCityId(), getTribeId());
        c.setCurrentHP(getCurrentHP());
        c.setMaxHP(getMaxHP());
        c.setActorId(getActorId());
        c.setStatus(getStatus());
        c.setBaseLandUnit(getBaseLandUnit());
        return hideInfo ? (Scout) c.hide() : c;
    }
}
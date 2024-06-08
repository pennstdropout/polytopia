package core.actors.units;

import core.Types;
import utils.Vector2d;

import static core.TribesConfig.*;

public class Rammer extends Unit
{
    private Types.UNIT baseLandUnit;

    public Rammer(Vector2d pos, int kills, boolean isVeteran, int cityId, int tribeId) {
        super(RAMMER_ATTACK, RAMMER_DEFENCE, RAMMER_MOVEMENT, -1, RAMMER_RANGE, RAMMER_COST, pos, kills, isVeteran, cityId, tribeId);
    }

    public Types.UNIT getBaseLandUnit() {
        return baseLandUnit;
    }

    public void setBaseLandUnit(Types.UNIT baseLandUnit) {
        this.baseLandUnit = baseLandUnit;
    }

    @Override
    public Types.UNIT getType() {
        return Types.UNIT.RAMMER;
    }

    @Override
    public Rammer copy(boolean hideInfo) {
        Rammer c = new Rammer(getPosition(), getKills(), isVeteran(), getCityId(), getTribeId());
        c.setCurrentHP(getCurrentHP());
        c.setMaxHP(getMaxHP());
        c.setActorId(getActorId());
        c.setStatus(getStatus());
        c.setBaseLandUnit(getBaseLandUnit());
        return hideInfo ? (Rammer) c.hide() : c;
    }
}
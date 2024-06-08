package core.actors.units;

import core.Types;
import utils.Vector2d;

import static core.TribesConfig.*;

public class Bomber extends Unit
{
    private Types.UNIT baseLandUnit;

    public Bomber(Vector2d pos, int kills, boolean isVeteran, int cityId, int tribeId) {
        super(BOMBER_ATTACK, BOMBER_DEFENCE, BOMBER_MOVEMENT, -1, BOMBER_RANGE, BOMBER_COST, pos, kills, isVeteran, cityId, tribeId);
    }

    public Types.UNIT getBaseLandUnit() {
        return baseLandUnit;
    }

    public void setBaseLandUnit(Types.UNIT baseLandUnit) {
        this.baseLandUnit = baseLandUnit;
    }

    @Override
    public Types.UNIT getType() {
        return Types.UNIT.BOMBER;
    }

    @Override
    public Bomber copy(boolean hideInfo) {
        Bomber c = new Bomber(getPosition(), getKills(), isVeteran(), getCityId(), getTribeId());
        c.setCurrentHP(getCurrentHP());
        c.setMaxHP(getMaxHP());
        c.setActorId(getActorId());
        c.setStatus(getStatus());
        c.setBaseLandUnit(getBaseLandUnit());
        return hideInfo ? (Bomber) c.hide() : c;
    }
}
package core.actors.units;

import core.Types;
import utils.Vector2d;

import static core.TribesConfig.*;

public class Pirate extends Unit
{
    private Types.UNIT baseLandUnit;

    public Pirate(Vector2d pos, int kills, boolean isVeteran, int cityId, int tribeId) {
        super(DAGGER_ATTACK, DAGGER_DEFENCE, DAGGER_MOVEMENT, DAGGER_MAX_HP, DAGGER_RANGE, DAGGER_COST, pos, kills, isVeteran, cityId, tribeId);
    }

    public Types.UNIT getBaseLandUnit() {
        return baseLandUnit;
    }

    public void setBaseLandUnit(Types.UNIT baseLandUnit) {
        this.baseLandUnit = baseLandUnit;
    }

    @Override
    public Types.UNIT getType() {
        return Types.UNIT.PIRATE;
    }

    @Override
    public Pirate copy(boolean hideInfo) {
        Pirate c = new Pirate(getPosition(), getKills(), isVeteran(), getCityId(), getTribeId());
        c.setCurrentHP(getCurrentHP());
        c.setMaxHP(getMaxHP());
        c.setActorId(getActorId());
        c.setStatus(getStatus());
        c.setBaseLandUnit(getBaseLandUnit());
        return hideInfo ? (Pirate) c.hide() : c;
    }
}

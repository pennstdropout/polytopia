package core.actors.units;

import core.Types;
import utils.Vector2d;

import static core.TribesConfig.*;

public class Juggernaut extends Unit
{
    private Types.UNIT baseLandUnit;

    public Juggernaut(Vector2d pos, int kills, boolean isVeteran, int cityId, int tribeId) {
        super(JUGGERNAUT_ATTACK, JUGGERNAUT_DEFENCE, JUGGERNAUT_MOVEMENT, -1, JUGGERNAUT_RANGE, JUGGERNAUT_COST, pos, kills, isVeteran, cityId, tribeId);
    }

    public Types.UNIT getBaseLandUnit() {
        return baseLandUnit;
    }

    public void setBaseLandUnit(Types.UNIT baseLandUnit) {
        this.baseLandUnit = baseLandUnit;
    }

    @Override
    public Types.UNIT getType() {
        return Types.UNIT.JUGGERNAUT;
    }

    @Override
    public Juggernaut copy(boolean hideInfo) {
        Juggernaut c = new Juggernaut(getPosition(), getKills(), isVeteran(), getCityId(), getTribeId());
        c.setCurrentHP(getCurrentHP());
        c.setMaxHP(getMaxHP());
        c.setActorId(getActorId());
        c.setStatus(getStatus());
        c.setBaseLandUnit(getBaseLandUnit());
        return hideInfo ? (Juggernaut) c.hide() : c;
    }
}

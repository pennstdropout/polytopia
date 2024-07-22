package core.actors.units;

import core.Types;
import utils.Vector2d;

import static core.TribesConfig.*;

public class Dingy extends Unit
{
    private Types.UNIT baseLandUnit;
    private boolean isVisible;

    public Dingy(Vector2d pos, int kills, boolean isVeteran, int cityId, int tribeId) {
        super(CLOAK_ATTACK, CLOAK_DEFENCE, CLOAK_MOVEMENT, CLOAK_MAX_HP, CLOAK_RANGE, CLOAK_COST, pos, kills, isVeteran, cityId, tribeId);
        isVisible = false;
    }

    public Types.UNIT getBaseLandUnit() {
        return baseLandUnit;
    }

    public void setBaseLandUnit(Types.UNIT baseLandUnit) {
        this.baseLandUnit = baseLandUnit;
    }

    public boolean getVisibility() {
        return isVisible;
    }

    public void setVisibility(boolean b) {
        isVisible = b;
    }

    @Override
    public Types.UNIT getType() {
        return Types.UNIT.DINGY;
    }

    @Override
    public Dingy copy(boolean hideInfo) {
        Dingy c = new Dingy(getPosition(), getKills(), isVeteran(), getCityId(), getTribeId());
        c.setVisibility(getVisibility());
        c.setCurrentHP(getCurrentHP());
        c.setMaxHP(getMaxHP());
        c.setActorId(getActorId());
        c.setStatus(getStatus());
        c.setBaseLandUnit(getBaseLandUnit());
        return hideInfo ? (Dingy) c.hide() : c;
    }
}

package players.portfolio.scripts;

import core.actions.Action;
import core.actors.Actor;
import core.game.GameState;
import players.portfolio.scripts.utils.BuildingFunc;
import utils.Pair;

import java.util.Random;

import static core.Types.BUILDING.*;

public class ClearForestForCustomScr extends BaseScript {

    //This script returns the Clear Forest action that makes room for a custom house.

    private Random rnd;

    public ClearForestForCustomScr(Random rnd)
    {
        this.rnd = rnd;
    }

    @Override
    public Pair<Action, Double> process(GameState gs, Actor ac)
    {
        return new BuildingFunc().buildSupportBuilding(MARKET, gs, actions, rnd);
    }
}

package core;

public class TribesConfig
{
    /* UNITS */

    // Warrior
    public static final double WARRIOR_ATTACK = 2;
    public static final double WARRIOR_DEFENCE = 2;
    public static final int WARRIOR_MOVEMENT = 1;
    public static final int WARRIOR_MAX_HP = 10;
    public static final int WARRIOR_RANGE = 1;
    public static final int WARRIOR_COST = 2;
    static final int WARRIOR_POINTS = 10;

    // Archer
    public static final double ARCHER_ATTACK = 2;
    public static final double ARCHER_DEFENCE = 1;
    public static final int ARCHER_MOVEMENT = 1;
    public static final int ARCHER_MAX_HP = 10;
    public static final int ARCHER_RANGE = 2;
    public static final int ARCHER_COST = 3;
    static final int ARCHER_POINTS = 15;

    // Catapult
    public static final double CATAPULT_ATTACK = 4;
    public static final double CATAPULT_DEFENCE = 0;
    public static final int CATAPULT_MOVEMENT = 1;
    public static final int CATAPULT_MAX_HP = 10;
    public static final int CATAPULT_RANGE = 3;
    public static final int CATAPULT_COST = 8;
    static final int CATAPULT_POINTS = 40;

    // Swordsman
    public static final double SWORDMAN_ATTACK = 3;
    public static final double SWORDMAN_DEFENCE = 3;
    public static final int SWORDMAN_MOVEMENT = 1;
    public static final int SWORDMAN_MAX_HP = 15;
    public static final int SWORDMAN_RANGE = 1;
    public static final int SWORDMAN_COST = 5;
    static final int SWORDMAN_POINTS = 25;

    // MindBender
    public static final double MINDBENDER_ATTACK = 0;
    public static final double MINDBENDER_DEFENCE = 1;
    public static final int MINDBENDER_MOVEMENT = 1;
    public static final int MINDBENDER_MAX_HP = 10;
    public static final int MINDBENDER_RANGE = 1;
    public static final int MINDBENDER_COST = 5;
    public static final int MINDBENDER_HEAL = 4;
    static final int MINDBENDER_POINTS = 25;

    // Defender
    public static final double DEFENDER_ATTACK = 1;
    public static final double DEFENDER_DEFENCE = 3;
    public static final int DEFENDER_MOVEMENT = 1;
    public static final int DEFENDER_MAX_HP = 15;
    public static final int DEFENDER_RANGE = 1;
    public static final int DEFENDER_COST = 3;
    static final int DEFENDER_POINTS = 15;

    // Knight
    public static final double KNIGHT_ATTACK = 4;
    public static final double KNIGHT_DEFENCE = 1;
    public static final int KNIGHT_MOVEMENT = 3;
    public static final int KNIGHT_MAX_HP = 15;
    public static final int KNIGHT_RANGE = 1;
    public static final int KNIGHT_COST = 8;
    static final int KNIGHT_POINTS = 40;

    // Rider
    public static final double RIDER_ATTACK = 2;
    public static final double RIDER_DEFENCE = 1;
    public static final int RIDER_MOVEMENT = 2;
    public static final int RIDER_MAX_HP = 10;
    public static final int RIDER_RANGE = 1;
    public static final int RIDER_COST = 3;
    static final int RIDER_POINTS = 15;

    // Cloak
    public static final double CLOAK_ATTACK = 2;
    public static final double CLOAK_DEFENCE = 0.5;
    public static final int CLOAK_MOVEMENT = 2;
    public static final int CLOAK_MAX_HP = 5;
    public static final int CLOAK_RANGE = 1;
    public static final int CLOAK_COST = 0; //TODO: revert testing config
    static final int CLOAK_POINTS = 15;

    // Dagger
    public static final double DAGGER_ATTACK = 2;
    public static final double DAGGER_DEFENCE = 2;
    public static final int DAGGER_MOVEMENT = 1;
    public static final int DAGGER_MAX_HP = 10;
    public static final int DAGGER_RANGE = 1;
    public static final int DAGGER_COST = 3;
    static final int DAGGER_POINTS = 0;

    // Raft
    public static final double RAFT_ATTACK = 0;
    public static final double RAFT_DEFENCE = 1;
    public static final int RAFT_MOVEMENT = 2;
    public static final int RAFT_RANGE = 2;
    public static final int RAFT_COST = 0;
    static final int RAFT_POINTS = 0;

    // Rammer
    public static final double RAMMER_ATTACK = 3;
    public static final double RAMMER_DEFENCE = 3;
    public static final int RAMMER_MOVEMENT = 3;
    public static final int RAMMER_RANGE = 1;
    public static final int RAMMER_COST = 5;
    static final int RAMMER_POINTS = 0;

    // Scout
    public static final double SCOUT_ATTACK = 2;
    public static final double SCOUT_DEFENCE = 1;
    public static final int SCOUT_MOVEMENT = 3;
    public static final int SCOUT_RANGE = 2;
    public static final int SCOUT_COST = 0;
    static final int SCOUT_POINTS = 0;

    // Bomber
    public static final double BOMBER_ATTACK = 3;
    public static final double BOMBER_DEFENCE = 2;
    public static final int BOMBER_MOVEMENT = 2;
    public static final int BOMBER_RANGE = 3;
    public static final int BOMBER_COST = 0;
    static final int BOMBER_POINTS = 0;

    // Superunit
    public static final double SUPERUNIT_ATTACK = 5;
    public static final double SUPERUNIT_DEFENCE = 4;
    public static final int SUPERUNIT_MOVEMENT = 1;
    public static final int SUPERUNIT_MAX_HP = 40;
    public static final int SUPERUNIT_RANGE = 1;
    public static final int SUPERUNIT_COST = 10; //Useful for when unit is disbanded.
    static final int SUPERUNIT_POINTS = 50;

    // Explorer
    public static final double NUM_STEPS = 15;

    // General Unit constants
    public static final double ATTACK_MODIFIER = 4.5;
    public static final double DEFENCE_BONUS = 1.5;
    public static final double DEFENCE_IN_WALLS = 4.0;
    public static final int VETERAN_KILLS = 3;
    public static final int VETERAN_PLUS_HP = 5;
    public static final int RECOVER_PLUS_HP = 2;
    public static final int RECOVER_IN_BORDERS_PLUS_HP = 2;

    /* BUILDINGS */

    // Farm
    static final int FARM_COST = 5;
    static final int FARM_BONUS = 2;
    static final Types.RESOURCE FARM_RES_CONSTRAINT = Types.RESOURCE.CROPS;

    // WindMill
    static final int WIND_MILL_COST = 5;
    static final int WIND_MILL_BONUS = 1;

    // LumberHut
    static final int LUMBER_HUT_COST = 2;
    static final int LUMBER_HUT_BONUS = 1;

    // SawMill
    static final int SAW_MILL_COST = 5;
    static final int SAW_MILL_BONUS = 1;

    // Mine
    static final int MINE_COST = 5;
    static final int MINE_BONUS = 2;
    static final Types.RESOURCE MINE_RES_CONSTRAINT = Types.RESOURCE.ORE;

    // Forge
    static final int FORGE_COST = 5;
    static final int FORGE_BONUS = 2;

    // Port
    static final int PORT_COST = 0;
    static final int PORT_BONUS = 1;
    public static final int PORT_TRADE_DISTANCE = 4; //Count includes destination port.

    // Market
    static final int MARKET_COST = 5;
    static final int MARKET_BONUS = 2;

    // Monuments
    static final int MONUMENT_BONUS = 3;
    public static final int MONUMENT_POINTS = 400;
    public static final int EMPERORS_TOMB_STARS = 100;
    public static final int GATE_OF_POWER_KILLS = 10;
    public static final int GRAND_BAZAR_CITIES = 5;
    public static final int ALTAR_OF_PEACE_TURNS = 5;
    public static final int PARK_OF_FORTUNE_LEVEL = 5;


    // Temple
    static final int TEMPLE_COST = 20;
    static final int TEMPLE_FOREST_COST = 15;
    static final int TEMPLE_BONUS = 1;
    public static final int TEMPLE_TURNS_TO_SCORE = 3;
    public static final int[] TEMPLE_POINTS = new int[]{100, 50, 50, 50, 150};

    // Resources
    static final int ANIMAL_COST = 2;
    static final int FISH_COST = 2;
    static final int STAR_COST = 0;
    static final int FRUIT_COST = 2;
    static final int ANIMAL_POP = 1;
    static final int FISH_POP = 1;
    static final int STAR_STARS = 8;
    static final int FRUIT_POP = 1;

    // ROAD
    public static final int ROAD_COST = 3;

    // City
    public static final int CITY_LEVEL_UP_WORKSHOP_PROD = 1;
    public static final int CITY_LEVEL_UP_RESOURCES = 5;
    public static final int CITY_LEVEL_UP_POP_GROWTH = 3;
    public static final int CITY_LEVEL_UP_PARK = 250;
    public static final int CITY_BORDER_POINTS = 20;
    public static final int CITY_CENTRE_POINTS = 100;
    public static final int PROD_CAPITAL_BONUS = 1;
    public static final int EXPLORER_CLEAR_RANGE = 1;
    public static final int FIRST_CITY_CLEAR_RANGE = 2;
    public static final int NEW_CITY_CLEAR_RANGE = 1;
    public static final int CITY_EXPANSION_TILES = 1;
    public static final int POINTS_PER_POPULATION = 5;

    // Diplomacy
    public static final int ALLEGIANCE_MAX = 60;
    public static final int ATTACK_REPERCUSSION = -5;
    public static final int CAPTURE_REPERCUSSION = -30;
    public static final int CONVERT_REPERCUSSION = -5;
    public static final int MIN_STARS_SEND = 15;

    // Research
    public static final int TECH_BASE_COST = 4;
    public static final Types.TECHNOLOGY TECH_DISCOUNT = Types.TECHNOLOGY.PHILOSOPHY;
    public static final double TECH_DISCOUNT_VALUE = 0.2;
    public static final int TECH_TIER_POINTS = 100;

    /* TRIBES */
    public static final int INITIAL_STARS = 5;//1000;

    /* ACTIONS */
    public static final int CLEAR_FOREST_STAR = 1;
    public static final int GROW_FOREST_COST = 5;
    public static final int BURN_FOREST_COST = 5;
    public static final int CLEAR_VIEW_POINTS = 5;

    // MAP
    public static final int[] DEFAULT_MAP_SIZE = new int[]{-1, 11, 14, 16, 18, 20, 22, 24};

}

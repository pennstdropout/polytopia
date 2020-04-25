package core.actors;

import core.TribesConfig;
import core.Types;
import core.game.Board;
import core.game.GameState;
import org.json.JSONArray;
import org.json.JSONObject;
import utils.Vector2d;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedList;

public class City extends Actor{

    private int level;
    private int population = 0;
    private int population_need;
    private boolean isCapital;
    private int production = 0;
    private boolean hasWalls = false;
    private int bound;
    private int pointsWorth;

    private ArrayList<Integer> unitsID = new ArrayList<>();
    private LinkedList<Building> buildings = new LinkedList<>();

    // The constructor to initial the valley
    public City(int x, int y, int tribeId) {
        this.position = new Vector2d(x,y);
        this.tribeId = tribeId;
        population_need = 2; //level 1 requires population_need = 2
        bound = 1; //cities start with 1 tile around it for territory
        level = 1; //and starting level is 1
        isCapital = false;
    }

    public City(JSONObject obj, int cityID){
        level = obj.getInt("level");
        population = obj.getInt("population");
        population_need = obj.getInt("population_need");
        isCapital = obj.getBoolean("isCapital");
        production = obj.getInt("production");
        hasWalls = obj.getBoolean("hasWalls");
        bound = obj.getInt("bound");
        this.position = new Vector2d(obj.getInt("x"),obj.getInt("y"));
        this.tribeId = obj.getInt("tribeID");
        pointsWorth = obj.getInt("pointsWorth");
        JSONArray jUnits = obj.getJSONArray("units");
        for (int i=0; i<jUnits.length(); i++){
            unitsID.add(jUnits.getInt(i));
        }

        JSONArray jBuildings = obj.getJSONArray("buildings");
        for (int i=0; i<jBuildings.length(); i++){
            JSONObject buildingINFO = jBuildings.getJSONObject(i);
            Types.BUILDING type = Types.BUILDING.getTypeByKey(buildingINFO.getInt("type"));
            if (type.isTemple()){
                buildings.add(new Temple(buildingINFO, type, cityID));
            }else{
                buildings.add(new Building(buildingINFO, type, cityID));
            }
        }


    }

    // Increase population
    public void addPopulation(Tribe tribe, int value){

        //-level is a maximum negative value.
        if(population + value < -level)
            value = - level - population;

        population += value;
        tribe.addScore(value * TribesConfig.POINTS_PER_POPULATION);
        addPointsWorth(value * TribesConfig.POINTS_PER_POPULATION);
    }

    public void addProduction(int prod) {
        production += prod;
        if(production < 0) production = 0;
    }

    public void addBuilding(GameState gameState, Building building)
    {
        updateBuildingEffects(gameState, building, false, false);
        buildings.add(building);
    }

    public void removeBuilding(GameState gameState, Building building)
    {
        updateBuildingEffects(gameState, building, true, false);
        buildings.remove(building);
    }

    public void updateBuildingEffects(GameState gameState, Building building, boolean negative, boolean onlyMatching)
    {
        int multiplier = negative ? -1 : 1;
        Tribe tribe = gameState.getTribe(this.tribeId);
        switch (building.type) {
            case FARM:
            case LUMBER_HUT:
            case MINE:
            case WINDMILL:
            case SAWMILL:
            case FORGE:
                changeBonus(gameState, building, true, onlyMatching, multiplier);
                break;
            case PORT:
                if(!onlyMatching) addPopulation(tribe, building.type.getBonus() * multiplier);
                changeBonus(gameState, building, false, onlyMatching, multiplier);
                break;
            case CUSTOM_HOUSE:
                changeBonus(gameState, building, false, onlyMatching, multiplier);
                break;
            case TEMPLE:
            case WATER_TEMPLE:
            case MOUNTAIN_TEMPLE:
            case FOREST_TEMPLE:
                if(!onlyMatching)
                {
                    addPopulation(tribe, building.type.getBonus() * multiplier);
                }
                int scoreDiff = negative ? ((Temple)building).getPoints() : TribesConfig.TEMPLE_POINTS[0];
                tribe.addScore(scoreDiff);
                break;
            case ALTAR_OF_PEACE:
            case EMPERORS_TOMB:
            case EYE_OF_GOD:
            case GATE_OF_POWER:
            case PARK_OF_FORTUNE:
            case TOWER_OF_WISDOM:
            case GRAND_BAZAR:
                if(!onlyMatching) addPopulation(tribe,building.type.getBonus() * multiplier);
                tribe.addScore(TribesConfig.MONUMENT_POINTS * multiplier);
                break;
        }
    }


    private void changeBonus(GameState gameState, Building building, boolean isPopulation, boolean onlyMatching, int multiplier){

        int bonusToAdd;
        boolean isBase = building.type.isBase();
        City cityToAddTo = this;
        Board board = gameState.getBoard();
        Tribe tribe = gameState.getTribe(this.tribeId);

        //Population added by the base building.
        if(isBase && isPopulation && !onlyMatching) addPopulation(tribe, multiplier * building.getBonus());

        //Check all buildings next to the new building position.
        for(Vector2d adjPosition : building.position.neighborhood(1, 0, board.getSize()))
        {
            //For each position, if there's a building and of the production matching point
            Types.BUILDING b = board.getBuildingAt(adjPosition.x, adjPosition.y);
            if(b != null && building.type.getMatchingBuilding() == b)
            {
                //Retrieve this building, which could be form this city or from another one from the tribe.
                Building existingBuilding = null;
                int cityId = board.getCityIdAt(adjPosition.x, adjPosition.y);
                if(cityId == actorId)
                {
                    //the matching building belongs to this city
                    existingBuilding = this.getBuilding(adjPosition.x, adjPosition.y);
                }else if(tribe.controlsCity(cityId)) {
                    //the matching building belongs to a city from a different tribe
                    City city = (City) gameState.getActor(cityId);
                    existingBuilding = city.getBuilding(adjPosition.x, adjPosition.y);
                    cityToAddTo = city;

                }else return; //This may happen if the building belongs to a city from another tribe.

                bonusToAdd = isBase ? existingBuilding.getBonus() : building.getBonus();

                if(isPopulation)
                    cityToAddTo.addPopulation(tribe, bonusToAdd * multiplier);
                else
                    cityToAddTo.addProduction(bonusToAdd * multiplier);

            }
        }
    }

    public int getProduction(){
        if(population >= 0) {
            int capitalBonus = isCapital ? TribesConfig.PROD_CAPITAL_BONUS : 0;
            return level + production + capitalBonus;
        }
        return population;
    }

    public boolean canLevelUp()
    {
        return population >= population_need;
    }

    // Level up
    public void levelUp(){
        level++;
        population = population - population_need;
        population_need = (level+1);
    }

    public boolean addUnit(int id){
        if (unitsID.size() < (level+1)){
            unitsID.add(id);
            return true;
        }
        return false;
    }

    public boolean canAddUnit(){
        return unitsID.size() < (level+1);
    }

    public void removeUnit(int id){
        for(int i=0; i<unitsID.size(); i++){
            if (unitsID.get(i) == id){
                unitsID.remove(i);
                return;
            }
        }
        System.out.println("Error!! Unit ID "+ id +" does not belong to this city");
    }

    public Integer removeUnitByIndex(int index){
        return unitsID.remove(index);
    }

    public int getLevel() {
        return level;
    }


    public int getPopulation() {
        return population;
    }

    public boolean isCapital() {
        return isCapital;
    }

    public void setCapital(boolean isCapital)
    {
        this.isCapital = isCapital;
    }

    public int getPopulation_need() {
        return population_need;
    }

    public void setUnitsID(ArrayList<Integer> unitsID) {
        this.unitsID = unitsID;
    }

    public void setBuildings(LinkedList<Building> buildings) {
        this.buildings = buildings;
    }

    public LinkedList<Building> copyBuildings() {
        LinkedList<Building> copyList = new LinkedList<>();
        for(Building building : buildings) {
            copyList.add(building.copy());
        }
        return copyList;
    }


    public City copy(boolean hideInfo){
        City c = new City(position.x, position.y, tribeId);
        c.level = level;
        c.population = hideInfo ? 0 : population;
        c.population_need = population_need;
        c.isCapital = isCapital;
        c.production = hideInfo ? 0 : production;
        c.hasWalls = hasWalls;
        c.bound = bound;
        c.actorId = actorId;
        c.setBuildings(copyBuildings());
        c.setUnitsID(hideInfo ? new ArrayList<>() : new ArrayList<>(unitsID));
        return c;
    }

    public void setWalls(boolean walls)
    {
        hasWalls = walls;
    }

    public boolean hasWalls()
    {
        return hasWalls;
    }

    public int getBound(){
        return this.bound;
    }
    public void setBound(int b){
        this.bound = b;
    }

    public ArrayList<Integer> getUnitsID() {
        return unitsID;
    }

    public int getNumUnits()
    {
        return unitsID.size();
    }

    public Building getBuilding(int x, int y){
        for(Building building :buildings){
            if (building.position.x == x && building.position.y == y){
                return building;
            }
        }
        return null;
    }

    public void subtractProduction(int production){
        if (this.production < production){
            System.out.println("Error in subtract Production!!! -> Destroy Custom House");
        }
        this.production -= production;
    }

    public void setTribeId(int tribeId) {
        this.tribeId = tribeId;
    }

    public LinkedList<Building> getBuildings() {
        return buildings;
    }

    public void addPointsWorth(int points){
        pointsWorth +=points;
    }

    public void subtractPointsWorth(int points){
        pointsWorth -=points;
    }
    public int getPointsWorth(){
        return pointsWorth;
    }

    public void setPopulation(int popValue) {
        this.population = popValue;
    }

    public void setProduction(int prodValue) {
        this.production = prodValue;
    }


    public void clearUnits() {
        unitsID.clear();
    }
}

/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2024 Ilya Zushinskiy
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.game;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.AI;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.model.events.RandomEvent;
import rotp.model.galaxy.GalaxyEllipticalShape;
import rotp.model.galaxy.GalaxyRectangularShape;
import rotp.model.galaxy.GalaxyShape;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.StarType;
import rotp.model.planet.Planet;
import rotp.model.planet.PlanetType;
import rotp.ui.game.SetupGalaxyUI;
import rotp.util.Base;

public class MOO1GameOptions implements Base, IGameOptions, Serializable {
    private static final long serialVersionUID = 1L;
    
    /** Set to a constant from AI class to enable the AI to play as the player */
    private final Integer autoPlayAIType = null;
    
    private final String[] opponentRaces = new String[MAX_OPPONENTS];
    private final List<Integer> colors = new ArrayList<>();
    private final List<Color> empireColors = new ArrayList<>();
    private final NewPlayer player = new NewPlayer();

    private String selectedGalaxySize;
    private String selectedGalaxyShape;
    private String selectedGalaxyShapeOption1;
    private String selectedGalaxyShapeOption2;
    
    private String selectedGameDifficulty;
    private String selectedRandomEventOption;
    private int selectedNumberOpponents;
    private String selectedOpponentAIOption;
    private final String[] specificOpponentAIOption = new String[MAX_OPPONENTS+1];
    
    private transient GalaxyShape galaxyShape;

    public MOO1GameOptions() {
        randomizeColors();
        setDefaultOptionValues();
    }
    private void resetSelectedOpponentRaces() {
        for (int i=0;i<opponentRaces.length;i++)
            selectedOpponentRace(i,null);
    }
    @Override
    public int numPlayers()                      { return 1; }
    @Override
    public int numColors()                       { return 10; }
    @Override
    public NewPlayer selectedPlayer()            { return player; }
    @Override
    public String selectedGalaxySize()           { return selectedGalaxySize; }
    @Override
    public void selectedGalaxySize(String s)     {
        int prevNumOpp = defaultOpponentsOptions();
        selectedGalaxySize = s;
        if (selectedNumberOpponents() == prevNumOpp)
            selectedNumberOpponents(defaultOpponentsOptions());
        generateGalaxy();
    }
    @Override
    public String selectedGalaxyShape()          { return selectedGalaxyShape; }
    @Override
    public void selectedGalaxyShape(String s)    { selectedGalaxyShape = s; setGalaxyShape(); generateGalaxy(); }
    @Override
    public String selectedGalaxyShapeOption1()       { return selectedGalaxyShapeOption1; }
    @Override
    public void selectedGalaxyShapeOption1(String s) { selectedGalaxyShapeOption1 = s; }
    @Override
    public String selectedGalaxyShapeOption2()       { return selectedGalaxyShapeOption2; }
    @Override
    public void selectedGalaxyShapeOption2(String s) { selectedGalaxyShapeOption2 = s; }
    @Override
    public String selectedGameDifficulty()       { return selectedGameDifficulty; }
    @Override
    public void selectedGameDifficulty(String s) { selectedGameDifficulty = s; }
    @Override
    public String selectedRandomEventOption()       { return selectedRandomEventOption == null ? RANDOM_EVENTS_ON : selectedRandomEventOption; }
    @Override
    public void selectedRandomEventOption(String s) { selectedRandomEventOption = s; }
    @Override
    public String selectedOpponentAIOption()       { return selectedOpponentAIOption == null ? OPPONENT_AI_BASE : selectedOpponentAIOption; }
    @Override
    public void selectedOpponentAIOption(String s) { selectedOpponentAIOption = s; }
    @Override
    public String specificOpponentAIOption(int n)  {
            if ((specificOpponentAIOption == null) || (specificOpponentAIOption.length < n))
                return selectedOpponentAIOption();
            else
                return specificOpponentAIOption[n];
    }
    @Override
    public void specificOpponentAIOption(String s, int n) {
        if (n < specificOpponentAIOption.length)
            specificOpponentAIOption[n] = s;
    }
    @Override
    public int selectedNumberOpponents()         { return selectedNumberOpponents; }
    @Override
    public void selectedNumberOpponents(int i)   { selectedNumberOpponents = i; generateGalaxy(); }
    @Override
    public String selectedPlayerRace()           { return selectedPlayer().race; }
    @Override
    public void selectedPlayerRace(String s)     { selectedPlayer().race = s;  resetSelectedOpponentRaces(); }
    @Override
    public int selectedPlayerColor()             { return selectedPlayer().color; }
    @Override
    public void selectedPlayerColor(int i)       { selectedPlayer().color = i; }
    @Override
    public String selectedLeaderName()           { return selectedPlayer().leaderName; }
    @Override
    public void selectedLeaderName(String s)     { selectedPlayer().leaderName = s.trim(); }
    @Override
    public String selectedHomeWorldName()        { return selectedPlayer().homeWorldName; }
    @Override
    public void selectedHomeWorldName(String s)  { selectedPlayer().homeWorldName = s.trim(); }
    @Override
    public String[] selectedOpponentRaces()      { return opponentRaces; }
    @Override
    public String selectedOpponentRace(int i)    { return i >= opponentRaces.length ? null : opponentRaces[i]; }
    @Override
    public void selectedOpponentRace(int i, String s) {
        if (i < opponentRaces.length)
            opponentRaces[i] = s;
    }
    @Override
    public int maximumOpponentsOptions() {
        int maxEmpires = min(numberStarSystems()/8, colors.size(), MAX_OPPONENT_TYPE*startingRaceOptions().size());
        int maxOpponents = min(SetupGalaxyUI.MAX_DISPLAY_OPPS);
        return min(maxOpponents, maxEmpires-1);
    }
    @Override
    public int defaultOpponentsOptions() {
        int maxEmpires = min((int)Math.ceil(numberStarSystems()/16f), colors.size(), MAX_OPPONENT_TYPE*startingRaceOptions().size());
        int maxOpponents = min(SetupGalaxyUI.MAX_DISPLAY_OPPS);
        return min(maxOpponents, maxEmpires-1);
    }
    @Override
    public String name()                 { return "SETUP_RULESET_ORION"; }
    @Override
    public void copyOptions(IGameOptions o) {
        if (!(o instanceof MOO1GameOptions))
            return;
        
        // copy only the options that are immediately visible
        // .. not the advanced options
        MOO1GameOptions opt = (MOO1GameOptions) o;
        
        selectedGalaxySize = opt.selectedGalaxySize;
        selectedGalaxyShape = opt.selectedGalaxyShape;
        selectedGalaxyShapeOption1 = opt.selectedGalaxyShapeOption1;
        selectedGalaxyShapeOption2 = opt.selectedGalaxyShapeOption2;
        selectedGameDifficulty = opt.selectedGameDifficulty;
        selectedNumberOpponents = opt.selectedNumberOpponents;

        selectedRandomEventOption = opt.selectedRandomEventOption;
        selectedOpponentAIOption = opt.selectedOpponentAIOption;
        
        if (opt.specificOpponentAIOption != null) {
            for (int i=0;i<specificOpponentAIOption.length;i++)
                specificOpponentAIOption[i] = opt.specificOpponentAIOption[i];
        }
        
        if (opt.player != null)
            player.copy(opt.player);
        
        setGalaxyShape();
        selectedGalaxyShapeOption1 = opt.selectedGalaxyShapeOption1;
        selectedGalaxyShapeOption2 = opt.selectedGalaxyShapeOption2;

        generateGalaxy();
    }
    @Override
    public GalaxyShape galaxyShape()   {
        if (galaxyShape == null)
            setGalaxyShape();
        return galaxyShape;
    }
    private void setGalaxyShape() {
        switch(selectedGalaxyShape) {
            case SHAPE_ELLIPTICAL:
                galaxyShape = new GalaxyEllipticalShape(this); break;
            case SHAPE_RECTANGLE:
            default:
                galaxyShape = new GalaxyRectangularShape(this);
        }
        selectedGalaxyShapeOption1 = galaxyShape.defaultOption1();
        selectedGalaxyShapeOption2 = galaxyShape.defaultOption2();
    }
    @Override
    public int numGalaxyShapeOption1() {  return galaxyShape.numOptions1(); }
    @Override
    public int numGalaxyShapeOption2() {  return galaxyShape.numOptions2(); }
    @Override
    public int numberStarSystems() {
            // MOO Strategy Guide, Table 3-2, p.50
        /*
        switch (selectedGalaxySize()) {
                case SIZE_SMALL:  return 24;
                case SIZE_MEDIUM: return 48;
                case SIZE_LARGE1:  return 70;
                case SIZE_HUGE:   return 108;
                default: return 48;
        }
        */
        switch (selectedGalaxySize()) {
            case SIZE_TINY:       return 33;
            case SIZE_SMALL:      return 50;
            case SIZE_SMALL2:     return 70;
            case SIZE_MEDIUM:     return 100;
            case SIZE_MEDIUM2:    return 150;
            case SIZE_LARGE:      return 225;
            case SIZE_LARGE2:     return 333;
            case SIZE_HUGE:       return 500;
            case SIZE_HUGE2:      return 700;
            case SIZE_MASSIVE:    return 1000;
            case SIZE_MASSIVE2:   return 1500;
            case SIZE_MASSIVE3:   return 2250;
            case SIZE_MASSIVE4:   return 3333;
            case SIZE_MASSIVE5:   return 5000;
            case SIZE_INSANE:     return 10000;
            case SIZE_LUDICROUS:  return 100000;
            case SIZE_MAXIMUM:    return maximumSystems();
        }
        return 8*(selectedNumberOpponents()+1);
    }
    @Override
    public int numberNebula() {
        float freq = 1.0f;
        // MOO Strategy Guide, Table 3-3, p.51
        /*
        switch (selectedGalaxySize()) {
        case SIZE_SMALL:     return roll(0,1);
        case SIZE_MEDIUM:    return roll(1,2);
        case SIZE_LARGE:     return roll(2,3);
        case SIZE_HUGE:      return roll(2,4);
        case SIZE_LUDICROUS: return roll(10,20);
        default: return roll(1,2);
        }
        */
        int nStars = numberStarSystems();
        float sizeMult = nebulaSizeMult();
        int nNeb = (int) nStars/20;
        
        return (int) (freq*nNeb/sizeMult/sizeMult);
    }
    @Override
    public float nebulaSizeMult() {
        int nStars = numberStarSystems();
        if (nStars < 200)
            return 1;
        else
            return min(10,sqrt(nStars/200f));
    }
    @Override
    public boolean isAutoPlay() {
        return autoPlayAIType != null;
    }
    @Override
    public int selectedAI(Empire e) {
        if (e.isPlayer()) {
            if (isAutoPlay()) {
                return autoPlayAIType;
            }
            else {
                return AI.BASE;  // doesn't matter; won't be used if autoplay off
            }
        }
        else {
            switch(selectedOpponentAIOption()) {
                case OPPONENT_AI_BASE:   return AI.BASE;
                case OPPONENT_AI_XILMI:  return AI.XILMI;
                case OPPONENT_AI_SELECTABLE:
                    String specificAI = specificOpponentAIOption(e.id);
                    switch(specificAI) {
                        case OPPONENT_AI_BASE:   return AI.BASE;
                        case OPPONENT_AI_XILMI:  return AI.XILMI;
                    }
            }
        }
        return AI.BASE;
    }
    @Override
    public boolean allowRandomEvent(RandomEvent ev) {
        switch(selectedRandomEventOption()) {
            case RANDOM_EVENTS_ON:  return true;
            case RANDOM_EVENTS_OFF: return false;
            case RANDOM_EVENTS_NO_MONSTERS: return !ev.monsterEvent();
        }
        return true;
    }
    @Override
    public String randomStarType() {
        // pcts represents star type distribution per MOO1 Official Strategy Guide
        //                RED, ORANG, YELL, BLUE,WHITE, PURP
        float[] pcts = { .30f, .55f, .70f, .85f, .95f, 1.0f };
        
        int typeIndex = 0;
        float r = random();
        for (int i=0;i<pcts.length;i++) {
            if (r <= pcts[i]) {
                typeIndex = i;
                break;
            }
        }

        switch(typeIndex) {
            case 0:  return StarType.RED;
            case 1:  return StarType.ORANGE;
            case 2:  return StarType.YELLOW;
            case 3:  return StarType.BLUE;
            case 4:  return StarType.WHITE;
            case 5:  return StarType.PURPLE;
            default: return StarType.RED;
        }
    }
    @Override
    public Planet randomPlanet(StarSystem s) {
        Planet p = new Planet(s);
        String[] planetTypes = { "PLANET_NONE", "PLANET_RADIATED", "PLANET_TOXIC", "PLANET_INFERNO",
                        "PLANET_DEAD", "PLANET_TUNDRA", "PLANET_BARREN", "PLANET_MINIMAL", "PLANET_DESERT",
                        "PLANET_STEPPE", "PLANET_ARID", "PLANET_OCEAN", "PLANET_JUNGLE", "PLANET_TERRAN" };

        float[] pcts;

        float[] redPcts =    { .05f, .10f, .15f, .20f, .25f, .30f, .35f, .40f, .50f, .60f, .75f, .85f, .95f, 1.0f };
        float[] greenPcts =  { .05f, .10f, .15f, .20f, .25f, .30f, .35f, .40f, .45f, .55f, .65f, .75f, .85f, 1.0f };
        float[] yellowPcts = { .00f, .00f, .00f, .05f, .05f, .10f, .15f, .20f, .25f, .30f, .40f, .50f, .60f, 1.0f };
        float[] bluePcts =   { .15f, .25f, .35f, .45f, .55f, .65f, .75f, .85f, .90f, .95f, 1.0f, 1.0f, 1.0f, 1.0f };
        float[] whitePcts =  { .10f, .15f, .25f, .35f, .45f, .55f, .65f, .75f, .85f, .90f, .95f, 1.0f, 1.0f, 1.0f };
        float[] purplePcts = { .20f, .45f, .60f, .75f, .85f, .90f, .95f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

        int typeIndex = 0;
        switch (s.starType().key()) {
            case StarType.RED:    pcts = redPcts;    break;
            case StarType.ORANGE:  pcts = greenPcts;  break;
            case StarType.YELLOW: pcts = yellowPcts; break;
            case StarType.BLUE:   pcts = bluePcts;   break;
            case StarType.WHITE:  pcts = whitePcts; break;
            case StarType.PURPLE: pcts = purplePcts; break;
            default:
                pcts = redPcts; break;
        }

        float r = random();
        for (int i=0;i<pcts.length;i++) {
            if (r <= pcts[i]) {
                typeIndex = i;
                break;
            }
        }
        p.initPlanetType(planetTypes[typeIndex]);

        checkForHostileEnvironment(p, s);

        checkForPoorResources(p, s);
        if (p.isResourceNormal())
            checkForRichResources(p, s);
        if (p.isResourceNormal())
            checkForArtifacts(p, s);
        return p;
    }
    @Override
    public String randomPlayerStarType(Race r)     { return StarType.YELLOW; }
    @Override
    public String randomRaceStarType(Race r)       {
        List<String> types = new ArrayList<>();
        types.add(StarType.RED);
        types.add(StarType.ORANGE);
        types.add(StarType.YELLOW);

        return random(types);
    }
    @Override
    public String randomOrionStarType()       {
        List<String> types = new ArrayList<>();
        types.add(StarType.RED);
        types.add(StarType.ORANGE);
        types.add(StarType.YELLOW);

        return random(types);
    }
    @Override
    public Planet orionPlanet(StarSystem s) {
        Planet p = new Planet(s);
        p.initPlanetType("PLANET_TERRAN");
        return p;
    }
    @Override
    public Planet randomPlayerPlanet(Race r, StarSystem s) {
        Planet p = new Planet(s);
        p.initPlanetType(r.homeworldPlanetType);
        return p;
    }
    @Override
    public List<String> galaxySizeOptions() {
        int max = maximumSystems();
        List<String> list = new ArrayList<>();
        list.add(SIZE_TINY);
        if (max > 50)
            list.add(SIZE_SMALL);
        if (max > 70)
            list.add(SIZE_SMALL2);
        if (max > 100)
            list.add(SIZE_MEDIUM);
        if (max > 150)
            list.add(SIZE_MEDIUM2);
        if (max > 225)
            list.add(SIZE_LARGE);
        if (max > 333)
            list.add(SIZE_LARGE2);
        if (max > 500)
            list.add(SIZE_HUGE);
        if (max > 700)
            list.add(SIZE_HUGE2);
        if (max > 1000)
            list.add(SIZE_MASSIVE);
        if (max > 1500)
            list.add(SIZE_MASSIVE2);
        if (max > 2250)
            list.add(SIZE_MASSIVE3);
        if (max > 3333)
            list.add(SIZE_MASSIVE4);
        if (max > 5000)
            list.add(SIZE_MASSIVE5);
        if (max > 10000)
            list.add(SIZE_INSANE);
        if (max > 100000)
            list.add(SIZE_LUDICROUS);
        list.add(SIZE_MAXIMUM);
        return list;
    }
    @Override
    public List<String> galaxyShapeOptions() {
        List<String> list = new ArrayList<>();
        list.add(SHAPE_RECTANGLE);
        list.add(SHAPE_ELLIPTICAL);
        return list;
    }
    
    @Override
    public List<String> galaxyShapeOptions1() { return galaxyShape.options1(); }
    @Override
    public List<String> galaxyShapeOptions2() { return galaxyShape.options2(); }
    @Override
    public List<String> gameDifficultyOptions() {
        List<String> list = new ArrayList<>();
        list.add(DIFFICULTY_EASIEST);
        list.add(DIFFICULTY_EASIER);
        list.add(DIFFICULTY_EASY);
        list.add(DIFFICULTY_NORMAL);
        list.add(DIFFICULTY_HARD);
        list.add(DIFFICULTY_HARDER);
        list.add(DIFFICULTY_HARDEST);
        return list;
    }
    @Override
    public List<String> randomEventOptions() {
        List<String> list = new ArrayList<>();
        list.add(RANDOM_EVENTS_ON);
        list.add(RANDOM_EVENTS_NO_MONSTERS);
        list.add(RANDOM_EVENTS_OFF);
        return list;
    }
    @Override
    public List<String> opponentAIOptions() {
        List<String> list = new ArrayList<>();
        list.add(OPPONENT_AI_BASE);
        list.add(OPPONENT_AI_XILMI);
        list.add(OPPONENT_AI_SELECTABLE);
        return list;
    }
    @Override
    public List<String> specificOpponentAIOptions() {
        List<String> list = new ArrayList<>();
        list.add(OPPONENT_AI_BASE);
        list.add(OPPONENT_AI_XILMI);
        return list;
    }
    @Override
    public List<String> startingRaceOptions() {
        List<String> list = new ArrayList<>();
        list.add("RACE_HUMAN");
        list.add("RACE_ALKARI");
        list.add("RACE_SILICOID");
        list.add("RACE_MRRSHAN");
        list.add("RACE_KLACKON");
        list.add("RACE_MEKLAR");
        list.add("RACE_PSILON");
        list.add("RACE_DARLOK");
        list.add("RACE_SAKKRA");
        list.add("RACE_BULRATHI");
        return list;
    }
    @Override
    public List<Integer> possibleColors() {
        return new ArrayList<>(colors);
    }
    protected void setDefaultOptionValues() {
        selectedGalaxySize = SIZE_SMALL;
        selectedGalaxyShape = galaxyShapeOptions().get(0);
        selectedNumberOpponents = defaultOpponentsOptions();
        selectedPlayerRace(random(startingRaceOptions()));
        selectedGameDifficulty = DIFFICULTY_EASY;
        selectedOpponentAIOption = OPPONENT_AI_BASE;
        for (int i=0;i<specificOpponentAIOption.length;i++)
            specificOpponentAIOption[i] = OPPONENT_AI_BASE;
        setToDefault();
        generateGalaxy();
    }
    @Override
    public void setToDefault() {
        selectedRandomEventOption = RANDOM_EVENTS_ON;
    }
    private void generateGalaxy() {
        galaxyShape().quickGenerate();
    }
    @Override
    public Color color(int i)  { return empireColors.get(i); }
    @Override
    public void randomizeColors() {
        empireColors.clear();
        empireColors.add(new Color(9,131,214));   // blue
        empireColors.add(new Color(132,57,20));   // brown
        empireColors.add(new Color(0,166,81));    // green
        empireColors.add(new Color(255,127,0));   // orange
        empireColors.add(new Color(247,127,230)); // pink
        empireColors.add(new Color(145,51,188));  // purple
        empireColors.add(new Color(237,28,36));   // red
        empireColors.add(new Color(56,232,186));  // teal
        empireColors.add(new Color(247,229,60));  // yellow
        empireColors.add(new Color(255,255,255)); // white
        colors.clear();
        //primary color list
        List<Integer> list1 = new ArrayList<>();
        list1.add(0);
        list1.add(1);
        list1.add(2);
        list1.add(3);
        list1.add(4);
        list1.add(5);

        //secondary color list
        List<Integer> list1a = new ArrayList<>();
        list1a.add(6);
        list1a.add(7);
        list1a.add(8);
        list1a.add(9);

        // start repeating the 10-color list for copies of races (up to 5 per race)
        List<Integer> list2 = new ArrayList<>(list1);
        list2.addAll(list1a);
        List<Integer> list3 = new ArrayList<>(list2);
        List<Integer> list4 = new ArrayList<>(list2);
        List<Integer> list5 = new ArrayList<>(list2);
            
        Collections.shuffle(list1);
        Collections.shuffle(list1a);
        Collections.shuffle(list2);
        Collections.shuffle(list3);
        Collections.shuffle(list4);
        Collections.shuffle(list5);
        colors.addAll(list1);
        colors.addAll(list1a);
        colors.addAll(list2);
        colors.addAll(list3);
        colors.addAll(list4);
        colors.addAll(list5);
    }
    private void checkForHostileEnvironment(Planet p, StarSystem s) {
        // these planet types and no chance for poor resources -- skip
        switch(p.type().key()) {
            case PlanetType.NONE:
                p.makeEnvironmentNone();
                break;
            case PlanetType.RADIATED:
            case PlanetType.TOXIC:
            case PlanetType.INFERNO:
            case PlanetType.DEAD:
            case PlanetType.TUNDRA:
            case PlanetType.BARREN:
                p.makeEnvironmentHostile();
                break;
            case PlanetType.DESERT:
            case PlanetType.STEPPE:
            case PlanetType.ARID:
            case PlanetType.OCEAN:
            case PlanetType.JUNGLE:
            case PlanetType.TERRAN:
                if (random() < .083333)
                    p.makeEnvironmentFertile();  // become fertile
                break;
        }
    }
    private void checkForPoorResources(Planet p, StarSystem s) {
        // these planet types and no chance for poor resources -- skip
        switch(p.type().key()) {
            case PlanetType.NONE:
            case PlanetType.RADIATED:
            case PlanetType.TOXIC:
            case PlanetType.INFERNO:
            case PlanetType.DEAD:
            case PlanetType.TUNDRA:
            case PlanetType.BARREN:
            case PlanetType.JUNGLE:
            case PlanetType.TERRAN:
                return;
        }

        float r1 = 0;
        float r2 = 0;
        switch(s.starType().key()) {
            case StarType.BLUE:
            case StarType.WHITE:
            case StarType.YELLOW:
                r1 = .025f; r2 = .10f;
                break;
            case StarType.RED:
                r1 = .06f;  r2 = .20f;
                break;
            case StarType.ORANGE:
                r1 = .135f; r2 = .30f;
                break;
            case StarType.PURPLE:
                // can never have poor/ultrapoor
                return;
            default:
                throw new RuntimeException(concat("Invalid star type for options: ", s.starType().key()));
        }
        float r = random();
        if (r <= r1)
            p.setResourceUltraPoor();
        else if (r <= r2)
            p.setResourcePoor();
    }
    private void checkForRichResources(Planet p, StarSystem s) {
        // planet/star ratios per Table 3-9a of Strategy Guide
        float r1 = 0;
        float r2 = 0;
        switch(s.starType().key()) {
            case StarType.RED:
            case StarType.WHITE:
            case StarType.YELLOW:
            case StarType.ORANGE:
                switch(p.type().key()) {
                    case PlanetType.RADIATED:   r1 = .2625f; r2 = .35f; break;
                    case PlanetType.TOXIC:      r1 = .225f;  r2 = .30f; break;
                    case PlanetType.INFERNO:    r1 = .1875f; r2 = .25f; break;
                    case PlanetType.DEAD:       r1 = .15f;   r2 = .20f; break;
                    case PlanetType.TUNDRA:     r1 = .1125f; r2 = .15f; break;
                    case PlanetType.BARREN:     r1 = .075f;  r2 = .10f; break;
                    case PlanetType.MINIMAL:    r1 = .0375f; r2 = .05f; break;
                }
                break;
            case StarType.BLUE:
                switch(p.type().key()) {
                    case PlanetType.RADIATED:   r1 = .2925f; r2 = .45f; break;
                    case PlanetType.TOXIC:      r1 = .26f;   r2 = .40f; break;
                    case PlanetType.INFERNO:    r1 = .2275f; r2 = .35f; break;
                    case PlanetType.DEAD:       r1 = .195f;  r2 = .30f; break;
                    case PlanetType.TUNDRA:     r1 = .1625f; r2 = .25f; break;
                    case PlanetType.BARREN:     r1 = .13f;   r2 = .20f; break;
                    case PlanetType.MINIMAL:    r1 = .0975f; r2 = .15f; break;
                    case PlanetType.DESERT:     r1 = .065f;  r2 = .10f; break;
                    case PlanetType.STEPPE:     r1 = .0325f; r2 = .05f; break;
                }
                break;
            case StarType.PURPLE:
                switch(p.type().key()) {
                    case PlanetType.RADIATED:   r1 = .30f;   r2 = .60f; break;
                    case PlanetType.TOXIC:      r1 = .275f;  r2 = .55f; break;
                    case PlanetType.INFERNO:    r1 = .25f;   r2 = .50f; break;
                    case PlanetType.DEAD:       r1 = .225f;  r2 = .45f; break;
                    case PlanetType.TUNDRA:     r1 = .20f;   r2 = .40f; break;
                    case PlanetType.BARREN:     r1 = .175f;  r2 = .35f; break;
                    case PlanetType.MINIMAL:    r1 = .15f;   r2 = .30f; break;
                }
                break;
            default:
                throw new RuntimeException(concat("Invalid star type for options: ", s.starType().key()));
        }

        float r = random();
        if (r <= r1)
            p.setResourceRich();
        else if (r <= r2)
            p.setResourceUltraRich();
    }
    private void checkForArtifacts(Planet p, StarSystem s) {
        switch(p.type().key()) {
            case PlanetType.STEPPE:
            case PlanetType.ARID:
            case PlanetType.OCEAN:
            case PlanetType.JUNGLE:
            case PlanetType.TERRAN:
                if (random() <= 0.10)
                    p.setArtifact();
        }
    }
}

/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2026 Ilya Zushinskiy
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
package rotp.model.tech;

import java.awt.*;
import java.util.Comparator;
import rotp.model.colony.Colony;
import rotp.model.combat.CombatEntity;
import rotp.model.empires.Empire;
import rotp.model.empires.Race;
import rotp.model.ships.ShipDesign;
import rotp.util.Base;

public class Tech implements Base {
    private static final float miniSlowRate = .97164f;
    private static final float miniFastRate = .933033f;

    public static final String ARMOR = "Armor";
    public static final String ATMOSPHERE_ENRICHMENT = "AtmosphereEnrichment";
    public static final String AUTOMATED_REPAIR = "AutomatedRepair";
    public static final String BATTLE_COMPUTER = "BattleComputer";
    public static final String BATTLE_SUIT = "BattleSuit";
    public static final String BEAM_FOCUS = "BeamFocus";
    public static final String BIOLOGICAL_ANTIDOTE = "BiologicalAntidote";
    public static final String BIOLOGICAL_WEAPON = "BiologicalWeapon";
    public static final String BLACK_HOLE = "BlackHole";
    public static final String BOMB_WEAPON = "BombWeapon";
    public static final String CLOAKING = "Cloaking";
    public static final String CLONING = "Cloning";
    public static final String COMBAT_TRANSPORTER = "CombatTransporter";
    public static final String CONTROL_ENVIRONMENT = "ControlEnvironment";
    public static final String DEFLECTOR_SHIELD = "DeflectorShield";
    public static final String DISPLACEMENT = "Displacement";
    public static final String ECM_JAMMER = "ECMJammer";
    public static final String ECO_RESTORATION = "EcoRestoration";
    public static final String ENERGY_PULSAR = "EnergyPulsar";
    public static final String ENGINE_WARP = "EngineWarp";
    public static final String FUEL_RANGE = "FuelRange";
    public static final String HAND_WEAPON = "HandWeapon";
    public static final String HYPERSPACE_COMM = "HyperspaceComm";
    public static final String IMPROVED_INDUSTRIAL = "ImprovedIndustrial";
    public static final String IMPROVED_TERRAFORMING = "ImprovedTerraforming";
    public static final String INDUSTRIAL_WASTE = "IndustrialWaste";
    public static final String MISSILE_SHIELD = "MissileShield";
    public static final String MISSILE_WEAPON = "MissileWeapon";
    public static final String PERSONAL_SHIELD = "PersonalShield";
    public static final String PLANETARY_SHIELD = "PlanetaryShield";
    public static final String REPULSOR = "Repulsor";
    public static final String RESERVE_FUEL_RANGE = "ReserveFuelRange";
    public static final String ROBOTIC_CONTROLS = "RoboticControls";
    public static final String SCANNER = "Scanner";
    public static final String SHIP_INERTIAL = "ShipInertial";
    public static final String SHIP_NULLIFIER = "ShipNullifier";
    public static final String SHIP_WEAPON = "ShipWeapon";
    public static final String SOIL_ENRICHMENT = "SoilEnrichment";
    public static final String STARGATE = "Stargate";
    public static final String STASIS_FIELD = "StasisField";
    public static final String STREAM_PROJECTOR = "StreamProjector";
    public static final String SUBSPACE_INTERDICTOR = "SubspaceInterdictor";
    public static final String TELEPORTER = "Teleporter";
    public static final String TORPEDO_WEAPON = "TorpedoWeapon";
    public static final String FUTURE_COMPUTER = "FutureComputer";
    public static final String FUTURE_CONSTRUCTION = "FutureConstruction";
    public static final String FUTURE_FORCE_FIELD = "FutureForceField";
    public static final String FUTURE_PLANETOLOGY = "FuturePlanetology";
    public static final String FUTURE_PROPULSION = "FuturePropulsion";
    public static final String FUTURE_WEAPON = "FutureWeapon";

    private final String id;
    private final String typeId;
    public final int typeSeq;
    public final int level;
    private final int quintile;
    private final TechCategory cat;

    public String iconFilename;

    private final String name;
    private final String detail;
    private final String shDesc;
    private final String shDesc2;
    private final String item;
    private final String item2;

    public boolean restricted = false;
    public boolean free = false;
    public float cost = 0;
    public float size = 0;
    public float power = 0;

    public Tech(String typeId, int typeSeq, int level, TechCategory category, String textKeyPrefix) {
        this.typeId = typeId;
        this.id = typeId + ":" + typeSeq;
        this.typeSeq = typeSeq;
        this.level = level;
        this.quintile = (level+4)/5;
        this.cat = category;
        
        this.name = textKeyPrefix + typeSeq + "_NAME";
        this.detail = textKeyPrefix + typeSeq + "_DETAIL";
        this.shDesc = textKeyPrefix + typeSeq + "_BRIEF";
        this.shDesc2 = textKeyPrefix + typeSeq + "_BRIEF2";
        this.item = textKeyPrefix + typeSeq + "_ITEM";
        this.item2 = textKeyPrefix + typeSeq + "_ITEM2";
    }
    
    public String id() {
        return id;
    }
    
    public String typeId() {
        return typeId;
    }
    
    public int categoryIndex() {
        return cat.index();
    }

    @Override
    public String toString() { return "Tech: " + name; }

    public String name()                  { return text(name); }
    public Integer level()                { return level; }
    public String detail()                { return text(detail); }
    public String brief()                 { return textOrDefault(shDesc, ""); }
    public String brief2()                { return textOrDefault(shDesc2, ""); }
    public String item()                  { return textOrDefault(item, name()); }
    public String item2()                 { return textOrDefault(item2, item()); }
    public String imageKey()              { return ""; }
    public Image image()                  { return iconFilename == null ? null : image(iconFilename); }
    public int futureTechLevel()          { return 0; }
    public boolean isWarpDissipator()     { return false; }
    public boolean isTechNullifier()      { return false; }

    public boolean isControlEnvironmentTech() { return false; }
    public boolean isFuelRangeTech()        { return false; }
    public boolean isFutureTech()           { return false; }
    public boolean isObsolete(Empire c)     { return false; }

    public boolean isType(String type)         { return typeId.equals(type); }

    public float warModeFactor()           { return 1; }
    public float expansionModeFactor()     { return 1; }
    public boolean promptToReallocate()     { return followup() != Colony.Orders.NONE; }
    public Colony.Orders followup()         { return Colony.Orders.NONE; }

    public int quintile()                   { return quintile; }
    public void provideBenefits(Empire c)   {  }
    public boolean canBeResearched(Race r)  { return true; }

    public float baseReallocateAmount()   { return 0.25f; }
    public float baseValue(Empire civ)     { return level; }

    public float baseCost(ShipDesign d)    { return cost; }
    public float baseSize(ShipDesign d)    { return size; }
    public float basePower(ShipDesign d)   { return power; }

    public boolean reducesEcoSpending()    { return false; }

    public void drawIneffectiveAttack(CombatEntity source, CombatEntity target, int wpnNum) {  }
    public void drawUnsuccessfulAttack(CombatEntity source, CombatEntity target, int wpnNum) {  }
    public void drawSuccessfulAttack(CombatEntity source, CombatEntity target, int wpnNum, float dmg) { }

    public float researchCost()            { return cat.costForTech(this); }
    public int maxMiniaturizationLevels()   { return 50; }
    public boolean canBeMiniaturized()      { return false; }

    public float sizeMiniaturization(Empire emp) {
        TechCategory empireCat = emp == null ? cat : emp.tech().category(categoryIndex());
        int catLevel = (int) empireCat.techLevel();
        if ((level >= catLevel) || !canBeMiniaturized())
            return 1;
        else {
            int minLevel = min(maxMiniaturizationLevels(), catLevel - level);
            if (empireCat.isWeaponTechCategory())
                return pow(miniFastRate, minLevel);
            else
                return pow(miniSlowRate, minLevel);
        }
    }
    public float costMiniaturization(Empire emp) {
        TechCategory empireCat = emp == null ? cat : emp.tech().category(categoryIndex());
        int catLevel = (int) empireCat.techLevel();
        if ((level >= catLevel) || !canBeMiniaturized())
            return 1;
        else {
            int minLevel = min(maxMiniaturizationLevels(), catLevel - level);
            return pow(miniFastRate, minLevel);
        }
    }
    public static Empire comparatorCiv;
    public static Comparator<String> LEVEL = (String o1, String o2) -> {
        Tech t1 = TechLibrary.current().tech(o1);
        Tech t2 = TechLibrary.current().tech(o2);
        return t1.level().compareTo(t2.level());
    };
    public static Comparator<String> REVERSE_LEVEL = (String o1, String o2) -> {
        Tech t1 = TechLibrary.current().tech(o1);
        Tech t2 = TechLibrary.current().tech(o2);
        return t2.level().compareTo(t1.level());
    };
    public static Comparator<Tech> RESEARCH_PRIORITY = new Comparator<Tech>() {
        @Override
        public int compare(Tech o1, Tech o2) {
            float pr1 = comparatorCiv.ai().scientist().researchPriority(o1);
            float pr2 = comparatorCiv.ai().scientist().researchPriority(o2);
            if (pr1 != pr2)
                return Float.compare(pr2, pr1);
            else
                return Integer.compare(o1.level, o2.level);
        }
    };
    public static Comparator<Tech> RESEARCH_VALUE = new Comparator<Tech>() {
        @Override
        public int compare(Tech o1, Tech o2) {
            float pr1 = comparatorCiv.ai().scientist().researchValue(o1);
            float pr2 = comparatorCiv.ai().scientist().researchValue(o2);
            if (pr1 != pr2)
                return Float.compare(pr2, pr1);
            else
                return Integer.compare(o2.level, o1.level);
        }
    };
    public static Comparator<Tech> WAR_TRADE_VALUE = (Tech o1, Tech o2) -> {
        float pr1 = comparatorCiv.ai().scientist().warTradeValue(o1);
        float pr2 = comparatorCiv.ai().scientist().warTradeValue(o2);
        return Float.compare(pr2, pr1);
    };
}

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

    public static final int ARMOR = 1;
    public static final int ATMOSPHERE_ENRICHMENT = 2;
    public static final int AUTOMATED_REPAIR = 3;
    public static final int BATTLE_COMPUTER = 4;
    public static final int BATTLE_SUIT = 5;
    public static final int BEAM_FOCUS = 6;
    public static final int BIOLOGICAL_ANTIDOTE = 7;
    public static final int BIOLOGICAL_WEAPON = 8;
    public static final int BLACK_HOLE = 9;
    public static final int BOMB_WEAPON = 10;
    public static final int CLOAKING = 11;
    public static final int CLONING = 12;
    public static final int COMBAT_TRANSPORTER = 13;
    public static final int CONTROL_ENVIRONMENT = 14;
    public static final int DEFLECTOR_SHIELD = 15;
    public static final int DISPLACEMENT = 16;
    public static final int ECM_JAMMER = 17;
    public static final int ECO_RESTORATION = 18;
    public static final int ENERGY_PULSAR = 19;
    public static final int ENGINE_WARP = 20;
    public static final int FUEL_RANGE = 21;
    public static final int HAND_WEAPON = 22;
    public static final int HYPERSPACE_COMM = 23;
    public static final int IMPROVED_INDUSTRIAL = 24;
    public static final int IMPROVED_TERRAFORMING = 25;
    public static final int INDUSTRIAL_WASTE = 26;
    public static final int MISSILE_SHIELD = 27;
    public static final int MISSILE_WEAPON = 28;
    public static final int PERSONAL_SHIELD = 29;
    public static final int PLANETARY_SHIELD = 30;
    public static final int REPULSOR = 31;
    public static final int RESERVE_FUEL_RANGE = 32;
    public static final int ROBOTIC_CONTROLS = 33;
    public static final int SCANNER = 34;
    public static final int SHIP_INERTIAL = 35;
    public static final int SHIP_NULLIFIER = 36;
    public static final int SHIP_WEAPON = 37;
    public static final int SOIL_ENRICHMENT = 38;
    public static final int STARGATE = 39;
    public static final int STASIS_FIELD = 40;
    public static final int STREAM_PROJECTOR = 41;
    public static final int SUBSPACE_INTERDICTOR = 42;
    public static final int TELEPORTER = 43;
    public static final int TORPEDO_WEAPON = 44;
    public static final int FUTURE_COMPUTER = 90;
    public static final int FUTURE_CONSTRUCTION = 91;
    public static final int FUTURE_FORCE_FIELD = 92;
    public static final int FUTURE_PLANETOLOGY = 93;
    public static final int FUTURE_PROPULSION = 94;
    public static final int FUTURE_WEAPON = 95;

    public final String id;
    public final int techType;
    public final int typeSeq;
    public final int level;
    private final TechCategory cat;
    public String iconFilename;
    public String effectKey;

    public String name = "";
    public String detail = "";
    public String item = null;
    public String shDesc = "";
    public String item2 = null;
    public String shDesc2 = "";

    public final int quintile;
    public boolean restricted = false;
    public boolean free = false;
    public float cost = 0;
    public float size = 0;
    public float power = 0;

    public Tech(TechCategory category, int techType, String typeId, int typeSeq, int level) {
        this.cat = category;
        this.techType = techType;
        this.id = typeId + ":" + typeSeq;
        this.typeSeq = typeSeq;
        this.level = level;
        this.quintile = (level+4)/5;
    }
    
    public String id() {
        return id;
    }
    
    public int categoryIndex() {
        return cat.index();
    }

    @Override
    public String toString() { return concat("Tech: ", name); }

    public String name()                  { return text(name); }
    public Integer level()                { return level; }
    public String detail()                { return text(detail); }
    public String brief()                 { return text(shDesc); }
    public String brief2()                { return text(shDesc2); }
    public String item()                  { return item == null ? name() : text(item); }
    public String item2()                 { return item2 == null ? item() : text(item2); }
    public String imageKey()              { return ""; }
    public Image image()                  { return iconFilename == null ? null : image(iconFilename); }
    public int futureTechLevel()          { return 0; }
    public boolean isWarpDissipator()     { return false; }
    public boolean isTechNullifier()      { return false; }

    public boolean isControlEnvironmentTech() { return false; }
    public boolean isFuelRangeTech()        { return false; }
    public boolean isFutureTech()           { return false; }
    public boolean isObsolete(Empire c)     { return false; }

    public boolean isType(int type)         { return techType == type; }

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
                return Base.compare(pr2, pr1);
            else
                return Base.compare(o1.level, o2.level);
        }
    };
    public static Comparator<Tech> RESEARCH_VALUE = new Comparator<Tech>() {
        @Override
        public int compare(Tech o1, Tech o2) {
            float pr1 = comparatorCiv.ai().scientist().researchValue(o1);
            float pr2 = comparatorCiv.ai().scientist().researchValue(o2);
            if (pr1 != pr2)
                return Base.compare(pr2, pr1);
            else
                return Base.compare(o2.level, o1.level);
        }
    };
    public static Comparator<Tech> BASE_VALUE = new Comparator<Tech>() {
        @Override
        public int compare(Tech o1, Tech o2) {
            float pr1 = o1.baseValue(comparatorCiv);
            float pr2 = o2.baseValue(comparatorCiv);
            if (pr1 != pr2)
                return Base.compare(pr2, pr1);
            else
                return Base.compare(o2.level, o1.level);
        }
    };
    public static Comparator<Tech> WAR_TRADE_VALUE = new Comparator<Tech>() {
        @Override
        public int compare(Tech o1, Tech o2) {
            float pr1 = comparatorCiv.ai().scientist().warTradeValue(o1);
            float pr2 = comparatorCiv.ai().scientist().warTradeValue(o2);
            return Base.compare(pr2, pr1);
        }
    };
    public Comparator<Tech> OBJECT_TRADE_PRIORITY = (Tech o1, Tech o2) -> {
        float pr1 = this.level() - o1.level();
        if(pr1 < 0)
            pr1 = o1.level() - this.level();
        float pr2 = this.level() - o2.level();
        if(pr2 < 0)
            pr2 = o2.level() - this.level();
        return Base.compare(pr2, pr1);
    };
    public static Comparator<Tech> TRADE_PRIORITY = (Tech o1, Tech o2) -> {
        return Base.compare(o2.level, o1.level);
    } // order that we are willing to trade away techs
    // from lowest-level to highest-level
    ;
}

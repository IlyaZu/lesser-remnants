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
package rotp.model.ships;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.PlanetType;
import rotp.util.Base;

public final class ShipDesign extends Design {
    private static final long serialVersionUID = 1L;
    public static final int maxWeapons = 4;
    public static final int maxSpecials = 3;
    public static final int[] shipColors = { 0,1,3,4,5,6,8,9,10,11};
    public static final int maxWeapons()                 { return maxWeapons; }
    public static final int maxSpecials()                { return maxSpecials; }

    public static final int COLONY = 1;
    public static final int SCOUT = 2;
    public static final int BOMBER = 3;
    public static final int FIGHTER = 4;
    public static final int DESTROYER = 5;

    public static final int MAX_SIZE = 3;
    public static final int SMALL = 0;
    public static final int MEDIUM = 1;
    public static final int LARGE = 2;
    public static final int HUGE = 3;

    private ShipComputer computer;
    private ShipShield shield;
    private ShipECM ecm;
    private ShipArmor armor;
    private ShipEngine engine;
    private ShipManeuver maneuver;
    private final ShipWeapon[] weapon = new ShipWeapon[maxWeapons];
    private final int[] wpnCount = new int[maxWeapons];
    private final ShipSpecial[] special = new ShipSpecial[maxSpecials];
    private int size = SMALL;
    private int mission = SCOUT;
    public int remainingLife = 999; // once obsolete, this is minimum num turns to survive before scrapping
    private float perTurnDmg = 0;
    private String iconKey;
    private int shipColor;
    private transient ImageIcon icon;
    private transient Image image;
    private transient float costBC;

    public static float hullPoints(int size)   { return Galaxy.current().pow(6, size); }
    @Override
    public boolean isShip()              { return true; }

    public ShipComputer computer()          { return computer; }
    public void computer(ShipComputer c)    { computer = c; }
    public ShipShield shield()              { return shield; }
    public void shield(ShipShield c)        { shield = c; }
    public ShipECM ecm()                    { return ecm; }
    public void ecm(ShipECM c)              { ecm = c; }
    public ShipArmor armor()                { return armor; }
    public void armor(ShipArmor c)          { armor = c; }
    public ShipEngine engine()              { return engine; }
    public void engine(ShipEngine c)        { engine = c; }
    public ShipManeuver maneuver()          { return maneuver; }
    public void maneuver(ShipManeuver c)    { maneuver = c; }
    public ShipWeapon weapon(int i)           { return weapon[i]; }
    public ShipSpecial special(int i)         { return special[i]; }
    public int wpnCount(int i)                { return wpnCount[i]; }
    public void weapon(int i, ShipWeapon c)   { weapon[i] = c; }
    public void special(int i, ShipSpecial c) { special[i] = c; }
    public void wpnCount(int i, int n)        { wpnCount[i] = n; }
    public int size()                       { return size; }
    public void size(int i)                 { size = i; }
    public int mission()                    { return mission; }
    public void mission(int i)              { mission = i; }
    public int remainingLife()              { return remainingLife; }
    public float perTurnDamage()            { return perTurnDmg; }
    public void perTurnDamage(float d)      { perTurnDmg = d; }
    public String iconKey()                 { return iconKey; }
    public void iconKey(String s)           { icon = null; iconKey = s; }
    public void seq(int i)                  { seq = i%6; setIconKey(); }
    public float scrapValue(int n)          { return cost() * n / 4.0f; }
    public void setIconKey() {
        iconKey(ShipLibrary.current().shipKey(lab().shipStyleIndex(), size(), seq()));
    }
    public int shipColor()                  { return shipColor; }
    public void shipColor(int i)            { shipColor = i; }
    public void resetImage()                { image = null; }
    @Override
    public Image image() {
        if (image == null) {
            ShipImage shipImage = shipImage();
            image = icon(shipImage.nextIcon()).getImage();
            if (shipColor > 0)
                image = Base.colorizer.makeColor(shipColor, image);
        }
        return image;
    }
    public String sizeDesc() {
        switch (size()) {
            case ShipDesign.SMALL:  return text("SHIP_DESIGN_SIZE_SMALL");
            case ShipDesign.MEDIUM: return  text("SHIP_DESIGN_SIZE_MEDIUM");
            case ShipDesign.LARGE:  return  text("SHIP_DESIGN_SIZE_LARGE");
            case ShipDesign.HUGE:  return  text("SHIP_DESIGN_SIZE_HUGE");
        }
        return "";
    }
    @Override
    public ImageIcon icon()                 {
        if (icon == null)
            icon = icon(iconKey);
        return icon;
    }
    public ShipDesign() {
        this(SMALL);
    }
    public ShipDesign(int sz) {
        size(sz);
        active = false;
        for (int i=0; i<maxWeapons(); i++)  wpnCount(i,0);
    }
    public boolean isScout()       { return (mission() == SCOUT); }
    public boolean isFighter()     { return (mission() == FIGHTER); }
    public boolean isColonyShip()  { return (mission() == COLONY); }
    public boolean isBomber()      { return (mission() == BOMBER); }
    public boolean isDestroyer()   { return (mission() == DESTROYER); }


    public void clearEmptyWeapons() {
        for (int i=0;i<wpnCount.length;i++) {
            if (wpnCount[i] == 0)
                weapon[i] = lab().noWeapon();
        }
    }

    public ShipImage shipImage() {
        return ShipLibrary.current().shipImage(lab().shipStyleIndex(), size(), seq());
    }
    public void nextImage() {
        seq++;
        if (seq > 5)
            seq = 0;
        setIconKey();
    }
    public void prevImage() {
        seq--;
        if (seq < 0)
            seq = 5;
        setIconKey();
    }
    public void recalculateCost() {
        costBC = -1;
    }
    public void copyFrom(ShipDesign d) {
        seq = d.seq();
        lab(d.lab());
        iconKey(d.iconKey());
        shipColor(d.shipColor());
        size(d.size());
        //name(d.name());
        computer(d.computer());
        shield(d.shield());
        ecm(d.ecm());
        armor(d.armor());
        engine(d.engine());
        maneuver(d.maneuver());
        mission(d.mission());
        for (int i=0;i<maxWeapons();i++) {
            weapon(i, d.weapon(i));
            wpnCount(i, d.wpnCount(i));
        }
        for (int i=0;i<maxSpecials();i++) {
            special(i, d.special(i));
        }
    }
    public boolean validConfiguration() {
        return availableSpace() >= 0;
    }
    public int nextEmptyWeaponSlot() {
        for (int i=0;i<maxWeapons;i++) {
            if (weapon(i).isNone())
                return i;
        }
        return -1;
    }
    public int nextEmptySpecialSlot() {
        for (int i=0;i<maxSpecials;i++) {
            if (special(i).isNone())
                return i;
        }
        return -1;
    }
    public boolean matchesDesign(ShipDesign d) {
        return matchesDesign(d, false);
    }
    public boolean matchesDesign(ShipDesign d, boolean ignoreWeapons) {
        if (scrapped() != d.scrapped())
            return false;
        if (size() != d.size())
            return false;
        if (armor() != d.armor())
            return false;
        if (shield() != d.shield())
            return false;
        if (computer() != d.computer())
            return false;
        if (ecm() != d.ecm())
            return false;
        if (engine() != d.engine())
            return false;
        if (maneuver() != d.maneuver())
            return false;
        if(!ignoreWeapons) {
            for (int i=0;i<ShipDesign.maxWeapons();i++) {
                if (weapon(i) != d.weapon(i) )
                    return false;
                if (wpnCount(i) != d.wpnCount(i))
                    return false;
            }
        }
        for (int i=0;i<ShipDesign.maxSpecials();i++) {
            if (special(i) != d.special(i))
                return false;
        }
        return true;
    }
    public boolean validMission(int destId) {
        if (mission != ShipDesign.COLONY)
            return true;
        ShipSpecialColony colonySpecial = colonySpecial();
        if (colonySpecial == null)
            return true;
        if (destId == StarSystem.NULL_ID)
            return false;
        StarSystem dest = galaxy().system(destId);
        PlanetType pt = dest.planet().type();
        // return if ordersStack can colonize the destination planet
        return empire().ignoresPlanetEnvironment() || (empire().canColonize(pt) && colonySpecial.canColonize(pt));
    }
    @Override
    public int cost() {
        if (costBC <= 0) {
            float cost = baseCost();
            cost += computer().cost(this);
            cost += shield().cost(this);
            cost += ecm().cost(this);
            cost += armor().cost(this);
            cost += (enginesRequired() * engine().cost(this));

            for (int i=0; i<maxWeapons(); i++)
                cost += (wpnCount(i) * weapon(i).cost(this));
            for (int i=0; i<maxSpecials(); i++)
                cost += special(i).cost(this);
            costBC = cost;
        }
        return (int) Math.ceil(costBC);
    }
    public float hullPoints() {
        return hullPoints(size());
    }
    public float totalSpace() {
        float techBonus = 1 + (.02f * empire().tech().construction().techLevel());
        switch(size()) {
            case SMALL  : return 40 * techBonus;
            case MEDIUM : return 200 * techBonus;
            case LARGE  : return 1000 * techBonus;
            case HUGE   : return 5000 * techBonus;
            default     : return 0;
        }
    }
    public void becomeObsolete(int turns) {
        if (!obsolete()) {
            obsolete(true);
            remainingLife = turns;
        }
    }
    public float spaceUsed() {
        float space = 0;
        space += computer().space(this);
        space += shield().space(this);
        space += ecm().space(this);
        space += armor().space(this);
        space += maneuver().space(this);
        for (int i=0; i<maxWeapons(); i++)
            space += (wpnCount(i) * weapon(i).space(this));
        for (int i=0; i<maxSpecials(); i++)
            space += special(i).space(this);
        return space;
    }
    public float enginesRequired() {
        float engines = 0;
        engines += computer().enginesRequired(this);
        engines += shield().enginesRequired(this);
        engines += ecm().enginesRequired(this);
        engines += armor().enginesRequired(this);
        engines += maneuver().enginesRequired(this);
        for (int i=0; i<maxWeapons(); i++)
            engines += (wpnCount(i) * weapon(i).enginesRequired(this));
        for (int i=0; i<maxSpecials(); i++)
            engines += special(i).enginesRequired(this);
        return engines;
    }
    public void addWeapon(ShipWeapon wpn, int count) {
        for (int i=0; i<maxWeapons(); i++) {
            if (weapon(i).noWeapon()) {
                weapon(i, wpn);
                wpnCount(i, count);
                return;
            }
            if (weapon(i).tech() == wpn.tech()) {
                wpnCount(i, wpnCount(i) + count);
                return;
            }
        }
    }
    public boolean canAttackPlanets() {
        for (int i=0;i<maxWeapons();i++) {
            if (weapon(i).canAttackPlanets())
                return true;
        }
        for (int i=0;i<maxSpecials();i++) {
            if (special(i).canAttackPlanets())
                return true;
        }
        return false;
    }
    public boolean isArmed() {
        for (int i=0;i<maxWeapons();i++) {
            if (!weapon(i).noWeapon() && (wpnCount(i)>0))
                return true;
        }
        for (int i=0;i<maxSpecials();i++) {
            if (special(i).isWeapon())
                return true;
        }
        return false;
    }
    public boolean isArmedForShipCombat() {
        for (int i=0;i<maxWeapons();i++) {
            if (weapon(i).canAttackShips() && (wpnCount(i)>0))
                return true;
        }
        for (int i=0;i<maxSpecials();i++) {
            if (special(i).canAttackShips())
                return true;
        }
        return false;
    }
    public boolean isExtendedRange() {
        for (int i=0;i<maxSpecials();i++) {
            if (special(i).isFuelRange())
                return true;
        }
        return false;
    }
    // modnar: add firepowerAntiShip to only count weapons that can attack ships
    public float firepowerAntiShip(float shield) {
        float dmg = 0;
        for (int i=0;i<maxWeapons();i++)
            if (weapon(i).canAttackShips()) {
                dmg += (wpnCount(i) * weapon(i).firepower(shield));
            }
        return dmg;
    }
    public float firepower(float shield) {
        float dmg = 0;
        for (int i=0;i<maxWeapons();i++)
            dmg += (wpnCount(i) * weapon(i).firepower(shield));
        return dmg;
    }
    public float availableSpace()                { return totalSpace() - spaceUsed(); }
    public float availableSpaceForWeaponSlot(int i)  { return availableSpace() + (wpnCount(i) * weapon(i).space(this)); }
    public List<ShipSpecial> availableSpecialsForSlot(int slot) {
        List<ShipSpecial> knownSpecials = lab().specials();
        List<ShipSpecial> allowedSpecials = new ArrayList<>();
        allowedSpecials.addAll(knownSpecials);
        
        for (int i=0;i<maxSpecials();i++) {
            ShipSpecial slotSpecial = special(i);
            if ((i != slot) && !slotSpecial.isNone()) {
                // remove any specials of the same typet that are already in other slots
                for (ShipSpecial sp: knownSpecials) {
                    if (sp.designGroup().equals(slotSpecial.designGroup()))
                        allowedSpecials.remove(sp);
                }
            }
        }
        return allowedSpecials;
    }
    public List<ShipManeuver> availableManeuvers() {
        int maxLevel = engine().tech().level();
        List<ShipManeuver> maneuvers = new ArrayList<>();
        for (ShipManeuver manv: lab().maneuvers()) {
            if (manv.tech().level() <= maxLevel)
                maneuvers.add(manv);
        }
        return maneuvers;
    }
    public void setSmallestSize() {
        for (int i=SMALL;i<=HUGE;i++) {
            size(i);
            iconKey(lab().nextAvailableIconKey(size(), null));
            if (availableSpace() >= 0)
                return;
        }
    }
    public int baseHits() {
        switch(size()) {
            case SMALL  : return 3;
            case MEDIUM : return 18;
            case LARGE  : return 100;
            case HUGE   : return 600;
            default     : return 0;
        }
    }
    private int baseCost() {
        switch(size()) {
            case SMALL  : return 6;
            case MEDIUM : return 36;
            case LARGE  : return 200;
            case HUGE   : return 1200;
            default     : return 0;
        }
    }
    private int baseDefense() {
        switch(size()) {
            case SMALL  : return 2;
            case MEDIUM : return 1;
            case LARGE  : return 0;
            case HUGE   : return -1;
            default     : return 0;
        }
    }
    public float hits()        { return armor().hits(this); }
    public int initiative() {
        int lvl = computer().level() + maneuverability();
        for (ShipSpecial spec: special)
            lvl += spec.initiativeBonus();
        return lvl;
    }
    public int attackLevel() {
        int lvl = computer().level();
        for (ShipSpecial spec: special)
            lvl += spec.attackBonus();
        return lvl;
    }
    public float shieldLevel() { return shield().level(); }
    public int combatSpeed() {
        int speed = maneuver().combatSpeed();
        for (int i=0;i<maxSpecials();i++)
            speed += special(i).speedBonus();
        return max(speed,1);
    }
    public int maneuverability() {
        int speed = baseDefense() + maneuver().level();
        for (int i=0;i<maxSpecials();i++)
            speed += special(i).speedBonus();
        // always guarantee a minimum design speed of 1
        return max(1, speed);
    }
    public float beamShieldMod() {
        float shieldMod = 1.0f;
        for (int i = 0; i < maxSpecials(); i++) {
            shieldMod *= special(i).beamShieldMod();
        }
        return shieldMod;
    }
    public int moveRange() { return max(1, combatSpeed()); }
    public int repulsorRange() {
        int r = 0;
        for (int i=0;i<maxSpecials();i++)
            r = max(r, special(i).repulsorRange());
        return r;
    }
    public float missileInterceptPct(ShipWeaponMissileType wpn) {
        float maxIntercept = 0;
        for (int i=0;i<maxSpecials();i++)
            maxIntercept = max(maxIntercept, special(i).missileIntercept(wpn));
        return maxIntercept;
    }
    public int missileDefense() {
        int defense = baseDefense() + ecm().level() + maneuver().level();
        for (int i=0;i<maxSpecials();i++)
            defense += special(i).defenseBonus();
        return defense;
    }
    public int beamDefense() {
        int defense = baseDefense() + maneuver().level();
        for (int i=0;i<maxSpecials();i++)
            defense += special(i).defenseBonus();
        return defense;
    }
    public boolean allowsCloaking() {
        for (int i=0;i<maxSpecials();i++) {
            if (special(i).allowsCloaking())
                return true;
        }
        return false;
    }
    public boolean allowsTeleporting() {
        for (int i=0;i<maxSpecials();i++) {
            if (special(i).allowsTeleporting())
                return true;
        }
        return false;
    }
    public boolean allowsScanning() {
        for (int i=0;i<maxSpecials();i++) {
            if (special(i).allowsScanning())
                return true;
        }
        return false;
    }
    public int range() {
        if (isExtendedRange())
            return (int) empire().tech().scoutRange();
        else
            return (int) empire().tech().shipRange();
    }
    public int warpSpeed() {
        return engine().warp();
    }
    public boolean hasColonySpecial() {
        return colonySpecial() != null;
    }
    public ShipSpecialColony colonySpecial() {
        for (int i=0; i<maxSpecials(); i++) {
            if (special(i).isColonySpecial())
                return (ShipSpecialColony) special(i);
        }
        return null;
    }
    public float repairPct() {
        float healPct = 0;
        for (ShipSpecial spec: special)
            healPct = max(healPct, spec.shipRepairPct());
        return healPct;
    }
    public void preNextTurn() {
        resetBuildCount();
    }
}

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
package rotp.model.combat;

import java.awt.Image;
import java.util.List;
import java.util.LinkedList;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipWeapon;

public class CombatEmpireShip extends CombatShip {
    private final ShipDesign design;
    private final ShipFleet fleet;
    private final boolean usingAI;
    private final boolean atLastColony;
    private boolean bombardedThisTurn;

    public static CombatEmpireShip make(ShipFleet fleet, int index, CombatManager manager) {
        ShipDesign design = fleet.empire().shipLab().design(index);
        Empire empire = fleet.empire();
        int attack = design.attackLevel() + empire.shipAttackBonus();
        int beamDefense = design.beamDefense() + empire.shipDefenseBonus();
        int missileDefense = design.missileDefense() + empire.shipDefenseBonus();
        boolean isAtLastColony = (empire == manager.system().empire()) && (empire.numColonies() == 1);
        
        List<WeaponGroup> weaponGroups = new LinkedList<>();
        for (int i=0;i<ShipDesign.maxWeapons();i++) {
            ShipWeapon weapon = design.weapon(i);
            int weaponCount = design.wpnCount(i);
            if (!weapon.noWeapon() && weaponCount > 0) {
                weaponGroups.add(new WeaponGroup(weapon, weaponCount));
            }
        }
        
        List<ShipSpecial> specials = new LinkedList<>();
        for (int i=0;i<ShipDesign.maxSpecials();i++) {
            specials.add(design.special(i));
        }
        
        return new CombatEmpireShip(fleet.num(index), design.hits(), design.shieldLevel(),
                attack, beamDefense, missileDefense,
                design.maneuverability(), design.moveRange(), design.initiative(),
                weaponGroups, specials,
                design.image(), design.name(),
                design, fleet, empire,
                empire.isAIControlled(), isAtLastColony,
                empire.ai().shipCaptain(), manager);
    }
    
    private CombatEmpireShip(int count, float hits, float shield,
            int attack, int beamDefense, int missileDefense,
            int maneuverability, int move, int initiative,
            List<WeaponGroup> weaponGroups, List<ShipSpecial> specials,
            Image image, String name,
            ShipDesign design, ShipFleet fleet, Empire empire,
            boolean isUsingAI, boolean isAtLastColony,
            ShipCaptain captian, CombatManager manager) {
        
        super(count, hits, shield,
                attack, beamDefense, missileDefense,
                maneuverability, move, initiative,
                weaponGroups, specials,
                image, name,
                captian, manager);
        
        this.design = design;
        this.fleet = fleet;
        this.empire = empire;
        this.usingAI = isUsingAI;
        this.atLastColony = isAtLastColony;
    }
    
    @Override
    public ShipDesign design() {
        return design;
    }
    
    @Override
    public float designCost() {
        return design.cost();
    }
    
    @Override
    public boolean isEmpireShip() {
        return true;
    }
    
    @Override
    public boolean usingAI() {
        return usingAI;
    }
    
    @Override
    public boolean hostileTo(CombatEntity st, StarSystem sys) {
        return st.isMonster() || empire.aggressiveWith(st.empire, sys);
    }
    
    @Override
    public float initiative() {
        return super.initiative() + empire.shipInitiativeBonus();
    }
    
    @Override
    public boolean canPotentiallyAttack(CombatEntity st) {
        if (st == null)
            return false;
        if (empire.alliedWith(id(st.empire)))
            return false;
        return super.canPotentiallyAttack(st);
    }
    
    @Override
    public void fireWeapon(CombatEntity targetStack, int index, boolean allShots) {
        super.fireWeapon(targetStack, index, allShots);
        if (targetStack.isColony())
            bombardedThisTurn = true;
    }
    
    @Override
    public void loseShip() {
        int orig = num;
        super.loseShip();
        int shipsLost = orig-num;
        
        // record losses
        if (!destroyed())  // if destroyed, already recorded lose in super.loseShip()
            mgr.results().addShipDestroyed(design, shipsLost);
        
        fleet.removeShips(design().id(), shipsLost, true);
        empire.shipLab().recordDestruction(design(), shipsLost);
    }
    
    @Override
    public void becomeDestroyed() {
        fleet.removeShips(design().id(), num, true);
        empire.shipLab().recordDestruction(design(), num);
        super.becomeDestroyed();
    }
    
    @Override
    public boolean canRetreat() {
        return !atLastColony && (maneuverability > 0);
    }
    
    @Override
    public boolean retreatToSystem(StarSystem s) {
        if (s == null)
            return false;

        galaxy().ships.retreatSubfleet(fleet, design().id(), s.id);
        return true;
    }
    
    @Override
    public void endTurn() {
        super.endTurn();
        if (bombardedThisTurn)
            fleet.bombarded(design().id());
        bombardedThisTurn = false;
    }
    
    @Override
    protected boolean useSmartRangeForBeams() {
        return empire.ai().shipCaptain().useSmartRangeForBeams();
    }
}

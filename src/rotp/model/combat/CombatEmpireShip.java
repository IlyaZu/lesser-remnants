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

import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;

public class CombatEmpireShip extends CombatShip {
    private ShipFleet fleet;
    private boolean usingAI;
    private boolean atLastColony;
    private boolean bombardedThisTurn;

    public CombatEmpireShip(ShipFleet fleet, int index, CombatManager manager) {
        super(fleet.num(index),
                fleet.empire().shipLab().design(index),
                fleet.empire().ai().shipCaptain(),
                manager);
        
        this.fleet = fleet;
        this.empire = fleet.empire();
        usingAI = empire.isAIControlled();
        
        this.attackLevel += empire.shipAttackBonus();
        this.missileDefense += empire.shipDefenseBonus();
        this.beamDefense += empire.shipDefenseBonus();
        
        this.atLastColony = (empire == mgr.system().empire()) && (empire.numColonies() == 1);
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

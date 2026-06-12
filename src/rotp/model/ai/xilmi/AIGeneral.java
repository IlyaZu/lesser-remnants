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
package rotp.model.ai.xilmi;

import static rotp.model.tech.TechTree.NUM_CATEGORIES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import rotp.model.ai.FleetPlan;
import rotp.model.ai.interfaces.General;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class AIGeneral implements Base, General {
    private final Empire empire;
    private final HashMap<StarSystem, List<Ship>> targetedSystems;
    private final List<StarSystem> rushDefenseSystems;
    private final List<StarSystem> rushShipSystems;
    //better buffer values in private-members instead of recalculating every time
    private float defenseRatio = -1;
    private float totalArmedFleetCost = -1;
    private int additionalColonizersToBuild = -1;
    private float totalEmpirePopulationCapacity = -1;
    private float visibleEnemyFighterCost = -1;
    private float myFighterCost = -1;
    private float smartPower = -1;

    public AIGeneral (Empire c) {
        empire = c;
        targetedSystems = new HashMap<>();
        rushDefenseSystems = new ArrayList<>();
        rushShipSystems = new ArrayList<>();
    }
    private HashMap<StarSystem, List<Ship>> targetedSystems() { return targetedSystems; }
    @Override
    public List<StarSystem> rushDefenseSystems() { return rushDefenseSystems; }
    @Override
    public List<StarSystem> rushShipSystems() { return rushShipSystems; }
    @Override
    public String toString()   { return "General: " + empire.raceName(); }
    @Override
    public boolean inWarMode()  { return empire.numEnemies() > 0; }
    @Override
    public void nextTurn() {
        resetTargetedSystems();
        rushDefenseSystems.clear();
        rushShipSystems.clear();
        defenseRatio = -1;
        additionalColonizersToBuild = -1;
        totalArmedFleetCost = -1;
        totalEmpirePopulationCapacity = -1;
        visibleEnemyFighterCost = -1;
        myFighterCost = -1;
        smartPower = -1;
        
        Galaxy gal = galaxy();
        for (int id=0;id<empire.sv.count();id++)
            reviseFleetPlan(gal.system(id));
        additionalColonizersToBuild = additionalColonizersToBuild(false);
        if(empire.atWar() || sensePotentialAttack())
        {
            int[] counts = galaxy().ships.shipDesignCounts(empire.id);
            ShipDesignLab lab = empire.shipLab();
            float fighterCost = 0.0f;
            float colonizerCost = 0.0f;
            for (int i=0;i<counts.length;i++)
            {
                if(lab.design(i).hasColonySpecial())
                {
                    colonizerCost += lab.design(i).cost() * counts[i];
                    continue;
                }
                fighterCost += lab.design(i).cost() * counts[i] * empire.shipDesignerAI().fightingAdapted(lab.design(i));
            }
            colonizerCost += additionalColonizersToBuild * empire.shipDesignerAI().BestDesignToColonize().cost();
            while(colonizerCost > fighterCost && additionalColonizersToBuild > 0)
            {
                additionalColonizersToBuild--;
                colonizerCost -= empire.shipDesignerAI().BestDesignToColonize().cost();
            }
            if(militaryRank(empire, false) > popCapRank(empire, false))
                additionalColonizersToBuild = 0;
        }
        ShipDesign design = empire.shipDesignerAI().BestDesignToColonize();
        while (additionalColonizersToBuild > 0)
        {
            float highestScore = 0;
            Colony bestCol = null;
            for (int id=0; id<empire.sv.count();id++) {
                if(empire.sv.empire(id) != empire)
                    continue;
                StarSystem sys = galaxy().system(id);
                Colony col = sys.colony();
                if(col.currentProductionCapacity() <= 0.5f && col.production() < design.cost() && col.shipyard().desiredShips() > 0)
                    continue;
                float score = empire.ai().governor().productionScore(sys);
                if(col.shipyard().building())
                    continue;
                if(score > highestScore)
                {
                    bestCol = col;
                    highestScore = score;
                }
            }
            if(bestCol == null)
                break;
            bestCol.shipyard().design(design);
            bestCol.shipyard().addQueuedBC(design.cost());
            float colonyProduction = (bestCol.totalIncome() - bestCol.minimumCleanupCost()) * bestCol.planet().productionAdj();
            int desiredCount = min(additionalColonizersToBuild, (int)Math.floor((float)colonyProduction / (float)design.cost()));
            desiredCount = max(1, desiredCount);
            bestCol.shipyard().addDesiredShips(desiredCount);
            additionalColonizersToBuild-=desiredCount;
        }
    }
    @Override
    public float invasionPriority(StarSystem sys) {
        int sysId = sys.id;
        if (!empire.sv.inShipRange(sysId))  return 0.0f;
        if (!empire.sv.isScouted(sysId))    return 0.0f;
        if (!empire.sv.isColonized(sysId))  return 0.0f;
        if (!empire.canColonize(sys.planet().type()))  return 0.0f;
        
        // modnar: increase invasion priority with planet size and factory count
        float pr = empire.sv.currentSize(sysId) + empire.sv.factories(sysId)/20.0f;
        
        // modnar: killer instinct
        // higher priority to take out the last few planets of an empire
        int remainingSystems = galaxy().empire(empire.sv.empId(sysId)).numColonies();
        if (remainingSystems <=3) {
            pr *= ((remainingSystems + 7)/(remainingSystems + 1));
        }
        
        if (empire.sv.isPoor(sysId))
            pr *= 2;
        else if (empire.sv.isResourceNormal(sysId))
            pr *= 3;
        else if (empire.sv.isRich(sysId))
            pr *= 4;
        else if (empire.sv.isUltraRich(sysId))
            pr *= 5;

        //float for artifacts, triple for super-artifacts
        if (empire.sv.isArtifact(sysId))
            pr *= 2;
        else if (empire.sv.isOrionArtifact(sysId))
            pr *= 3;
        pr /= Math.sqrt(max(1,empire.sv.distance(sysId)));
        pr /= Math.sqrt(max(1,empire.sv.bases(sysId)));
        return pr/10;
    }
    private void reviseFleetPlan(StarSystem sys) {
        int sysId = sys.id;
        
        // if out of ship range, ignore
        if (!empire.sv.inShipRange(sysId))
            return;

        if(needScoutRepellers() && (sys.empire() == empire || !empire.sv.isColonized(sysId)) && !sys.hasMonster())
        {
            if(empire.shipDesignerAI().BestDesignToRepell() != null)
            {
                FleetPlan fp = empire.sv.fleetPlan(sys.id);
                fp.priority = 1100;
                if(empire.sv.isBorderSystem(sysId))
                    fp.priority += 50;
                fp.addShips(empire.shipDesignerAI().BestDesignToRepell(), 1);
            }
        }
        
        // for uncolonized systems
        if (!empire.sv.isColonized(sysId)) {
            return;
        }

        // for our systems
        if (empire == empire.sv.empire(sysId)) {
            if (sys.colony().inRebellion())
                orderRebellionFleet(sys);
            return;
        }

        EmpireView ev = empire.viewForEmpire(empire.sv.empId(sysId));
        
        // for empires we are at war with.. we always invade or bomb
        if (ev.embassy().war()) {
            if (willingToInvade(ev, sys))
                orderInvasionFleet(ev, sys);
            return;
        }
    }
    private float invasionCost(EmpireView v, StarSystem sys)
    {
        float needed = troopsNecessaryToTakePlanet(v, sys);
        needed += empire.sv.currentSize(sys.id) * 0.25f * (1 - empire.fleetCommanderAI().bridgeHeadConfidence(sys));
        float invasionCost = needed * empire.tech().populationCost() / empire.race().growthRateMod();
        return invasionCost;
    }
    private float invasionGain(EmpireView v, StarSystem sys)
    {
        float facSavings = empire.sv.factories(sys.id) * (empire.tech().baseFactoryCost() - 2) + sys.planet().alienFactories(empire.id) * empire.tech().baseFactoryCost();
        float invasionGain = facSavings;
        List<Tech> possibleTechs = v.empire().tech().techsUnknownTo(empire);
        float avgTechCost = 0;
        int techCount = 0;
        for(Tech possi:possibleTechs)
        {
            avgTechCost += possi.researchCost();
            techCount++;
        }
        if(techCount > 0)
            avgTechCost /= techCount;
        float techCaptureCountEstimate = min(6, techCount, 0.02f * empire.sv.factories(sys.id));
        float techCaputureGain = techCaptureCountEstimate * avgTechCost;
        invasionGain += techCaputureGain;
        return invasionGain;
    }
    private boolean willingToInvade(EmpireView v, StarSystem sys) {
        if(!empire.enemies().contains(sys.empire()) && !empire.generalAI().strongEnoughToAttack())
            return false;
        if (!empire.canSendTransportsTo(sys))
            return false;
        //we gain factories, save us from building a colonizer and killing enemy-population also has value to us of half of what they pay for it
        float invasionGain = invasionGain(v, sys) + empire.shipDesignerAI().BestDesignToColonize().cost();
        invasionGain *= empire.fleetCommanderAI().bridgeHeadConfidence(sys);
        return invasionCost(v, sys) <= invasionGain;
    }
    private void orderRebellionFleet(StarSystem sys) {
        launchRebellionTroops(sys);
    }
    private void orderInvasionFleet(EmpireView v, StarSystem sys) {
        float expectedDefenders = 0;
        float attackers = 0;
        float allowableTurns = (float) (1 + Math.min(7, Math.floor(22 / empire.tech().topSpeed())));
        if(sys.colony() != null)
            expectedDefenders += allowableTurns * sys.colony().totalProductionIncome() * sys.planet().productionAdj() + sys.colony().defense().bases() * sys.colony().defense().missileBase().cost(sys.empire());
        for(ShipFleet orbiting : sys.orbitingFleets())
        {
            if(!orbiting.isArmed())
                continue;
            if(orbiting.empire() == empire)
                attackers += empire.ai().fleetCommander().bcValue(orbiting, false, true, false, false);
            else if (orbiting.empire().aggressiveWith(empire.id) && empire.visibleShips().contains(orbiting))
                expectedDefenders += empire.ai().fleetCommander().bcValue(orbiting, false, true, false, false);
        }
        for(ShipFleet incoming : sys.incomingFleets())
        {
            if(!incoming.isArmed())
                continue;
            if(incoming.empire() == empire)
                attackers += empire.ai().fleetCommander().bcValue(incoming, false, true, false, false);
            else if (incoming.empire().aggressiveWith(empire.id) && empire.visibleShips().contains(incoming))
                expectedDefenders += empire.ai().fleetCommander().bcValue(incoming, false, true, false, false);
        }
        //ail: old check would also be positive when our fleet is retreating
        if (attackers > expectedDefenders && attackers > troopsNecessaryToTakePlanet(v, sys) * empire.tech().populationCost())
            launchGroundTroops(v, sys, 1);
    }
    
    private void launchGroundTroops(EmpireView v, StarSystem target, float mult) {
        //float troops0 = troopsNecessaryToBypassBases(target);
        float troops1 = mult*troopsNecessaryToTakePlanet(v, target);
        int alreadySent = empire.transportsInTransit(target);
        float troopsDesired = troops1 + empire.sv.currentSize(target.id) * 0.25f - alreadySent;
        if (troopsDesired < 1)
            return;

        List<StarSystem> allSystems = empire.allColonizedSystems();
        List<StarSystem> launchPoints = new ArrayList<>();
        StarSystem.TARGET_SYSTEM = target;
        Collections.sort(allSystems,StarSystem.DISTANCE_TO_TARGET_SYSTEM);

        float troopsAvailable = 0;
        float maxTravelTime = 0;

        for (StarSystem sys : allSystems) {
            if (troopsAvailable < troopsDesired) {
                float travelTime = sys.colony().transport().travelTime(target);
                // modnar: only consider systems within 8 travel turns at the start of the game
                // decrease with faster warp (faster transport speed)
                // down to 3 travel turns with warp-9
                // warp (topSpeed): 1, 2, 3, 4, 5, 6, 7, 8, 9
                // transport speed: 1, 1, 2, 3, 4, 5, 6, 7, 8
                // allowableTurns:  8, 8, 8, 6 ,5, 4, 4, 3, 3
                // max distance:    8, 8,16,18,20,20,24,21,24
                float topSpeed = empire.tech().topSpeed();
                float allowableTurns = (float) (1 + Math.min(7, Math.floor(22 / topSpeed)));
                if ((travelTime <= allowableTurns) && sys.colony().canTransport()) {
                    launchPoints.add(sys);
                    maxTravelTime = max(maxTravelTime, travelTime);
                    // modnar: keep planets at least 60% full
                    // to prevent complete draining of planets
                    // TODO: modify with leader personality and source planet fertility
                    //troopsAvailable += sys.colony().maxTransportsAllowed();
                    troopsAvailable += Math.max(0.0f, sys.colony().population() - 0.6f*sys.colony().planet().currentSize());
                }
            }
        }

        //not enough troops to take planet! switch to defense
        if (troopsAvailable < troops1)
            return;

        for (StarSystem sys: launchPoints)
            maxTravelTime = max(maxTravelTime, sys.colony().transport().travelTime(target));

        // send transports from launch points
        for (StarSystem sys : launchPoints) {
            // modnar: keep planets at least 60% full
            // to prevent complete draining of planets
            // TODO: modify with leader personality and source planet fertility
            // int troops = sys.colony().maxTransportsAllowed();
            int troops = (int) Math.floor(Math.max(0.0f, sys.colony().population() - 0.6f*sys.colony().planet().currentSize()));
            sys.colony().scheduleTransportsToSystem(target, troops, maxTravelTime);
        }
    }
    private void launchRebellionTroops(StarSystem target) {
        float troops1 =  target.colony().rebels()*2;
        int alreadySent = empire.transportsInTransit(target);
        float troopsDesired = troops1 - alreadySent;

        if (troopsDesired < 1)
            return;

        List<StarSystem> allSystems = empire.allColonizedSystems();
        List<StarSystem> launchPoints = new ArrayList<>();
        StarSystem.TARGET_SYSTEM = target;
        Collections.sort(allSystems,StarSystem.DISTANCE_TO_TARGET_SYSTEM);

        float troopsAvailable = 0;

        for (StarSystem sys : allSystems) {
            if (troopsAvailable < troopsDesired) {
                if (sys.colony().canTransport()) {
                    launchPoints.add(sys);
                    troopsAvailable += sys.colony().maxTransportsAllowed();
                }
            }
        }

        // send transports from launch points
        for (StarSystem sys : launchPoints) {
            int troops = sys.colony().maxTransportsAllowed();
            sys.colony().scheduleTransportsToSystem(target, troops);
        }
    }
    private float troopsNecessaryToTakePlanet(EmpireView ev, StarSystem sys) {
        int id = sys.id;
        
        // modnar: (?) this old estimate gives completely wrong results for ground combat
        //return empire.sv.population(id) * (50 + ev.spies().tech().troopCombatAdj(true)) / (50 + empire.tech().troopCombatAdj(false));
        
        // modnar: correct ground combat ratio estimates for troopsNecessary
        if (ev.spies().tech().troopCombatAdj(true) >= empire.tech().troopCombatAdj(false)) {
            float defAdv = ev.spies().tech().troopCombatAdj(true) - empire.tech().troopCombatAdj(false);
            float killRatio = (float) ((Math.pow(100,2) - Math.pow(100-defAdv,2)/2) / (Math.pow(100-defAdv,2)/2));
            return empire.sv.population(id) * killRatio;
        }
        else {
            float atkAdv = empire.tech().troopCombatAdj(false) - ev.spies().tech().troopCombatAdj(true);
            float killRatio = (float) ((Math.pow(100-atkAdv,2)/2) / (Math.pow(100,2) - Math.pow(100-atkAdv,2)/2));
            return empire.sv.population(id) * killRatio;
        }
    }
    private void resetTargetedSystems() {
        Set<StarSystem> systems = targetedSystems().keySet(); // re-inits
        for (StarSystem s: systems)
            targetedSystems.get(s).clear();

        Galaxy gal = galaxy();
        for (Ship ship: empire.visibleShips()){
            if (ship.inTransit() && empire.aggressiveWith(ship.empId())) {
                if (empire.knowETA(ship)) {
                    StarSystem dest = gal.system(ship.destSysId());
                    if (!targetedSystems.containsKey(dest))
                        targetedSystems.put(dest, new ArrayList<>());
                    targetedSystems.get(dest).add(ship);
                }
            }
        }
    }
    @Override
    public float totalEmpirePopulationCapacity(Empire emp)
    {
        if(totalEmpirePopulationCapacity >= 0 && emp == empire && emp.isAIControlled())
            return totalEmpirePopulationCapacity;
        float capacity = 0;
        for (int id=0;id<emp.sv.count();id++)
        {
            StarSystem current = galaxy().system(id);
            if(current.colony() == null)
                continue;
            if(current.empId() != emp.id)
                continue;
            capacity += current.planet().currentSize();
        }
        if(empire == emp)
            totalEmpirePopulationCapacity = capacity;
        return capacity;
    }
    private float visibleEnemyFighterCost()
    {
        if(visibleEnemyFighterCost >= 0)
            return visibleEnemyFighterCost;
        float cost = 0;
        for(ShipFleet fl:empire.enemyFleets())
        {
            if(empire.enemies().contains(fl.empire()))
            {
                cost += empire.fleetCommanderAI().bcValue(fl, false, true, false, false);
            }
        }
        visibleEnemyFighterCost = cost;
        return visibleEnemyFighterCost;
    }
    private float myFighterCost()
    {
        if(myFighterCost >= 0)
            return myFighterCost;
        float fighterCost = 0.0f;
        for (ShipDesign design:empire.shipLab().designs())
        {
            if(design.hasColonySpecial())
                continue;
            fighterCost += design.cost() * galaxy().ships.shipDesignCount(empire.id, design.id()) * empire.shipDesignerAI().fightingAdapted(design);
        }
        myFighterCost = fighterCost;
        return myFighterCost;
    }
    @Override
    public float defenseRatio()
    {
        if(defenseRatio >= 0)
        {
            return defenseRatio;
        }
        float dr = 0.0f;
        float totalMissileBaseCost = 0.0f;
        float totalShipCost = 0.0f;
        float highestPower = 0.0f;
        float enemyPop = 0.0f;
        float biggestPop = 0.0f;
        float totalKillingPower = 0.0f;
        StarSystem dummySys = null;
        float dummyScore = 0.0f;
        boolean empireInRange = false;
        for(Empire enemy : empire.contactedEmpires())
        {
            if(empire.enemies().contains(enemy))
                enemyPop += enemy.totalPlanetaryPopulation();
            if(enemy.totalPlanetaryPopulation() > biggestPop)
                biggestPop = enemy.totalPlanetaryPopulation();
            if(!empireInRange && empire.inShipRange(enemy.id))
                empireInRange = true;
            totalMissileBaseCost += enemy.missileBaseCostPerBC();
            totalShipCost += enemy.shipMaintCostPerBC();
            if(enemy.militaryPowerLevel() > highestPower)
                highestPower = enemy.militaryPowerLevel();
            for(StarSystem sys : enemy.allColonizedSystems())
            {
                if(sys.colony() != null)
                {
                    float score = (1 + sys.colony().defense().shieldLevel()) * sys.population();
                    if(score > dummyScore)
                    {
                        dummyScore = score;
                        dummySys = sys;
                    }
                }
            }
        }
        if(dummySys != null)
        {
            for(ShipFleet fl : empire.allFleets())
            {
                totalKillingPower += fl.expectedBombardDamage(dummySys) / 200.0;
            }
        }
        float overKill = 0.0f;
        enemyPop = max(enemyPop, biggestPop);
        if(enemyPop > 0)
            overKill = totalKillingPower / enemyPop;
        if(highestPower + smartPowerLevel() > 0)
            dr = 0.25f + 0.75f * (highestPower / (highestPower + smartPowerLevel()));
        if(overKill > 1)
            dr = 1 - ((1 - dr) / overKill);
        if(totalMissileBaseCost+totalShipCost > 0)
        {
            dr = min(dr, totalShipCost / (totalMissileBaseCost+totalShipCost));
        }
        if(myFighterCost() < visibleEnemyFighterCost() || !empireInRange)
            dr = 1.0f;
        defenseRatio = dr;
        return defenseRatio;
    }
    private boolean amSieging(StarSystem sys)
    {
        for(ShipFleet fl : sys.orbitingFleets())
        {
            if(fl.empire() != empire)
                continue;
            if(fl.expectedBombardDamage() > 0 && allowedToBomb(sys))
                return true;
        }
        return false;
    }
    private int additionalColonizersToBuild(boolean returnPotentialUncolonizedInstead)
    {
        if(additionalColonizersToBuild >= 0 && !returnPotentialUncolonizedInstead)
            return additionalColonizersToBuild;
        double additional = 0;
        int colonizerRange = empire.shipDesignerAI().BestDesignToColonize().range();
        List<StarSystem> alreadyCounted = new ArrayList<>();
        for(StarSystem sys : empire.uncolonizedPlanetsInRange(colonizerRange))
        {
            if(empire.sv.isColonized(sys.id) && !amSieging(sys))
                continue;
            if(sys.monster() == null)
            {
                additional+=colonizationProbability(sys);
                alreadyCounted.add(sys);
            }
        }
        for(StarSystem sys : empire.unexploredSystems())
        {
            if(empire.sv.isColonized(sys.id))
                continue;
            if(empire.sv.distance(sys.id) > colonizerRange)
                continue;
            if(sys.monster() != null)
                continue;
            additional+=colonizationProbability(sys);
            alreadyCounted.add(sys);
        }
        //ail: when we have huge colonizer, don't count the unlocks for how many we need since we don't want to spam them like normal one's
        if(empire.shipDesignerAI().BestDesignToColonize().size() < 3)
        {
            for(ShipFleet fleet:empire.allFleets())
            {
                if(!fleet.hasColonyShip())
                {
                    continue;
                }
                if(fleet.destination() != null)
                {
                    for(StarSystem sys : galaxy().systemsInRange(fleet.destination(), empire.shipRange()))
                    {
                        if(alreadyCounted.contains(sys))
                        {
                            break;
                        }
                        if(sys.colony() != null)
                        {
                            continue;
                        }
                        if(!empire.sv.inShipRange(sys.id))
                        {
                            if(empire.canColonize(sys.id)
                                    || empire.unexploredSystems().contains(sys))
                            {
                                additional+=colonizationProbability(sys);
                                alreadyCounted.add(sys);
                            }
                        }
                    }
                }
            }
        }
        if(returnPotentialUncolonizedInstead)
            return (int)Math.ceil(additional);
        boolean knowSomeoneAtWar = false;
        for(EmpireView contact : empire.contacts())
        {
            if(!empire.inShipRange(contact.empId()))
                continue;
            if(!contact.empire().enemies().isEmpty())
                knowSomeoneAtWar = true;
        }
        if(knowSomeoneAtWar)
            additional = max((int)Math.ceil(additional), empire.numColonies() / 5);
        int[] counts = galaxy().ships.shipDesignCounts(empire.id);
        for (int i=0;i<counts.length;i++)
        {
            if(empire.shipLab().design(i).hasColonySpecial())
            {
                if(empire.shipLab().design(i).range() < empire.shipDesignerAI().BestDesignToColonize().range())
                    continue;
                //ail: no idea how this can be null, but I have a savegame from /u/Elkad, where this is the case
                if(empire.tech().topControlEnvironmentTech() == null)
                    additional -= counts[i];
                else if(empire.shipLab().design(i).colonySpecial().tech().level == empire.tech().topControlEnvironmentTech().level
                        || empire.ignoresPlanetEnvironment())
                    additional -= counts[i];
            }
        }
        additional = max((float)additional, 0);
        additionalColonizersToBuild = (int)Math.ceil(additional);
        return additionalColonizersToBuild;
    }
    private float colonizationProbability(StarSystem sys)
    {
        if(sys.orbitingFleets().size() == 1)
        {
            for(ShipFleet fl : sys.orbitingFleets())
            {
                if(fl.isArmed() && fl.empire() == empire)
                    return 1;
            }
        }
        float myProduction = empire.totalPlanetaryProduction();
        float myDistance = colonyCenter(empire).distanceTo(sys);
        float myScore = myProduction / myDistance;
        float totalScore = myScore;
        for(Empire emp : empire.contactedEmpires())
        {
            if(empire.sv.planetType(sys.id) != null && !emp.canColonize(empire.sv.planetType(sys.id)))
                continue;
            float currentProd = emp.totalPlanetaryProduction();
            float currentDistance = colonyCenter(emp).distanceTo(sys);
            totalScore += currentProd / currentDistance;
        }
        float colProb = myScore / totalScore;
        return colProb;
    }
    @Override
    public boolean strongEnoughToAttack()
    {
        float attackThreshold = empire.totalPlanetaryProduction();
        if(totalArmedFleetCost < 0)
        {
            int[] counts = galaxy().ships.shipDesignCounts(empire.id);
            for (int i=0;i<ShipDesignLab.MAX_DESIGNS; i++) {
                ShipDesign d = empire.shipLab().design(i);
                if (d.active() && d.isArmed() && !d.isColonyShip())
                    totalArmedFleetCost += (counts[i] * d.cost());
            }
        }
        if(totalArmedFleetCost > attackThreshold && militaryRank(empire, false) <= popCapRank(empire, false))
            return true;
        return false;
    }
    @Override
    public boolean allowedToBomb(StarSystem sys) {
        Empire emp = sys.empire();
        if(empire.enemies().contains(emp))
        {
            if(empire.transportsInTransit(sys) > troopsNecessaryToTakePlanet(empire.viewForEmpire(emp), sys))
            {
                float cost = invasionCost(empire.viewForEmpire(emp), sys);
                float gain = invasionGain(empire.viewForEmpire(emp), sys);
                if(cost > gain)
                    return true;
            }
            else
                return true;
        }
        return false;
    }
    @Override
    public int minTransportSize()
    {
        return 1;
    }
    private Location fleetCenter(Empire emp)
    {
        float x = 0;
        float y = 0;
        float totalValue = 0;
        for(ShipFleet fleet: emp.allFleets())
        {
            x += fleet.x() * fleet.bcValue();
            y += fleet.y() * fleet.bcValue();
            totalValue += fleet.bcValue();
        }
        x /= totalValue;
        y /= totalValue;
        Location center = new Location(x, y);
        if(totalValue == 0)
            center = colonyCenter(emp);
        return center;
    }
    @Override
    public Location colonyCenter(Empire emp)
    {
        float x = 0;
        float y = 0;
        float totalPopCap = 0;
        for(StarSystem sys: emp.allColonizedSystems())
        {
            x += sys.x() * sys.colony().population();
            y += sys.y() * sys.colony().population();
            totalPopCap += sys.colony().population();
        }
        x /= totalPopCap;
        y /= totalPopCap;
        Location center = new Location(x, y);
        return center;
    }
    @Override
    public boolean needScoutRepellers()
    {
        if(empire.tech().topFuelRangeTech().unlimited == true || empire.scanPlanets() || !empire.shipLab().needScouts)
            return false;
        if(empire.enemies().isEmpty() && !empire.enemyFleets().isEmpty())
            return true;
        return false;
    }
    @Override
    public boolean sensePotentialAttack()
    {
        boolean senseDanger = false;
        for(Ship sh : empire.visibleShips())
        {
            if(empire.aggressiveWith(sh.empId()))
            {
                if(!sh.nullDest() && galaxy().system(sh.destSysId()).empire() == empire)
                {
                    if(sh.isTransport())
                    {
                        senseDanger = true;
                        break;
                    }
                    else
                    {
                        ShipFleet sf = (ShipFleet)sh;
                        if(sf.isArmed())
                        {
                            senseDanger = true;
                            break;
                        }
                    }
                }
            }
        }
        if(techIsAdequateForWar() && !senseDanger)
        {
            for(Empire contact : empire.contactedEmpires())
            {
                if(!contact.inShipRange(empire.id))
                    continue;
                if(contact.atWar())
                    continue;
                if(contact.alliedWith(empire.id))
                    continue;
                float bestScoreForContactToAttack = 0;
                Empire bestTargetOfContact = null;
                for(Empire contactOfContact : contact.contactedEmpires())
                {
                    if(!contact.inShipRange(contactOfContact.id))
                        continue;
                    if(contactOfContact.alliedWith(contact.id))
                        continue;
                    float score = 1 / fleetCenter(contact).distanceTo(colonyCenter(contactOfContact));
                    if(score > bestScoreForContactToAttack)
                    {
                        bestTargetOfContact = contactOfContact;
                        bestScoreForContactToAttack = score;
                    }
                }
                if(bestTargetOfContact == empire && contact.militaryPowerLevel() > smartPowerLevel())
                {
                    senseDanger = true;
                    break;
                }
            }
        }
        return senseDanger;
    }
    @Override
    public Empire biggestThreat()
    {
        Empire biggestThreat = empire;
        float highestThreat = 0;
        for(Empire emp : empire.contactedEmpires())
        {
            if(!empire.enemies().isEmpty() && !empire.enemies().contains(emp))
                continue;
            if(emp == empire)
                continue;
            if(empire.alliedWith(emp.id))
                continue;
            float threat = emp.powerLevel(emp) * 1 / (fleetCenter(emp).distanceTo(colonyCenter(empire)));
            if(!empire.inShipRange(emp.id))
                threat /= 2;
            if(threat > highestThreat)
            {
                highestThreat = threat;
                biggestThreat = emp;
            }
        }
        return biggestThreat;
    }
    @Override
    public float smartPowerLevel()
    {
        if(smartPower > -1)
            return smartPower;
        float power = 0;
        int[] counts = galaxy().ships.shipDesignCounts(empire.id);
        for (int i=0;i<ShipDesignLab.MAX_DESIGNS; i++) {
            ShipDesign d = empire.shipLab().design(i);
            if (d.active() && d.isArmed() && !d.isColonyShip())
            {
                float keepScore = (1 - d.availableSpace()/d.totalSpace()) * (float)d.engine().warp() / (float)empire.shipLab().fastestEngine().warp();
                keepScore *= keepScore;
                power += (counts[i] *d.hullPoints() * keepScore);
            }
        }
        power *= empire.tech().avgTechLevel();
        smartPower = power;
        return smartPower;
    }
    @Override
    public int popCapRank(Empire etc, boolean inAttackRange)
    {
        int rank = 1;
        float myPopCap = empire.generalAI().totalEmpirePopulationCapacity(empire);
        float etcPopCap = empire.generalAI().totalEmpirePopulationCapacity(etc);
        if(empire != etc && myPopCap > etcPopCap)
            rank++;
        for(Empire emp:empire.contactedEmpires())
        {
            if(!empire.inEconomicRange(emp.id))
                continue;
            if(inAttackRange && !empire.inShipRange(emp.id))
                continue;
            if(empire.generalAI().totalEmpirePopulationCapacity(emp) > etcPopCap)
                rank++;
        }
        return rank;
    }
    @Override
    public int techLevelRank()
    {
        int rank = 1;
        float myTechLevel = empire.tech().avgTechLevel();
        for(Empire emp:empire.contactedEmpires())
        {
            if(!empire.inEconomicRange(emp.id))
                continue;
            if(emp.tech().avgTechLevel() > myTechLevel)
                rank++;
        }
        if(myTechLevel >= 99)
            rank = 1;
        return rank;
    }
    @Override
    public int militaryRank(Empire etc, boolean inAttackRange)
    {
        int rank = 1;
        float myMilitaryPower = empire.militaryPowerLevel();
        float etcMilitaryPower = etc.militaryPowerLevel();
        if(empire != etc && myMilitaryPower > etcMilitaryPower)
            rank++;
        for(Empire emp:empire.contactedEmpires())
        {
            if(!empire.inEconomicRange(emp.id))
                continue;
            if(inAttackRange && !empire.inShipRange(emp.id))
                continue;
            if(emp.militaryPowerLevel() > etcMilitaryPower)
                rank++;
        }
        return rank;
    }
    @Override
    public boolean techIsAdequateForWar()
    {
        boolean warAllowed = true;
        int popCapRank = popCapRank(empire, false);
        boolean reseachHasGoodROI = hasGoodTechRoi();
        if(reseachHasGoodROI && techLevelRank() > 1)
            warAllowed = false;
        if(techLevelRank() > popCapRank)
        {
            warAllowed = false;
        }
        return warAllowed;
    }
    private boolean hasGoodTechRoi()
    {
        boolean reseachHasGoodROI = false;
        for(int i = 0; i < NUM_CATEGORIES; ++i)
        {
            int levelToCheck = (int)Math.ceil(empire.tech().category(i).techLevel());
            float techCost = empire.tech().category(i).baseResearchCost() * levelToCheck * levelToCheck * empire.techMod(i);
            if(techCost < empire.totalIncome())
            {
                reseachHasGoodROI = true;
                break;
            }
        }
        return reseachHasGoodROI;
    }
}

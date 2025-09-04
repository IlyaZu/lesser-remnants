/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2025 Ilya Zushinskiy
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.base.AIDiplomat;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.TreatyWar;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.tech.Tech;
import static rotp.model.tech.TechTree.NUM_CATEGORIES;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomaticReply;
import rotp.ui.notifications.DiplomaticNotification;

public class AIXilmiDiplomat extends AIDiplomat {
    private final Empire empire;

    public AIXilmiDiplomat (Empire c) {
        super(c);
        empire = c;
    }

    //-----------------------------------
    //  EXCHANGE TECHNOLOGY
    //-----------------------------------

    @Override
    public List<Tech> techsAvailableForRequest(Empire diplomat) {
        DiplomaticEmbassy embassy = diplomat.viewForEmpire(empire).embassy();
        List<Tech> allUnknownTechs = embassy.offerableTechnologies();

        List<Tech> allTechs = new ArrayList<>();
        for (int i=0; i<allUnknownTechs.size();i++) {
            Tech tech = allUnknownTechs.get(i);
            if (!diplomat.diplomatAI().techsRequestedForCounter(empire, tech).isEmpty())
                allTechs.add(allUnknownTechs.get(i));
        }

        int maxTechs = 5;
        // sort unknown techs by our research value
        Tech.comparatorCiv = empire;
        Collections.sort(allTechs, Tech.RESEARCH_VALUE);
        if (allTechs.size() <= maxTechs)
            return allTechs;
        List<Tech> techs = new ArrayList<>(maxTechs);
        for (int i=0; i<maxTechs;i++)
            techs.add(allTechs.get(i));
        return techs;
    }
    @Override
    public List<Tech> techsRequestedForCounter(Empire requestor, Tech tech) {
        if (tech.isObsolete(requestor))
            return new ArrayList<>();
        
        if(!willingToTradeTech(tech, requestor))
            return new ArrayList<>();

        // what are all of the unknown techs that we could ask for
        DiplomaticEmbassy embassy = requestor.viewForEmpire(empire).embassy();
        List<Tech> allTechs = embassy.offerableTechnologies();
        Tech.comparatorCiv = empire;
        Collections.sort(allTechs, tech.OBJECT_TRADE_PRIORITY);
        // include only those techs which have a research value >= the trade value
        // of the requestedTech we would be trading away
        List<Tech> worthyTechs = new ArrayList<>(allTechs.size());
        for (Tech t: allTechs) {
            if(!empire.scientistAI().isOptional(tech))
                if(empire.scientistAI().isOptional(t) && t.level() < tech.level() + 5 )
                    continue;
            if(empire.scientistAI().isImportant(tech))
            {
                if(empire.scientistAI().isOptional(t))
                    continue;
                if(t.level() < tech.level() + 5
                    && !empire.scientistAI().isImportant(t))
                    continue;
            }
            if (!t.isObsolete(empire) && t.baseValue(empire) > 0)
                worthyTechs.add(t);
        }

        // sort techs by the diplomat's research priority (hi to low)
        Collections.sort(worthyTechs, tech.OBJECT_TRADE_PRIORITY);
        
        // limit return to top 5 techs
        Tech.comparatorCiv = requestor;
        int maxTechs = 3;
        if (worthyTechs.size() <= maxTechs)
            return worthyTechs;
        List<Tech> topFiveTechs = new ArrayList<>(maxTechs);
        for (int i=0; i<maxTechs;i++)
            topFiveTechs.add(worthyTechs.get(i));
        Collections.sort(topFiveTechs, tech.OBJECT_TRADE_PRIORITY);
        return topFiveTechs;
    }
    //-----------------------------------
    //  PEACE
    //-----------------------------------
    @Override
    public DiplomaticReply receiveOfferPeace(Empire requestor) {
        log(empire.name(), " receiving offer of Peace from: ", requestor.name());
        if (empire.isPlayerControlled()) {
            DiplomaticNotification.create(requestor.viewForEmpire(empire), DialogueManager.OFFER_PEACE);
            return null;
        }

        int bonus = requestor.diplomacyBonus();
        EmpireView v = empire.viewForEmpire(requestor);
        if ((bonus+random(100)) < empire.leader().diplomacyAnnoyanceMod(v)) {
            v.embassy().withdrawAmbassador();
            return v.refuse(DialogueManager.DECLINE_ANNOYED);
        }

        v.embassy().noteRequest();

        if (!v.embassy().readyForPeace())
            return v.refuse(DialogueManager.DECLINE_OFFER);

        v.embassy().resetPeaceTimer();
        
        if (!warWeary(v))
            return refuseOfferPeace(requestor);

        DiplomaticIncident inc = v.embassy().signPeace();
        return v.otherView().accept(DialogueManager.ACCEPT_PEACE, inc);
    }
//-----------------------------------
//  JOINT WARS
//-----------------------------------
    @Override
    public DiplomaticReply receiveOfferJointWar(Empire requestor, Empire target) {
        log(empire.name(), " receiving offer of Joint War from: ", requestor.name());
        if (empire.isPlayerControlled()) {
            DiplomaticNotification.create(requestor.viewForEmpire(empire), DialogueManager.OFFER_JOINT_WAR, target);
            return null;
        }
        
        if (empire.atWarWith(target.id))
            return new DiplomaticReply(false, "Already at war with that empire");

        EmpireView v = empire.viewForEmpire(requestor);
        
        // not helping someone whom I don't have real contact with
        if (!empire.inEconomicRange(requestor.id))
            return v.refuse(DialogueManager.DECLINE_OFFER, target);

        // never willing to declare war on an ally
        if (empire.alliedWith(target.id))
            return v.refuse(DialogueManager.DECLINE_NO_WAR_ON_ALLY, target);
        
        // never willing to declare war on an NAP partner
        if (empire.pactWith(target.id))
            return v.refuse(DialogueManager.DECLINE_OFFER, target);
        
        // if a peacy treaty is in effect with the target, then refuse
        if (empire.viewForEmpire(target.id).embassy().atPeace()) {
            return v.refuse(DialogueManager.DECLINE_PEACE_TREATY, target);
        }
        
        //ail: if we are preparing a war against them anyways, we can also make it official here
        if(empire.enemies().contains(target) && !empire.warEnemies().contains(target))
            return agreeToJointWar(requestor, target);
        
         // will always declare war if allied with the requestor and he is already at war with the target
        if (requestor.alliedWith(id(empire)) && requestor.atWarWith(target.id))
            return agreeToJointWar(requestor, target);
        
        if(!empire.enemies().isEmpty())
            return v.refuse(DialogueManager.DECLINE_OFFER, target);

        //ail: refuse offer if we like the target more than the one who asks
        if(empire.viewForEmpire(target).embassy().relations() > v.embassy().relations())
            return v.refuse(DialogueManager.DECLINE_OFFER, target);
        
        return v.refuse(DialogueManager.DECLINE_OFFER, target);
    }
    private DiplomaticReply agreeToJointWar(Empire requestor, Empire target) {
        int targetId = target.id;
        if (!requestor.atWarWith(targetId))
            requestor.viewForEmpire(targetId).embassy().declareWar();
 
        DiplomaticIncident inc =  empire.viewForEmpire(targetId).embassy().declareJointWar(requestor);
        return empire.viewForEmpire(requestor).accept(DialogueManager.ACCEPT_JOINT_WAR, inc);
    }
    //-----------------------------------
    //  BREAK TREATIES
    //-----------------------------------
    @Override
    public boolean canDeclareWar(Empire e)                 { return empire.inShipRange(id(e)) && !empire.atWarWith(id(e)) && !empire.alliedWith(id(e)); }
    //----------------
//
//----------------
    @Override
    public boolean wantToDeclareWarOfOpportunity(EmpireView v) {
        return wantToDeclareWar(v);
    }
    private boolean readyForWar() {
        boolean warAllowed = true;
        if(empire.generalAI().additionalColonizersToBuild(false) > 0)
            warAllowed = false;
        if(!techIsAdequateForWar())
            warAllowed = false;
        float enemyPower = empire.powerLevel(empire);
        Empire victim = empire.generalAI().bestVictim();
        if(victim != null)
        {
            for(Empire enemy : victim.warEnemies())
            {
                enemyPower += enemy.powerLevel(enemy);
            }
            if(victim.powerLevel(victim) > enemyPower && empire.diplomatAI().facCapRank() > 1)
                warAllowed = false;
        }
        //Ail: If there's only two empires left, there's no time for preparation. We cannot allow them the first-strike-advantage!
        if(galaxy().numActiveEmpires() < 3)
            warAllowed = true;
        return warAllowed;
    }
    private boolean wantToDeclareWar(EmpireView v) {
        if (v.embassy().atPeace())
        {
            return false;
        }
        if(!empire.inShipRange(v.empId()))
            return false;
        if (!empire.enemies().isEmpty())
            return false;
        if(readyForWar())
            if(v.empire() == empire.generalAI().bestVictim())
            {
                return true;
            }
        return false;
    }
    private void beginIncidentWar(EmpireView view, DiplomaticIncident inc) {
        log(view.toString(), " - Declaring war based on incident: ", inc.toString(), " id:", inc.declareWarId());
        view.embassy().beginWarPreparations(inc.declareWarId(), inc);
        if (inc.triggersImmediateWar())
            view.embassy().declareWar();
    }
    //-----------------------------------
    // INCIDENTS
    //-----------------------------------
    @Override
    public void noticeIncident(DiplomaticIncident inc, Empire emp) {
        EmpireView view = empire.viewForEmpire(emp);
        
        view.embassy().addIncident(inc);

        if (inc.triggersWar() && !view.embassy().war())
            beginIncidentWar(view, inc);
    }
   private boolean warWeary(EmpireView v) {
        if (galaxy().activeEmpires().size() < 3)
            return false;
        //ail: when we have incoming transports, we don't want them to perish
        for(Transport trans:empire.transports())
        {
            if(trans.destination().empire() == v.empire())
                return false;
        }
        if(!empire.inShipRange(v.empId()))
        {
            return true;
        }
        //new: If we are strong enough, we are okay with fighting the wrong target or several enemies at once
        float enemyPower = 0;
        for(Empire enemy : empire.enemies())
        {
            enemyPower+= enemy.militaryPowerLevel();
        }
        boolean scared = false;
        if(empire.militaryPowerLevel() < enemyPower)
        {
            //ail: If we are not fighting our preferred target, we don't really want a war
            if(v.empire() != empire.generalAI().bestVictim())
                return true;
            //ail: If I have more than one war, we try to go to peace with everyone of our multiple enemies to increase the likelyness of at least one saying yes
            if(empire.warEnemies().size() > 1)
                return true;
            scared = true;
        }
        //ail: If I'm outteched by others I also don't really want to stick to a war anymore, except for aggressive leader as that would lead to contradictory behavior
        if(techLevelRank() > popCapRank(empire, false))
            return true;
        boolean everythingUnderSiege = true;
        for(StarSystem sys : empire.allColonizedSystems())
        {
            if(sys.colony() == null)
                continue;
            if(!sys.enemyShipsInOrbit(empire) && sys.colony().currentProductionCapacity() > 0.5)
            {
                everythingUnderSiege = false;
                break;
            }
        }
        if(scared && v.embassy().treaty() != null && v.embassy().treaty().isWar())
        {
            TreatyWar treaty = (TreatyWar) v.embassy().treaty();
            if (treaty.colonyChange(empire) < 1.0f)
                return true;
        }
        if(everythingUnderSiege)
            return true;
        return false;
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
    public int facCapRank()
    {
        int rank = 1;
        float myFacCap = facCapPct(empire, true);
        for(Empire emp:empire.contactedEmpires())
        {
            if(!empire.inEconomicRange(emp.id))
                continue;
            if(facCapPct(emp, true) > myFacCap)
                rank++;
        }
        if(myFacCap >= 1)
            rank = 1;
        return rank;
    }
    private float facCapPct(Empire emp, boolean ignorePoor)
    {
        float factories = 0;
        float factoryCap = 0;
        for (StarSystem sys: emp.allColonizedSystems())
        {
            if(sys.planet().productionAdj() < 1 && ignorePoor)
                continue;
            factories += sys.colony().industry().factories();
            factoryCap += sys.colony().industry().maxFactories();
        }
        return factories / factoryCap;
    }
    @Override
    public boolean minWarTechsAvailable()
    {
        if(empire.shipLab().fastestEngine().warp() < 2)
            return false;
        if(empire.tech().topShipWeaponTech().damageHigh() <= 4)
            return false;
        if(empire.tech().topDeflectorShieldTech().level() < 2)
            return false;
        return true;
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
    private boolean willingToTradeTech(Tech tech, Empire tradePartner)
    {
        //The player can decide for themselves what they want to give away!
        if(!empire.isAIControlled())
            return true;
        if(!tech.isObsolete(empire) && !empire.alliedWith(tradePartner.id))
            return false;
        for(Empire emp : empire.contactedEmpires())
        {
            EmpireView ev = empire.viewForEmpire(emp);
            if(!ev.inEconomicRange())
                continue;
            if(ev.spies().tech().allKnownTechs().contains(tech.id()))
                return true;
            if(emp.viewForEmpire(empire).spies().possibleTechs().contains(tech.id()) && emp.viewForEmpire(empire).spies().isEspionage())
                return true;
        }
        return false;
    }
}

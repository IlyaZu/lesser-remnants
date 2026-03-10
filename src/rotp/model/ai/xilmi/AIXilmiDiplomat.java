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

import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.base.AIDiplomat;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
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
    public List<Tech> techsRequestedForCounter(Empire requestor, Tech tech) {
        if (tech.isObsolete(requestor))
            return new ArrayList<>();
        
        if(!willingToTradeTech(tech, requestor))
            return new ArrayList<>();

        // what are all of the unknown techs that we could ask for
        DiplomaticEmbassy embassy = requestor.viewForEmpire(empire).embassy();
        List<Tech> allUnknownTechs = embassy.offerableTechnologies();

        // include only those techs which have a research value >= the trade value
        // of the requestedTech we would be trading away
        int maxTechs = 3;
        List<Tech> worthyTechs = new ArrayList<>(maxTechs);
        for (int i=0; i < allUnknownTechs.size() && worthyTechs.size() < maxTechs; i++) {
            Tech t = allUnknownTechs.get(i);
            if (!t.isObsolete(empire) && t.baseValue(empire) > 0)
                worthyTechs.add(t);
        }
        return worthyTechs;
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
        
         // will always declare war if allied with the requestor and he is already at war with the target
        if (requestor.alliedWith(id(empire)) && requestor.atWarWith(target.id))
            return acceptOfferJointWar(requestor, target);
        
        if(!empire.enemies().isEmpty())
            return v.refuse(DialogueManager.DECLINE_OFFER, target);

        //ail: refuse offer if we like the target more than the one who asks
        if(empire.viewForEmpire(target).embassy().relations() > v.embassy().relations())
            return v.refuse(DialogueManager.DECLINE_OFFER, target);
        
        return v.refuse(DialogueManager.DECLINE_OFFER, target);
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
}

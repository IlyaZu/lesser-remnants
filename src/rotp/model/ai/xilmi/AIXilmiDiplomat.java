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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.base.AIDiplomat;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.TreatyWar;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.incidents.ColonyAttackedIncident;
import rotp.model.incidents.ColonyCapturedIncident;
import rotp.model.incidents.ColonyDestroyedIncident;
import rotp.model.incidents.ColonyInvadedIncident;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.EspionageTechIncident;
import rotp.model.incidents.ExpansionIncident;
import rotp.model.incidents.MilitaryBuildupIncident;
import rotp.model.incidents.OathBreakerIncident;
import rotp.model.incidents.SabotageBasesIncident;
import rotp.model.incidents.SabotageFactoriesIncident;
import rotp.model.incidents.SimpleIncident;
import rotp.model.incidents.SkirmishIncident;
import rotp.model.incidents.SpyConfessionIncident;
import rotp.model.tech.Tech;
import static rotp.model.tech.TechTree.NUM_CATEGORIES;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomaticReplies;
import rotp.ui.diplomacy.DiplomaticReply;
import rotp.ui.notifications.DiplomaticNotification;

public class AIXilmiDiplomat extends AIDiplomat {
    private final Empire empire;
    private float cumulativeSeverity = 0;

    public AIXilmiDiplomat (Empire c) {
        super(c);
        empire = c;
    }

    private boolean diplomats(int empId) {
        return empire.viewForEmpire(empId).diplomats();
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

    private boolean decidedToExchangeTech(EmpireView v) {
        if (!willingToOfferTechExchange(v))
            return false;

        DiplomaticEmbassy otherEmbassy = v.otherView().embassy();
        List<Tech> availableTechs = otherEmbassy.offerableTechnologies();
        if (availableTechs.isEmpty())
            return false;

        // iterate over each of available techs, starting with the most desired
        // until one is found that we can make counter-offers for... use that one
        while (!availableTechs.isEmpty()) {
            Tech wantedTech = empire.ai().scientist().mostDesirableTech(availableTechs);
            //System.out.println(empire.galaxy().currentTurn()+" "+empire.name()+" wants from "+v.empire().name()+" the tech "+wantedTech.name() + " value: "+empire.ai().scientist().researchValue(wantedTech));
            availableTechs.remove(wantedTech);
            if (empire.ai().scientist().researchValue(wantedTech) > 1) {
                List<Tech> counterTechs = v.empire().diplomatAI().techsRequestedForCounter(empire, wantedTech);
                List<Tech> willingToTradeCounterTechs = new ArrayList<>(counterTechs.size());
                for (Tech t: counterTechs) {
                    if (willingToTradeTech(t, v.empire()))
                    {
                        //now check if I would give them something for their counter
                        List<Tech> countersToCounter = techsRequestedForCounter(v.empire(), t);
                        if(countersToCounter.contains(wantedTech))
                            willingToTradeCounterTechs.add(t);
                    }
                }
                //System.out.println(empire.galaxy().currentTurn()+" "+empire.name()+" wants from "+v.empire().name()+" the tech "+wantedTech.name() +" countertechs: "+willingToTradeCounterTechs.size());
                if (!willingToTradeCounterTechs.isEmpty()) {
                    List<Tech> previouslyOffered;
                    previouslyOffered = v.embassy().alreadyOfferedTechs(wantedTech);
                    // simplified logic so that if we have ever asked for wantedTech before, don't ask again
                    if (previouslyOffered == null || !previouslyOffered.containsAll(willingToTradeCounterTechs)) {
                        //System.out.println(empire.galaxy().currentTurn()+" "+empire.name()+" ask "+v.empire().name()+" for "+wantedTech.name());
                        v.embassy().logTechExchangeRequest(wantedTech, willingToTradeCounterTechs);
                        //only now send the request
                        DiplomaticReply reply = v.empire().diplomatAI().receiveRequestTech(empire, wantedTech);
                        if ((reply != null) && reply.accepted()) {
                            // techs the AI is willing to consider in exchange for wantedTech
                            // find the tech with the lowest trade value
                            Collections.sort(willingToTradeCounterTechs, Tech.TRADE_PRIORITY);
                            Collections.reverse(willingToTradeCounterTechs);
                            Tech cheapestCounter = willingToTradeCounterTechs.get(0);
                            // if the lowest trade value tech is not the requested tech, then make the deal
                            if (cheapestCounter != wantedTech)
                                v.empire().diplomatAI().receiveCounterOfferTech(empire, cheapestCounter, wantedTech);
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private boolean willingToOfferTechExchange(EmpireView v) {
        if (!canExchangeTechnology(v.empire()))
            return false;
        if(empire.enemies().contains(v.empire()))
        {
            return false;
        }
        return true;
    }
    //-----------------------------------
    //  TRADE TREATIES
    //-----------------------------------

    @Override
    public DiplomaticReply receiveOfferTrade(Empire requestor, int level) {
        // if the AI is asking the player, create an OfferTrade notification
        log(empire.name(), " receiving offer trade from: ", requestor.name(), "  for:", str(level), " BC");
        if (empire.isPlayerControlled()) {
            DiplomaticNotification.create(requestor.viewForEmpire(empire), DialogueManager.OFFER_TRADE);
            return null;
        }
        EmpireView v = empire.viewForEmpire(requestor);
        if (requestor.isPlayerControlled()) {
            if (random(100) < empire.leader().diplomacyAnnoyanceMod(v)) {
                v.embassy().withdrawAmbassador();
                return v.refuse(DialogueManager.DECLINE_ANNOYED);
            }
        }

        v.embassy().noteRequest();
        if (!v.embassy().readyForTrade(level))
            return v.refuse(DialogueManager.DECLINE_OFFER);

        v.embassy().resetTradeTimer(level);

        if(empire.enemies().contains(v.empire()))
        {
            return refuseOfferTrade(requestor, level);
        }

        v.otherView().embassy().tradeAccepted();
        v.otherView().embassy().establishTradeTreaty(level);
        return DiplomaticReplies.acceptTrade(v.otherView(), level);
    }
    @Override
    public DiplomaticReply immediateRefusalToTrade(Empire requestor) {
        return null;
    }
    @Override
    public DiplomaticReply refuseOfferTrade(Empire requestor, int level) {
        EmpireView v = empire.viewForEmpire(requestor);
        v.embassy().resetTradeTimer(level);
        return new DiplomaticReply(false, declineReasonText(v));
    }
    private boolean willingToOfferTrade(EmpireView v, int level) {
        if (!canOfferTradeTreaty(v.empire()))
            return false;
        if (v.embassy().alliedWithEnemy())
            return false;
        
        if (v.trade().active()
            && (v.trade().profit() <= 0))
            return false;
        if (!v.trade().atMaxProfit())
            return false;
        
        // if asking player, check that we don't spam him
        if (v.empire().isPlayerControlled()) {
             if (!v.otherView().embassy().readyForTrade(level))
                return false;
        }

        float currentTrade = v.trade().level();
        float maxTrade = v.trade().maxLevel();
        if (maxTrade < (currentTrade * 1.5))
            return false;

        log(v.toString(), ": willing to offer trade. Max:", str(maxTrade), "    current:", str(currentTrade));
        if(empire.enemies().contains(v.empire()))
        {
            return false;
        }
        return true;
    }
    private String declineReasonText(EmpireView v) {
        DialogueManager dlg = DialogueManager.current();
        DiplomaticIncident inc = worstWarnableIncident(v.embassy().allIncidents());

        // no reason or insignificant, so give generic error
        if ((inc == null) || (inc.severity() > -5))
            return v.decode(dlg.randomMessage(DialogueManager.DECLINE_OFFER, v.owner()));

        if (inc instanceof OathBreakerIncident)
            return v.decode(inc.decode(dlg.randomMessage(DialogueManager.DECLINE_OATHBREAKER, v.owner())));

        if (inc instanceof MilitaryBuildupIncident)
            return v.decode(inc.decode(dlg.randomMessage(DialogueManager.DECLINE_BUILDUP, v.owner())));

        if (inc instanceof SkirmishIncident)
            return v.decode(inc.decode(dlg.randomMessage(DialogueManager.DECLINE_SKIRMISH, v.owner())));

        if (inc instanceof ColonyAttackedIncident)
            return v.decode(inc.decode(dlg.randomMessage(DialogueManager.DECLINE_ATTACK, v.owner())));

        if ((inc instanceof ColonyCapturedIncident)
        || (inc instanceof ColonyDestroyedIncident)
        || (inc instanceof ColonyInvadedIncident))
            return v.decode(inc.decode(dlg.randomMessage(DialogueManager.DECLINE_INVASION, v.owner())));

        if (inc instanceof EspionageTechIncident)
            return v.decode(inc.decode(dlg.randomMessage(DialogueManager.DECLINE_ESPIONAGE, v.owner())));

        if ((inc instanceof SabotageBasesIncident)
        || (inc instanceof SabotageFactoriesIncident))
            return v.decode(inc.decode(dlg.randomMessage(DialogueManager.DECLINE_SABOTAGE, v.owner())));

        // unknown reason, return generic error
        return v.decode(dlg.randomMessage(DialogueManager.DECLINE_OFFER, v.owner()));
    }
    //-----------------------------------
    //  PEACE
    //-----------------------------------
    private boolean canOfferPeaceTreaty(Empire e)           { return diplomats(id(e)) && empire.atWarWith(id(e)); }
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
    private boolean willingToOfferPeace(EmpireView v) {
        if (!v.embassy().war())
            return false;
        if (!v.embassy().onWarFooting() && !canOfferPeaceTreaty(v.empire()))
            return false;
        if (v.embassy().contactAge() < 1)
            return false;
        if (!v.otherView().embassy().readyForPeace())
            return false;
        return warWeary(v);
    }
    //-----------------------------------
    //  PACT
    //-----------------------------------
    private boolean canOfferPact(Empire e){
        if (!diplomats(id(e)))
            return false;
        if (!empire.inEconomicRange(id(e)))
            return false;
        if (empire.atWarWith(id(e)))
            return false;
        if (!empire.hasTradeWith(e))
            return false;
        if (empire.pactWith(id(e)) || empire.alliedWith(id(e)))
            return false;
        return true;
    }
    @Override
    public DiplomaticReply receiveOfferPact(Empire requestor) {
        log(empire.name(), " receiving offer of Pact from: ", requestor.name());
        EmpireView v = empire.viewForEmpire(requestor);
        if (empire.isPlayerControlled()) {
            DiplomaticNotification.create(requestor.viewForEmpire(empire), DialogueManager.OFFER_PACT);
            return null;
        }

        if (requestor.isPlayerControlled()) {
            if (random(100) < empire.leader().diplomacyAnnoyanceMod(v)) {
                v.embassy().withdrawAmbassador();
                return v.refuse(DialogueManager.DECLINE_ANNOYED);
            }
        }

        v.embassy().noteRequest();

        if (!v.embassy().readyForPact())
            return v.refuse(DialogueManager.DECLINE_OFFER);

        v.embassy().resetPactTimer();
        
        //ail: just use the same logic we'd use for offering
        if(willingToOfferPact(empire.viewForEmpire(requestor))) {
            v.embassy().signPact();
            return DiplomaticReplies.acceptPact(v.otherView());
        }
        else
            return v.refuse(DialogueManager.DECLINE_OFFER);
    }
    //ail: pacts just restrict us unnecessarily
    private boolean willingToOfferPact(EmpireView v) {
        return false;
    }
    //-----------------------------------
    //  ALLIANCE
    //-----------------------------------
    private boolean canOfferAlliance(Empire e) {
        if (!diplomats(id(e)))
            return false;
        if (!empire.inEconomicRange(id(e)))
            return false;
        if (!empire.pactWith(id(e)))
            return false;
        if (empire.alliedWith(id(e)))
            return false;
        return true;
    }
    @Override
    public DiplomaticReply receiveOfferAlliance(Empire requestor) {
        log(empire.name(), " receiving offer of Alliance from: ", requestor.name());
        if (empire.isPlayerControlled()) {
            DiplomaticNotification.create(requestor.viewForEmpire(empire), DialogueManager.OFFER_ALLIANCE);
            return null;
        }

        EmpireView v = empire.viewForEmpire(requestor);
        if (requestor.isPlayerControlled()) {
            if (random(100) < empire.leader().diplomacyAnnoyanceMod(v)) {
                v.embassy().withdrawAmbassador();
                return v.refuse(DialogueManager.DECLINE_ANNOYED);
            }
        }
        v.embassy().noteRequest();

        List<Empire> myEnemies = v.owner().warEnemies();
        List<Empire> hisAllies = v.empire().allies();
        for (Empire enemy: myEnemies) {
            if (hisAllies.contains(enemy))
                return v.refuse(DialogueManager.DECLINE_ENEMY_ALLY, enemy);
        }
        if(willingToOfferAlliance(requestor)) {
            v.embassy().signAlliance();
            return DiplomaticReplies.acceptAlliance(v);
        }
        else
            return refuseOfferAlliance(requestor);
    }
    private boolean willingToOfferAlliance(Empire e) {
        EmpireView v = empire.viewForEmpire(e);
        // if we are asking the player, respect the alliance-countdown
        // timer to avoid spamming player with requests
        if (e.isPlayerControlled()) {
            //return true;
            if (!v.otherView().embassy().readyForAlliance())
                return false;
        }
        // is asking for an alliance even allowed per game rules
        if (!canOfferAlliance(e))
            return false;
        if (v.embassy().alliedWithEnemy())
            return false;
        return false;
    }
//-----------------------------------
//  JOINT WARS
//-----------------------------------
    private boolean willingToOfferJointWar(Empire friend, Empire target) {
        // this method is called only for targets that we are at war with
        // or targets we are preparing for war with
        // only ask people who we are in real contact with
        //xilmi: It is possible to be in range but not have contact
        if(!friend.hasContact(target))
            return false;
        if (!empire.inEconomicRange(friend.id))
            return false;
        if (friend.isPlayerControlled() && !friend.alliedWith(empire.id)) {
            EmpireView v = empire.viewForEmpire(friend);
            if (!v.otherView().embassy().readyForJointWar())
                return false;
        }
        // if he's already at war, don't bother
        if (friend.atWarWith(target.id))
            return false;
        // if he's allied with the target, don't bother
        if (friend.alliedWith(target.id))
            return false;
        // if he's not in ship range, don't bother
        if (!friend.inShipRange(target.id))
            return false;
        EmpireView v = friend.viewForEmpire(target);
        if(v.embassy().atPeace())
            return false;
        return true;
    }
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
    private boolean canBreakAlliance(Empire e)              { return empire.alliedWith(id(e)); }
    @Override
    public boolean canDeclareWar(Empire e)                 { return empire.inShipRange(id(e)) && !empire.atWarWith(id(e)) && !empire.alliedWith(id(e)); }
    @Override
    public boolean canThreatenSpying(Empire e) {
        return false;
    }
    @Override
    public boolean canEvictSpies(Empire e) {
        return false;
    }
    @Override
    public DiplomaticReply receiveThreatStopSpying(Empire dip) {
        EmpireView v = empire.viewForEmpire(dip);
        
        v.embassy().noteRequest();
        v.embassy().withdrawAmbassador();
        
        v.spies().ignoreThreat();
        return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
    }
    @Override
    public DiplomaticReply receiveThreatEvictSpies(Empire dip) {
        EmpireView v = empire.viewForEmpire(dip);
        
        v.embassy().noteRequest();
        v.embassy().withdrawAmbassador();

        v.embassy().addIncident(SimpleIncident.createEvictedSpiesIncident(v));
        
        v.spies().ignoreThreat();
        return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
    }
    @Override
    public DiplomaticReply receiveThreatStopAttacking(Empire dip) {
        EmpireView v = empire.viewForEmpire(dip);

        v.embassy().noteRequest();
        v.embassy().withdrawAmbassador();
        
        v.embassy().ignoreThreat();
        return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
    }
    private boolean decidedToBreakAlliance(EmpireView view) {
        if (!wantToBreakAlliance(view))
            return false;
        view.embassy().breakAlliance();
        if (view.empire().isPlayerControlled())
            DiplomaticNotification.create(view, DialogueManager.BREAK_ALLIANCE);
        return true;
    }
    //ail: no good reason to ever break an alliance
    private boolean wantToBreakAlliance(EmpireView v) {
        if(!canBreakAlliance(v.empire()))
            return false;
        return false;
    }
    private boolean decidedToBreakPact(EmpireView view) {
        if (!wantToBreakPact(view))
            return false;

        view.embassy().breakPact();
        if (view.empire().isPlayerControlled())
            DiplomaticNotification.create(view, DialogueManager.BREAK_PACT);
        return true;
    }
    private boolean wantToBreakPact(EmpireView v) {
        if (!v.embassy().pact())
            return false;
        if(empire.generalAI().bestVictim() == v.empire())
            return true;
        return false;
    }
    private boolean decidedToBreakTrade(EmpireView view) {
        if (!wantToBreakTrade(view))
            return false;

        view.embassy().breakTrade();
        if (view.empire().isPlayerControlled())
            DiplomaticNotification.create(view, DialogueManager.BREAK_TRADE);
        return true;
    }
    private boolean wantToBreakTrade(EmpireView v) {
        //ail: no need to break trade. War declaration will do it for us, otherwise it just warns our opponent
        return false;
    }
    //----------------
//
//----------------
    @Override
    public void makeDiplomaticOffers(EmpireView v) {
        //updatePersonality(); this is too telling but I'll leave the code in
        if(empire.enemies().contains(v.empire()) && !empire.warEnemies().contains(v.empire()))
        {
            if(!empire.inShipRange(v.empId()))
                v.embassy().endWarPreparations();
        }
        if(v.embassy().diplomatGone()) {
            v.embassy().openEmbassy();
        }

        // check diplomat offers from worst to best
        if (decidedToDeclareWar(v))
            return;
        decidedToBreakAlliance(v);
        decidedToBreakPact(v);
        //It should be possible to declare war or break an alliance with the diplomat gone
        if (v.embassy().diplomatGone() || v.otherView().embassy().diplomatGone())
            return;
        decidedToBreakTrade(v);
        decidedToIssueWarning(v);

        if (willingToOfferPeace(v)) {
            if (v.embassy().war())
                v.empire().diplomatAI().receiveOfferPeace(empire);
            else
                v.embassy().endWarPreparations();
        }
        
        // if this empire is at war with us or we are preparing
        // for war, then stop now. No more Mr. Nice Guy.
        List<Empire> enemies = empire.enemies();
        if (enemies.contains(v.empire()))
            return;
        
        // build a priority list for Joint War offers:
        for (Empire target: empire.enemies()) {
            if (willingToOfferJointWar(v.empire(), target)) {
                //System.out.println(empire.galaxy().currentTurn()+" "+ empire.name()+" asks "+v.empire().name()+" to declare war on "+target.name());
                v.empire().diplomatAI().receiveOfferJointWar(v.owner(), target);
            }
        }
        
        if (willingToOfferTrade(v, v.trade().maxLevel())) {
            v.empire().diplomatAI().receiveOfferTrade(v.owner(), v.trade().maxLevel());
        }
        
        decidedToExchangeTech(v);

        if (canOfferPact(v.empire()) && willingToOfferPact(v)) {
            v.empire().diplomatAI().receiveOfferPact(empire);
        }
        if (canOfferAlliance(v.empire()) && willingToOfferAlliance(v.empire())) {
            v.empire().diplomatAI().receiveOfferAlliance(v.owner());
        }
        decidedToIssuePraise(v);
    }
    private boolean decidedToIssuePraise(EmpireView view) {
        if (!view.inEconomicRange())
            return false;

        log(view+": checkIssuePraise");
        DiplomaticIncident maxIncident = null;
        for (DiplomaticIncident ev: view.embassy().newIncidents()) {
            if (ev.triggersPraise() && ev.moreSevere(maxIncident))
                maxIncident = ev;
        }

        if (maxIncident == null)
            return false;

        log("cum.sev: ", str(cumulativeSeverity), "   maxInc:", maxIncident.praiseMessageId(), "  maxSev:", str(maxIncident.severity()));

        // don't issue praise unless new incidents are high enough
        if (maxIncident.severity() < view.embassy().minimumPraiseLevel())
            return false;

        maxIncident.notifyOfPraise();
        view.embassy().praiseSent();
        if (view.empire().isPlayerControlled())
            DiplomaticNotification.create(view, maxIncident, maxIncident.praiseMessageId());

        return true;
    }
    private int warningThreshold(EmpireView view) {
        DiplomaticEmbassy emb = view.embassy();
        int warnLevel = emb.minimumWarnLevel();
        if (emb.alliance())
            return warnLevel / 4;
        else if (emb.pact())
            return warnLevel /2;
        else
            return warnLevel;
    }
    private boolean decidedToIssueWarning(EmpireView view) {
        if (!view.inEconomicRange())
            return false;
        // no warnings if at war
        DiplomaticEmbassy emb = view.embassy();
        if (emb.war())
            return false;
        float threshold = 0 - warningThreshold(view);
        log(view+": checkIssueWarning. Threshold: "+ threshold);
        DiplomaticIncident maxIncident = null;
        cumulativeSeverity = 0;
        for (DiplomaticIncident ev: emb.newIncidents()) {
            log(view.toString(), "new incident:", ev.toString());
            float sev = ev.severity();
            cumulativeSeverity += sev;
            if (ev.triggersWarning() && ev.moreSevere(maxIncident))
                maxIncident = ev;
        }
        
        if (maxIncident == null)
            return false;
        
        if (maxIncident.severity() > threshold)
            return false;

        log("cumulative severity: "+cumulativeSeverity);
        view.embassy().logWarning(maxIncident);
        
        // if we are warning player, send a notification
        if (view.empire().isPlayerControlled()) {
            // we will only give one expansion warning
            if (maxIncident instanceof ExpansionIncident) {
                if (view.embassy().gaveExpansionWarning())
                    return true;
                view.embassy().giveExpansionWarning();
            }
            //ail: don't nag about spy-confession-incidents
            if(!(maxIncident instanceof SpyConfessionIncident
                    || maxIncident instanceof EspionageTechIncident))
                DiplomaticNotification.create(view, maxIncident, maxIncident.warningMessageId());
        }
        return true;
    }
    private boolean decidedToDeclareWar(EmpireView view) {
        if (empire.isPlayerControlled())
            return false;
        if (view.embassy().war())
            return false;
        if (!view.inEconomicRange())
            return false;
        if(empire.enemies().contains(view.empire()))
            return false;
        
        // look at new incidents. If any trigger war, pick
        // the one with the greatest severity
        DiplomaticIncident warIncident = null;
        float worstNewSeverity = 0;
        
        // check for a war incident if we are not at peace, or the start
        // date of our peace treaty precedes the current time
        if (!view.embassy().atPeace()
        || (view.embassy().treatyTurn() < galaxy().currentTurn())) {
            for (DiplomaticIncident ev: view.embassy().newIncidents()) {
                if (!ev.declareWarId().isEmpty()) {
                    if (ev.triggersWar()) {
                        float sev = ev.severity();
                        if (ev.triggersWarning() && (sev < worstNewSeverity))
                            warIncident = ev;
                    }
                    else if (view.embassy().timerIsActive(ev.timerKey()))
                        warIncident = ev;
                }
            }
            if (warIncident != null) {
                if(!warIncident.isSpying())
                {
                    //System.out.println(empire.galaxy().currentTurn()+" "+ empire.name()+" starts Incident-War ("+warIncident.toString()+") vs. "+view.empire().name());
                    beginIncidentWar(view, warIncident);
                    return true;
                }
            }
        }
        
        if (wantToDeclareWarOfOpportunity(view)) {
            //ail: even if the real reason is because of geopolitics, we can still blame it on an incident, if there ever was one, so the player thinks it is their own fault
            //System.out.println(empire.galaxy().currentTurn()+" "+empire.name()+" starts Opportunity-War vs. "+view.empire().name());
            beginOpportunityWar(view);
            return true;
        }
        return false;
    }
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
    private DiplomaticIncident worstWarnableIncident(Collection<DiplomaticIncident> incidents) {
        DiplomaticIncident worstIncident = null;
        float worstNewSeverity = 0;
        for (DiplomaticIncident ev: incidents) {
            float sev = ev.severity();
            if (ev.triggersWarning() && (sev < worstNewSeverity))
                worstIncident = ev;
        }
        return worstIncident;
    }
    private void beginIncidentWar(EmpireView view, DiplomaticIncident inc) {
        log(view.toString(), " - Declaring war based on incident: ", inc.toString(), " id:", inc.declareWarId());
        view.embassy().beginWarPreparations(inc.declareWarId(), inc);
        if (inc.triggersImmediateWar())
            view.embassy().declareWar();
    }
    private void beginOpportunityWar(EmpireView view) {
        log(view+" - Declaring war based on opportunity");
        view.embassy().beginWarPreparations(DialogueManager.DECLARE_OPPORTUNITY_WAR, null);
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


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
package rotp.model.ai.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.model.ai.interfaces.Diplomat;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import static rotp.model.empires.EmpireStatus.FLEET;
import rotp.model.empires.EmpireView;
import rotp.model.empires.Leader;
import rotp.model.empires.SpyNetwork.Mission;
import rotp.model.empires.SpyReport;
import rotp.model.empires.TreatyWar;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.ExpansionIncident;
import rotp.model.incidents.SimpleIncident;
import rotp.model.tech.Tech;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.diplomacy.DiplomacyTechOfferMenu;
import rotp.ui.diplomacy.DiplomaticCounterReply;
import rotp.ui.diplomacy.DiplomaticMessage;
import rotp.ui.diplomacy.DiplomaticReplies;
import rotp.ui.diplomacy.DiplomaticReply;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.util.Base;

public class AIDiplomat implements Base, Diplomat {
    private static final float ERRATIC_WAR_PCT = .02f;
    private final Empire empire;
    private float cumulativeSeverity = 0;

    public AIDiplomat (Empire c) {
        empire = c;
    }
    @Override
    public String toString()   { return concat("Diplomat: ", empire.raceName()); }

    private boolean diplomats(int empId) {
        return empire.viewForEmpire(empId).diplomats();
    }

    //-----------------------------------
    //  EXCHANGE TECHNOLOGY
    //-----------------------------------
    @Override
    public boolean canExchangeTechnology(Empire e) {
        // to trade technology with another empire, all of the following must be true:
        // 1 - we have diplomats active
        // 2 - we are not at war
        // 3 - we are in economic range
        // 4 - they have techs they are willing to trade to us (i.e. do we have compensation)
        return diplomats(id(e))
                && !empire.atWarWith(id(e))
                && empire.inEconomicRange(id(e))
                && !techsAvailableForRequest(e).isEmpty();
    }

    @Override
    public DiplomaticReply receiveRequestTech(Empire diplomat, Tech tech) {
        if (empire.isPlayerControlled()) {
            EmpireView v = diplomat.viewForEmpire(empire);
            // 1st, create the reply for the AI asking the player for the tech
            DiplomaticReply reply = v.otherView().accept(DialogueManager.OFFER_TECH_EXCHANGE);
            // decode the [tech] field in the reply text
            reply.decode("[tech]", tech.name());
            // 2nd, create the counter-offer menu that the player would present to the AI
            DiplomacyTechOfferMenu menu = DiplomacyTechOfferMenu.create(empire, diplomat, reply, tech);
            // if counter offers available, display the request in modal
            if (menu.hasCounterOffers())
                DiplomaticMessage.replyModal(menu);
            return null;
        }

        EmpireView v = empire.viewForEmpire(diplomat);
        
        // modnar: add in readyForTech check, limits one tech trade per turn per empire
        // this also prevents trading the same tech multiple times to the same empire
        if (!v.embassy().readyForTech())
            return v.refuse(DialogueManager.DECLINE_OFFER);
        
        List<Tech> counterTechs = empire.diplomatAI().techsRequestedForCounter(diplomat, tech);
        if (counterTechs.isEmpty())
            return v.refuse(DialogueManager.DECLINE_TECH_TRADE);

        // accept and present a menu of counter-offer techs
        return v.otherView().accept(DialogueManager.DIPLOMACY_TECH_CTR_MENU);
    }
    @Override
    public DiplomaticReply receiveCounterOfferTech(Empire diplomat, Tech offeredTech, Tech requestedTech) {
        EmpireView view = empire.viewForEmpire(diplomat);
        view.embassy().resetTechTimer();

        DiplomaticIncident inc = view.embassy().exchangeTechnology(offeredTech, requestedTech);
        return view.otherView().accept(DialogueManager.ACCEPT_TECH_EXCHANGE, inc);
    }
    @Override
    public List<Tech> techsAvailableForRequest(Empire diplomat) {
        EmpireView view = empire.viewForEmpire(diplomat);
        List<Tech> allUnknownTechs = view.spies().unknownTechs();

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
        
        EmpireView view = empire.viewForEmpire(requestor);

        // what is this times the value of the request tech?dec
        float maxTechValue = techDealValue(view) * max(tech.level(), tech.baseValue(requestor));

        // what are all of the unknown techs that we could ask for
        List<Tech> allTechs = view.spies().unknownTechs();

        // include only those techs which have a research value >= the trade value
        // of the requestedTech we would be trading away
        List<Tech> worthyTechs = new ArrayList<>(allTechs.size());
        for (Tech t: allTechs) {
            if (t.quintile() == tech.quintile()) {
                if (t.baseValue(empire) > maxTechValue) {
                    if (!t.isObsolete(empire))
                        worthyTechs.add(t);
                }
            }
        }

        // sort techs by the diplomat's research priority (hi to low)
        Tech.comparatorCiv = empire;
        Collections.sort(worthyTechs, Tech.BASE_VALUE);
        
        // limit return to top 5 techs
        Tech.comparatorCiv = requestor;
        int maxTechs = 3;
        if (worthyTechs.size() <= maxTechs)
            return worthyTechs;
        List<Tech> topFiveTechs = new ArrayList<>(maxTechs);
        for (int i=0; i<maxTechs;i++)
            topFiveTechs.add(worthyTechs.get(i));
        Collections.sort(topFiveTechs, Tech.RESEARCH_VALUE);
        return topFiveTechs;
    }
    private boolean decidedToExchangeTech(EmpireView v) {
        if (!willingToOfferTechExchange(v))
            return false;

        List<Tech> availableTechs = v.spies().unknownTechs();
        if (availableTechs.isEmpty())
            return false;

        // iterate over each of available techs, starting with the most desired
        // until one is found that we can make counter-offers for... use that one
        while (!availableTechs.isEmpty()) {
            Tech wantedTech = empire.ai().scientist().mostDesirableTech(availableTechs);
            availableTechs.remove(wantedTech);
            if (empire.ai().scientist().researchValue(wantedTech) > 0) {
                List<Tech> counterTechs = v.empire().diplomatAI().techsRequestedForCounter(empire, wantedTech);
                if (!counterTechs.isEmpty()) {
                    List<Tech> previouslyOffered = v.embassy().alreadyOfferedTechs(wantedTech);
                    // simplified logic so that if we have ever asked for wantedTech before, don't ask again
                    if (previouslyOffered == null) {
                         v.embassy().logTechExchangeRequest(wantedTech, counterTechs);
                        // there are counters available.. send request
                        DiplomaticReply reply = v.empire().diplomatAI().receiveRequestTech(empire, wantedTech);
                        if ((reply != null) && reply.accepted()) {
                            // techs the AI is willing to consider in exchange for wantedTech
                            // find the tech with the lowest trade value
                            counterTechs.add(wantedTech);
                            Collections.sort(counterTechs, Tech.TRADE_PRIORITY);
                            Tech cheapestCounter = counterTechs.get(0);
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

        float adjustedRelations = v.embassy().relations();
        adjustedRelations += empire.leader().acceptTradeMod();
        adjustedRelations += v.embassy().alliedWithEnemy() ? -100 : 0;
        return adjustedRelations > 20;
    }
    private float techDealValue(EmpireView v) {
        if (v.embassy().alliance())
            return 1.0f;
        else if (v.embassy().pact())
            return 0.9f;
        else
            return 0.8f;
    }
    //-----------------------------------
    //  TRADE TREATIES
    //-----------------------------------
    @Override
    public boolean canOfferDiplomaticTreaties(Empire e) {
        if (!empire.inEconomicRange(id(e)))
            return false;
        return true;
    }
    @Override
    public boolean canOfferTradeTreaty(Empire e) {
        if (!empire.inEconomicRange(id(e)))
            return false;
        if(!e.inEconomicRange(empire.id))
            return false;

        EmpireView view = empire.viewForEmpire(id(e));

        if (!view.embassy().contact())
            return false;

        // no trade if no diplomats or at war
        if (!diplomats(id(e)) || empire.atWarWith(id(e)) )
            return false;
        // no trade offer if can't increase from current lvl
        if (view.nominalTradeLevels().isEmpty())
            return false;

        return true;
    }
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
        if (calculateTradeChance(v) < 0) {
            v.otherView().embassy().tradeRefused();
            return refuseOfferTrade(requestor, level);
        }

        v.otherView().embassy().tradeAccepted();
        v.otherView().embassy().establishTradeTreaty(level);
        return DiplomaticReplies.acceptTrade(v.otherView(), level);
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
        if (v.trade().active() && (v.trade().currentProfit() <= 0))
            return false;
        if (!v.trade().atProfitLimit())
            return false;
        
        // if asking player, check that we don't spam him
        if (v.empire().isPlayerControlled()) {
             if (!v.otherView().embassy().readyForTrade(level))
                return false;
        }

        float currentTrade = v.trade().profitLimit();
        float maxTrade = v.trade().maxProfitLimit();
        if (maxTrade < (currentTrade * 1.5))
            return false;

        log(v.toString(), ": willing to offer trade. Max:", str(maxTrade), "    current:", str(currentTrade));
        return calculateTradeChance(v) > 0;
    }
    private float calculateTradeChance(EmpireView v) {
        // -50 relations is minimum allowed to accept trade
        float chance = v.embassy().relations()+50;
        chance += v.empire().diplomacyBonus();
        chance += empire.leader().acceptTradeMod();
        chance += v.embassy().alliedWithEnemy() ? -50 : 0;
        return chance;
    }
    private String declineReasonText(EmpireView v) {
        DialogueManager dlg = DialogueManager.current();
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
        
        float autoAccept = bonus/200.0f;  //30% chance for humans
        if ((random() > autoAccept) && !warWeary(v))
            return refuseOfferPeace(requestor);

        DiplomaticIncident inc = v.embassy().signPeace();
        return v.otherView().accept(DialogueManager.ACCEPT_PEACE, inc);
    }
    @Override
    public DiplomaticReply acceptOfferPeace(Empire requestor) {
        EmpireView v = requestor.viewForEmpire(empire);
        DiplomaticIncident inc = v.embassy().signPeace();
        return v.otherView().accept(DialogueManager.ANNOUNCE_PEACE, inc);
    }
    @Override
    public DiplomaticReply refuseOfferPeace(Empire requestor) {
        EmpireView v = empire.viewForEmpire(requestor);
        v.embassy().resetPeaceTimer();
        return new DiplomaticReply(false, declineReasonText(v));
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
        
        float adjustedRelations = v.embassy().relations();
        adjustedRelations += empire.leader().acceptPactMod(requestor);
        adjustedRelations += requestor.diplomacyBonus();
        if (adjustedRelations < 20)
            return refuseOfferPact(requestor);

        v.embassy().signPact();
        return DiplomaticReplies.acceptPact(v.otherView());
    }
    @Override
    public DiplomaticReply refuseOfferPact(Empire requestor) {
        EmpireView v = empire.viewForEmpire(requestor);
        v.embassy().resetPactTimer();
        return new DiplomaticReply(false, declineReasonText(v));
    }
    private boolean willingToOfferPact(EmpireView v) {
        // if asking player, check that we don't spam him
        if (v.empire().isPlayerControlled()) {
            if (!v.otherView().embassy().readyForPact())
                return false;
        }
        if (!canOfferPact(v.empire()))
            return false;
        // how do we feel about them
        float adjustedRelations = v.embassy().relations();
        adjustedRelations += empire.leader().acceptPactMod(v.empire());
        adjustedRelations += v.embassy().alliedWithEnemy() ? -50 : 0;
        return adjustedRelations > 30;
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
        
        // be more willing if the requestor is someone we can use the alliance
        // to help us fight a war
        int joinWarBonus = 0;
        for (Empire enemy: myEnemies) {
            if (!requestor.atWarWith(enemy.id) && requestor.inEconomicRange(enemy.id))
                joinWarBonus = 30;
        }
        int erraticLeaderPenalty = requestor.leader().isErratic() ? -40 : 0;
 
        // if we don't like the requestor well enough, refuse now
        float adjustedRelations = v.embassy().relations();
        adjustedRelations += empire.leader().acceptAllianceMod(requestor);
        adjustedRelations += requestor.diplomacyBonus();
        adjustedRelations += joinWarBonus;
        adjustedRelations += erraticLeaderPenalty;
        if (adjustedRelations < 60)
            return refuseOfferAlliance(requestor);
        
        v.embassy().signAlliance();
        return DiplomaticReplies.acceptAlliance(v);
    }
    @Override
    public DiplomaticReply refuseOfferAlliance(Empire requestor) {
        EmpireView v = empire.viewForEmpire(requestor);
        v.embassy().resetAllianceTimer();
        return new DiplomaticReply(false, declineReasonText(v));
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
        // do we like the other to want to join an alliance
        float adjustedRelations = v.embassy().relations();
        adjustedRelations += empire.leader().acceptAllianceMod(e);
        return adjustedRelations > 70;
    }
//-----------------------------------
//  JOINT WARS
//-----------------------------------
    private boolean willingToRequestAllyToJoinWar(Empire friend, Empire target) {
        // this method is called only for targets that we are at explicit war with
        // and the friend is our ALLY
        
        // if he's already at war, don't bother
        if (friend.atWarWith(target.id))
            return false;
        // if he's not in economic range, don't bother
        if (!friend.inEconomicRange(target.id))
            return false;
        return true;
    }
    private boolean willingToOfferJointWar(Empire friend, Empire target) {
        // this method is called only for targets that we are at war with
        // or targets we are preparing for war with
        
        if (friend.isPlayerControlled()) {
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
        // if he's not in economic range, don't bother
        if (!friend.inEconomicRange(target.id))
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

        // never willing to declare war on an ally unless we are ruthless
        if (empire.alliedWith(target.id) && !empire.leader().isRuthless())
            return v.refuse(DialogueManager.DECLINE_NO_WAR_ON_ALLY, target);
        
        // never willing to declare war on an NAP partner if we are honorable
        if (empire.pactWith(target.id) && empire.leader().isHonorable())
            return v.refuse(DialogueManager.DECLINE_OFFER, target);
        
        // if a peacy treaty is in effect with the target, then refuse
        if (empire.viewForEmpire(target.id).embassy().atPeace()) {
            return v.refuse(DialogueManager.DECLINE_PEACE_TREATY, target);
        }

         // will always declare war if allied with the requestor and he is already at war with the target
        if (requestor.alliedWith(id(empire)) && requestor.atWarWith(target.id))
            return agreeToJointWar(requestor, target);

        int maxBribe = galaxy().numberTurns()*50;
        float bribeValue = bribeAmountToJointWar(target);
        
        if (empire.alliedWith(target.id))
            bribeValue *= 2;
        else if (empire.pactWith(target.id))
            bribeValue *= 1.5;
        if (empire.leader().isPacifist())
            bribeValue *= 2;
        
        List<Tech> allTechs = v.spies().unknownTechs();
        if (allTechs.isEmpty())
            return v.refuse(DialogueManager.DECLINE_OFFER, target);

        Tech.comparatorCiv = empire;
        Collections.sort(allTechs, Tech.WAR_TRADE_VALUE);
        
        List<String> requestedTechs = new ArrayList<>();
        for (Tech tech : allTechs) {
            if ((bribeValue > 0) && (requestedTechs.size() < 3)) {
                requestedTechs.add(tech.id());
                bribeValue -= tech.researchCost();
            }
        }
        if (requestedTechs.isEmpty())
            requestedTechs.add(allTechs.get(0).id());
        
        if (bribeValue > maxBribe)
            return v.refuse(DialogueManager.DECLINE_OFFER, target);
        return v.counter(DialogueManager.COUNTER_JOINT_WAR, target, requestedTechs, bribeValue);
    }
    @Override
    public DiplomaticReply receiveCounterJointWar(Empire requestor, DiplomaticCounterReply counter) {
        for (String techId: counter.techs())
            empire.tech().acquireTechThroughTrade(techId, requestor.id);
        
        if (counter.bribe() > 0) {
            empire.addToTreasury(counter.bribe());
            requestor.addToTreasury(0-counter.bribe());
        }
        return agreeToJointWar(requestor, counter.target());
    }
    private DiplomaticReply agreeToJointWar(Empire requestor, Empire target) {
        int targetId = target.id;
        if (!requestor.atWarWith(targetId))
            requestor.viewForEmpire(targetId).embassy().declareWar();
 
        DiplomaticIncident inc =  empire.viewForEmpire(targetId).embassy().declareJointWar(requestor);
        return empire.viewForEmpire(requestor).accept(DialogueManager.ACCEPT_JOINT_WAR, inc);
    }
    private float bribeAmountToJointWar(Empire target) {
        EmpireView v = empire.viewForEmpire(target);
        float myFleets = empire.totalArmedFleetSize();
        float tgtFleets = empire.totalFleetSize(target);
        float myTech = empire.tech().avgTechLevel();
        float tgtTech = v.spies().tech().avgTechLevel();
        float fleetShortcoming = (tgtFleets*tgtTech)-(myFleets*myTech);
        return max(0, fleetShortcoming);
    }
    @Override
    public DiplomaticReply acceptOfferJointWar(Empire requestor, Empire target) {
        int targetId = target.id;
        if (!requestor.atWarWith(targetId))
            requestor.viewForEmpire(targetId).embassy().declareWar();
 
        DiplomaticIncident inc = empire.viewForEmpire(targetId).embassy().declareJointWar(requestor);
        return empire.viewForEmpire(requestor).accept(DialogueManager.ACCEPT_JOINT_WAR, inc);
    }
    @Override
    public DiplomaticReply refuseOfferJointWar(Empire requestor, Empire target) {
        EmpireView v = empire.viewForEmpire(requestor);
        v.embassy().resetJointWarTimer();
        
        if (empire.alliedWith(requestor.id) && requestor.atWarWith(target.id))
            return requestor.diplomatAI().receiveBreakAlliance(empire);
        return null;
    }
    //-----------------------------------
    //  BREAK TREATIES
    //-----------------------------------
    @Override
    public boolean canDeclareWar(Empire e)                 { return empire.inEconomicRange(id(e)) && !empire.atWarWith(id(e)) && !empire.alliedWith(id(e)); }
    @Override
    public boolean canThreaten(Empire e) {
        if (!diplomats(id(e)))
            return false;
        return canEvictSpies(e) || canThreatenSpying(e) || canThreatenAttacking(e);
    }
    @Override
    public boolean canThreatenSpying(Empire e) {
        if (!empire.inEconomicRange(id(e)))
            return false;
        if (empire.atWarWith(id(e)))
            return false;
        
        SpyReport rpt = e.viewForEmpire(empire).spies().report();
        Mission miss = rpt.confessedMission();
        return ((rpt.spiesLost() > 0)
            && ((miss == Mission.ESPIONAGE) || (miss == Mission.SABOTAGE)));
            
    }
    @Override
    public boolean canEvictSpies(Empire e) {
        if (!empire.inEconomicRange(id(e)))
            return false;
        if (empire.atWarWith(id(e)))
            return false;
       
        SpyReport rpt = e.viewForEmpire(empire).spies().report();
        return rpt.spiesLost() > 0;
    }
    @Override
    public boolean canThreatenAttacking(Empire e) {
        if (!empire.inEconomicRange(id(e)))
            return false;
        if (empire.atWarWith(id(e)))
            return false;
        
        EmpireView v = e.viewForEmpire(empire);
        if (v.embassy().hasCurrentAttackIncident())
            return true;
        return false;
    }
    @Override
    public DiplomaticReply receiveBreakPact(Empire e) {
        EmpireView v = empire.viewForEmpire(e);
        v.embassy().noteRequest();
        DiplomaticIncident inc = v.otherView().embassy().breakPact();
        v.embassy().withdrawAmbassador();
        return v.otherView().accept(DialogueManager.RESPOND_BREAK_PACT, inc);
    }
    @Override
    public DiplomaticReply receiveBreakAlliance(Empire e) {
        EmpireView v = empire.viewForEmpire(e);
        v.embassy().noteRequest();
        DiplomaticIncident inc = v.otherView().embassy().breakAlliance();
        v.embassy().withdrawAmbassador();
        return v.otherView().accept(DialogueManager.RESPOND_BREAK_ALLIANCE, inc);
    }
    @Override
    public DiplomaticReply receiveBreakTrade(Empire e) {
        EmpireView v = empire.viewForEmpire(e);
        v.embassy().noteRequest();
        DiplomaticIncident inc = v.otherView().embassy().breakTrade();
        v.embassy().withdrawAmbassador();
        return v.otherView().accept(DialogueManager.RESPOND_BREAK_TRADE, inc);
    }
    @Override
    public DiplomaticReply receiveThreatStopSpying(Empire dip) {
        EmpireView v = empire.viewForEmpire(dip);
        
        v.embassy().noteRequest();
        v.embassy().withdrawAmbassador();
        
        if (empire.atWarWith(dip.id) || v.embassy().onWarFooting()) {
            v.spies().ignoreThreat();
            return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
        }

        if (empire.leader().isPacifist() || empire.leader().isHonorable()) {
            if (dip.leader().isXenophobic()) {
                empire.shutdownSpyNetworksAgainst(dip.id);
                v.spies().heedEviction();
            }
            else {
                empire.hideSpiesAgainst(dip.id);
                v.spies().heedThreat();
            }
            return empire.respond(DialogueManager.RESPOND_STOP_SPYING, dip);
        }
                   
        float otherPower = empire.militaryPowerLevel(dip);
        float myPower = empire.militaryPowerLevel();
        float powerRatio = myPower/otherPower;

        if (powerRatio > 2) {
            v.spies().ignoreThreat();
            return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
        }
            
        if (dip.leader().isXenophobic()) {
            empire.shutdownSpyNetworksAgainst(dip.id);
            v.spies().heedEviction();
        }
        else {
            empire.hideSpiesAgainst(dip.id);
            v.spies().heedThreat();
        }
        return empire.respond(DialogueManager.RESPOND_STOP_SPYING, dip);
    }
    @Override
    public DiplomaticReply receiveThreatEvictSpies(Empire dip) {
        EmpireView v = empire.viewForEmpire(dip);
        
        v.embassy().noteRequest();
        v.embassy().withdrawAmbassador();

        v.embassy().addIncident(SimpleIncident.createEvictedSpiesIncident(v));
        
        if (empire.atWarWith(dip.id) || v.embassy().onWarFooting()) {
            v.spies().ignoreThreat();
            return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
        }

        if (empire.leader().isPacifist() || empire.leader().isHonorable()) {
            empire.shutdownSpyNetworksAgainst(dip.id);
            v.spies().heedEviction();
            return empire.respond(DialogueManager.RESPOND_STOP_SPYING, dip);
        }
                   
        float otherPower = empire.militaryPowerLevel(dip);
        float myPower = empire.militaryPowerLevel();
        float powerRatio = myPower/otherPower;

        if (powerRatio > 2) {
            v.spies().ignoreThreat();
            return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
        }
            
        empire.shutdownSpyNetworksAgainst(dip.id);
        v.spies().heedEviction();
        return empire.respond(DialogueManager.RESPOND_STOP_SPYING, dip);
    }
    @Override
    public DiplomaticReply receiveThreatStopAttacking(Empire dip) {
        EmpireView v = empire.viewForEmpire(dip);

        v.embassy().noteRequest();
        v.embassy().withdrawAmbassador();
        
        if (empire.atWarWith(dip.id) || v.embassy().onWarFooting()) {
            v.embassy().ignoreThreat();
            return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
        }

        if (empire.leader().isPacifist()) {
            empire.retreatShipsFrom(dip.id);
            v.embassy().heedThreat();
            return empire.respond(DialogueManager.RESPOND_STOP_ATTACKING, dip);
        }
                   
        float otherPower = empire.militaryPowerLevel(dip);
        float myPower = empire.militaryPowerLevel();
        float powerRatio = myPower/otherPower;

        if (powerRatio > 2) {
            v.embassy().ignoreThreat();
            return empire.respond(DialogueManager.RESPOND_IGNORE_THREAT, dip);
        }
            
        empire.retreatShipsFrom(dip.id);
        v.embassy().heedThreat();
        return empire.respond(DialogueManager.RESPOND_STOP_ATTACKING, dip);
    }
    @Override
    public DiplomaticReply receiveDeclareWar(Empire e) {
        EmpireView v = empire.viewForEmpire(e);

        v.embassy().noteRequest();
        DiplomaticIncident inc = v.embassy().declareWar();

        return empire.respond(DialogueManager.DECLARE_HATE_WAR, inc, e);
    }
    private boolean decidedToBreakAlliance(EmpireView view) {
        if (!wantToBreakAlliance(view))
            return false;

        view.embassy().breakAlliance();
        if (view.empire().isPlayerControlled())
            DiplomaticNotification.create(view, DialogueManager.BREAK_ALLIANCE);
        return true;
    }
    private boolean wantToBreakAlliance(EmpireView v) {
        if (!v.embassy().alliance())
            return false;
        
        if (wantToDeclareWarOfOpportunity(v))
            return true;
        
        float adjustedRelations = v.embassy().relations();
        adjustedRelations += empire.leader().preserveTreatyMod();
        return adjustedRelations < 20;
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

        float adjustedRelations = v.embassy().relations();
        adjustedRelations += empire.leader().preserveTreatyMod();
        return adjustedRelations < -20;
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
        if (!v.trade().active())
            return false;
        
        float treatyMod = empire.leader().preserveTreatyMod();
        return calculateTradeChance(v) + treatyMod < -40;
    }
    //----------------
//
//----------------
    @Override
    public void makeDiplomaticOffers(EmpireView v) {
        if (v.embassy().contactAge() < 2)
            return;

        if (v.embassy().diplomatGone() || v.otherView().embassy().diplomatGone())
            return;

        // check diplomat offers from worst to best
        if (decidedToDeclareWar(v))
            return;
        if (decidedToBreakAlliance(v))
            return;
        if (decidedToBreakPact(v))
            return;
        if (decidedToBreakTrade(v))
            return;
        if (decidedToIssueWarning(v))
            return;

        if (willingToOfferPeace(v)) {
            if (v.embassy().war())
                v.empire().diplomatAI().receiveOfferPeace(empire);
            else
                v.embassy().endWarPreparations();
            return;
        }
        
        // we can issue praise even to people we don't like
        if (decidedToIssuePraise(v))
            return;

        // if this empire is at war with us or we are preparing
        // for war, then stop now. No more Mr. Nice Guy.
        List<Empire> enemies = empire.enemies();
        if (enemies.contains(v.empire()))
            return;
        
        if (decidedToExchangeTech(v))
            return;

        if (willingToOfferTrade(v, v.trade().maxProfitLimit())) {
            v.empire().diplomatAI().receiveOfferTrade(v.owner(), v.trade().maxProfitLimit());
            return;
        }
        if (willingToOfferPact(v)) {
            v.empire().diplomatAI().receiveOfferPact(empire);
            return;
        }
        if (willingToOfferAlliance(v.empire())) {
            v.empire().diplomatAI().receiveOfferAlliance(v.owner());
            return;
        }
        // build a priority list for Joint War offers:
        // 1. see if we can draw our ally into our existing war
        // 2. if not, what about the empire we are about to war with?
        List<Empire> warEnemies = empire.warEnemies();
        List<Empire> comingWarEnemies = empire.enemies();
        comingWarEnemies.removeAll(warEnemies);

        // ask only allies for now, to avoid spam
        if (v.embassy().alliance()) {
            for (Empire target: warEnemies) {
                if (willingToRequestAllyToJoinWar(v.empire(), target)) {
                    v.empire().diplomatAI().receiveOfferJointWar(v.owner(), target);
                    return;
                }
            }
            for (Empire target: comingWarEnemies) {
                if (willingToOfferJointWar(v.empire(), target)) {
                    v.empire().diplomatAI().receiveOfferJointWar(v.owner(), target);
                    return;
                }
            }
        }
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
            DiplomaticNotification.create(view, maxIncident, maxIncident.warningMessageId());
        }
        return true;
    }
    private boolean decidedToDeclareWar(EmpireView view) {
        if (empire.isPlayerControlled())
            return false;
        if (view.embassy().war())
            return false;
        if(!empire.inShipRange(view.empId()))
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
                beginIncidentWar(view, warIncident);
                return true;
            }
        }
        
        // 2% chance of war if erratic leader (these guys are crazy)
        if (empire.leader().isErratic() && (random() <= ERRATIC_WAR_PCT)) {
            beginErraticWar(view);
            return true;
        }
        // modnar: less likely war when already in some wars
        // asymptotic x/(1+abs(x))
        if (empire.numEnemies()/(1+Math.abs(empire.numEnemies())) > random())
            return false;
        // automatic war of hate if relations less < -90
        // and not currently in a timed peace treaty
        if (wantToDeclareWarOfHate(view)){
            beginHateWar(view);
            return true;
        }
        
        // must break alliance before declaring war
        if (wantToDeclareWarOfOpportunity(view)) {
            beginOpportunityWar(view);
            return true;
        }

        return false;
    }
    @Override
    public boolean wantToDeclareWarOfHate(EmpireView v) {
        if (v.embassy().atPeace())
            return false;
        
        float warThreshold = -80;
        warThreshold += v.empire().leader().isPacifist() ? -10 : 0;
        warThreshold += v.empire().leader().isAggressive() ? 10 : 0;
        
        // modnar: change war threshold by number of our wars vs. number of their wars
        // try not to get into too many wars, and pile on if target is in many wars
        warThreshold += (float) (10 * (v.empire().numEnemies() - empire.numEnemies()));
        
        // allied with an enemy? not good
        if (v.embassy().alliedWithEnemy())
            warThreshold += 30;
        
        // higher contempt = more likely to increase war
        // positive contempt raises the threshold = more likely for war
        // if relative power is 3, then contempt mod is 30 or -30
        warThreshold += 10 * scaleOfContempt(v);
        
        return (v.embassy().relations() <= warThreshold);
    }
    private float scaleOfContempt(EmpireView view) {
        // returns 0 if equal power
        // returns 1, 2, 3 if we are 2x,3x,4x more powerful
        // reutrns -1,-2,-3 if we are 1/2x, 1/3x, 1/4x as powerful
        float powerRatio = view.empirePower();
        if (powerRatio >= 1) {
            return -powerRatio+1;
        } else {
            return (1/powerRatio)-1;
        }
    }
    @Override
    public boolean wantToDeclareWarOfOpportunity(EmpireView v) {
        if (v.embassy().atPeace())
            return false;
        if (v.embassy().alliance())
            return false;
        if (v.owner().leader().isPacifist())
            return false;
        if (v.owner().leader().isHonorable()
        && (v.embassy().pact() || v.embassy().alliance()))
            return false;
        
        // modnar: less likely war when already in some wars
        // asymptotic x/(1+abs(x))
        if (empire.numEnemies()/(1+Math.abs(empire.numEnemies())) > random())
            return false;
        
        // don't declare if we have no spy data or data is too old
        int reportAge = v.spies().reportAge();
        if ((reportAge < 0) || (reportAge > 10))
            return false;
        
        // base power is an minimum power level that is added to both empires to
        // keep their power ratios from wildly fluctuating early in the game when
        // everyone has small fleets, so that wars aren't triggered because I have
        // 4 fighters and you have 1.
        // modnar: reduce basePower due to other changes (techMod, enemyMod)
        int basePower = 200;
        
        float otherPower = basePower+v.empire().status().lastViewValue(empire, FLEET) * v.spies().tech().avgTechLevel();
        float myPower = basePower+v.owner().totalFleetSize() * empire.tech().avgTechLevel();
        
        // modnar: due to other changes (techMod, enemyMod), reduce baseThreshold
        float baseThreshold = v.owner().atWar() ? 8.0f : 4.0f;
        float treatyMod = v.embassy().pact() || v.embassy().alliance() ? 1.5f : 1.0f;
        
        // modnar: factor in own empire average tech level
        // suppress war in early game when average tech level is below 8
        float myTechLvl = v.owner().tech().avgTechLevel(); // minimum average tech level is 1.0
        float techMod = 1.0f;
        if (myTechLvl < 8.0f) {
            techMod = 8.0f / myTechLvl; // inverse change with tech level (range from 8.0 to 1.0)
        }
        
        // modnar: scale war threshold by number of our wars vs. number of their wars
        // try not to get into too many wars, and pile on if target is in many wars
        float enemyMod = (float)(empire.numEnemies() + 1) / (v.empire().numEnemies() + 1);
        float warThreshold = baseThreshold * techMod * enemyMod * treatyMod * exploitWeakerEmpiresRatio(v.owner().leader());
        
        return (myPower/otherPower) > warThreshold;
    }
    private float exploitWeakerEmpiresRatio(Leader leader) {
        float ratio = 1.0f;
        if (leader.isAggressive())
            ratio /= 2;
        if (leader.isMilitarist())
            ratio /= 1.5;
        if (leader.isHonorable())
            ratio *= 2;
        if (leader.isPacifist())
            ratio *= 2;
        if (leader.isXenophobic())
            ratio *= 1.5;
        if (leader.isExpansionist())
            ratio /= 2;
        return ratio;
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
    private void beginHateWar(EmpireView view) {
        log(view+" - Declaring war based on hate");
        view.embassy().beginWarPreparations(DialogueManager.DECLARE_HATE_WAR, null);
    }
    private void beginErraticWar(EmpireView view) {
        log(view+" - Declaring war based on erratic");
        view.embassy().beginWarPreparations(DialogueManager.DECLARE_ERRATIC_WAR, null);
    }
    @Override
    public Empire councilVoteFor(Empire civ1, Empire civ2) {
        // always vote for yourself
        if (civ1 == empire) return castVoteFor(civ1);
        if (civ2 == empire) return castVoteFor(civ2);
        
        float voteWeight = calculateVoteWeight(civ1) - calculateVoteWeight(civ2);

        // Select prime candidate
        Empire primeCandidate = civ1;
        if (voteWeight < 0) {
            primeCandidate = civ2;
            voteWeight = -voteWeight;
        }
        
        // Truncate vote probabilities
        if (voteWeight < 0.3f) {
            return castVoteFor(null);
        } else if (voteWeight > 0.7f) {
            return castVoteFor(primeCandidate);
        } else if (random() <= voteWeight) {
            return castVoteFor(primeCandidate);
        } else {
            return castVoteFor(null);
        }
    }
    
    private float calculateVoteWeight(Empire candidate) {
        EmpireView view = empire.viewForEmpire(candidate);
        float weight = view.embassy().relations()/100.0f + candidate.orionCouncilBonus();
        weight += candidate.id == empire.lastCouncilVoteEmpId() ? 0.5f : 0;
        weight += view.embassy().alliance() ? 1 : 0;
        weight += view.embassy().pact() ? 0.5f : 0;
        weight += view.embassy().war() ? -1 : 0;
        return weight;
    }
    
    private Empire castVoteFor(Empire c) {
        if (c == null)
            empire.lastCouncilVoteEmpId(Empire.ABSTAIN_ID);
        else
            empire.lastCouncilVoteEmpId(c.id);
        return c;
    }
    //-----------------------------------
    // INCIDENTS
    //-----------------------------------
    @Override
    public void noticeIncident(DiplomaticIncident inc, Empire emp) {
        EmpireView view = empire.viewForEmpire(emp);
        
        view.embassy().addIncident(inc);

        if (inc.triggersWar())
            beginIncidentWar(view, inc);
    }
   private boolean warWeary(EmpireView v) {
        if(!empire.inShipRange(v.empId()))
            return true;
        // modnar: scale warWeary by number of our wars vs. number of their wars
        // more weary (willing to take less losses) if we are in more wars than they are
        // willing to take at least 15% losses
        float enemyMod = (float) ((empire.numEnemies() + 10) / (v.empire().numEnemies() + 10));
        
        Empire emp = v.owner();
        TreatyWar treaty = (TreatyWar) v.embassy().treaty();
        if (treaty.colonyChange(emp) < (int)Math.min(0.85, enemyMod*warColonyLossLimit(v)))
            return true;
        if (treaty.populationChange(emp) < (int)Math.min(0.85, enemyMod*warPopulationLossLimit(v)))
            return true;
        if (treaty.factoryChange(emp) < (int)Math.min(0.85, enemyMod*warFactoryLossLimit(v)))
            return true;
        if (treaty.fleetSizeChange(emp) < (int)Math.min(0.85, enemyMod*warFleetSizeLossLimit(v)))
            return true;

        // for pop, factories and ships, calculate the pct lost vs the
        // pct we were willing to lose (1-limit). If any of those are >1
        // or if they total up to > 2, then we are tired.
        
        // Example: Pacifist will quit at 20% pop loss,
        float popPct = treaty.populationLostPct(emp) / (1-warPopulationLossLimit(v));
        if (popPct >= 1)
            return true;
        
        float factPct = treaty.factoryLostPct(emp) / (1-warFactoryLossLimit(v));
        if (factPct >= 1)
            return true;
        
        float fleetPct = treaty.fleetSizeLostPct(emp) / (1-warFleetSizeLossLimit(v));
        if (fleetPct >= 1)
            return true;
        
        return (popPct + factPct + fleetPct) > 2;
    }
    private float warColonyLossLimit(EmpireView v) {
        switch(v.owner().leader().objective) {
            case MILITARIST:    return 0.6f;
            case ECOLOGIST:     return 0.8f;
            case DIPLOMAT:      return 0.6f;
            case INDUSTRIALIST: return 0.6f;
            case EXPANSIONIST:  return 0.8f;
            case TECHNOLOGIST:  return 0.6f;
            default:            return 0.6f;
        }
    }
    private float warPopulationLossLimit(EmpireView v) {
        switch(v.owner().leader().personality) {
            case PACIFIST:   return 0.8f;
            case HONORABLE:  return 0.6f;
            case XENOPHOBIC: return 0.6f;
            case RUTHLESS:   return 0.4f;
            case AGGRESSIVE: return 0.6f;
            case ERRATIC:    return 0.6f;
            default:         return 0.6f;
        }
    }
    private float warFactoryLossLimit(EmpireView v) {
        switch(v.owner().leader().objective) {
            case MILITARIST:    return 0.6f;
            case ECOLOGIST:     return 0.4f;
            case DIPLOMAT:      return 0.6f;
            case INDUSTRIALIST: return 0.8f;
            case EXPANSIONIST:  return 0.6f;
            case TECHNOLOGIST:  return 0.6f;
            default:            return 0.6f;
        }
    }
    private float warFleetSizeLossLimit(EmpireView v) {
        switch(v.owner().leader().objective) {
            case MILITARIST:    return 0.5f;
            case ECOLOGIST:     return 0.3f;
            case DIPLOMAT:      return 0.3f;
            case INDUSTRIALIST: return 0.3f;
            case EXPANSIONIST:  return 0.3f;
            case TECHNOLOGIST:  return 0.3f;
            default:            return 0.3f;
        }
    }
}

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
package rotp.model.ai.interfaces;

import java.util.List;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.tech.Tech;
import rotp.ui.diplomacy.DiplomaticCounterReply;
import rotp.ui.diplomacy.DiplomaticReply;

public interface Diplomat {
    boolean canOfferDiplomaticTreaties(Empire e);
    boolean canOfferTradeTreaty(Empire e);
    boolean canExchangeTechnology(Empire e);
    boolean canDeclareWar(Empire e);
    boolean canThreaten(Empire e);
    boolean canEvictSpies(Empire e);
    boolean canThreatenSpying(Empire e);
    boolean canThreatenAttacking(Empire e);
    
    void noticeIncident(DiplomaticIncident inc, Empire e);
    void makeDiplomaticOffers(EmpireView ev);
    Empire councilVoteFor(Empire emp1, Empire emp2);
    
    DiplomaticReply receiveThreatEvictSpies(Empire e);
    DiplomaticReply receiveThreatStopSpying(Empire e);
    DiplomaticReply receiveThreatStopAttacking(Empire e);
    DiplomaticReply receiveDeclareWar(Empire e);
    DiplomaticReply receiveOfferPeace(Empire e);
    DiplomaticReply receiveOfferTrade(Empire e, int level);
    DiplomaticReply receiveOfferPact(Empire e);
    DiplomaticReply receiveOfferAlliance(Empire e);
    DiplomaticReply receiveOfferJointWar(Empire e, Empire target);
    DiplomaticReply receiveBreakTrade(Empire e);
    DiplomaticReply receiveBreakPact(Empire e);
    DiplomaticReply receiveBreakAlliance(Empire e);
    DiplomaticReply receiveCounterJointWar(Empire e, DiplomaticCounterReply reply);
    DiplomaticReply acceptOfferPeace(Empire e);
    DiplomaticReply refuseOfferPeace(Empire e);
    DiplomaticReply refuseOfferTrade(Empire e, int level);
    DiplomaticReply refuseOfferPact(Empire e);
    DiplomaticReply refuseOfferAlliance(Empire e);
    DiplomaticReply acceptOfferJointWar(Empire e, Empire target);
    DiplomaticReply refuseOfferJointWar(Empire e, Empire target);

    boolean wantToDeclareWarOfHate(EmpireView v);
    boolean wantToDeclareWarOfOpportunity(EmpireView v);
    
    List<Tech> techsAvailableForRequest(Empire emp);
    List<Tech> techsRequestedForCounter(Empire emp, Tech t);
    DiplomaticReply receiveRequestTech(Empire emp, Tech t);
    DiplomaticReply receiveCounterOfferTech(Empire e, Tech counter, Tech wanted);
    
    //Xilmi-AI:
    default int popCapRank(Empire etc, boolean inAttackRange) { return 1; }
    default int techLevelRank() { return 1; }
    default int facCapRank() { return 1; }
    default int militaryRank(Empire etc, boolean inAttackRange) { return 1; }
    default boolean techIsAdequateForWar() { return true; }
    default boolean minWarTechsAvailable() { return true; }
}

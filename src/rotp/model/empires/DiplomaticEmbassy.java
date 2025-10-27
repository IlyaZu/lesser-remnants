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
package rotp.model.empires;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import rotp.model.galaxy.StarSystem;
import rotp.model.incidents.AlliedWithEnemyIncident;
import rotp.model.incidents.AtWarWithAllyIncident;
import rotp.model.incidents.BreakAllianceIncident;
import rotp.model.incidents.BreakPactIncident;
import rotp.model.incidents.DeclareWarIncident;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.DriftRelationsIncident;
import rotp.model.incidents.ErraticWarIncident;
import rotp.model.incidents.ExchangeTechnologyIncident;
import rotp.model.incidents.ExpansionIncident;
import rotp.model.incidents.FinancialAidIncident;
import rotp.model.incidents.MilitaryBuildupIncident;
import rotp.model.incidents.OathBreakerIncident;
import rotp.model.incidents.SignPeaceIncident;
import rotp.model.incidents.SimpleIncident;
import rotp.model.incidents.TechnologyAidIncident;
import rotp.model.incidents.TrespassingIncident;
import rotp.model.tech.Tech;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.DiplomaticNotification;
import rotp.ui.notifications.GNNAllianceBrokenNotice;
import rotp.ui.notifications.GNNAllianceFormedNotice;
import rotp.ui.notifications.GNNAllyAtWarNotification;
import rotp.util.Base;

public class DiplomaticEmbassy implements Base, Serializable {
    private static final long serialVersionUID = 1L;

    private static final int TECH_DELAY = 1;
    private static final int TRADE_DELAY = 10;
    private static final int PEACE_DELAY = 10;
    private static final int PACT_DELAY = 20;
    private static final int ALLIANCE_DELAY = 30;
    private static final int JOINT_WAR_DELAY = 20;

    private final EmpireView view;
    private HashMap<String, List<String>> offeredTechs = new HashMap<>();
    private final List<DiplomaticIncident> incidents = new LinkedList<>();
    private transient List<DiplomaticIncident> newIncidents = new ArrayList<>();

    private boolean contact = false;
    private int contactTurn = 0;
    private int treatyTurn = -1;
    private DiplomaticTreaty treaty;

    private float relations;
    private int tradeTimer = 0;
    private int lastRequestedTradeLevel = 0;
    private int tradeRefusalCount = 0;
    private int techTimer = 0;
    private int peaceTimer = 0;
    private int pactTimer = 0;
    private int allianceTimer = 0;
    private int jointWarTimer = 0;
    private int diplomatGoneTimer = 0;
    private boolean tradePraised = false;
    private int requestCount = 0;
    private int minimumPraiseLevel = 0;
    private boolean givenAidThisTurn = false;
    private boolean onLastWarning = false;

    public Empire empire()                               { return view.empire(); }
    public Empire owner()                                { return view.owner(); }
    public int treatyTurn()                            { return treatyTurn; }
    public DiplomaticTreaty treaty()                     { return treaty; }
    public String treatyStatus()                         { return treaty.status(owner()); }
    public Collection<DiplomaticIncident> allIncidents() { return incidents; }
    public int requestCount()                            { return requestCount; }
    public float relations()                             { return relations; }
    public boolean contact()                             { return contact; }
    public List<DiplomaticIncident> newIncidents() {
        if (newIncidents == null)
            newIncidents = new ArrayList<>();
        return newIncidents;
    }
    private HashMap<String, List<String>> offeredTechs() {
        if (offeredTechs == null)
            offeredTechs = new HashMap<>();
        return offeredTechs;
    }
    public DiplomaticEmbassy(EmpireView v) {
        view = v;
        setNoTreaty();
        relations = baseRelations();
    }
    public final void setNoTreaty() {
        treaty = new TreatyNone(view.owner(), view.empire());
    }
    public float currentSpyIncidentSeverity() {
        float sev = 0;
        for (DiplomaticIncident inc: allIncidents()) {
            if (inc.isSpying())
                sev += inc.severity();
        }
        return max(-50,sev);
    }
    public void nextTurn(float prod) {
        treaty.nextTurn(empire());
    }
    public boolean war()                    { return treaty.isWar();     }
    public boolean noTreaty()               { return treaty.isNoTreaty(); }
    public boolean pact()                   { return treaty.isPact(); }
    public boolean alliance()               { return treaty.isAlliance(); }
    public boolean isFriend()               { return pact() || alliance(); }
    public boolean atPeace()                { return treaty.isPeace(); }
    
    public boolean readyForTrade(int level) {
        // trade cooldown timer must be back to zero -AND-
        // new trade level must exceed last requested level by 25% * each consecutive refusal
        return (tradeTimer <= 0)
        && (level > (lastRequestedTradeLevel*(1+(tradeRefusalCount/4.0))));
    }
    public void resetTradeTimer(int level)  {
        if (empire().isPlayerControlled())
            tradeTimer = 1;
        else {
            tradeTimer = TRADE_DELAY;
            lastRequestedTradeLevel = level;
        }
    }
    public void tradeRefused()              { tradeRefusalCount++; }
    public void tradeAccepted()             { tradeRefusalCount = 0; }
    public boolean alreadyOfferedTrade()    { return tradeTimer == TRADE_DELAY; }
    public boolean readyForTech()           { return techTimer <= 0; }
    public void resetTechTimer()            { techTimer = TECH_DELAY; }
    public boolean alreadyOfferedTech()     { return techTimer == TECH_DELAY; }
    public boolean readyForPeace()          { return peaceTimer <= 0; }
    public void resetPeaceTimer()           { resetPeaceTimer(1); }
    public void resetPeaceTimer(int mult)   { peaceTimer = mult*PEACE_DELAY; }
    public boolean alreadyOfferedPeace()    { return peaceTimer == PEACE_DELAY; }
    public boolean readyForPact()           { return pactTimer <= 0; }
    public void resetPactTimer()            { pactTimer = PACT_DELAY; }
    public boolean alreadyOfferedPact()     { return pactTimer == PACT_DELAY; }
    public boolean readyForAlliance()       { return allianceTimer <= 0; }
    public void resetAllianceTimer()        { allianceTimer = ALLIANCE_DELAY; }
    public boolean alreadyOfferedAlliance() { return allianceTimer == ALLIANCE_DELAY; }
    public boolean readyForJointWar()       { return jointWarTimer <= 0; }
    public void resetJointWarTimer()        { jointWarTimer = empire().isPlayerControlled() ? JOINT_WAR_DELAY : 1; }
    public boolean alreadyOfferedJointWar() { return jointWarTimer == JOINT_WAR_DELAY; }
    public int minimumPraiseLevel()         {
        // raise threshold for praise when at war
        if (war())
            return max(50, minimumPraiseLevel);
        else
            return max(10, minimumPraiseLevel);
    }
    public void praiseSent() {
        minimumPraiseLevel = minimumPraiseLevel()+10;
        onLastWarning = false;
    }
    public void logWarning() {
        onLastWarning = true;
    }
    public boolean onLastWarning() {
        return onLastWarning;
    }
    public void noteRequest() {
        requestCount++;
    }
    
    public float otherRelations()          { return otherEmbassy().relations(); }
    public int contactAge()                 { return (galaxy().currentTurn() - contactTurn); }
    public DiplomaticEmbassy otherEmbassy() { return view.otherView().embassy(); }
    public boolean tradePraised()           { return tradePraised; }
    public void tradePraised(boolean b)     { tradePraised = b; }
    public void logTechExchangeRequest(Tech wantedTech, List<Tech> counterTechs) {
        if (!offeredTechs().containsKey(wantedTech.id()))
            offeredTechs().put(wantedTech.id(), new ArrayList<>());

        List<String> list = offeredTechs().get(wantedTech.id());
        for (Tech t: counterTechs) {
            if (!list.contains(t.id()))
                list.add(t.id());
        }
    }
    public List<Tech> alreadyOfferedTechs(Tech wantedTech) {
        if (!offeredTechs().containsKey(wantedTech.id()))
            return null;

        List<Tech> techs = new ArrayList<>();
        for (String s: offeredTechs().get(wantedTech.id()))
            techs.add(tech(s));

        return techs;
    }
    private void withdrawAmbassador(int turns) {
        diplomatGoneTimer = turns;
    }
    public void withdrawAmbassador() {
        int baseTurns = 2;
        if (empire().leader().isDiplomat())
            baseTurns /= 2;
        else if (empire().leader().isXenophobic())
            baseTurns *= 2;

        if (war())
            baseTurns *= 2;
        withdrawAmbassador(baseTurns+1);
    }
    public void assessTurn() {
        log(view+" Embassy: assess turn");
        resetIncidents();

        // player refusals are remembered for the
        // entire duration to avoid the AI spamming the player
        // AI  refusals are completely reset after each turn
        // to allow players to continue asking once each turn
        // if they want
        if (view.owner().isPlayerControlled()) {
            tradeTimer--;
            techTimer--;
            peaceTimer--;
            pactTimer--;
            allianceTimer--;
        }
        else {
            tradeTimer = 0;
            techTimer = 0;
            peaceTimer = 0;
            pactTimer = 0;
            allianceTimer = 0;

        }
        
        diplomatGoneTimer--;
        requestCount = 0;
        minimumPraiseLevel = min(20,minimumPraiseLevel);
        minimumPraiseLevel = minimumPraiseLevel() - 1;
        givenAidThisTurn = false;
    }
    public void recallAmbassador()     { diplomatGoneTimer = Integer.MAX_VALUE; }
    public void openEmbassy()          { diplomatGoneTimer = 0; }
    public boolean diplomatGone()      { return diplomatGoneTimer > 0;  }
    public boolean wantWar()           { return otherEmbassy().relations() < -50; }
    public boolean isAlly()            { return alliance(); }
    public boolean alliedWithEnemy() {
        List<Empire> myEnemies = owner().enemies();
        List<Empire> hisAllies = empire().allies();
        for (Empire cv1 : myEnemies) {
            for (Empire cv2 : hisAllies) {
                if (cv1 == cv2)
                    return true;
            }
        }
        return false;
    }
    public boolean canAttackWithoutPenalty() { return war() || noTreaty(); }
    public boolean canAttackWithoutPenalty(StarSystem s) {
        if (war() || noTreaty())
            return true;
        if (pact())
            return (s.hasColonyForEmpire(owner()) || s.hasColonyForEmpire(empire()));
        if (alliance())
            return false;
        return !atPeace();
    }
    private void setTreaty(DiplomaticTreaty tr) {
        treaty = tr;
        otherEmbassy().treaty = tr;
        view.setSuggestedAllocations();
        view.otherView().setSuggestedAllocations();
    }

    public DiplomaticIncident exchangeTechnology(Tech offeredTech, Tech requestedTech) {
        // civ() is the requestor, and will be learning the requested tech
        // owner() is the requestee, who will be learning the counter-offered tech
        owner().tech().acquireTechThroughTrade(offeredTech.id, empire().id);
        empire().tech().acquireTechThroughTrade(requestedTech.id, owner().id);

        view.spies().noteTradedTech(requestedTech);
        view.otherView().spies().noteTradedTech(offeredTech);
        DiplomaticIncident inc = ExchangeTechnologyIncident.create(owner(), empire(), offeredTech, requestedTech);
        addIncident(inc);
        otherEmbassy().addIncident(ExchangeTechnologyIncident.create(empire(), owner(), requestedTech, offeredTech));
        return inc;
    }
    public void establishTradeTreaty(int level) {
        view.embassy().tradePraised(false);
        view.trade().startRoute(level);
    }
    public DiplomaticIncident declareJointWar(Empire requestor) {
        // when we are declaring a war as a result of a joint war request, ignore
        // any existing war cause. This ensures that a DeclareWarIncident is returned
        // instead of some existing war case incident. This ensures that [other...]
        // tags are replaced properly in the war announcement to the player
        return declareWar(requestor, null, null);
    }
    public DiplomaticIncident declareWar() {
        return declareWar(null, null, null);
    }
    public DiplomaticIncident declareWar(String warCauseId) {
        return declareWar(null, warCauseId, null);
    }
    public DiplomaticIncident declareWar(DiplomaticIncident warCauseIncident) {
        return declareWar(null, warCauseIncident.declareWarId(), warCauseIncident);
    }
    private DiplomaticIncident declareWar(Empire requestor, String warCauseId, DiplomaticIncident warCauseIncident) {
        endTreaty();
        int oathBreakType = 0;
        if (alliance())
            oathBreakType = 1;
        else if (pact())
            oathBreakType = 2;

        view.trade().stopRoute();

        // if we're not at war yet, start it and inform player if he is involved
        if (!war()) {
            setTreaty(new TreatyWar(view.owner(), view.empire()));
            if (view.empire().isPlayerControlled()) {
                if ((warCauseId == null) || warCauseId.isEmpty())
                    DiplomaticNotification.createAndNotify(view, DialogueManager.DECLARE_HATE_WAR);
                else
                    DiplomaticNotification.createAndNotify(view, warCauseId);
            }
        }

        resetPeaceTimer(3);
        withdrawAmbassador();
        otherEmbassy().withdrawAmbassador();

        // add war-causing incident to embassy
        DiplomaticIncident inc = warCauseIncident;
        if (inc == null) {
            if (warCauseId == null)
                inc = DeclareWarIncident.create(owner(), empire());
            else {
                switch(warCauseId) {
                    case DialogueManager.DECLARE_ERRATIC_WAR :
                        inc = ErraticWarIncident.create(owner(), empire()); break;
                    case DialogueManager.DECLARE_HATE_WAR:
                    default:
                        inc = DeclareWarIncident.create(owner(), empire());
                        oathBreakType = 0;
                        break;
                }
            }
        }
        otherEmbassy().addIncident(inc);

        // if oath broken, then create that incident as well
        switch(oathBreakType) {
            case 1:
                GNNAllianceBrokenNotice.create(owner(), empire());
                OathBreakerIncident.alertBrokenAlliance(owner(),empire(),requestor,false); break;
            case 2: OathBreakerIncident.alertBrokenPact(owner(),empire(),requestor,false); break;
        }
        
        // if the player is one of our allies, let him know
        for (Empire ally : owner().allies()) {
            if (ally.isPlayerControlled())
                GNNAllyAtWarNotification.create(owner(), empire());
        }
        // if the player is one of our enemy's allies, let him know
        for (Empire ally : empire().allies()) {
            if (ally.isPlayerControlled())
                GNNAllyAtWarNotification.create(empire(), owner());
        }

        return inc;
    }
    public DiplomaticIncident breakTrade() {
        view.trade().stopRoute();
        DiplomaticIncident inc = SimpleIncident.createBreakTradeIncident(view);
        otherEmbassy().addIncident(inc);
        return inc;
    }
    public DiplomaticIncident signPeace() {
        beginTreaty();
        int duration = roll(10,15);
        beginPeace(duration);
        otherEmbassy().beginPeace(duration);
        owner().hideSpiesAgainst(empire().id);
        empire().hideSpiesAgainst(owner().id);
        DiplomaticIncident inc = SignPeaceIncident.create(owner(), empire(), duration);
        addIncident(inc);
        otherEmbassy().addIncident(SignPeaceIncident.create(empire(), owner(), duration));
        return inc;
    }
    public void signPact() {
        beginTreaty();
        owner().hideSpiesAgainst(empire().id);
        empire().hideSpiesAgainst(owner().id);
        setTreaty(new TreatyPact(view.owner(), view.empire()));
    }
    public void reopenEmbassy() {
        diplomatGoneTimer = 0;
    }
    public void closeEmbassy() {
        withdrawAmbassador(Integer.MAX_VALUE);
    }
    public DiplomaticIncident breakPact() { return breakPact(false); }
    public DiplomaticIncident breakPact(boolean caughtSpying) {
        endTreaty();
        setTreaty(new TreatyNone(view.owner(), view.empire()));
        DiplomaticIncident inc = BreakPactIncident.create(owner(), empire(), caughtSpying);
        otherEmbassy().addIncident(inc);
        OathBreakerIncident.alertBrokenPact(owner(),empire(), caughtSpying);
        return inc;
    }
    public void signAlliance() {
        beginTreaty();
        setTreaty(new TreatyAlliance(view.owner(), view.empire()));
        owner().setRecalcDistances();
        empire().setRecalcDistances();
        owner().shareSystemInfoWithAlly(empire());
        owner().hideSpiesAgainst(empire().id);
        empire().hideSpiesAgainst(owner().id);
        GNNAllianceFormedNotice.create(owner(), empire());
    }
    public DiplomaticIncident breakAlliance() { return breakAlliance(false); }
    public DiplomaticIncident breakAlliance(boolean caughtSpying) {
        endTreaty();
        setTreaty(new TreatyNone(view.owner(), view.empire()));
        DiplomaticIncident inc = BreakAllianceIncident.create(owner(), empire(), caughtSpying);
        otherEmbassy().addIncident(inc);
        GNNAllianceBrokenNotice.create(owner(), empire());
        OathBreakerIncident.alertBrokenAlliance(owner(),empire(),caughtSpying);
        return inc;
    }
    public void makeContact() {
        if (!contact()) {
            contactTurn = galaxy().currentTurn();
            contact = true;
        }
    }
    public void makeFirstContact() {
        log("First Contact: ", owner().name(), " & ", empire().name());
        makeContact();
        if (empire().isPlayerControlled())
            DiplomaticNotification.create(view, dialogueContactType(owner().leader()));
    }
    private String dialogueContactType(Leader leader) {
        return switch(leader.personality()) {
            case PACIFIST -> DialogueManager.CONTACT_PACIFIST;
            case HONORABLE -> DialogueManager.CONTACT_HONORABLE;
            case RUTHLESS -> DialogueManager.CONTACT_RUTHLESS;
            case AGGRESSIVE -> DialogueManager.CONTACT_AGGRESSIVE;
            case XENOPHOBIC -> DialogueManager.CONTACT_XENOPHOBIC;
            case ERRATIC -> DialogueManager.CONTACT_ERRATIC;
        };
    }
    public void removeContact() {
        contact = false;
        resetTreaty();
        view.spies().beginHide();
        view.trade().stopRoute();
        if (otherEmbassy().contact)
            otherEmbassy().removeContact();
    }
    public void resetTreaty()   { setTreaty(new TreatyNone(view.owner(), view.empire())); }
    public void addIncident(DiplomaticIncident inc) {
        log(view.toString(), ": Adding incident- ", str(inc.severity()), ":", inc.toString());
        incidents.add(inc);
        updateRelations(inc.severity());
    }
    
    private void updateRelations(float severity) {
        severity = adjustSeverity(severity);
        relations = bounds(-100, relations+severity, 100);
    }
    
    private float adjustSeverity(float severity) {
        float adjustedRelations = relations - baseRelations();
        // Negative severity is treated the same with the relations range flipped.
        adjustedRelations = adjustedRelations * Math.signum(severity);
        
        float modifier;
        if (adjustedRelations < 0) {
            // relations is negative at this point so this is an add.
            modifier = 1 - adjustedRelations/50; // 1 to 3
        } else {
            modifier = 1 / (1 + adjustedRelations/25); // 1 to 1/5
        }
        
        return severity * modifier;
    }
    
    public float baseRelations() {
        return owner().baseRelations(empire());
    }

    private void resetIncidents() {
        // Drift relations first so it can be forgotten immediately as the incident cannot be displayed.
        addIncident(DriftRelationsIncident.create(view));
        
        newIncidents().clear();
        clearForgottenIncidents();
        
        if (pact()) {
            addIncident(SimpleIncident.createPactIncident(view));
        }
        if (alliance()) {
            addIncident(SimpleIncident.createAllianceIncident(view));
        }
        AtWarWithAllyIncident.create(view);
        AlliedWithEnemyIncident.create(view);
        TrespassingIncident.create(view);
        ExpansionIncident.create(view);
        MilitaryBuildupIncident.create(view);

        // make special list of incidents added in this turn
        for (DiplomaticIncident incident: incidents) {
            if ((galaxy().currentTurn() - incident.turnOccurred()) < 1)
                newIncidents().add(incident);
        }
    }
    
    private void clearForgottenIncidents() {
        Iterator<DiplomaticIncident> incidentIterator = incidents.iterator();
        while (incidentIterator.hasNext()) {
            DiplomaticIncident incident = incidentIterator.next();
            if (incident.isForgotten()) {
                log("Forgetting: ", incident.toString());
                incidentIterator.remove();
            }
        }
    }
    private void beginTreaty() {
        treatyTurn = galaxy().currentTurn();
        otherEmbassy().treatyTurn = galaxy().currentTurn();
    }
    private void endTreaty() {
        treatyTurn = -1;
        otherEmbassy().treatyTurn = -1;
        resetPactTimer();
        resetAllianceTimer();
        otherEmbassy().resetPactTimer();
        otherEmbassy().resetAllianceTimer();
        owner().setRecalcDistances();
        empire().setRecalcDistances();
    }
    private void beginPeace(int duration) {
        treaty = new TreatyPeace(view.empire(), view.owner(), duration);
        view.setSuggestedAllocations();
        onLastWarning = false;
    }
    
    public void receiveFinancialAid(int amt) {
        if (amt > 0) {
            owner().addToTreasury(amt);
            empire().addToTreasury(0-amt);
        }
        otherEmbassy().givenAidThisTurn = true;
        FinancialAidIncident.create(owner(), empire(), amt);
    }
    
    public void receiveTechnologyAid(String techId) {
        owner().tech().acquireTechThroughTrade(techId, empire().id);
        otherEmbassy().givenAidThisTurn = true;
        TechnologyAidIncident.create(owner(), empire(), techId);
    }
    
    public boolean canOfferAid() {
        if (givenAidThisTurn || !view.diplomats() || war() || !owner().inEconomicRange(view.empId())) {
            return false;
        }
        
        // do we have money to give?
        if (!offerAidAmounts().isEmpty())
            return true;
        
        // if not, do we have techs to give?
        return !offerableTechnologies().isEmpty();
    }
    
    public List<Integer> offerAidAmounts() {
        float reserve = owner().totalReserve();
        List<Integer> amts = new ArrayList<>();
        if (reserve > 25000) {
            amts.add(10000); amts.add(5000); amts.add(1000); amts.add(500);
        }
        else if (reserve > 10000) {
            amts.add(5000); amts.add(1000); amts.add(500); amts.add(100);
        }
        else if (reserve > 2500) {
            amts.add(1000); amts.add(500); amts.add(100); amts.add(50);
        }
        else if (reserve > 1000) {
            amts.add(500); amts.add(100); amts.add(50);
        }
        else if (reserve > 250) {
            amts.add(100); amts.add(50);
        }
        else if (reserve > 100) {
            amts.add(50);
        }
        return amts;
    }
    
    public List<Tech> offerableTechnologies() {
        List<String> allMyTechIds = owner().tech().allKnownTechs();
        List<String> theirTechIds = empire().tech().allKnownTechs();
        List<String> theirTradedTechIds = empire().tech().tradedTechs();
        allMyTechIds.removeAll(theirTechIds);
        allMyTechIds.removeAll(theirTradedTechIds);
         
        List<Tech> techs = new ArrayList<>();
        for (String id: allMyTechIds) {
            techs.add(tech(id));
        }
        
        // sort unknown techs by research value to the empire receiving
        var techTree = empire().tech();
        Comparator<Tech> researchComparator =
                (a, b) -> techTree.researchCost(b) - techTree.researchCost(a);
        Collections.sort(techs, researchComparator);

        return techs;
    }
}
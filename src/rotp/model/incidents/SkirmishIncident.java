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
package rotp.model.incidents;

import rotp.model.combat.CombatEntity;
import rotp.model.combat.CombatResults;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.ships.ShipDesign;
import rotp.ui.diplomacy.DialogueManager;

public class SkirmishIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int sysId;
    private final int empMe;
    private final int empYou;
    
    public static void create(CombatResults result, Empire empire) {
        for (Empire emp: result.empires()) {
            if  (!empire.alliedWith(emp.id)) {
                float winModifier = victoryModifier(result, empire);
                float skirmishSeverity = skirmishSeverity(result, empire);
                float severity = Math.min(-1.0f, winModifier*skirmishSeverity);
                EmpireView view = empire.viewForEmpire(emp.id);
                view.embassy().addIncident(new SkirmishIncident(view, result, severity));
            }
        }
    }
    private static float victoryModifier(CombatResults res, Empire empire) {
        // how much do we magnify lost ships when we lose
        // how much do we minimize lost ships when we lose

        //  do we hate everyone else?
        float multiplier = 1.0f;
        if (empire.leader().isXenophobic())
            multiplier *= 2;

        // did we win? if aggressive stacks still active, then no
        boolean won = true;
        for (CombatEntity st: res.activeStacks()) {
            if (st.empire.aggressiveWith(empire.id))
                won = false;
        }
        // if we won, then losses don't seem as bad
        if (won)
                    multiplier /= 2;

        // was this attack at our colonies?
        if (res.defender() == empire)
            multiplier *= 2;

        return multiplier;
    }
    private static float skirmishSeverity(CombatResults res, Empire empire) {
        float lostBC = 0;
        // how many ships & bases were lost, relative to empire production
        for (ShipDesign d: res.shipsDestroyed().keySet()) {
            if (d.empire() == empire) {
                int num = res.shipsDestroyed().get(d);
                lostBC += (num * d.cost());
            }
        }
        if (res.defender() == empire) {
            lostBC += (res.basesDestroyed() * empire.tech().newMissileBaseCost());
            lostBC += (res.factoriesDestroyed() * empire.tech().maxFactoryCost());
        }
        float totalIndustry = empire.totalPlanetaryProduction();

        // -1 severity for each 1% of total production lost
        return -1.0f*lostBC*100/totalIndustry;
    }
    
    private SkirmishIncident(EmpireView ev,CombatResults res, float sev) {
        super(Math.max(-2.5f, sev/4));
        sysId = res.system().id;
        empMe = ev.owner().id;
        empYou = ev.empire().id;
    }
    private String systemName() { return player().sv.name(sysId); }
    @Override
    public String title()               { return text("INC_SKIRMISH_TITLE"); }
    @Override
    public String description()         { return decode(text("INC_SKIRMISH_DESC")); }
    @Override
    public String warningMessageId() {  return DialogueManager.WARNING_SKIRMISH; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[system]", systemName());
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}

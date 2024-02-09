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
package rotp.model.incidents;

import java.util.List;
import rotp.model.combat.CombatResults;
import rotp.model.empires.Empire;
import rotp.ui.diplomacy.DialogueManager;

public class AttackedAllyIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empAttacker;
    public final int empAlly;
    public final int empMe;
    public static void alert(Empire attacker, Empire defender, CombatResults res) {
        List<Empire> allies = defender.allies();

        float severity = Math.min(5, 25 * res.damageSustained(defender));
        if (((int)severity) > 0) {
            for (Empire emp: allies) {
                if (emp != attacker) {
                    AttackedAllyIncident inc = new AttackedAllyIncident(attacker, defender, emp, -severity);
                    emp.viewForEmpire(attacker).embassy().addIncident(inc);
                }
            }
        }
    }
    private AttackedAllyIncident(Empire e1, Empire e2, Empire e3, float sev) {
        empAttacker = e1.id;
        empAlly = e2.id;
        empMe = e3.id;
        severity = sev;
        turnOccurred = galaxy().currentTurn();
    }
    @Override
    public String title()            { return text("INC_ATTACKED_ALLY_TITLE", galaxy().empire(empAlly).raceName()); }
    @Override
    public String description()      { return  decode(text("INC_ATTACKED_ALLY_DESC")); }
    @Override
    public String warningMessageId() { return DialogueManager.WARNING_ATTACKED_ALLY; }
    @Override
    public String key() {
        return concat(str(turnOccurred), ":AttackedAlly:", str(empAlly));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empAttacker).replaceTokens(s1, "attacker");
        s1 = galaxy().empire(empAlly).replaceTokens(s1, "defender");
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        return s1;
    }
}

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

import java.util.ArrayList;
import java.util.List;
import rotp.model.combat.CombatResults;
import rotp.model.empires.Empire;
import rotp.ui.diplomacy.DialogueManager;

public class AttackedEnemyIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empAttacker;
    public final int empEnemy;
    public final int empMe;
    public static void alert(Empire attacker, Empire defender, CombatResults res) {
        List<Empire> allEnemies = new ArrayList<>();
        
        for (Empire emp: defender.contactedEmpires()) {
            if (defender.atWarWith(emp.id))
                allEnemies.add(emp);
        }        

        float severity = Math.min(3.75f, 12.5f * res.damageSustained(defender));
        if (((int)severity) > 0) {
            for (Empire enemy: allEnemies) {
                if (enemy != attacker) {
                    AttackedEnemyIncident inc = new AttackedEnemyIncident(attacker, defender, enemy, severity);
                    enemy.viewForEmpire(attacker).embassy().addIncident(inc);
                }
            }
        }
    }
    private AttackedEnemyIncident(Empire e1, Empire e2, Empire e3, float sev) {
        empAttacker = e1.id;
        empEnemy = e2.id;
        empMe = e3.id;
        severity = sev;
    }
    @Override
    public String title()           { return text("INC_ATTACKED_ENEMY_TITLE", galaxy().empire(empEnemy).raceName()); }
    @Override
    public String description()      { return  decode(text("INC_ATTACKED_ENEMY_DESC")); }
    @Override
    public String praiseMessageId() { return DialogueManager.PRAISE_ATTACKED_ENEMY; }
    @Override
    public String key() {
        return concat(str(turnOccurred()), ":AttackedEnemy:", str(empEnemy));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empAttacker).replaceTokens(s1, "attacker");
        s1 = galaxy().empire(empEnemy).replaceTokens(s1, "defender");
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        return s1;
    }
}

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

import rotp.model.combat.CombatResults;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.ui.diplomacy.DialogueManager;

public class ColonyAttackedIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int sysId;
    private final int empDefender;
    private final int empAttacker;
    private final int popLost;

    public static void create(CombatResults r) {
        ColonyAttackedIncident inc = new ColonyAttackedIncident(r);
        r.defender().diplomatAI().noticeIncident(inc, r.attacker());
    }
    private ColonyAttackedIncident(CombatResults r) {
        super(-1.25f + Math.max(-10f, -r.popDestroyed()/4.0f));
        sysId = r.system().id;
        empDefender = r.defender().id;
        empAttacker = r.attacker().id;
        popLost = r.popDestroyed();
    }
    private String systemName() { return player().sv.name(sysId); }
    @Override
    public String title()         { return text("INC_ATTACKED_COLONY_TITLE", systemName()); }
    @Override
    public String description()   { return  decode(text("INC_ATTACKED_COLONY_DESC")); }
    @Override
    public String warningMessageId()    { return DialogueManager.WARNING_COLONY_ATTACKED; }
    @Override
    public String declareWarId()        { return DialogueManager.DECLARE_ATTACKED_WAR; }
    @Override
    public boolean isAttacking()        { return true; }
    @Override
    public boolean triggersWar()        { return popLost > 0; }
    @Override
    public int timerKey()               { return DiplomaticEmbassy.TIMER_ATTACK_WARNING; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[system]", systemName());
        s1 = s1.replace("[amt]", str(popLost));
        s1 = galaxy().empire(empAttacker).replaceTokens(s1, "attacker");
        s1 = galaxy().empire(empDefender).replaceTokens(s1, "defender");
        return s1;
    }
}

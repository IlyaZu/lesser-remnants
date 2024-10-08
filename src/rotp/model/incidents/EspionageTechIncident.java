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

import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.EspionageMission;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.TechStolenAlert;

public class EspionageTechIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int empSpy;
    private final int empVictim;
    private int empThief;
    private String techId;

    public EspionageTechIncident(EmpireView ev, EspionageMission m) {
        super(calculateSeverity(ev));
        ev.embassy().resetAllianceTimer();
        // empSpy is the actual spy
        // empThief is the suspected spy (the one who was framed)
        empSpy = m.spyEmpire().id;
        empVictim = ev.owner().id;
        empThief = m.thief().id;
        techId = m.stolenTech();
        m.incident(this);

    }
    private static float calculateSeverity(EmpireView view) {
        float multiplier = view.empire().leader().isTechnologist()? 2 : 1;
        return Math.max(-20,-10+view.embassy().currentSpyIncidentSeverity()) * multiplier;
    }
    @Override
    public boolean isSpying()         { return true; }
    @Override
    public int timerKey()           { return DiplomaticEmbassy.TIMER_SPY_WARNING; }
    @Override
    public String title()             { return text("INC_TECH_STOLEN_TITLE"); }
    @Override
    public String description()       {
        if (empSpy == empThief)
            return  decode(text("INC_TECH_STOLEN_DESC"));
        else
            return decode(text("INC_TECH_FRAMED_DESC"));
    }
    public void frameEmpire(Empire e) {
        empThief = e.id;
        if (galaxy().empire(empVictim).isPlayerControlled())
            TechStolenAlert.create(empThief);
    }
    @Override
    public String warningMessageId() { return galaxy().empire(empVictim).isPlayerControlled() ? "" : DialogueManager.WARNING_ESPIONAGE; }
    @Override
    public String declareWarId()     { return DialogueManager.DECLARE_SPYING_WAR; }
    @Override
    public boolean triggersWar()        { return false; } // war is only triggered after a warning
    @Override
    public String decode(String s) {
        String frameMessage = empThief == empSpy ? "" : text("SPY_FRAMED");
        String s1 = super.decode(s);
        s1 = galaxy().empire(empThief).replaceTokens(s1, "spy");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        s1 = s1.replace("[tech]", tech(techId).name());
        s1 = s1.replace("[framed]", frameMessage);
        return s1;
    }
}

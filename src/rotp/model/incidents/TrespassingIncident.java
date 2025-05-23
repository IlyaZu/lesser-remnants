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

import java.util.List;

import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.ui.diplomacy.DialogueManager;
import rotp.ui.notifications.TrespassingAlert;

public class TrespassingIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int sysId;
    private final int empMe;
    private final int empYou;
    
    public static void create(EmpireView view) {
        if (view.embassy().alliance() || view.embassy().war())
            return;
        for (StarSystem sys: view.owner().allColonizedSystems()) {
            List<ShipFleet> fleets = sys.orbitingFleets();
            for (ShipFleet fl: fleets) {
                if (!fl.retreating() && (fl.empire() == view.empire()))
                    view.embassy().addIncident(new TrespassingIncident(view,sys,fl));
            }
        }
    }

    private TrespassingIncident(EmpireView ev, StarSystem sys, ShipFleet fl) {
        super(calculateSeverity(sys, fl));
        sysId = sys.id;
        empMe = ev.owner().id;
        empYou = ev.empire().id;

        if (ev.empire().isPlayerControlled())
            TrespassingAlert.create(empMe, sysId);
    }

    private static float calculateSeverity(StarSystem sys, ShipFleet fl) {
        float multiplier = -1.0f;
        if (sys.empire().leader().isXenophobic())
            multiplier *= 2;
        
        float fleetPower = fl.firepower(sys.colony().defense().shieldLevel())/100.0f;
        
        float severity = multiplier * Math.max(1.0f, fleetPower);
        return Math.max(-10, severity);
    }

    private String systemName()         { return player().sv.name(sysId); }
    @Override
    public String title()               { return text("INC_TRESPASSING_TITLE", systemName()); }
    @Override
    public String description()         { return decode(text("INC_TRESPASSING_DESC")); }
    @Override
    public String warningMessageId()    { return DialogueManager.WARNING_TRESPASSING; }
    @Override
    public int timerKey()               { return DiplomaticEmbassy.TIMER_ATTACK_WARNING; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[system]", systemName());
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}

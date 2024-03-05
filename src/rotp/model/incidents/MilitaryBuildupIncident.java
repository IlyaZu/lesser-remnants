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

import rotp.model.empires.EmpireView;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.ui.diplomacy.DialogueManager;

public class MilitaryBuildupIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    final int sysId;
    final int empMe;
    final int empYou;

    public static void create(EmpireView view) {
        float shipRange = view.owner().shipRange();

        float multiplier = -0.0125f;
        if (view.owner().atWarWith(view.empId()))
            multiplier *= 2;
        else if (view.owner().pactWith(view.empId()))
            multiplier /= 8;
        else if (view.owner().alliedWith(view.empId()))
            multiplier /= 64;

        if (view.owner().leader().isXenophobic())
            multiplier *= 2;

        for (StarSystem sys: view.owner().allColonizedSystems()) {
            float systemSeverity = 0;
            for (ShipFleet fl: view.owner().fleetsForEmpire(view.empire())) {
                if (fl.isActive() && (sys.distanceTo(fl) <= shipRange)) {
                    float fleetThreat = fl.visibleFirepower(view.owner().id, sys.colony().defense().missileShieldLevel());
                    systemSeverity += (multiplier*fleetThreat);
                }
            }
            if (systemSeverity > 0)
            	view.embassy().addIncident(new MilitaryBuildupIncident(view,sys, systemSeverity));
        }
    }
    
    private MilitaryBuildupIncident(EmpireView ev, StarSystem sys, float sev) {
    	super(Math.max(-2.5f, sev));
        sysId = sys.id;
        empMe = ev.owner().id;
        empYou = ev.empire().id;
    }
    private String systemName() { return player().sv.name(sysId); }
    @Override
    public String title()            { return text("INC_MILITARY_BUILDUP_TITLE"); }
    @Override
    public String description()   { return decode(text("INC_MILITARY_BUILDUP_DESC")); }
    @Override
    public String warningMessageId() { return DialogueManager.WARNING_BUILDUP; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[system]", systemName());
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}

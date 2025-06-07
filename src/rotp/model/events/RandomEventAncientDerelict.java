/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024-2025 Ilya Zushinskiy
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
package rotp.model.events;

import rotp.model.empires.Empire;
import rotp.model.tech.Tech;
import rotp.model.tech.TechTree;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RandomEventAncientDerelict implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    private static final int MAX_TECHS_DISCOVERED = 10;
    private int empId;
    @Override
    public boolean goodEvent()            { return true; }
    @Override
    public boolean repeatable()            { return false; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_DERELICT");
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        TechTree empTech = emp.tech();
        
        List<Tech> availableTechs = new ArrayList<>();
        availableTechs.addAll(empTech.forceField().unknownTechs(0, 10));
        availableTechs.addAll(empTech.weapon().unknownTechs(0, 10));

        if (availableTechs.isEmpty()) {
            return;
        }

        List<Tech> discoveredTechs = new ArrayList<>();
        for (int i = 0; i < MAX_TECHS_DISCOVERED && !availableTechs.isEmpty(); i++) {
            int techIndex = random.nextInt(availableTechs.size());
            Tech tech = availableTechs.remove(techIndex);
            discoveredTechs.add(tech);
        }

        empId = emp.id;
        if (emp.isPlayerControlled() || player().hasContact(emp))
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Derelict");
        
        for (Tech tech : discoveredTechs)
            emp.plunderShipTech(tech, -1);
    }
}

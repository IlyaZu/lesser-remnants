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

import rotp.model.empires.EmpireView;
import rotp.model.galaxy.Galaxy;
import rotp.ui.diplomacy.DialogueManager;

public class ExpansionIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private int numSystems;
    private final int empYou;
    
    public static void create(EmpireView view) {
        int numberSystems = view.empire().numSystemsForCiv(view.empire());
        if (numberSystems < 6)
            return;

        Galaxy gal = Galaxy.current();
        int allSystems = gal.numStarSystems();
        int numCivs = gal.numEmpires();
        int maxSystemsWithoutPenalty = Math.max(5, allSystems/(numCivs+1));

        if (numberSystems > maxSystemsWithoutPenalty)
            view.embassy().addIncident(new ExpansionIncident(view,numberSystems, maxSystemsWithoutPenalty));
    }
    private ExpansionIncident(EmpireView ev, int num, float max) {
        super(calculateSeverity(ev, num, max));
        numSystems = num;
        empYou = ev.empire().id;
    }
    private static float calculateSeverity(EmpireView view, int numSystems, float maxSystems) {
        int empireId = view.empire().id;
        float multiplier = 1.0f;
        // penalty doubled for xenophobes
        if (view.owner().leader().isXenophobic())
            multiplier *= 2;
        // allies are more tolerant of growth, NAPS less so
        if (view.owner().alliedWith(empireId))
            multiplier /= 3;
        else if (view.owner().pactWith(empireId))
            multiplier /= 1.5;
        
        float n = -5*((numSystems*numSystems/maxSystems/maxSystems) - 1);

        return Math.max(-12.5f, multiplier*n);
    }
    @Override
    public String title()            { return text("INC_EXPANSION_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_EXPANSION_DESC")); }
    @Override
    public String warningMessageId() { return  galaxy().empire(empYou).newSystems().isEmpty() ? "" :DialogueManager.WARNING_EXPANSION; }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[num]", str(numSystems));
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}

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
import rotp.model.galaxy.Galaxy;
import rotp.ui.diplomacy.DialogueManager;

public class ExpansionIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    int numSystems;
    float maxSystems;
    final int empYou;
    public static void create(EmpireView view) {
        int numberSystems = view.empire().numSystemsForCiv(view.empire());
        if (numberSystems < 6)
            return;

        Galaxy gal = Galaxy.current();
        int allSystems = gal.numColonizedSystems();
        int numCivs = gal.numActiveEmpires();

        // modnar: scale expansion penalty with ~1/[(numCivs)^(0.75)] rather than 1/numCivs
        // this allows empires to be somewhat bigger than average before the diplomatic size penalty kicks in
        // not linear with numCivs to account for expected fluctuation of empire sizes with larger number of empires
        // at the max number of empires (50), you can be ~2 times as large as average before being penalized
        // use a denominator coefficient factor of ~1.44225 (3^(1/3)) to maps the expression
        // back to the equal 1/3 "share" of planets when only three empires are remaining
        // (and when only two are remaining, they won't like you even if you have slightly less planets than they do)
        //
        // numCivs(X)   1/X     1/[(1.44225*X)^(0.75)]
        //      2       50.00%  45.18%
        //      3       33.33%  33.33%
        //      4       25.00%  26.86%
        //      5       20.00%  22.72%
        //      6       16.67%  19.82%
        //      8       12.50%  15.97%
        //      10      10.00%  13.51%
        //      15      6.67%   9.97%
        //      20      5.00%   8.03%
        //      30      3.33%   5.93%
        //      50      2.00%   4.04%
        //
        //int maxSystemsWithoutPenalty = max(5, (allSystems /numCivs)+1);
        int maxSystemsWithoutPenalty = Math.max(5, (int) Math.ceil(allSystems / Math.pow(1.44225*numCivs, 0.75)));

        if (numberSystems > maxSystemsWithoutPenalty)
        	view.embassy().addIncident(new ExpansionIncident(view,numberSystems, maxSystemsWithoutPenalty));
    }
    @Override
    public boolean triggeredByAction()   { return false; }
    private ExpansionIncident(EmpireView ev, int num, float max) {
        numSystems = num;
        maxSystems = max;
        empYou = ev.empire().id;
        turnOccurred = galaxy().currentTurn();

        float multiplier = 1.0f;
        // penalty doubled for xenophobes
        if (ev.owner().leader().isXenophobic())
            multiplier *= 2;
        // allies are more tolerant of growth, NAPS less so
        if (!ev.owner().alliedWith(empYou))
            multiplier /= 3;
        else if (!ev.owner().pactWith(empYou))
            multiplier /= 1.5;
        
        // if you are bigger than average but the viewer is 
        // even larger, the penalty is lessened by the square
        // of the proportion... i.e. if you are 1/2 the size
        // the penalty is 1/4th
        int ownerNum = ev.owner().numColonizedSystems();
        if (ownerNum > numSystems) {
            float ratio = (float) numSystems / ownerNum;
            multiplier = multiplier * ratio * ratio;
        }
        
        float n = -5*((num*num/max/max) - 1);

        severity = max(-12.5f, multiplier*n);
    }
    @Override
    public String title()            { return text("INC_EXPANSION_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_EXPANSION_DESC")); }
    @Override
    public String warningMessageId() { return  galaxy().empire(empYou).newSystems().isEmpty() ? "" :DialogueManager.WARNING_EXPANSION; }
    @Override
    public String key() {
        return concat("EmpireGrowth:", str(turnOccurred));
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = s1.replace("[num]", str(numSystems));
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        return s1;
    }
}

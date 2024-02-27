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
import rotp.model.tech.Tech;

public class TechnologyAidIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empMe;
    public final int empYou;
    private final String techId;
    public static TechnologyAidIncident create(Empire emp, Empire donor, String techId) {
        DiplomaticEmbassy emb = emp.viewForEmpire(donor).embassy();
        TechnologyAidIncident inc = new TechnologyAidIncident(emp, donor, techId);
        emb.addIncident(inc);
        emb.otherEmbassy().giveAid();
        
        for (Empire enemy: emp.warEnemies()) 
            EnemyAidIncident.create(enemy, emp, donor, techId);

        return inc;
    }
    private TechnologyAidIncident(Empire emp, Empire donor, String tId) {
    	super(calculateSeverity(emp, tId));
        empYou = donor.id;
        empMe = emp.id;
        techId = tId;
    }
    private static float calculateSeverity(Empire emp, String tId) {
        Tech tech = emp.tech(tId);
        int rpValue = (int)emp.ai().scientist().researchBCValue(tech);

        float pct = rpValue / emp.totalPlanetaryProduction();
        return Math.min(15,25*pct); 
    }
    @Override
    public String title()        { return text("INC_TECHNOLOGY_AID_TITLE"); }
    @Override
    public String description()  { 
    	return decode(text("INC_TECHNOLOGY_AID_DESC"));
    }
    @Override
    public String key()          { return "Technology Aid:"+turnOccurred(); }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = s1.replace("[tech]", tech(techId).name());
        return s1;
    }
}

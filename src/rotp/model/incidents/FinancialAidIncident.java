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

import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;

public class FinancialAidIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    public final int empMe;
    public final int empYou;
    private int amount;
    
    public static void create(Empire emp, Empire donor, int amt) {
        DiplomaticEmbassy emb = emp.viewForEmpire(donor).embassy();
        emb.addIncident(new FinancialAidIncident(emp, donor, amt));
        
        for (Empire enemy: emp.enemies())
            EnemyAidIncident.create(enemy, emp, donor, amt);
    }
    private FinancialAidIncident(Empire emp, Empire donor, int amt) {
        super(calculateSeverity(emp, amt));
        empYou = donor.id;
        empMe = emp.id;
        amount = amt;
    }
    private static float calculateSeverity(Empire emp, int amt) {
        float pct = (float) amt / emp.totalPlanetaryProduction();
        return Math.min(10,10*pct);
    }
    @Override
    public String title()        { return text("INC_FINANCIAL_AID_TITLE"); }
    @Override
    public String description()  { return decode(text("INC_FINANCIAL_AID_DESC")); }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = s1.replace("[amt]", str(amount));
        return s1;
    }
}

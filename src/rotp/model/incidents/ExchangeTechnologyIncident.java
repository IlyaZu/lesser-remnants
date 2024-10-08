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

import rotp.model.empires.Empire;
import rotp.model.tech.Tech;

public class ExchangeTechnologyIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int empMe;
    private final int empYou;
    private final String received;
    private final String given;
    
    public static ExchangeTechnologyIncident create(Empire e1, Empire e2, Tech recv, Tech give) {
        ExchangeTechnologyIncident inc = new ExchangeTechnologyIncident(e1, e2, recv, give);
        return inc;
    }
    private ExchangeTechnologyIncident(Empire e1, Empire e2, Tech recv, Tech give) {
        super(5);
        empMe = e1.id;
        empYou = e2.id;
        received = recv.id();
        given = give.id();
    }
    @Override
    public String title()        { return text("INC_TECH_EXCHANGE_TITLE"); }
    @Override
    public String description()  { return decode(text("INC_TECH_EXCHANGE_DESC")); }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empMe).replaceTokens(s1, "my");
        s1 = galaxy().empire(empYou).replaceTokens(s1, "your");
        s1 = s1.replace("[techReceived]", tech(received).name());
        s1 = s1.replace("[techGiven]", tech(given).name());
        return s1;
    }
}

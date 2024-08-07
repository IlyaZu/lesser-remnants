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

public class DeclareWarIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    private final int empAggressor;
    private final int empVictim;
    
    public static DeclareWarIncident create(Empire e1, Empire e2) {
        DeclareWarIncident inc = new DeclareWarIncident(e1, e2);
        return inc;
    }
    private DeclareWarIncident(Empire e1, Empire e2) {
        super(-70);
        empAggressor = e1.id;
        empVictim = e2.id;
    }
    @Override
    public String title()            { return text("INC_DECLARED_WAR_TITLE"); }
    @Override
    public String description()      { return decode(text("INC_DECLARED_WAR_DESC")); }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(empAggressor).replaceTokens(s1, "aggressor");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "victim");
        s1 = galaxy().empire(empVictim).replaceTokens(s1, "other");
        return s1;
    }
}

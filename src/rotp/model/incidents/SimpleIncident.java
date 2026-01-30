/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2026 Ilya Zushinskiy
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
import rotp.model.empires.EmpireView;

public class SimpleIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    
    private final int myEmpireId;
    private final int yourEmpireId;

    public static SimpleIncident createAllianceIncident(EmpireView view) {
        return new SimpleIncident(3,
                "INC_SIGNED_ALLIANCE_TITLE", "INC_SIGNED_ALLIANCE_DESC",
                view.ownerId(), view.empId());
    }
    
    public static SimpleIncident createPactIncident(EmpireView view) {
        return new SimpleIncident(1.5f,
                "INC_SIGNED_PACT_TITLE", "INC_SIGNED_PACT_DESC",
                view.ownerId(), view.empId());
    }
    
    public static SimpleIncident createBreakTradeIncident(EmpireView view) {
        return new SimpleIncident(-5,
                "INC_BROKE_TRADE_TITLE", "INC_BROKE_TRADE_DESC",
                view.empId(), view.ownerId());
    }
    
    public static SimpleIncident createDriftRelationsIncident(EmpireView view) {
        DiplomaticEmbassy embassy = view.embassy();
        float severity = (embassy.baseRelations() - embassy.relations()) / 50;
        return new SimpleIncident(severity,
                // Due to licensing restrictions these keys do not currently map to any values.
                // TODO Add key/values to a new file under a more permissible license.
                "INC_DRIFT_RELATIONS_TITLE", "INC_DRIFT_RELATIONS_DESC",
                view.ownerId(), view.empId());
    }
    
    private SimpleIncident(float severity,
            String titleKey, String descriptionKey,
            int myEmpireId, int yourEmpireId) {
        
        super(severity, titleKey, descriptionKey);
        this.myEmpireId = myEmpireId;
        this.yourEmpireId = yourEmpireId;
    }
    
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(myEmpireId).replaceTokens(s1, "my");
        s1 = galaxy().empire(yourEmpireId).replaceTokens(s1, "your");
        return s1;
    }
}

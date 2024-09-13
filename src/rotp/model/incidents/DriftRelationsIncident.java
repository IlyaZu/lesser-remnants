/*
 * Copyright 2024 Ilya Zushinskiy
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

public class DriftRelationsIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;

    public static DriftRelationsIncident create(EmpireView view) {
        DiplomaticEmbassy embassy = view.embassy();
        float severity = (embassy.baseRelations() - embassy.relations()) / 50;
        return new DriftRelationsIncident(severity);
    }
    
    private DriftRelationsIncident(float severity) {
        super(severity);
    }
    
    @Override
    public boolean isForgotten(){
        // Cannot be displayed as there is no text for this incident.
        return true;
    }

    @Override
    public String title() {
        // Due to licensing restrictions there is currently no text for this incident.
        return "";
    }

    @Override
    public String description() {
        // Due to licensing restrictions there is currently no text for this incident.
        return "";
    }
    
}

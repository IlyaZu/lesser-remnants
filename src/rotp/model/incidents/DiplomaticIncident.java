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

import java.io.Serializable;
import rotp.util.Base;

public abstract class DiplomaticIncident implements Base, Serializable {
    private static final long serialVersionUID = 1L;

    private final int turnOccurred = galaxy().currentTurn();
    private final float severity;
    private final String titleKey;
    private final String descriptionKey;
    
    public DiplomaticIncident(float severity,
            String titleKey, String descriptionKey) {
        
        this.severity = severity;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
    }
    
    public int turnOccurred() {
        return turnOccurred;
    }
    
    public float severity() {
        return severity;
    }

    public String title() {
        return text(titleKey);
    }

    public String description() {
        return decode(text(descriptionKey));
    }

    public String praiseMessageId()      { return ""; }
    public String warningMessageId()     { return ""; }
    public String declareWarId()         { return ""; }
    public void notifyOfPraise()         { }  // provides hook to avoid constant praise

    @Override
    public String toString() {  return concat(str(turnOccurred), ": ", title(), " = ", fmt(severity(),1)); }

    public String decode(String s)       { return s.replace("[year]", str(turnOccurred)); }

    public boolean triggersPraise()      { return !praiseMessageId().isEmpty(); }
    public boolean triggersWarning()     { return !warningMessageId().isEmpty(); }
    public boolean triggersWar()         { return !declareWarId().isEmpty(); }
}

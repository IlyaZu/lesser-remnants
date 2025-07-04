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
package rotp.model.empires;

import java.io.Serializable;
import rotp.util.Base;

public class DiplomaticTreaty implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    final int empire1;
    final int empire2;
    final String statusKey;
    public DiplomaticTreaty(Empire e1, Empire e2, String key) {
        empire1 = e1.id;
        empire2 = e2.id;
        statusKey = key;
    }
    public String status(Empire e)        { return text(statusKey); }
    public void nextTurn(Empire emp)      { }
    public boolean isWar()                { return false; }
    public boolean isNoTreaty()           { return false; }
    public boolean isPact()               { return false; }
    public boolean isAlliance()           { return false; }
    public boolean isTrade()              { return false; }
    public boolean isPeace()              { return false; }
    public int listOrder()                { return 0; }
    public void losePopulation(Empire e, float amt) {  }
    public void loseFactories(Empire e, float amt)  {  }
    public void loseFleet(Empire e, float amt)    {  }
}

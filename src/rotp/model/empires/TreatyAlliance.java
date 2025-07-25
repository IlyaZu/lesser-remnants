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

import rotp.util.Base;

public class TreatyAlliance extends DiplomaticTreaty implements Base {
    private static final long serialVersionUID = 1L;
    
    public TreatyAlliance(Empire e1, Empire e2) {
        super(e1,e2,"RACES_ALLY");
    }
    
    @Override
    public boolean isAlliance()               { return true; }
    @Override
    public int listOrder()                    { return 3; }

}

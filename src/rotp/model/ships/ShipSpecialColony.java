/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024 Ilya Zushinskiy
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
package rotp.model.ships;

import rotp.model.planet.PlanetType;
import rotp.model.tech.TechControlEnvironment;
 
public final class ShipSpecialColony extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialColony(TechControlEnvironment t) {
        tech(t);
        sequence(t.level);
    }
    @Override
    public boolean isColonySpecial()         { return true; }
    @Override
    public String designGroup()              { return "Colony"; }
    @Override
    public TechControlEnvironment tech()     { return (TechControlEnvironment) super.tech(); }
    public boolean canColonize(PlanetType pt)  { return tech().canColonize(pt); }
}

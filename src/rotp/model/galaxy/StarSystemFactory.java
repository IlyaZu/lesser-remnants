/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2025 Ilya Zushinskiy
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
package rotp.model.galaxy;

import rotp.model.empires.Race;
import rotp.model.planet.PlanetFactory;
import rotp.util.Base;

public class StarSystemFactory implements Base {

    public StarSystem newNeutralSystem(Galaxy gal) {
        String type = neutralStarType();
        StarSystem sys = new StarSystem(type, gal.systemCount);
        return sys;
    }
    
    private String neutralStarType() {
        float starTypeRoll = random();
        if (starTypeRoll <= .30) {
            return StarType.RED;
        } else if (starTypeRoll <= .55) {
            return StarType.ORANGE;
        } else if (starTypeRoll <= .70) {
            return StarType.YELLOW;
        } else if (starTypeRoll <= .85) {
            return StarType.BLUE;
        } else if (starTypeRoll <= .95) {
            return StarType.WHITE;
        } else {
            return StarType.PURPLE;
        }
    }
    
    public StarSystem newOrionSystem(Galaxy gal) {
        String type = homeworldStarType();
        StarSystem sys = new StarSystem(type, gal.systemCount);
        sys.planet(PlanetFactory.createOrion(sys));
        sys.monster(new OrionGuardianShip());
        sys.name(text("PLANET_ORION"));
        return sys;
    }
    
    public StarSystem newHomeworldSystem(Race r, Galaxy gal) {
        String type = homeworldStarType();
        StarSystem sys = new StarSystem(type, gal.systemCount);
        sys.planet(PlanetFactory.createHomeworld(r, sys));
        return sys;
    }
    
    private String homeworldStarType() {
        return StarType.YELLOW;
    }
}

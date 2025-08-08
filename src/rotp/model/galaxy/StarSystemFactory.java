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
    private static final StarSystemFactory instance = new StarSystemFactory();
    public static StarSystemFactory current()   { return instance; }

    public StarSystem newSystem(Galaxy gal) {
        String type = randomStarType();
        StarSystem sys = StarSystem.create(type, gal);
        return sys;
    }
    
    public StarSystem newOrionSystem(Galaxy gal) {
        String type = homeworldStarType();
        StarSystem sys = StarSystem.create(type, gal);
        sys.planet(PlanetFactory.createOrion(sys));
        sys.monster(new OrionGuardianShip());
        sys.name(text("PLANET_ORION"));
        return sys;
    }
    
    public StarSystem newHomeworldSystem(Race r, Galaxy gal) {
        String type = homeworldStarType();
        StarSystem sys = StarSystem.create(type, gal);
        sys.planet(PlanetFactory.createHomeworld(r, sys));
        return sys;
    }
    
    private String randomStarType() {
        // pcts represents star type distribution per MOO1 Official Strategy Guide
        //                RED, ORANG, YELL, BLUE,WHITE, PURP
        float[] pcts = { .30f, .55f, .70f, .85f, .95f, 1.0f };
        
        int typeIndex = 0;
        float r = random();
        for (int i=0;i<pcts.length;i++) {
            if (r <= pcts[i]) {
                typeIndex = i;
                break;
            }
        }

        switch(typeIndex) {
            case 0:  return StarType.RED;
            case 1:  return StarType.ORANGE;
            case 2:  return StarType.YELLOW;
            case 3:  return StarType.BLUE;
            case 4:  return StarType.WHITE;
            case 5:  return StarType.PURPLE;
            default: return StarType.RED;
        }
    }
    
    private String homeworldStarType() {
        return StarType.YELLOW;
    }
}

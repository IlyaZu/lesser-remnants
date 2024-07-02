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
package rotp.model.galaxy;

import rotp.model.colony.Colony;
import rotp.model.combat.CombatAmoeba;
import rotp.model.combat.CombatManager;
import rotp.model.events.RandomEventSpaceAmoeba;
import rotp.model.planet.PlanetType;

public class SpaceAmoeba extends SpaceMonster {
    private static final long serialVersionUID = 1L;
    public SpaceAmoeba() {
        super("SPACE_AMOEBA");
    }
    @Override
    public void initCombat(CombatManager manager) {
        combatStacks().clear();
        combatStacks().add(new CombatAmoeba());
    }
    public void degradePlanet(StarSystem sys) {
        Colony col = sys.colony();
        if (col != null) {
            float prevFact = col.industry().factories();
            col.industry().factories(prevFact*0.1f);
            sys.empire().lastAttacker(RandomEventSpaceAmoeba.monster);
            col.destroy();
        }
        sys.planet().degradeToType(PlanetType.BARREN);
        sys.planet().resetWaste();
        sys.abandoned(false);
    }
}
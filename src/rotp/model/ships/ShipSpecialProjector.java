/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023 Ilya Zushinskiy
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

import rotp.model.combat.CombatEntity;
import rotp.model.combat.CombatColony;
import rotp.model.combat.CombatShip;
import rotp.model.tech.TechStreamProjector;

public final class ShipSpecialProjector extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    public ShipSpecialProjector(TechStreamProjector t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public TechStreamProjector tech() { return (TechStreamProjector) super.tech(); }
    @Override
    public String name()              { return tech().name(); }
    @Override
    public String desc()              { return tech().brief(); }
    @Override
    public int range()                { return tech().range; }
    @Override
    public boolean isWeapon()         { return true; }
    @Override
    public float estimatedKills(CombatEntity source, CombatEntity target, int num) {
        float armorMod = tech().armorMod(num);
        float dam = max(1, target.maxHits*armorMod) * target.num;
        return dam / target.maxHits * target.num;
    }
    @Override
    public void fireUpon(CombatEntity source, CombatEntity target, int count)      {
        float armorMod = tech().armorMod(count);
        if (target.isShip()) {
            CombatShip st = (CombatShip) target;
            st.maxHits = (int) st.maxHits*armorMod;
            st.hits = min(st.hits-1, st.maxHits);
        }
        else if (target.isColony()) {
            CombatColony st = (CombatColony) target;
            st.maxHits = (int) st.maxHits*armorMod;
            st.hits = min(st.hits-1, st.maxHits);
        }
        if (source.mgr.showAnimations())
        {
            tech().drawSpecialAttack(source, target, 1, 0);
            tech().drawSuccessfulAttack(source, target, source.weaponNum(this), 0);
        }
        if (target.hits <= 0)
            target.loseShip();
        if (target.maxHits <= 0)
            source.mgr.destroyStack(target);
    }
}

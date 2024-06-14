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
package rotp.model.ships;

import rotp.model.combat.CombatEntity;
import rotp.model.combat.CombatEmpireShip;
import rotp.model.tech.TechStasisField;

public final class ShipSpecialStasisField extends ShipSpecial {
    private static final long serialVersionUID = 1L;
    transient CombatEmpireShip target;
    public ShipSpecialStasisField (TechStasisField t) {
        tech(t);
        sequence(t.level + .05f);
    }
    @Override
    public TechStasisField tech()   { return (TechStasisField) super.tech(); }
    @Override
    public boolean isWeapon()      { return true; }
    @Override
    public int range()             { return tech().range; }
    @Override
    public String name()           { return tech().name(); }
    @Override
    public String desc()           { return tech().brief(); }
    @Override
    public boolean validTarget(CombatEntity tgt) {
        if (!tgt.isEmpireShip())
            return false;
        if (tgt.immuneToStasis())
            return false;
        return !tgt.cloaked;
    }
    @Override
    public void reload()           { breakStasis(); }
    @Override
    public void becomeDestroyed()  { breakStasis(); }
    @Override
    public void fireUpon(CombatEntity source, CombatEntity tgt, int count)   {
        if (tgt.immuneToStasis())
            return;
        if (tgt.isEmpireShip()) {
            target = (CombatEmpireShip) tgt;
            tech().drawSpecialAttack(source, target, 1, 0);
            target.inStasis = true;
            target.missiles().clear();
            if (source.mgr.ui != null)
                source.mgr.ui.repaint();
        }
    }
    private void breakStasis() {
        if (target != null) {
            target.inStasis = false;
            target = null;
        }
    }
}

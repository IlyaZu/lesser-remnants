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
package rotp.model.ships;

import java.awt.Component;
import java.awt.Image;
import rotp.model.combat.CombatEntity;
import rotp.model.combat.CombatColony;
import rotp.model.tech.TechTorpedoWeapon;

public final class ShipWeaponTorpedo extends ShipWeaponMissileType {
    private static final long serialVersionUID = 1L;
    public ShipWeaponTorpedo(TechTorpedoWeapon t) {
        tech(t);
        sequence(t.level + .1f);
    }
    @Override
    public float planetDamageMod()       { return CombatColony.TORPEDO_DAMAGE_MOD; }
    @Override
    public int bombardAttacks()           { return 5; }
    @Override
    public TechTorpedoWeapon tech()       { return (TechTorpedoWeapon) super.tech(); }
    @Override
    public int computerLevel()            { return tech().computer; }
    @Override
    public float damageLoss(float dist) { return dist*tech().damageLoss; }
    @Override
    public Image image(int num)           { return tech().image(num); }
    @Override
    public int range()                    { return tech().range; }
    @Override
    public int shots()                    { return tech().shots; }
    @Override
    public int turnsToFire()              { return tech().turnsToFire; }
    @Override
    public float speed()                 { return tech().speed; }
    @Override
    public int minDamage()                { return tech().damage(); }
    @Override
    public int maxDamage()                { return tech().damage(); }
    @Override
    public void dealDamage(CombatEntity target, float damage, float shieldMod) {
        target.takeTorpedoDamage(damage, shieldMod);
    }
    @Override
    public float estimatedBombardDamage(CombatEntity source, CombatColony target) {
        return super.estimatedBombardDamage(source, target) * target.torpedoDamageMod();
    }
    @Override
    public void drawAttackEffect(CombatEntity source, CombatEntity target, Component comp) { }
}

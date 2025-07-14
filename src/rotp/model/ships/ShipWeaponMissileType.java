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

public class ShipWeaponMissileType extends ShipWeapon {
    private static final long serialVersionUID = 1L;
    @Override
    public boolean isMissileWeapon()      { return true; }
    @Override
    public boolean isLimitedShotWeapon()  { return false; }
    @Override
    public float planetDamageMod()        { return 1; }
    @Override
    public int bombardAttacks()           { return 1;}
    public float damageLoss(float dist)   { return 0; }
    @Override
    public boolean canAttackShips()       { return true; }
    @Override
    public int range()                    { return 0; }
    @Override
    public int shots()                    { return 1; }
    public float speed()                  { return 1; }
    public Image image(int num)           { return null; }
    public void dealDamage(CombatEntity target, float damage, float shieldMod) {
        target.takeMissileDamage(damage, shieldMod);
    }
    @Override
    public float estimatedBombardDamage(CombatEntity source, CombatColony target) {
        // missiles always do max damage on bombardment
        return super.estimatedBombardDamage(source, target) * bombardAttacks();
    }
    @Override
    public void drawAttackEffect(CombatEntity source, CombatEntity target, Component comp) { }
    @Override
    public void fireUpon(CombatEntity source, CombatEntity target, int count) {
        if (random() < target.autoMissPct())
        {
            if(target.mgr.showAnimations())
                drawUnsuccessfulAttack(source, target);
            return;
        }
        if (target.interceptsMissile(this))
        {
            if(target.mgr.showAnimations())
                drawUnsuccessfulAttack(source, target);
            return;
        }

        boolean isColony = target.isColony();
        int minDamage = minDamage();
        int maxDamage = maxDamage();
        int defense = target.missileDefense();
        int attack = source.attackLevel() + computerLevel();
        float hitPct = (5 + attack - defense) / 10f;
        hitPct = max(.05f, hitPct);

        float totalDamage = 0;
        float damageLoss = damageLoss(source.distance);
        float shieldMod = source.targetShieldMod(this)*shieldMod();
        boolean successfullyHit = false;
        for (int i=0;i<count;i++) {
            if (random() <= hitPct) {
                successfullyHit = true;
                float damage = 0;
                if (isColony)
                    damage = maxDamage;
                else
                    damage = roll(minDamage, maxDamage);

                // adjust dmg for missiles that lose strength as they travel
                damage = max(0,damage-damageLoss);
                damage = target.takeMissileDamage(damage, shieldMod);
                totalDamage += damage;
            }
        }
        if (target.mgr.showAnimations())
        {
            if(totalDamage >0)
                tech().drawSuccessfulAttack(null, target, 0, totalDamage);
            else if (successfullyHit)
                drawIneffectiveAttack(source, target);
            else
                drawUnsuccessfulAttack(source, target);
        }
    }
}

/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024-2025 Ilya Zushinskiy
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
package rotp.model.tech;

import rotp.model.empires.Empire;
import rotp.model.ships.ShipWeaponTorpedo;

public final class TechTorpedoWeapon extends Tech {
    public String imageKey = "";
    private int damage = 0;
    public float speed = 1;
    public int computer = 0;
    public int attacks = 1;
    public int turnsToFire = 1;
    public int damageLoss = 0;

    public int shots = 1;
    public int range = 0;

    public int damage()  { return damage; }

    @Override
    public String imageKey()   { return imageKey; }

    public TechTorpedoWeapon (String typeId, int lv, int seq, boolean b, TechCategory c) {
        super(c, Tech.TORPEDO_WEAPON, typeId, seq, lv);
        free = b;
        turnsToFire = 2;

        switch(typeSeq) {
            case 0: // ANTI-MATTER TORPEDOS
                damage = 30;
                speed = 3;
                size = 75;
                power = 300;
                cost = 56;
                computer = 4;
                range = 8;
                imageKey = "TORPEDO_ANTI_MATTER";
                break;
            case 1: // HELLFIRE TORPEDOS
                damage = 25;
                speed = 5;
                size = 150;
                power = 350;
                cost = 110;
                computer = 6;
                attacks = 4;
                range = 10;
                imageKey = "TORPEDO_HELLFIRE";
                break;
            case 2: // PROTON TORPEDOS
                damage = 75;
                speed = 5;
                cost = 120;
                size = 100;
                power = 400;
                computer = 6;
                range = 10;
                imageKey = "TORPEDO_PROTON";
                break;
            case 3: // PLASMA TORPEDOS
                damage = 150;
                speed = 6;
                cost = 150;
                size = 150;
                power = 450;
                computer = 7;
                damageLoss = 15;
                range = 10;
                imageKey = "TORPEDO_PLASMA";
                break;
        }
    }
    
    @Override
    public float warModeFactor()        { return 2; }
    public float range()   { return 0; }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public boolean isObsolete(Empire c) {
        return false;
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        ShipWeaponTorpedo sh = new ShipWeaponTorpedo(this);
        c.shipLab().addWeapon(sh);
    }
}

/* Copyright 2024 Ilya Zushinskiy
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
package rotp.model.combat;

import rotp.model.ships.ShipWeapon;

public class WeaponGroup {
    
    private final ShipWeapon shipWeapon;
    private final int weaponCount;
    
    public WeaponGroup(ShipWeapon weapon, int count) {
        this.shipWeapon = weapon;
        this.weaponCount = count;
    }
    
    public ShipWeapon getWeapon() {
        return shipWeapon;
    }

    public int getCount() {
        return weaponCount;
    }
}

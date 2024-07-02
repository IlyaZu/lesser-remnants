/*
 * Copyright 2015-2020 Ray Fowler
 * Copyright 2024 Ilya Zushinskiy
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

import java.awt.Image;
import java.util.List;
import java.util.Arrays;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipSpecialBeamFocus;
import rotp.model.ships.ShipSpecialMissileShield;
import rotp.model.ships.ShipSpecialRepair;
import rotp.model.ships.ShipWeaponBeam;
import rotp.model.ships.ShipWeaponMissile;
import rotp.model.ships.ShipWeaponTorpedo;
import rotp.model.tech.TechAutomatedRepair;
import rotp.model.tech.TechBeamFocus;
import rotp.model.tech.TechLibrary;
import rotp.model.tech.TechMissileShield;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechShipWeapon;
import rotp.model.tech.TechTorpedoWeapon;
import rotp.util.ImageManager;
import rotp.util.LabelManager;

public class CombatMonster extends CombatShip {

    public static CombatMonster makeGuardian(ShipCaptain captian, CombatManager manager) {
        List<WeaponGroup> weaponGroup = Arrays.asList(
                new WeaponGroup(new ShipWeaponMissile((TechMissileWeapon)TechLibrary.current().tech("MissileWeapon:10"), true, 5, 7, 3.5f), 85),
                new WeaponGroup(new ShipWeaponBeam((TechShipWeapon)TechLibrary.current().tech("ShipWeapon:20"), false), 45),
                new WeaponGroup(new ShipWeaponTorpedo((TechTorpedoWeapon)TechLibrary.current().tech("TorpedoWeapon:3")), 18),
                new WeaponGroup(new ShipWeaponBeam((TechShipWeapon)TechLibrary.current().tech("ShipWeapon:16"), false), 1));
        
        List<ShipSpecial> specials = Arrays.asList(
                new ShipSpecialBeamFocus((TechBeamFocus)TechLibrary.current().tech("BeamFocus:0")),
                new ShipSpecialRepair((TechAutomatedRepair)TechLibrary.current().tech("AutomatedRepair:1")),
                new ShipSpecialMissileShield((TechMissileShield)TechLibrary.current().tech("MissileShield:2")));
        
        return new CombatMonster(
                1, 10000, 9,
                10, 9, 9,
                2, 2, 12,
                weaponGroup, specials,
                ImageManager.current().image("ORION_GUARDIAN"), LabelManager.current().label("PLANET_ORION_GUARDIAN"),
                captian, manager);
    }
    
    private CombatMonster(int count, float hits, float shield,
            int attack, int beamDefense, int missileDefense,
            int maneuverability, int move, int initiative,
            List<WeaponGroup> weaponGroups, List<ShipSpecial> specials,
            Image image, String name,
            ShipCaptain captian, CombatManager manager) {
        
        super(count, hits, shield,
                attack, beamDefense, missileDefense,
                maneuverability, move, initiative,
                weaponGroups, specials,
                image, name,
                captian, manager);
    }
    
    @Override
    public boolean isMonster() {
        return true;
    }
    
}

/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024-2026 Ilya Zushinskiy
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

import java.util.HashMap;

public final class TechLibrary {
    private static final TechLibrary instance = new TechLibrary();
    public static TechLibrary current() {
        return instance;
    }

    private final TechCategory[] baseCategory = new TechCategory[TechTree.NUM_CATEGORIES];
    private final HashMap <String, Tech> techMap = new HashMap<>();

    private TechLibrary() {
        var computerCategory = makeCategory(0);
        addTech(new TechECMJammer(0, 0, true, computerCategory));
        addTech(new TechBattleComputer(0, 0, true, computerCategory));
        addTech(new TechBattleComputer(0, 1, true, computerCategory));
        addTech(new TechScanner(1, 0, true, computerCategory));
        addTech(new TechECMJammer(2, 1, false, computerCategory));
        addTech(new TechScanner(4, 1, false, computerCategory));
        addTech(new TechBattleComputer(5, 2, false, computerCategory));
        addTech(new TechECMJammer(7, 2, false, computerCategory));
        addTech(new TechRoboticControls(8, 0, false, computerCategory));
        addTech(new TechBattleComputer(10, 3, false, computerCategory));
        addTech(new TechECMJammer(12, 3, false, computerCategory));
        addTech(new TechScanner(13, 2, false, computerCategory));
        addTech(new TechBattleComputer(15, 4, false, computerCategory));
        addTech(new TechECMJammer(17, 4, false, computerCategory));
        addTech(new TechRoboticControls(18, 1, false, computerCategory));
        addTech(new TechBattleComputer(20, 5, false, computerCategory));
        addTech(new TechECMJammer(22, 5, false, computerCategory));
        addTech(new TechScanner(23, 3, false, computerCategory));
        addTech(new TechBattleComputer(25, 6, false, computerCategory));
        addTech(new TechECMJammer(27, 6, false, computerCategory));
        addTech(new TechRoboticControls(28, 2, false, computerCategory));
        addTech(new TechBattleComputer(30, 7, false, computerCategory));
        addTech(new TechECMJammer(32, 7, false, computerCategory));
        addTech(new TechHyperspaceComm(34, 0, false, computerCategory));
        addTech(new TechBattleComputer(35, 8, false, computerCategory));
        addTech(new TechECMJammer(37, 8, false, computerCategory));
        addTech(new TechRoboticControls(38, 3, false, computerCategory));
        addTech(new TechBattleComputer(40, 9, false, computerCategory));
        addTech(new TechECMJammer(42, 9, false, computerCategory));
        addTech(new TechBattleComputer(45, 10, false, computerCategory));
        addTech(new TechBeamFocus(46, 1, false, computerCategory));
        addTech(new TechECMJammer(47, 10, false, computerCategory));
        addTech(new TechRoboticControls(48, 4, false, computerCategory));
        addTech(new TechShipNullifier(49, 0, false, computerCategory));
        addTech(new TechBattleComputer(50, 11, false, computerCategory));
        addTech(new TechFutureComputer(55, 0, false, computerCategory));
        addTech(new TechFutureComputer(60, 1, false, computerCategory));
        addTech(new TechFutureComputer(65, 2, false, computerCategory));
        addTech(new TechFutureComputer(70, 3, false, computerCategory));
        addTech(new TechFutureComputer(75, 4, false, computerCategory));
        addTech(new TechFutureComputer(80, 5, false, computerCategory));
        addTech(new TechFutureComputer(85, 6, false, computerCategory));
        addTech(new TechFutureComputer(90, 7, false, computerCategory));
        addTech(new TechFutureComputer(95, 8, false, computerCategory));
        addTech(new TechFutureComputer(100, 9, false, computerCategory));
        
        var contructionCategory = makeCategory(1);
        addTech(new TechBattleSuit(0, 0, true, contructionCategory));
        addTech(new TechReserveFuelRange(1, 0, true, contructionCategory));
        addTech(new TechArmor(1, 0, true, contructionCategory));
        addTech(new TechImprovedIndustrial(3, 0, false, contructionCategory));
        addTech(new TechIndustrialWaste(5, 0, false, contructionCategory));
        addTech(new TechImprovedIndustrial(8, 1, false, contructionCategory));
        addTech(new TechArmor(10, 1, false, contructionCategory));
        addTech(new TechBattleSuit(11, 1, false, contructionCategory));
        addTech(new TechImprovedIndustrial(13, 2, false, contructionCategory));
        addTech(new TechAutomatedRepair(14, 0, false, contructionCategory));
        addTech(new TechIndustrialWaste(15, 1, false, contructionCategory));
        addTech(new TechArmor(17, 2, false, contructionCategory));
        addTech(new TechImprovedIndustrial(18, 3, false, contructionCategory));
        addTech(new TechImprovedIndustrial(23, 4, false, contructionCategory));
        addTech(new TechBattleSuit(24, 2, false, contructionCategory));
        addTech(new TechIndustrialWaste(25, 2, false, contructionCategory));
        addTech(new TechArmor(26, 3, false, contructionCategory));
        addTech(new TechImprovedIndustrial(28, 5, false, contructionCategory));
        addTech(new TechImprovedIndustrial(33, 6, false, contructionCategory));
        addTech(new TechArmor(34, 4, false, contructionCategory));
        addTech(new TechIndustrialWaste(35, 3, false, contructionCategory));
        addTech(new TechAutomatedRepair(36, 1, false, contructionCategory));
        addTech(new TechImprovedIndustrial(38, 7, false, contructionCategory));
        addTech(new TechBattleSuit(40, 3, false, contructionCategory));
        addTech(new TechArmor(42, 5, false, contructionCategory));
        addTech(new TechIndustrialWaste(45, 4, false, contructionCategory));
        addTech(new TechArmor(50, 6, false, contructionCategory));
        addTech(new TechFutureConstruction(55, 0, false, contructionCategory));
        addTech(new TechFutureConstruction(60, 1, false, contructionCategory));
        addTech(new TechFutureConstruction(65, 2, false, contructionCategory));
        addTech(new TechFutureConstruction(70, 3, false, contructionCategory));
        addTech(new TechFutureConstruction(75, 4, false, contructionCategory));
        addTech(new TechFutureConstruction(80, 5, false, contructionCategory));
        addTech(new TechFutureConstruction(85, 6, false, contructionCategory));
        addTech(new TechFutureConstruction(90, 7, false, contructionCategory));
        addTech(new TechFutureConstruction(95, 8, false, contructionCategory));
        addTech(new TechFutureConstruction(100, 9, false, contructionCategory));
        
        var forceFieldsCategory = makeCategory(2);
        addTech(new TechDeflectorShield(0, 0, true, forceFieldsCategory));
        addTech(new TechPersonalShield(0, 0, true, forceFieldsCategory));
        addTech(new TechDeflectorShield(1, 1, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(4, 2, false, forceFieldsCategory));
        addTech(new TechPersonalShield(8, 1, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(10, 3, false, forceFieldsCategory));
        addTech(new TechPlanetaryShield(12, 0, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(14, 4, false, forceFieldsCategory));
        addTech(new TechRepulsor(15, 0, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(20, 5, false, forceFieldsCategory));
        addTech(new TechPersonalShield(21, 2, false, forceFieldsCategory));
        addTech(new TechPlanetaryShield(22, 1, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(24, 6, false, forceFieldsCategory));
        addTech(new TechCloaking(27, 0, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(30, 7, false, forceFieldsCategory));
        addTech(new TechMissileShield(31, 1, false, forceFieldsCategory));
        addTech(new TechPlanetaryShield(32, 2, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(34, 8, false, forceFieldsCategory));
        addTech(new TechStasisField(37, 0, false, forceFieldsCategory));
        addTech(new TechPersonalShield(38, 3, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(40, 9, false, forceFieldsCategory));
        addTech(new TechPlanetaryShield(42, 3, false, forceFieldsCategory));
        addTech(new TechBlackHole(43, 0, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(44, 10, false, forceFieldsCategory));
        addTech(new TechMissileShield(46, 2, false, forceFieldsCategory));
        addTech(new TechDeflectorShield(50, 11, false, forceFieldsCategory));
        addTech(new TechFutureForceField(55, 0, false, forceFieldsCategory));
        addTech(new TechFutureForceField(60, 1, false, forceFieldsCategory));
        addTech(new TechFutureForceField(65, 2, false, forceFieldsCategory));
        addTech(new TechFutureForceField(70, 3, false, forceFieldsCategory));
        addTech(new TechFutureForceField(75, 4, false, forceFieldsCategory));
        addTech(new TechFutureForceField(80, 5, false, forceFieldsCategory));
        addTech(new TechFutureForceField(85, 6, false, forceFieldsCategory));
        addTech(new TechFutureForceField(90, 7, false, forceFieldsCategory));
        addTech(new TechFutureForceField(95, 8, false, forceFieldsCategory));
        addTech(new TechFutureForceField(100, 9, false, forceFieldsCategory));
        
        var planetaryCategory = makeCategory(3);
        addTech(new TechControlEnvironment(1, 0, true, planetaryCategory));
        addTech(new TechEcoRestoration(1, 0, true, planetaryCategory));
        addTech(new TechImprovedTerraforming(2, 0, false, planetaryCategory));
        addTech(new TechControlEnvironment(3, 1, false, planetaryCategory));
        addTech(new TechEcoRestoration(5, 1, false, planetaryCategory));
        addTech(new TechControlEnvironment(6, 2, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(8, 1, false, planetaryCategory));
        addTech(new TechControlEnvironment(9, 3, false, planetaryCategory));
        addTech(new TechBiologicalWeapon(10, 0, false, planetaryCategory));
        addTech(new TechControlEnvironment(12, 4, false, planetaryCategory));
        addTech(new TechEcoRestoration(13, 2, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(14, 2, false, planetaryCategory));
        addTech(new TechControlEnvironment(15, 5, false, planetaryCategory));
        addTech(new TechSoilEnrichment(16, 0, false, planetaryCategory));
        addTech(new TechBiologicalAntidote(17, 0, false, planetaryCategory));
        addTech(new TechControlEnvironment(18, 6, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(20, 3, false, planetaryCategory));
        addTech(new TechCloning(21, 0, false, planetaryCategory));
        addTech(new TechAtmosphereEnrichment(22, 0, false, planetaryCategory));
        addTech(new TechEcoRestoration(24, 3, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(26, 4, false, planetaryCategory));
        addTech(new TechBiologicalWeapon(27, 1, false, planetaryCategory));
        addTech(new TechSoilEnrichment(30, 1, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(32, 5, false, planetaryCategory));
        addTech(new TechEcoRestoration(34, 4, false, planetaryCategory));
        addTech(new TechBiologicalAntidote(36, 1, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(38, 6, false, planetaryCategory));
        addTech(new TechBiologicalWeapon(40, 2, false, planetaryCategory));
        addTech(new TechCloning(42, 1, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(44, 7, false, planetaryCategory));
        addTech(new TechImprovedTerraforming(50, 8, false, planetaryCategory));
        addTech(new TechFuturePlanetology(55, 0, false, planetaryCategory));
        addTech(new TechFuturePlanetology(60, 1, false, planetaryCategory));
        addTech(new TechFuturePlanetology(65, 2, false, planetaryCategory));
        addTech(new TechFuturePlanetology(70, 3, false, planetaryCategory));
        addTech(new TechFuturePlanetology(75, 4, false, planetaryCategory));
        addTech(new TechFuturePlanetology(80, 5, false, planetaryCategory));
        addTech(new TechFuturePlanetology(85, 6, false, planetaryCategory));
        addTech(new TechFuturePlanetology(90, 7, false, planetaryCategory));
        addTech(new TechFuturePlanetology(95, 8, false, planetaryCategory));
        addTech(new TechFuturePlanetology(100, 9, false, planetaryCategory));
        
        var propulsionCategory = makeCategory(4);
        addTech(new TechEngineWarp(1, 0, true, propulsionCategory));
        addTech(new TechFuelRange(1, 0, true, propulsionCategory));
        addTech(new TechFuelRange(3, 1, false, propulsionCategory));
        addTech(new TechFuelRange(5, 2, false, propulsionCategory));
        addTech(new TechEngineWarp(6, 1, false, propulsionCategory));
        addTech(new TechFuelRange(9, 3, false, propulsionCategory));
        addTech(new TechShipInertial(10, 0, false, propulsionCategory));
        addTech(new TechEngineWarp(12, 2, false, propulsionCategory));
        addTech(new TechFuelRange(14, 4, false, propulsionCategory));
        addTech(new TechEnergyPulsar(16, 0, false, propulsionCategory));
        addTech(new TechEngineWarp(18, 3, false, propulsionCategory));
        addTech(new TechFuelRange(19, 5, false, propulsionCategory));
        addTech(new TechShipNullifier(20, 1, false, propulsionCategory));
        addTech(new TechFuelRange(23, 6, false, propulsionCategory));
        addTech(new TechEngineWarp(24, 4, false, propulsionCategory));
        addTech(new TechStargate(27, 0, false, propulsionCategory));
        addTech(new TechFuelRange(29, 7, false, propulsionCategory));
        addTech(new TechEngineWarp(30, 5, false, propulsionCategory));
        addTech(new TechBeamFocus(34, 0, false, propulsionCategory));
        addTech(new TechEngineWarp(36, 6, false, propulsionCategory));
        addTech(new TechTeleporter(38, 0, false, propulsionCategory));
        addTech(new TechEnergyPulsar(40, 1, false, propulsionCategory));
        addTech(new TechFuelRange(41, 8, false, propulsionCategory));
        addTech(new TechEngineWarp(42, 7, false, propulsionCategory));
        addTech(new TechSubspaceInterdictor(43, 0, false, propulsionCategory));
        addTech(new TechCombatTransporter(45, 0, false, propulsionCategory));
        addTech(new TechShipInertial(46, 1, false, propulsionCategory));
        addTech(new TechEngineWarp(48, 8, false, propulsionCategory));
        addTech(new TechDisplacement(50, 0, false, propulsionCategory));
        addTech(new TechFuturePropulsion(55, 0, false, propulsionCategory));
        addTech(new TechFuturePropulsion(60, 1, false, propulsionCategory));
        addTech(new TechFuturePropulsion(65, 2, false, propulsionCategory));
        addTech(new TechFuturePropulsion(70, 3, false, propulsionCategory));
        addTech(new TechFuturePropulsion(75, 4, false, propulsionCategory));
        addTech(new TechFuturePropulsion(80, 5, false, propulsionCategory));
        addTech(new TechFuturePropulsion(85, 6, false, propulsionCategory));
        addTech(new TechFuturePropulsion(90, 7, false, propulsionCategory));
        addTech(new TechFuturePropulsion(95, 8, false, propulsionCategory));
        addTech(new TechFuturePropulsion(100, 9, false, propulsionCategory));
        
        var weaponsCategory = makeCategory(5);
        addTech(new TechHandWeapon(0, 0, true, weaponsCategory));
        addTech(new TechBombWeapon(1, 0, true, weaponsCategory));
        addTech(new TechMissileWeapon(1, 0, true, weaponsCategory));
        addTech(new TechShipWeapon(1, 0, true, weaponsCategory));
        addTech(new TechHandWeapon(2, 1, true, weaponsCategory));
        addTech(new TechMissileWeapon(4, 1, false, weaponsCategory));
        addTech(new TechShipWeapon(5, 1, false, weaponsCategory));
        addTech(new TechMissileShield(6, 0, false, weaponsCategory));
        addTech(new TechShipWeapon(7, 2, false, weaponsCategory));
        addTech(new TechMissileWeapon(8, 2, false, weaponsCategory));
        addTech(new TechBombWeapon(9, 1, false, weaponsCategory));
        addTech(new TechShipWeapon(10, 3, false, weaponsCategory));
        addTech(new TechMissileWeapon(11, 3, false, weaponsCategory));
        addTech(new TechHandWeapon(12, 2, true, weaponsCategory));
        addTech(new TechShipWeapon(13, 4, false, weaponsCategory));
        addTech(new TechMissileWeapon(14, 4, false, weaponsCategory));
        addTech(new TechShipWeapon(15, 5, false, weaponsCategory));
        addTech(new TechBombWeapon(16, 2, false, weaponsCategory));
        addTech(new TechShipWeapon(17, 6, false, weaponsCategory));
        addTech(new TechMissileWeapon(18, 5, false, weaponsCategory));
        addTech(new TechShipWeapon(19, 7, false, weaponsCategory));
        addTech(new TechShipWeapon(20, 8, false, weaponsCategory));
        addTech(new TechStreamProjector(21, 0, false, weaponsCategory));
        addTech(new TechBombWeapon(22, 3, false, weaponsCategory));
        addTech(new TechTorpedoWeapon(23, 0, false, weaponsCategory));
        addTech(new TechHandWeapon(24, 3, true, weaponsCategory));
        addTech(new TechShipWeapon(25, 9, false, weaponsCategory));
        addTech(new TechShipWeapon(26, 10, false, weaponsCategory));
        addTech(new TechMissileWeapon(27, 6, false, weaponsCategory));
        addTech(new TechShipWeapon(28, 11, false, weaponsCategory));
        addTech(new TechMissileWeapon(29, 7, false, weaponsCategory));
        addTech(new TechShipWeapon(30, 12, false, weaponsCategory));
        addTech(new TechHandWeapon(31, 4, true, weaponsCategory));
        addTech(new TechShipWeapon(32, 13, false, weaponsCategory));
        addTech(new TechShipWeapon(33, 14, false, weaponsCategory));
        addTech(new TechMissileWeapon(34, 8, false, weaponsCategory));
        addTech(new TechShipWeapon(35, 15, false, weaponsCategory));
        addTech(new TechShipWeapon(36, 16, false, weaponsCategory));
        addTech(new TechShipWeapon(37, 17, false, weaponsCategory));
        addTech(new TechShipWeapon(38, 18, false, weaponsCategory));
        addTech(new TechBombWeapon(39, 4, false, weaponsCategory));
        addTech(new TechTorpedoWeapon(40, 1, false, weaponsCategory));
        addTech(new TechMissileWeapon(41, 9, false, weaponsCategory));
        addTech(new TechHandWeapon(42, 5, true, weaponsCategory));
        addTech(new TechTorpedoWeapon(43, 2, false, weaponsCategory));
        addTech(new TechMissileWeapon(44, 10, false, weaponsCategory));
        addTech(new TechShipWeapon(45, 19, false, weaponsCategory));
        addTech(new TechShipWeapon(46, 20, false, weaponsCategory));
        addTech(new TechStreamProjector(47, 1, false, weaponsCategory));
        addTech(new TechShipWeapon(48, 21, false, weaponsCategory));
        addTech(new TechTorpedoWeapon(50, 3, false, weaponsCategory));
        addTech(new TechFutureWeapon(55, 0, false, weaponsCategory));
        addTech(new TechFutureWeapon(60, 1, false, weaponsCategory));
        addTech(new TechFutureWeapon(65, 2, false, weaponsCategory));
        addTech(new TechFutureWeapon(70, 3, false, weaponsCategory));
        addTech(new TechFutureWeapon(75, 4, false, weaponsCategory));
        addTech(new TechFutureWeapon(80, 5, false, weaponsCategory));
        addTech(new TechFutureWeapon(85, 6, false, weaponsCategory));
        addTech(new TechFutureWeapon(90, 7, false, weaponsCategory));
        addTech(new TechFutureWeapon(95, 8, false, weaponsCategory));
        addTech(new TechFutureWeapon(100, 9, false, weaponsCategory));
        addTech(new TechShipWeapon(100, 22, false, weaponsCategory));
        addTech(new TechShipWeapon(100, 23, false, weaponsCategory));
    }
    
    private TechCategory makeCategory(int index) {
        var category = new TechCategory(index);
        baseCategory[index] = category;
        return category;
    }
    
    private void addTech(Tech tech) {
        baseCategory[tech.categoryIndex()].addPossibleTech(tech.id());
        techMap.put(tech.id(), tech);
    }
    
    public TechCategory techCategory(int index) {
        return baseCategory[index];
    }
    
    public Tech tech(String id) {
        return techMap.get(id);
    }

}

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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import rotp.util.Base;

public final class TechLibrary implements Base {
    private static final String techDataFile = "data/techs.txt";
    private static final TechLibrary instance;
    public static TechLibrary current()   { return instance; }
    public static TechCategory[] baseCategory = new TechCategory[TechTree.NUM_CATEGORIES];
    private static TechCategory loadingCat;

    private final HashMap <String, Tech> techMap = new HashMap<>();

    static {
        instance = new TechLibrary();
        instance.loadTechDataFile(techDataFile);
    }
    @Override
    public Tech tech(String id)               { return techMap.get(id); }
    private void loadTechDataFile(String filename) {
        BufferedReader in = reader(filename);
        if (in == null)
            return;

        try {
            String input;
            while ((input = in.readLine()) != null)
                loadTechDataLine(input);
            in.close();
        }
        catch (IOException e) {
            err("TechTree.loadTechDataFiles -- IOException: " + e);
        }
    }
    private void loadTechDataLine(String input) {
        String line = input.trim();
        if (isComment(input))
            return;

        List<String> vals = this.substrings(input, ':');
        if (vals.size() < 2)
            return;

        String key = vals.get(0);
        String value = vals.get(1);
        if (key.equalsIgnoreCase("cat"))           { parseCategoryLine(value); return; }
        if (key.equalsIgnoreCase("tech"))          { parseTechLine(value); return; }

        err("unknown tech key->", line);
    }
    private void parseCategoryLine(String input) {
        // field #1 is category index (only field for now)
        int index = parseInt(input);
        TechCategory newCat = new TechCategory();
        newCat.index(index);
        loadingCat = newCat;
        baseCategory[index] = newCat;
    }
    private void parseTechLine(String input) {
        List<String> fields = substrings(input, ',');
        if (fields.size() < 5)
            err("Invalid tech line, <5 fields: ", input);

        int researchLevel = parseInt(fields.get(0));
        String techType = fields.get(1);
        int techSeq = parseInt(fields.get(2));
        boolean techFree = parseInt(fields.get(3)) == 1;
        String iconName = fields.get(4);
        String effect = fields.size() > 5 ? fields.get(5) : "";

        Tech newTech = newLoadedTech(researchLevel, techType, techSeq, techFree);
        if (newTech != null) {
            newTech.iconFilename = iconName;
            newTech.effectKey = effect;
            loadingCat.addPossibleTech(newTech.id());
            techMap.put(newTech.id(), newTech);
        }
    }
    private Tech newLoadedTech(int level, String type, int seq, boolean free) {
        if (type.equalsIgnoreCase("Scanner"))              { return new TechScanner(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BattleComputer"))       { return new TechBattleComputer(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ECMJammer"))            { return new TechECMJammer(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("RoboticControls"))      { return new TechRoboticControls(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("HyperspaceComm"))       { return new TechHyperspaceComm(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BeamFocus"))            { return new TechBeamFocus(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ShipNullifier"))        { return new TechShipNullifier(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Armor"))                { return new TechArmor(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ReserveFuelRange"))     { return new TechReserveFuelRange(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ImprovedIndustrial"))   { return new TechImprovedIndustrial(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("IndustrialWaste"))      { return new TechIndustrialWaste(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BattleSuit"))           { return new TechBattleSuit(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("AutomatedRepair"))      { return new TechAutomatedRepair(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("DeflectorShield"))      { return new TechDeflectorShield(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Cloaking"))             { return new TechCloaking(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Repulsor"))             { return new TechRepulsor(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("PersonalShield"))       { return new TechPersonalShield(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("PlanetaryShield"))      { return new TechPlanetaryShield(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("MissileShield"))        { return new TechMissileShield(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("StasisField"))          { return new TechStasisField(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BlackHole"))            { return new TechBlackHole(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ControlEnvironment"))   { return new TechControlEnvironment(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("EcoRestoration"))       { return new TechEcoRestoration(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ImprovedTerraforming")) { return new TechImprovedTerraforming(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BiologicalWeapon"))     { return new TechBiologicalWeapon(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("SoilEnrichment"))       { return new TechSoilEnrichment(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BiologicalAntidote"))   { return new TechBiologicalAntidote(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Cloning"))              { return new TechCloning(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("AtmosphereEnrichment")) { return new TechAtmosphereEnrichment(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("EngineWarp"))           { return new TechEngineWarp(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FuelRange"))            { return new TechFuelRange(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ShipInertial"))         { return new TechShipInertial(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("EnergyPulsar"))         { return new TechEnergyPulsar(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Stargate"))             { return new TechStargate(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Teleporter"))           { return new TechTeleporter(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("SubspaceInterdictor"))  { return new TechSubspaceInterdictor(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("CombatTransporter"))    { return new TechCombatTransporter(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("Displacement"))         { return new TechDisplacement(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("BombWeapon"))           { return new TechBombWeapon(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("MissileWeapon"))        { return new TechMissileWeapon(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("ShipWeapon"))           { return new TechShipWeapon(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("HandWeapon"))           { return new TechHandWeapon(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("StreamProjector"))      { return new TechStreamProjector(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("TorpedoWeapon"))        { return new TechTorpedoWeapon(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureComputer"))       { return new TechFutureComputer(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureConstruction"))   { return new TechFutureConstruction(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureForceField"))     { return new TechFutureForceField(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FuturePlanetology"))    { return new TechFuturePlanetology(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FuturePropulsion"))     { return new TechFuturePropulsion(level, seq, free, loadingCat); }
        if (type.equalsIgnoreCase("FutureWeapon"))         { return new TechFutureWeapon(level, seq, free, loadingCat); }

        return null;
    }
}

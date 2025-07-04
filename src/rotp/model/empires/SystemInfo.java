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
package rotp.model.empires;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import rotp.model.ai.FleetPlan;
import rotp.model.colony.Colony;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.planet.PlanetType;
import rotp.util.Base;
import rotp.util.ImageManager;

public class SystemInfo implements Serializable, Base {
    private static final long serialVersionUID = 1L;
    private static final Color CAN_COLONIZE_COLOR = new Color(255, 255, 255);
    private static final Color LEARNING_TO_COLONIZE_COLOR = new Color(192,192,128);
    private static final Color CAN_NOT_COLONIZE_COLOR = new Color(128, 128, 128);
    
    private final int empireId;
    private final SystemView[] views;
    private final float[] distances;
    private transient Empire empire;
    private transient BufferedImage starBackground;
    
    public void addView(SystemView sv)      { views[sv.system().id] = sv; }
    public int count()                      { return views.length; }
    public boolean missing(int i)          { return (i < 0) || views[i] == null; }
    public SystemView view(int sysId) {
        if (views[sysId] == null)
            views[sysId] = SystemView.create(sysId, empireId);
        return views[sysId];
    }
    private Empire empire() {
        if (empire == null)
            empire = galaxy().empire(empireId);
        return empire;
    }
    public String name(int i)            { return missing(i) ? "" : view(i).name(); }
    public void name(int i, String s)    { view(i).name(s); }
    public String descriptiveName(int i) { return missing(i) ? "" : view(i).descriptiveName(); }
    public StarSystem system(int i)      { return galaxy().system(i); }
    public Empire empire(int i)          { return missing(i) ? null : view(i).empire(); }
    public int empId(int i)              { return missing(i) ? Empire.NULL_ID : view(i).empId(); }
    public Colony colony(int i)          { return missing(i) ? null : view(i).colony(); }
    public PlanetType planetType(int i)  { return view(i).planetType(); }
    public float distance(int i)        { return distances[i]; }
    public int population(int i)         { return missing(i) ? 0  : view(i).population(); }
    public int deltaPopulation(int i)    { return missing(i) ? 0  : view(i).deltaPopulation(); }
    public int factories(int i)          { return missing(i) ? 0  : view(i).factories(); }
    public int deltaFactories(int i)     { return missing(i) ? 0  : view(i).deltaFactories(); }
    public int shieldLevel(int i)        { return missing(i) ? 0  : view(i).shieldLevel(); }
    public int bases(int i)              { return missing(i) ? 0  : view(i).bases(); }
    public int deltaBases(int i)         { return missing(i) ? 0  : view(i).deltaBases(); }
    public int currentSize(int i)        { return missing(i) ? 0  : view(i).currentSize(); }
    public int desiredMissileBases(int i)  { return missing(i) ? 0  : view(i).desiredMissileBases(); }
    public float defenderCombatAdj(int i)  { return missing(i) ? 0  : view(i).defenderCombatAdj(); }
    public int spyReportAge(int i)       { return missing(i) ? -1 : view(i).spyReportAge(); }
    public int maxTransportsToSend(int i) { return missing(i) ? 0  : view(i).maxTransportsToSend(); }
    public int maxTransportsToReceive(int i) { return missing(i) ? 0  : view(i).maxTransportsToReceive(); }
    public int rallyTurnsTo(int i, StarSystem s) { return view(i).rallyTurnsTo(s); }
    public float maxPopToGive(int i, float pct)    { return missing(i) ? 0 : view(i).maxPopToGive(pct); }

    public float hostilityLevel(int i)     { return missing(i) ? 0 : view(i).hostilityLevel(); }
    public int artifactLevel(int i)      { return missing(i) ? 0 : view(i).artifacts(); }
    public boolean isScouted(int i)      { return missing(i) ? false : view(i).scouted(); }
    public boolean isSpied(int i)        { return missing(i) ? false : view(i).spied(); }
    public boolean isColonized(int i)    { return missing(i) ? false : view(i).isColonized(); }
    public boolean isAttackTarget(int i) { return missing(i) ? false : view(i).attackTarget(); }
    public boolean isBorderSystem(int i) { return missing(i) ? false : view(i).borderSystem(); }
    public boolean isInnerSystem(int i)  { return missing(i) ? false : view(i).innerSystem(); }
    public boolean isGuarded(int i)      { return missing(i) ? false : view(i).isGuarded(); }
    public boolean isArtifact(int i)     { return missing(i) ? false : view(i).artifact(); }
    public boolean isOrionArtifact(int i){ return missing(i) ? false : view(i).orionArtifact(); }
    public boolean isUltraRich(int i)    { return missing(i) ? false : view(i).resourceUltraRich(); }
    public boolean isRich(int i)         { return missing(i) ? false : view(i).resourceRich(); }
    public boolean isPoor(int i)         { return missing(i) ? false : view(i).resourcePoor(); }
    public boolean isResourceNormal(int i)         { return missing(i) ? false : view(i).resourceNormal(); }
    public boolean isUltraPoor(int i)          { return missing(i) ? false : view(i).resourceUltraPoor(); }
    public boolean hasRallyPoint(int i)        { return missing(i) ? false : view(i).hasRallyPoint(); }
    public boolean hasActiveTransport(int i)   { return missing(i) ? false : view(i).hasActiveTransport(); }
    public boolean canSabotageBases(int i)     { return missing(i) ? false : view(i).canSabotageBases(); }
    public boolean canSabotageFactories(int i) { return missing(i) ? false : view(i).canSabotageFactories(); }
    public boolean canInciteRebellion(int i)   { return missing(i) ? false : view(i).canInciteRebellion(); }
    public int flagColorId(int i)              { return missing(i) ? -1  : view(i).flagColorId(); }
    public Image flagImage(int i)              { return missing(i) ? null  : view(i).flagImage(); }
    public Image mapFlagImage(int i)           { return missing(i) ? null  : view(i).mapFlagImage(); }
    public Image flagHover(int i)              { return ImageManager.current().image("Flag_Hover"); }
    public void toggleFlagColor(int i)          { toggleFlagColor(i, false); }
    public void toggleFlagColor(int i, boolean b) { view(i).toggleFlagColor(b); }
    public void resetFlagColor(int i)            { view(i).resetFlagColor(); }
    public FleetPlan fleetPlan(int i)            { return view(i).fleetPlan(); }
    public ShipFleet orbitingFleet(int i)        { return system(i).orbitingFleetForEmpire(empire()); }
    public List<ShipFleet> orbitingFleets(int i) { return missing(i) ? null : view(i).orbitingFleets(); }
    public List<ShipFleet> exitingFleets(int i)  { return missing(i) ? null : view(i).exitingFleets(); }
    public StarSystem rallySystem(int i)         { return missing(i) ? null : view(i).rallySystem(); }
    public void rallySystem(int i, StarSystem sys) { view(i).rallySystem(sys);}
    public void stopRally(int i)                 { view(i).stopRally(); }
    public boolean forwardRallies(int i)         { return missing(i) ? false : view(i).forwardRallies(); }
    public void toggleForwardRallies(int i)      { if (!missing(i)) view(i).toggleForwardRallies(); }
    public void raiseHostility(int i)            { view(i).raiseHostility(); }
    public boolean hasStargate(int i)            { return missing(i) ? false : view(i).stargate(); }
    public void removeStargate(int i)            { if (!missing(i))
                                                      view(i).removeStargate();
                                                 }
    public boolean hasFleetPlan(int i)           { return missing(i) ? false : view(i).hasFleetPlan(); }
    public boolean inShipRange(int i)            { return distances[i] <= empire().shipRange(); }
    public boolean inScoutRange(int i)           { return distances[i] <= empire().scoutRange(); }
    public boolean withinRange(int i, float r)   { return distances[i] <= r; }
    public BufferedImage planetTerrain(int i)    { return view(i).planetTerrain(); }

    public void refreshFullScan(int i)           { view(i).refreshFullScan(); }
    public void refreshSpyScan(int i)            { view(i).refreshSpyScan(); }
    public void refreshLongRangePlanetScan(int i) { view(i).refreshLongRangePlanetScan(); }
    public void refreshLongRangeShipScan(int i)  { view(i).refreshLongRangeShipScan(); }
    public void refreshAllySharingScan(int i)    { view(i).refreshAllySharingScan(); }
    public void resetSystemData(int i)           { if (!missing(i)) view(i).resetSystemData(); }

    public SystemInfo(Empire e) {
        empireId = e.id;
        int n = galaxy().maxNumStarSystems();
        views = new SystemView[n];
        distances = new float[n];
    }
    public void shareAllyData(SystemInfo you) {
        int n = count();
        for (int i=0;i<n;i++) {
            boolean iKnow = isScouted(i);
            boolean youKnow = you.isScouted(i);
            if (iKnow && !youKnow)
                you.refreshAllySharingScan(i);
            else if (youKnow && !iKnow)
                refreshAllySharingScan(i);
        }
    }
    public void calculateSystemDistances() {
        Galaxy gal = galaxy();
        float minX = gal.width();
        float minY = gal.height();
        float maxX = 0;
        float maxY = 0;

        List<StarSystem> alliedSystems = new ArrayList<>(empire().allColonizedSystems());
        for (StarSystem sys : alliedSystems) {
            minX = min(minX,sys.x());
            maxX = max(maxX,sys.x());
            minY = min(minY,sys.y());
            maxY = max(maxY,sys.y());
        }
        float r = empire().scoutReach(6);
        minX = max(0,minX-r);
        maxX = min(gal.width(), maxX+r);
        minY = max(0,minY-r);
        maxY = min(gal.height(), maxY+r);
        empire().setBounds(minX, maxX, minY, maxY);

        for (Empire ally: empire().allies())
            alliedSystems.addAll(ally.allColonizedSystems());
        for (int i=0;i<distances.length;i++)
            distances[i] = empire().distanceToSystem(gal.system(i), alliedSystems);
    }
    public void clearFleetPlan(int i)  {
        if (!missing(i))
            view(i).clearFleetPlan();
    }
    public Color empireColor(int id) {
        StarSystem sys = galaxy().system(id);
        PlanetType planetType = sys.planet().type();
        if (isColonized(id))
            return options().color(empire(id).shipColorId());
        else if (isScouted(id) && empire().canColonize(id))
            return CAN_COLONIZE_COLOR;
        else if (isScouted(id) && empire().isLearningToColonize(planetType))
            return LEARNING_TO_COLONIZE_COLOR;
        else
            return CAN_NOT_COLONIZE_COLOR;
    }
    public BufferedImage starBackground(JPanel obs) {
        if (starBackground == null) {
            starBackground = new BufferedImage(obs.getWidth(), obs.getHeight(), BufferedImage.TYPE_INT_ARGB);
            drawBackgroundStars(starBackground, obs);
        }
        return starBackground;
    }
}

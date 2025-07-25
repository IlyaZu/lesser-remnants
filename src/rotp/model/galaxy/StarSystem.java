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
package rotp.model.galaxy;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.events.StarSystemEvent;
import rotp.model.planet.Planet;
import rotp.model.planet.PlanetFactory;
import rotp.model.ships.Design;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipLibrary;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.sprites.ShipRelocationSprite;
import rotp.ui.sprites.SystemTransportSprite;
import rotp.util.Base;

public class StarSystem implements Base, Sprite, IMappedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final float clickRadius = 0.9f;
    private static final Color shield5C = new Color(32,255,0); // Green
    private static final Color shield10C = new Color(0,112,224); // Blue
    private static final Color shield15C = new Color(160,48,240); // Purple
    private static final Color shield20C = new Color(255,128,0); // Orange
    private static final Color selectionC = new Color(160,160,0);
    private static final Color systemNameBackC = Color.BLACK;
    public static final int NULL_ID = -1;

    private String name = "";
    private float x, y;
    private Planet planet;
    private final String starTypeKey;
    public final int id;

    private boolean piracy = false;
    private boolean inNebula = false;
    private final List<Transport> orbitingTransports = new ArrayList<>();
    private int[] nearbySystems;
    private String notes;
    private String eventKey;
    private SpaceMonster monster;
    private final List<StarSystemEvent> events = new ArrayList<>();

    public int transportDestId;
    public int transportAmt;
    public float transportTravelTime;
    
    private transient SystemTransportSprite transportSprite;
    private transient ShipRelocationSprite rallySprite;
    private transient StarType starType;
    private transient boolean hovering;
    private transient int twinkleCycle, twinkleOffset, drawRadius;
    private transient boolean displayed = false;

    public SystemTransportSprite transportSprite() {
        if ((transportSprite == null) && isColonized()) {
            transportSprite = new SystemTransportSprite(this);
            if (transportAmt > 0) {
                transportSprite.clickedDest(galaxy().system(transportDestId));
                if (transportTravelTime == 0)
                    transportSprite.accept();
                else
                    transportSprite.accept(transportTravelTime);
            }
        }
        return transportSprite;
    }
    public ShipRelocationSprite rallySprite() {
        if (rallySprite == null)
            rallySprite = new ShipRelocationSprite(this);
        return rallySprite;
    }
    public int[] nearbySystems() {
        if (nearbySystems == null)
            initNearbySystems();
        return nearbySystems;
    }
    public int numNearbySystems() {
        return nearbySystems().length;
    }
    public StarSystem nearbySystem(int i) {
        if (nearbySystems == null)
            initNearbySystems();
        if ((i<0) || (i >= nearbySystems.length))
            return null;
        else
            return galaxy().system(nearbySystems[i]);
    }
    private void initNearbySystems() {
        TARGET_SYSTEM = this;
        float maxDist = 8; // 2 * TechEngineWarp.MAX_SPEED; // modnar: change nearby distance to be more reasonable
        Galaxy gal = galaxy();
        List<StarSystem> nearSystems = new ArrayList<>();
        for (int n=0;n<gal.numStarSystems();n++) {
            StarSystem other = gal.system(n);
            if (distanceTo(other) < maxDist)
                nearSystems.add(other);
        }
        nearSystems.remove(this);
        Collections.sort(nearSystems, DISTANCE_TO_TARGET_SYSTEM);

        int size = min(10,nearSystems.size());
        nearbySystems = new int[size];
        for (int i=0;i<size;i++)
            nearbySystems[i] = nearSystems.get(i).id;
    }
    public static StarSystem create(String key, Galaxy gal) {
        StarSystem s = new StarSystem(key, gal.systemCount);
        return s;
    }
    private StarSystem(String key, int num) {
        starTypeKey = key;
        id = num;
    }
    @Override
    public int displayPriority()           { return 6; }
    @Override
    public boolean hasDisplayPanel()       { return true; }
    @Override
    public float x()                       { return x;  }
    @Override
    public float y()                       { return y;  }
    public void setXY(float x0, float y0) {
        x = x0;
        y = y0;
    }
    @Override
    public String toString()                    { return concat("Star System: ", name()); }

    public boolean unnamed()                    { return name().isEmpty(); }
    public SpaceMonster monster()               { return monster; }
    public void monster(SpaceMonster sm)        { monster = sm; }

    public String name()                        { return name; }
    public void name(String s)                  { name = s; }
    public String ruinsKey()                    { return planet().ruinsKey(); }
    public String notes()                       { return notes == null ? "" : notes; }
    public void notes(String s)                 { notes = s.length() <= 40 ? s : s.substring(0,40); }
    public String eventKey()                    { return eventKey == null ? "": eventKey; }
    public void eventKey(String s)              { eventKey = s; }
    public boolean hasEvent()                   { return eventKey != null; }
    public void clearEvent()                    { eventKey = null; }
    public boolean hasMonster()                 { return monster != null; }
    public void addEvent(StarSystemEvent e)     { events.add(e); }
    public List<StarSystemEvent> events()       { return events; }
    public void clearTransportSprite()          {
        transportSprite = null;
        transportDestId = StarSystem.NULL_ID;
        transportAmt = 0;
    }
    
    public StarType starType()                  {
        if (starType == null)
            starType = StarType.keyed(starTypeKey);
        return starType;
    }
    public boolean piracy()                     { return piracy; }
    public void piracy(boolean b)               { piracy = b; }

    public int orbitingTransports(int empId) {
        for (Transport tr: orbitingTransports) {
            if (tr.empId() == empId)
                return tr.size();
        }
        return 0;
    }
    public List<ShipFleet> orbitingFleets()     { return galaxy().ships.orbitingFleets(id); }
    public List<ShipFleet> exitingFleets()      { return galaxy().ships.deployedFleets(id); }
    public List<ShipFleet> incomingFleets()     { return galaxy().ships.incomingFleets(id); }

    public boolean isColonized()                { return planet().isColonized(); }
    public Colony becomeColonized(String n, Empire e) {
        if (unnamed())
            name = n;
        return planet().becomeColonized(e);
    }
    public float population()   { return isColonized() ? colony().population() : 0.0f; }
    public Planet planet() {
        if (planet == null)
            planet = PlanetFactory.createPlanet(this);
        return planet;
    }
    public void planet(Planet p)                { planet = p; }
    public Colony colony()                      { return planet().colony(); }
    public boolean hasBonusTechs()              { return planet().hasBonusTechs(); }

    public boolean inNebula()                   { return inNebula; }
    public void inNebula(boolean b)             { inNebula = b; }
    public boolean canStargateTravelTo(StarSystem s) {
        return isColonized() && s.isColonized() && (s.empire() == empire()) && colony().hasStargate() && s.colony().hasStargate();
    }
    public float transportTimeTo(StarSystem s) {
        if (canStargateTravelTo(s))
            return 1.0f;
        else
            return distanceTo(s) / empire().transportTravelSpeed(this, s);
    }
    public float rallyTimeTo(StarSystem s) {
        Design d = colony().shipyard().design();
        if (!colony().isBuildingShip())
            return -1;

        if (colony().hasStargate() && s.colony().hasStargate())
            return 0;

        ShipDesign sd = (ShipDesign) d;
        return travelTime(this, s, sd.engine().warp());
    }

    public Empire empire()                      { return planet().empire(); }
    public int empId() {
        Empire e = empire();
        return e == null ? Empire.NULL_ID : e.id;
    }
    public boolean hasColonyForEmpire(Empire c) { return empire() == c; }
    public boolean hasStargate(Empire e)        { return isColonized() && colony().hasStargate(e); }

    public void launchTransports() {
        if (planet().isColonized()) {
            colony().launchTransports();
        }
    }
    public ShipFleet orbitingFleetForEmpire(Empire emp) {
        return emp == null ? null : galaxy().ships.orbitingFleet(emp.id, id);
    }
    public void resolveAnyShipConflict() {
        if (orbitingShipsInConflict())
            galaxy().shipCombat().battle(this);
        else
            resolvePeacefulShipScans();
    }
    private void resolvePeacefulShipScans() {
        List<ShipFleet> fleets = galaxy().ships.orbitingFleets(id);
        
        if (fleets.size() < 2)
            return;
        
        int sysEmpId = this.empId();
                
        for (ShipFleet fl1: fleets) {
            boolean canScan = (fl1.empId() == sysEmpId) || (fl1.allowsScanning());
            for (ShipFleet fl2: fleets) {
                if (fl1 != fl2) {
                    if (canScan)
                        fl1.empire().scanFleet(fl2);
                    else
                        fl1.empire().encounterFleet(fl2);
                }
            }
        }
        
    }
    public void resolvePendingTransports() {
        // land orbiting transports in random order
        while (!orbitingTransports.isEmpty()) {
            Transport tr = random(orbitingTransports);
            orbitingTransports.remove(tr);
            tr.land();
        }
    }
    public boolean enemyShipsInOrbit(Empire emp) {
        List<ShipFleet> fleets = orbitingFleets();
        for (ShipFleet fleet1: fleets) {
            if (emp.aggressiveWith(fleet1.empire(), this))
                return true;
        }
        return false;
    }
    public boolean orbitingShipsInConflict() {
        List<ShipFleet> fleets = orbitingFleets();
        if (hasMonster() && !fleets.isEmpty())
            return true;
        int i=0;
        for (ShipFleet fleet1: fleets) {
            // if this planet is colonized, has bases & is aggressive with the fleet, trigger combat
            // if aggressive but the colony is unarmed, then combat is handled by planetary bombardment
            if (planet().isColonized()
            && planet().colony().defense().isArmed()
            && empire().aggressiveWith(fleet1.empire(), this))
                return true;
            for (int j=i+1;j<fleets.size();j++) {
                ShipFleet fleet2 = fleets.get(j);
                if (fleet1.aggressiveWith(fleet2, this)
                && (fleet1.isArmedForShipCombat() || fleet2.isArmedForShipCombat()))
                    return true;
            }
            i++;
        }
        return false;
    }
    public List<Empire> empiresInConflict() {
        List<Empire> emps = new ArrayList<>();
        List<ShipFleet> fleets = orbitingFleets();
        for (ShipFleet fl: fleets)
            emps.add(fl.empire());

        if (isColonized() && !emps.contains(colony().empire()))
            emps.add(colony().empire());

        return emps;
    }
    public Transport acceptTransport(Transport tr) {
        for (Transport trans : orbitingTransports) {
            if (tr.empire() == trans.empire()) {
                trans.joinWith(tr);
                return trans;
            }
        }
        session().replaceVarValue(tr, this);
        orbitingTransports.add(tr);
        return tr;
    }
    public String getAttribute(String key) {
        switch (key) {
            case "NAME":             return empire().sv.name(id);
            case "POPULATION":       return str(empire().sv.population(id));
            case "DELTA_POPULATION": return str(empire().sv.deltaPopulation(id));
            case "SIZE":
                int maxSize = (int)this.colony().maxSize();
                int currSize =empire().sv.currentSize(id);
                if (maxSize == currSize)
                    return str(currSize)+" ";
                else
                    return concat(str(currSize),"+");
            case "PLANET_TYPE":      return planet().type().name();
            case "NOTES":            return notes();
            case "DELTA_FACTORIES":  return str(empire().sv.deltaFactories(id));
            case "CAPACITY":         return concat(str((int)(colony().currentProductionCapacity()*100)),"%");
            case "RESERVE":          return str((int)colony().reserveIncome());
            case "SHIPYARD":         return colony().shipyardProject();
            case "DELTA_BASES":      return str(empire().sv.deltaBases(id));
            case "TRANSPORT_TURNS":  return str((int)Math.ceil(transportTimeTo(TARGET_SYSTEM)));
            case "RESOURCES":        return text(empire().sv.view(id).resourceType());
        }
        return "";
    }
    public static Comparator<StarSystem> NAME               = (StarSystem o1,   StarSystem o2)   -> o1.name().compareTo(o2.name());
    public static Comparator<StarSystem> PLANET_TYPE        = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.planet().type().hostility(),sys2.planet().type().hostility());
    public static Comparator<StarSystem> NOTES              = (StarSystem sys1, StarSystem sys2) -> sys1.notes().compareTo(sys2.notes());
    public static Comparator<StarSystem> SHIPYARD           = (StarSystem sys1, StarSystem sys2) -> sys1.colony().shipyardProject().compareTo(sys2.colony().shipyardProject());
    public static Comparator<StarSystem> RESOURCES          = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.planet().resourcesSort(),sys2.planet().resourcesSort());
    public static Comparator<StarSystem> INDUSTRY_RESERVE   = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().reserveIncome(),sys2.colony().reserveIncome());
    public static Comparator<StarSystem> BASE_PRODUCTION    = (StarSystem o1,   StarSystem o2)   -> Base.compare(o1.colony().production(),o2.colony().production());
    public static Comparator<StarSystem> CAPACITY           = (StarSystem sys1, StarSystem sys2) -> Base.compare(sys1.colony().currentProductionCapacity(),sys2.colony().currentProductionCapacity());
    public static Comparator<StarSystem> VFLAG = (StarSystem sys1, StarSystem sys2) -> {
        Empire pl = Empire.thePlayer();
        return Base.compare(pl.sv.flagColorId(sys1.id),pl.sv.flagColorId(sys2.id));
    };
    public static Empire VIEWING_EMPIRE;
    public static Comparator<StarSystem> VDISTANCE = (StarSystem sys1, StarSystem sys2) -> {
        return Base.compare(VIEWING_EMPIRE.sv.distance(sys1.id),VIEWING_EMPIRE.sv.distance(sys2.id));
    };
    public static Comparator<StarSystem> VPOPULATION = (StarSystem sys1, StarSystem sys2) -> {
        return Base.compare(VIEWING_EMPIRE.sv.population(sys1.id),VIEWING_EMPIRE.sv.population(sys2.id));
    };
    public static Comparator<StarSystem> POPULATION = (StarSystem sys1, StarSystem sys2) -> {
        return Base.compare(sys1.population(),sys2.population());
    };
    public static Comparator<StarSystem> CURRENT_SIZE = (StarSystem sys1, StarSystem sys2) -> {
        Empire emp = sys1.empire();
        return Base.compare(emp.sv.currentSize(sys1.id),emp.sv.currentSize(sys2.id));
    };
    public static StarSystem TARGET_SYSTEM;
    public static Comparator<StarSystem> DISTANCE_TO_TARGET_SYSTEM = new Comparator<StarSystem>() {
        @Override
        public int compare(StarSystem sys1, StarSystem sys2) {
            float pr1 = sys1.distanceTo(TARGET_SYSTEM);
            float pr2 = sys2.distanceTo(TARGET_SYSTEM);
            return Base.compare(pr1, pr2);
        }
    };
    public static Comparator<StarSystem> TRANSPORT_TIME_TO_TARGET_SYSTEM = new Comparator<StarSystem>() {
        @Override
        public int compare(StarSystem sys1, StarSystem sys2) {
            float pr1 = sys1.transportTimeTo(TARGET_SYSTEM);
            float pr2 = sys2.transportTimeTo(TARGET_SYSTEM);
            return Base.compare(pr1, pr2);
        }
    };
    public static Empire TARGET_EMPIRE;
    public static Comparator<StarSystem> DISTANCE_TO_TARGET_EMPIRE = new Comparator<StarSystem>() {
        @Override
        public int compare(StarSystem sys1, StarSystem sys2) {
            float pr1 = TARGET_EMPIRE.sv.distance(sys1.id);
            float pr2 = TARGET_EMPIRE.sv.distance(sys2.id);
            return Base.compare(pr1, pr2);
        }
    };
    //
    // SUPPORTING BEHAVIOR FOR SPRITES
    //
    private int twinkleCycle() {
        if (twinkleCycle == 0)
            twinkleCycle = roll(20,50);
        return twinkleCycle;
    }
    private int twinkleOffset() {
        if (twinkleOffset == 0)
            twinkleOffset = roll(0,500);
        return twinkleOffset;
    }
    private int drawRadius() {
        if (drawRadius == 0)
            drawRadius = scaled(roll(4,6));
        return drawRadius;
    }
    @Override
    public IMappedObject source() { return this; }
    @Override
    public boolean persistOnClick()      { return true; }
    @Override
    public boolean hovering()                   { return hovering; }
    @Override
    public void hovering(boolean b)             { hovering = b; }
    @Override
    public void repaint(GalaxyMapPanel map)     {
        int r = map.scale(1.0f);
        int x1 = map.mapX(x());
        int y1 = map.mapY(y());
        map.repaint(x1-r,y1-r,r+r,r+r);
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        displayed = false;
        if (!map.displays(this))
            return;

        Empire pl = player();

        displayed = true;
        int x0 = mapX(map);
        int y0 = mapY(map);
        int r0 = drawRadius(map);
        twinkleOffset++;

        if (!session().performingTurn()) {
            SystemView sv = pl.sv.view(id);
            Color c0 = map.parent().alertColor(sv);
            if (c0 != null)
                drawAlert(map, g2, c0, x0, y0);
        }
        drawStar(map, g2, x0, y0);
        
        Empire emp = map.parent().knownEmpire(id, pl);
        if (map.parent().isClicked(this)
        || map.parent().isClicked(transportSprite()))
            drawSelection(g2, map, emp, x0, y0);
        else if (map.parent().isHovering(this))
            drawHovering(g2, map, x0, y0);

        // draw shield?
        if ((emp != null) && map.parent().drawShield())
            drawShield(g2, pl.sv.shieldLevel(id), x0, y0, map.scale(0.25f));

        // draw stargate icon?
        boolean colonized = (emp != null) && pl.sv.isColonized(id);
        if (colonized && map.parent().drawStargate() && pl.sv.hasStargate(id)) {
            float mult = max(4, min(60,map.scaleX()));
            int x1 = x0+(int)(scaled(200)/mult);
            int y1 = y0-(int)(scaled(500)/mult);
            Image img = ShipLibrary.current().stargate.getImage();
            int w = img.getWidth(null);
            int h = img.getHeight(null);
            g2.drawImage(img, x1, y1, x1+BasePanel.s14, y1+BasePanel.s14, 0, 0, w, h, map);
        }
        
        if (map.parent().drawFlag()) {
            Image flag = pl.sv.mapFlagImage(id);
            if (flag != null) {
                int sz = BasePanel.s30;
                g2.drawImage(flag, x0-BasePanel.s15, y0-BasePanel.s30, sz, sz,null);
            }
        }
        
        // draw star name
        String label1 = map.parent().systemLabel(this);
        String label2 = map.parent().systemLabel2(this);
        if (label2.isEmpty())
            label2 = name2(map);
        
        Font prevFont = g2.getFont();
        int fontSize = fontSize(map);
        int yAdj = scaled(fontSize)+r0;
        if (!label1.isEmpty()) {
            g2.setFont(narrowFont(fontSize));
            int sw = g2.getFontMetrics().stringWidth(label1);
            if (!colonized) {
                g2.setColor(map.parent().systemLabelColor(this));
                drawString(g2,label1, x0-(sw/2), y0+yAdj);
                y0 += scaled(fontSize-2);
            }
            else {
                int s1 = BasePanel.s1;
                int s2 = BasePanel.s2;
                int boxW = sw + BasePanel.s8;
                int fontH = scaled(fontSize);
                int cnr = fontH/2;
                int x0a = x0-(boxW/2);
                g2.setColor(systemNameBackC);
                Stroke prevStroke = g2.getStroke();
                g2.setStroke(BasePanel.stroke1);
                g2.fillRoundRect(x0a, y0+yAdj-(fontH*3/4)-s1, boxW, fontH+s2, cnr,cnr);
                g2.setColor(map.parent().systemLabelColor(this));
                g2.drawRoundRect(x0a, y0+yAdj-(fontH*3/4)-s1, boxW, fontH+s2, cnr,cnr);
                g2.setStroke(prevStroke);
                drawString(g2,label1, x0-(sw/2), y0+yAdj+s1);
                y0 += fontH+s2;
            }
        }
        if (!label2.isEmpty()) {
            g2.setColor(map.parent().systemLabelColor(this));
            g2.setFont(narrowFont(fontSize-2));
            int sw2 = g2.getFontMetrics().stringWidth(label2);
            drawString(g2,label2, x0-(sw2/2), y0+yAdj);
        }
        g2.setFont(prevFont);
    }
    private String name2(GalaxyMapPanel map) {
        IMappedObject obj = map.parent().distanceOrigin();
        if (obj == null)
            return "";
        float dist = (float)Math.ceil(distanceTo(obj)*10)/10;
        if (dist == 0)
            return "";
        String dist1 = df1.format(dist);
        return text("SYSTEMS_RANGE",dist1);
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) {
        if (!displayed)
            return false;
        int spriteX = map.mapX(x());
        int spriteY = map.mapY(y());
        float clickR = map.scale(clickRadius);
        float dist = distance(spriteX, spriteY, mapX, mapY);
        return dist <= max(BasePanel.s2, clickR);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean sound) { }
    @Override
    public void mouseEnter(GalaxyMapPanel map) {
        hovering = true;
    }
    @Override
    public void mouseExit(GalaxyMapPanel map) {
        hovering = false;
    }
    private float flareSize(GalaxyMapPanel map)  {
        if (!playAnimations())
            return 1.0f;
        //return 0;
        int rem = twinkleOffset() % twinkleCycle(map);
        switch (rem) {
            case 0: return 1.0f;
            case 1: return 2.0f;
            case 2: return 1.5f;
            case 3: return 1.0f;
            default: return 1.0f;
        }
    }
    private int twinkleCycle(GalaxyMapPanel map) {
        // adjust by map scale to avoid excessive
        // twinkling when zoomed way out
        return (int) max(100,(twinkleCycle()*map.scaleY()/20));
    }
    private int drawRadius(GalaxyMapPanel map) {
        return (int) max(BasePanel.s1, (drawRadius() * 60 / map.scaleX()));
    }
    private void drawAlert(GalaxyMapPanel map, Graphics2D g2, Color alertC, int x, int y) {
        int r = map.scale(0.75f);
        int r1 = map.scale(0.25f);
        
        if (!map.parent().animating() ||(map.animationCount() % 20 > 0)) {
            g2.setColor(alertC);
            Area a = new Area(new RoundRectangle2D.Float(x-r, y-(r/6), r+r, r/3, r1/3, r1/3));
            a.add(new Area(new RoundRectangle2D.Float(x-(r/6), y-r, r/3, r+r, r1/3, r1/3)));
            a.subtract(new Area(new Ellipse2D.Float(x-r1,y-r1,r1+r1,r1+r1)));
            g2.fill(a);
        }
    }
    private void drawStar(GalaxyMapPanel map, Graphics2D g2, int x, int y) {
        int r0 = drawRadius(map);
        if (r0 < BasePanel.s4)
            r0 = (int) (r0 * flareSize(map));

        Composite prev = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver);
        BufferedImage img = starType().image(r0,0);
        int w = img.getWidth();
        g2.drawImage(img,x-(w/2),y-(w/2),null);
        g2.setComposite(prev);
    }
    private void drawSelection(Graphics2D g, GalaxyMapPanel map, Empire emp, int x, int y) {
        Stroke prev = g.getStroke();
        int mod = map.animationCount()%15/5;
        switch(mod) {
            case 0: g.setStroke(BasePanel.stroke2); break;
            case 1: g.setStroke(BasePanel.stroke3); break;
            case 2:
            default: g.setStroke(BasePanel.stroke4); break;
        }

        if (emp == null)
            g.setColor(selectionC);
        else
            g.setColor(emp.color());
        
        int r0 = map.scale(clickRadius);
        g.drawOval(x-r0, y-r0, r0+r0, r0+r0);
        g.setStroke(prev);
    }
    private void drawHovering(Graphics2D g, GalaxyMapPanel map, int x, int y) {
        int r = map.scale(clickRadius);

        Stroke prev = g.getStroke();
        g.setStroke(BasePanel.stroke1);
        g.setColor(selectionC);
        g.drawOval(x-r, y-r, r+r, r+r);
        g.setStroke(prev);
    }
    private static void drawShield(Graphics2D g, int shieldLevel, int x, int y, int r) {
        if (shieldLevel == 0)
            return;
        
        Stroke prevStroke = g.getStroke();
        Stroke shieldStroke = BasePanel.stroke4;
        Stroke shieldBorderStroke = BasePanel.stroke6;
        
        if (r < 16) {
            shieldStroke = BasePanel.stroke2;
            shieldBorderStroke = BasePanel.stroke3;
        }
        else if (r < 24) {
            shieldStroke = BasePanel.stroke3;
            shieldBorderStroke = BasePanel.stroke5;
        }
        g.setStroke(shieldStroke);
        switch (shieldLevel) {
            case 5:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 30, 120);
                g.setColor(shield5C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 30, 120);
                break;
            case 10:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r,0, 180);
                g.setColor(shield10C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r,0, 180);
                break;
            case 15:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 330, 240);
                g.setColor(shield15C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 330, 240);
                break;
            case 20:
                g.setColor(Color.black);
                g.setStroke(shieldBorderStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 0, 360);
                g.setColor(shield20C);
                g.setStroke(shieldStroke);
                g.drawArc(x-r, y-r, r+r, r+r, 0, 360);
                break;
        }
        g.setStroke(prevStroke);
    }
    private int fontSize(GalaxyMapPanel map) {
        int maxFont = 72;
        int minFont = 4;
        return bounds(minFont, (int)(maxFont * 10 / map.scaleX()), maxFont);
    }
}

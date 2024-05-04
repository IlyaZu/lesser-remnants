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
package rotp.ui.main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Nebula;
import rotp.model.galaxy.Ship;
import static rotp.model.galaxy.Ship.EMPIRE_ID;
import rotp.model.galaxy.StarSystem;
import rotp.model.tech.TechCategory;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.map.IMapHandler;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.DistanceDisplaySprite;
import rotp.ui.sprites.SpyReportSprite;
import rotp.ui.sprites.TechStatusSprite;
import rotp.ui.sprites.TreasurySprite;
import rotp.ui.sprites.ZoomInWidgetSprite;
import rotp.ui.sprites.ZoomOutWidgetSprite;

public class GalaxyMapPanel extends BasePanel implements ActionListener, MouseListener, MouseWheelListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    
    private static final Color unreachableBackground = new Color(0,0,0);

    public static Color gridLight = new Color(160,160,160);
    public static Color gridDark = new Color(64,64,64);

    private final IMapHandler parent;

    // static fields shared across all galaxy map panels to keep them
    // visually in synch
    private static Location center = new Location();
    private static float scaleX, scaleY;
    private static float sizeX, sizeY;
    private static final List<Sprite> baseControls = new ArrayList<>();
    private static boolean showDistance = false;

    private float desiredScale;
    private Image mapBuffer;
    private Image rangeMapBuffer;
    public static BufferedImage sharedStarBackground;
    private final float zoomBase = 1.1f;
    boolean dragSelecting = false;
    private int selectX0, selectY0, selectX1, selectY1;
    private int lastMouseX, lastMouseY;
    private long lastMouseTime;
    private boolean redrawRangeMap = true;
    public Sprite hoverSprite;
    int backOffsetX = 0;
    int backOffsetY = 0;
    float areaOffsetX = 0;
    float areaOffsetY = 0;
    Area shipRangeArea;
    Area scoutRangeArea;
    private int maxMouseVelocity = -1;
    private boolean searchingSprite = false;

    private final Timer zoomTimer;

    public IMapHandler parent()                 { return parent; }

    public void toggleDistanceDisplay()         { showDistance = !showDistance; }
    public boolean showDistance()               { return showDistance; }

    public void clearHoverSprite()              { hoverSprite = null; }
    public Location currentFocus()        { return parent.mapFocus();  }
    public void currentFocus(IMappedObject o)  { parent.mapFocus(o); }
    private Location center()              { return center; }
    public void center(Location o)        { center = o; }
    public float mapMinX()                { return center.x()-(scaleX*2/5); }
    public float mapMinY()                { return center.y()-(scaleY*2/5); }
    public float mapMaxX()                { return center.x()+(scaleX*3/5); }
    public float mapMaxY()                { return center.y()+(scaleY*3/5); }
    public void centerX(float x)       { center.x(x); }
    public void centerY(float y)       { center.y(y); }
    public float centerX()                { return center.x(); }
    public float centerY()                { return center.y(); }
    public float sizeX()            { return sizeX; }
    public float sizeY()            { return sizeY; }
    public void sizeX(float s)      { sizeX = s; }
    public void sizeY(float s)      { sizeY = s; }

    public float scaleX()            { return scaleX; }
    public float scaleY()            { return scaleY; }
    public void scaleX(float s)      { scaleX = s; }
    public void scaleY(float s)      { scaleY = s; }

    public boolean inOverview() { return 80f <= scaleX; }

    public boolean displays(IMappedObject obj) {
        if (center() == null)
            return true;

        float x = obj.x();
        float y = obj.y();
        return (x >= (mapMinX()*0.9f)) && (x <= (mapMaxX()*1.1f)) && (y >= (mapMinY()*0.9f)) && (y <= (mapMaxY()*1.1f));
    }
    public GalaxyMapPanel(IMapHandler p) {
        parent = p;
        zoomTimer = new Timer(10, this);

        setBackground(Color.BLACK);
        setOpaque(true);

        if (baseControls.isEmpty()) {
            baseControls.add(new DistanceDisplaySprite(10,120,30,30));
            baseControls.add(new ZoomOutWidgetSprite(10,85,30,30));
            baseControls.add(new ZoomInWidgetSprite(10,50,30,30));

            int y0 = unscaled(getHeight())-310;
            baseControls.add(new TechStatusSprite(TechCategory.WEAPON,       10,y0+210,30,30));
            baseControls.add(new TechStatusSprite(TechCategory.PROPULSION,   10,y0+175,30,30));
            baseControls.add(new TechStatusSprite(TechCategory.PLANETOLOGY,  10,y0+140,30,30));
            baseControls.add(new TechStatusSprite(TechCategory.FORCE_FIELD,  10,y0+105, 30,30));
            baseControls.add(new TechStatusSprite(TechCategory.CONSTRUCTION, 10,y0+70, 30,30));
            baseControls.add(new TechStatusSprite(TechCategory.COMPUTER,     10,y0+35, 30,30));
            baseControls.add(new TreasurySprite(10,y0, 30,30));

            baseControls.add(new SpyReportSprite(10,y0-70, 30,30));
        }
        
        addMouseListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
    }
    public void repaintTechStatus() {
        int y = getHeight()-scaled(275);
        this.repaint(s10,y,s30,scaled(205));
    }
    // scale(float) will translate any arbitrary "real" distancce
    // into a map distance in pixels
    public int scale(float d) {
        return (int) (d*this.getSize().width/scaleX());
    }
    // objX(int) and objY(int) will translate any arbitrary on-screen
    // map coordinates into "real" coordinates
    public float objX(int x) {
        int w = getSize().width;
        return mapMinX()+(scaleX()*x/w);
    }
    public float objY(int y) {
        int h = getSize().height;
        return mapMinY()+(scaleY()*y/h);
    }
    // mapX(float) and mapY(float) will translate any arbitrary "real"
    // coordinates into map coordinates
    public int mapX(float x) {
        float rX = (x-center.x())/scaleX()+.4f;
        return (int) (rX*getSize().width);
    }
    public int mapY(float y) {
        float rY = (y-center.y())/scaleY()+.4f;
        return (int) (rY*getSize().height);
    }
    private float fMapX(float x) {
        float rX = (x-center.x())/scaleX()+.4f;
        return rX*getSize().width;
    }
    private float fMapY(float y) {
        float rY = (y-center.y())/scaleY()+.4f;
        return rY*getSize().height;
    }
    public void setBounds(float x1, float x2, float y1, float y2) {
        float scaleFromY = y2-y1;
        float scaleFromX = (x2-x1)*getSize().height/getSize().width;

        setScale(max(scaleFromY, scaleFromX));
        center().x((x1+x2)/2);
        center().y((y1+y2)/2);
    }
    public void setScale(float scale) {
        int mapSizeX = getSize().width;
        int mapSizeY = getSize().height;
        if (scaleY != scale) {
            clearRangeMap();
            resetRangeAreas();
        }
        scaleY(scale);
        scaleX(scale*mapSizeX/mapSizeY);
    }
    public float maxScale() {
        float largestAxis = Math.max(sizeX(), sizeY());
        return galaxy().maxScaleAdj()*largestAxis;
    }
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        setFontHints(g2);
        parent.checkMapInitialized();
        paintToImage(mapBuffer());
        g.drawImage(mapBuffer,0,0,null);
        parent.paintOverMap(this, g2);
    }
    public void paintToImage(Image img) {
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        super.paintComponent(g2); //paint background

        setScale(scaleY());
        drawBackground(g2);
        if (parent.drawBackgroundStars()) {
            float alpha = 8/5*sizeX()/scaleX();
            Composite prev = g2.getComposite();
            if (alpha < 1) {
                Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , alpha);
                g2.setComposite(comp );
            }
            drawBackgroundStars(g2);
            g2.setComposite(prev);
        }

        drawNebulas(g2);
        drawStarSystems(g2);
        drawShips(g2);
        drawWorkingFlightPaths(g2);
        parent.drawYear(g2);
        parent.drawTitle(g2);
        parent.drawAlerts(g2);
        drawControlSprites(g2);
        drawNextTurnSprites(g2);
        drawRangeSelect(g2);
        g2.dispose();
    }
    private Image mapBuffer() {
        if ((mapBuffer == null) || (mapBuffer.getWidth(this) != getWidth()) || (mapBuffer.getHeight(this) != getHeight()))
            generateNewMapBuffers();
        
        return mapBuffer;
    }
    private void generateNewMapBuffers() {
        int w = getWidth();
        int h = getHeight();
        mapBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        rangeMapBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        if (sharedStarBackground == null) {
            sharedStarBackground = new BufferedImage(RotPUI.instance().getWidth(), RotPUI.instance().getHeight(), BufferedImage.TYPE_INT_ARGB);
            drawBackgroundStars(sharedStarBackground, this);
        }
    }
    public void init() {
        resetRangeAreas();
        
        if (UserPreferences.sensitivityMedium())
            maxMouseVelocity = 500;
        else if (UserPreferences.sensitivityLow())
            maxMouseVelocity = 100;
        else
            maxMouseVelocity = -1;
    }
    public void clearRangeMap()    { redrawRangeMap = true; }
    public void resetRangeAreas() {
        clearRangeMap();
        shipRangeArea = null;
        scoutRangeArea = null;
        areaOffsetX = 0;
        areaOffsetY = 0;
    }
    private void setFocusToCenter() {
        currentFocus(new Location());
        currentFocus().setXY(sizeX()/2, sizeY()/2);
        center(currentFocus());
    }
    public void recenterMapOn(IMappedObject obj) {
        recenterMap(obj.x(), obj.y());
    }
    private void recenterMap(float x, float y) {
        float bestX = bounds(0, x, sizeX());
        float bestY = bounds(0, y, sizeY());

        areaOffsetX += (currentFocus().x()-bestX);
        areaOffsetY += (currentFocus().y()-bestY);
        
        currentFocus().setXY(bestX, bestY);
        center(parent.mapFocus());
        
        clearRangeMap();
    }
    public void adjustZoom(int z) {
        int MIN_SCALE = 4;
        if (parent.canChangeMapScales()) {
            float multiplier = pow(zoomBase,z);
            float newScale = scaleY()*multiplier;
            newScale = max(MIN_SCALE, min(2*maxScale(), newScale));
            desiredScale = newScale;
            zoomTimer.start();
        }
    }
    public void initializeMapData() {
        sizeX(galaxy().width());
        sizeY(galaxy().height());
        clearRangeMap();

        setFocusToCenter();

        // try to set starting scale to what parent UI wants
        float largestAxis = max(sizeX(), sizeY());
        float parentStartingScale = parent.startingScalePct()*largestAxis;
        // however, if parent allows us to adjust the scale, then the
        // starting scale must be capped at the maximum adjustable scale
        if (parent.canChangeMapScales())
            parentStartingScale = min(parentStartingScale, maxScale());
        
        setScale(parentStartingScale);
    }
    private void drawBackground(Graphics2D g) {
        if (redrawRangeMap) {
            redrawRangeMap = false;
            Graphics2D g0 =  (Graphics2D) rangeMapBuffer.getGraphics();
            setFontHints(g0);
            g0.setColor(unreachableBackground);
            g0.fillRect(0,0,getWidth(),getHeight());
            if (parent.showShipRanges())
                drawExtendedRangeDisplay(g0);
            drawOwnershipDisplay(g0);
        }
        g.drawImage(rangeMapBuffer,0,0,null);
    }
    private void drawOwnershipDisplay(Graphics2D g) {
        int r0 = scale(0.9f);
        int r1 = scale(0.8f);

        Galaxy gal = galaxy();
        Empire pl = player();
        for (int id=0; id < pl.sv.count(); id++) {
            Empire emp = parent.knownEmpire(id, pl);
            StarSystem sys = gal.system(id);
            if ((emp != null) && parent.showOwnership(sys)) {
                int shape = emp.shape();
                g.setColor(emp.ownershipColor());
                int x = mapX(sys.x());
                int y = mapY(sys.y());
                switch(shape) {
                    case Empire.SHAPE_SQUARE:
                        g.fillRect(x-r1, y-r1, r1+r1, r1+r1); break;
                    case Empire.SHAPE_DIAMOND:
                        Polygon p = new Polygon();
                        p.addPoint(x, y-r0);
                        p.addPoint(x-r0, y);
                        p.addPoint(x, y+r0);
                        p.addPoint(x+r0, y);
                        g.fill(p); break;
                    case Empire.SHAPE_TRIANGLE1:
                        Polygon p1 = new Polygon();
                        p1.addPoint(x, y-r0);
                        p1.addPoint(x-r0, y+r1);
                        p1.addPoint(x+r0, y+r1);
                        g.fill(p1); break;
                    case Empire.SHAPE_TRIANGLE2:
                        Polygon p2 = new Polygon();
                        p2.addPoint(x, y+r0);
                        p2.addPoint(x-r0, y-r1);
                        p2.addPoint(x+r0, y-r1);
                        g.fill(p2);
                        break;
                    case Empire.SHAPE_CIRCLE:
                    default:
                        g.fillOval(x-r0, y-r0, r0+r0, r0+r0); break;
                }
            }
        }
    }
    private void drawExtendedRangeDisplay(Graphics2D g) {
        Empire emp = parent.empireBoundaries();
        float shipRange = emp.shipRange();
        float scoutRange = emp.scoutRange();
        if (shipRange > galaxy().width())
            return;

        Empire pl = player();
        Color normalBorder = emp.shipBorderColor();
        Color extendedBorder = emp.scoutBorderColor();
        // draw extended range
        List<StarSystem> systems = player().systemsForCiv(emp);
        List<StarSystem> alliedSystems = new ArrayList<>();
        // only show range for allied systems when player is selected
        if (emp.isPlayer()) {
            for (Empire ally: emp.allies())
                for (StarSystem sys: ally.allColonizedSystems()) {
                    if (pl.sv.empId(sys.id) == ally.id)
                        alliedSystems.add(sys);
                }
        }

        float scale = getWidth()/scaleX();

        AffineTransform prevXForm = g.getTransform();
        if ((areaOffsetX != 0) || (areaOffsetY != 0)) {
            float ctrX = parent.mapFocus().x();
            float ctrY = parent.mapFocus().y();
            float mapOffsetX = fMapX(ctrX)- fMapX(ctrX-areaOffsetX);
            float mapOffsetY = fMapY(ctrY)-fMapY(ctrY-areaOffsetY);
            AffineTransform xForm = g.getTransform();
            xForm.setToIdentity();
            xForm.translate(mapOffsetX, mapOffsetY);
            g.setTransform(xForm);
        }
        float extR = scoutRange*scale;
        float baseR = shipRange*scale;
        Area tmpRangeArea = scoutRangeArea;
        if (tmpRangeArea == null) {
            tmpRangeArea = new Area();
            for (StarSystem sv: alliedSystems)
                tmpRangeArea.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-extR, fMapY(sv.y())-extR, 2*extR, 2*extR) ));
            for (StarSystem sv: systems)
                tmpRangeArea.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-extR, fMapY(sv.y())-extR, 2*extR, 2*extR) ));
            scoutRangeArea = tmpRangeArea;
        }
        g.setColor(extendedBorder);
        g.setStroke(stroke2);
        g.draw(tmpRangeArea);


        tmpRangeArea = shipRangeArea;
        if (tmpRangeArea == null) {
            tmpRangeArea = new Area();
            for (StarSystem sv: alliedSystems)
                tmpRangeArea.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-baseR, fMapY(sv.y())-baseR, 2*baseR, 2*baseR) ));
            for (StarSystem sv: systems)
                tmpRangeArea.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-baseR, fMapY(sv.y())-baseR, 2*baseR, 2*baseR) ));
            shipRangeArea = tmpRangeArea;
        }
        g.setColor(normalBorder);
        g.setStroke(stroke2);
        g.draw(tmpRangeArea);
        
        g.setTransform(prevXForm);
    }
    private void drawBackgroundStars(Graphics2D g) {
        int w = sharedStarBackground.getWidth();
        int h = sharedStarBackground.getHeight();
        // java modulo does not handle negative numbers properly
        int x0 = backOffsetX;
        while (x0<0)   x0+=w;
        int y0 = backOffsetY;
        while (y0<0)  y0 +=h;
        
        int x = x0 % w;
        int y = y0 % h;
        
        if ((x > 0) && (y > 0)) {
            BufferedImage topL = sharedStarBackground.getSubimage(w-x,h-y,x, y);
            g.drawImage(topL,0,0, null);
        }
        if (y > 0) {
            BufferedImage topR = sharedStarBackground.getSubimage(0,h-y,w-x, y);
            g.drawImage(topR,x,0, null);
        }
        if (x > 0) {
            BufferedImage botL = sharedStarBackground.getSubimage(w-x,0,x, h-y);
            g.drawImage(botL,0,y, null);
        }
        
        BufferedImage botRight = sharedStarBackground.getSubimage(0,0,w-x, h-y);
        g.drawImage(botRight,x,y,null);
    }
    public void drawNebulas(Graphics2D g) {
        for (Nebula neb: galaxy().nebulas()) {
            if (parent.shouldDrawSprite(neb))
                neb.draw(this, g);
        }
    }
    public void drawStarSystems(Graphics2D g) {
        Galaxy gal = galaxy();
        for (int id=0; id< gal.numStarSystems();id++) {
            StarSystem sys = gal.system(id);
            if (parent.shouldDrawSprite(sys))
                sys.draw(this, g);
            if (parent.shouldDrawSprite(sys.transportSprite()))
                sys.transportSprite().draw(this, g);
            if (parent.shouldDrawSprite(sys.rallySprite()))
                sys.rallySprite().draw(this, g);
        }
    }
    public void drawShips(Graphics2D g) {
        if (!parent.drawShips())
            return;
        Empire pl = player();
        // comodification exception here without this copy
        List<Ship> visibleShips = new ArrayList<>(pl.visibleShips());
        for (Ship sh: visibleShips) {
            // Draw paths even if the ship is not because the path could still cross the viewing window
            boolean isShipMoving = sh.deployed() || sh.retreating() || sh.inTransit() || sh.isRallied();
            if (pl.knowETA(sh) && isShipMoving && parent.shouldDrawSprite(sh.pathSprite())) {
                sh.pathSprite().draw(this, g);
            }
            
            sh.setDisplayed(this);
            if (sh.displayed()) {
                Sprite spr = (Sprite) sh;
                spr.draw(this, g);
            }
        }
    }
    public void drawWorkingFlightPaths(Graphics2D g) {
        for (FlightPathSprite spr: FlightPathSprite.workingPaths()) {
            if (parent.shouldDrawSprite(spr))
                spr.draw(this, g);
        }
    }
    public void drawControlSprites(Graphics2D g) {
        for (Sprite sprite: baseControls) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
        for (Sprite sprite: parent.controlSprites()) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
    }
    public void drawNextTurnSprites(Graphics2D g) {
        for (Sprite sprite: parent.nextTurnSprites()) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
    }
    public void drawRangeSelect(Graphics2D g) {
        if ((selectX0 == selectX1) || (selectY0 == selectY1))
            return;
        
        int x = min(selectX0, selectX1);
        int y = min(selectY0, selectY1);
        int w = Math.abs(selectX0-selectX1);
        int h = Math.abs(selectY0-selectY1);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(Color.white);
        g.drawRect(x,y,w,h);
        
        g.setStroke(prev);
    }
    private Sprite spriteAt(int x1, int y1) {
        // In order of hovering priority:
        // 1. Next Turn Sprites
        // 2. Map Control Sprites
        // 3. Ships & Flight Paths
        // 4. Systems
        for (Sprite sprite: parent.nextTurnSprites()) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        for (Sprite sprite: parent.controlSprites()) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        for (Sprite sprite: baseControls) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        // is there any overlay covering the map and preventing a search
        if (parent.masksMouseOver(x1,y1))
            return null;

        Galaxy gal = galaxy();
        Empire pl = player();
        List<Ship> ships = null;
        if (!inOverview()) {
            if (parent.hoverOverFleets()) {
                ships = new ArrayList<>(pl.visibleShips());
                ships.sort(EMPIRE_ID);
                float minDistance = Float.MAX_VALUE;
                Sprite closestShip = null;
                for (Ship sh: ships) {
                    if (sh.displayed()) {
                        Sprite spr = (Sprite) sh;
                        if (parent.shouldDrawSprite(spr)
                        && spr.isSelectableAt(this, x1, y1)) {
                            float dist = spr.selectDistance(this, x1, y1);
                            if (dist == 0)
                               return spr;
                            if (dist < minDistance) {
                                minDistance = dist;
                                closestShip = spr;
                            }
                        }
                    }
                }
                if (closestShip != null)
                    return closestShip;
            }
        }
        if (parent.hoverOverSystems()) {
            for (int id=0;id<gal.numStarSystems();id++) {
                if (gal.system(id).isSelectableAt(this, x1, y1))
                    return gal.system(id);
            }
        }
        // working flight paths are always displayed and can always be selected
        List<FlightPathSprite> workingPaths = FlightPathSprite.workingPaths();
        for (FlightPathSprite path: workingPaths) {
            if (path.isSelectableAt(this,x1,y1))
                return path;
        }
        if (!inOverview()) {
            if (parent.hoverOverFlightPaths()) {
                if (ships == null)
                    ships = new ArrayList<>(pl.visibleShips());
                for (Ship sh: ships) {
                    if (sh.displayed()) {
                        FlightPathSprite fpSpr = sh.pathSprite();
                        if (parent.shouldDrawSprite(fpSpr)
                        && fpSpr.isSelectableAt(this, x1, y1))
                            return fpSpr;
                    }
                }
            }
        }
        return null;
    }
    public void dragMap(int deltaX, int deltaY) {
        backOffsetX += deltaX/10;
        backOffsetY += deltaY/10;

        float objX = parent.mapFocus().x();
        float objY = parent.mapFocus().y();
        float newObjX = objX;
        float newObjY = objY;
        
        // we need to recalculate the new focusX/Y before
        // recentering so that we can have the proper pixel
        // offset for the range areas
        // only do this for changed X/Y values to avoid
        // introducing rounding errors
        if (deltaX != 0) {
            int focusX = mapX(parent.mapFocus().x());
            newObjX = bounds(0, objX(focusX-deltaX), sizeX());
        }
        if (deltaY != 0) {
            int focusY = mapY(parent.mapFocus().y());
            newObjY = bounds(0, objY(focusY-deltaY), sizeY());
        }
        
        areaOffsetX += (objX-newObjX);
        areaOffsetY += (objY-newObjY);
        
        parent.mapFocus().setXY(newObjX, newObjY);
        center(parent.mapFocus());
        clearRangeMap();
        repaint();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        float currentScale = scaleY();
        float zoomAdj = desiredScale/currentScale;
        if (zoomAdj == 1.0) {
            zoomTimer.stop();
            return;
        }

        float zoomAmt = zoomBase;
        if (zoomAdj > zoomAmt)
            zoomAdj = zoomAmt;
        else if (zoomAdj < (1.0f/zoomAmt))
            zoomAdj = 1.0f/zoomAmt;

        setScale(currentScale*zoomAdj);
        repaint();
    }
    @Override
    public void animate() {
        if (session().performingTurn() && parent.suspendAnimationsDuringNextTurn())
            return;
        if (zoomTimer.isRunning())
            return;

        if (playAnimations() && (animationCount() % 5 == 0))
            repaint();
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        // if we are hovering over a sprite and it accepts mousewheel
        // then do that
        if ((hoverSprite != null) && hoverSprite.acceptWheel()) {
            hoverSprite.wheel(this, e.getWheelRotation(), false);
            return;
        }
        
        if (parent.forwardMouseEvents())
            parent.mouseWheelMoved(e);
        else
            adjustZoom(e.getWheelRotation());
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        boolean rightClick = SwingUtilities.isRightMouseButton(e) && parent.allowsDragSelect();
        int deltaX = x - lastMouseX;
        int deltaY = y - lastMouseY;

        if (rightClick) {
            int prevSelectX0 = selectX0;
            int prevSelectY0 = selectY0;
            int prevSelectX1 = selectX1;
            int prevSelectY1 = selectY1;
            int boundsX0 = min(prevSelectX0, prevSelectX1, x);
            int boundsY0 = min(prevSelectY0, prevSelectY1, y);
            int boundsX1 = max(prevSelectX0, prevSelectX1, x);
            int boundsY1 = max(prevSelectY0, prevSelectY1, y);
            if (x > selectX0) {
                selectX1 = x;
            }
            else if (x < selectX0) {
                selectX0 = x; selectX1 = prevSelectX1;
            }
            if (y > selectY0) {
                selectY1 = y;
            }
            else if (y < selectY0) {
                selectY0 = y; selectY1 = prevSelectY1;
            }
            paintImmediately(boundsX0, boundsY0, s5+boundsX1-boundsX0, s5+boundsY1-boundsY0);
        }
        else if (parent.canChangeMapScales()) {
            // find the xy map coords of current focus, adjust by the
            // dragged deltas, then recenter the map on the new focus
            dragMap(deltaX, deltaY);
            lastMouseX = x;
            lastMouseY = y;
            lastMouseTime = System.currentTimeMillis();
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        long prevTime = lastMouseTime;
        int prevX = lastMouseX;
        int prevY = lastMouseY;
        lastMouseX = e.getX();
        lastMouseY = e.getY();
        lastMouseTime = System.currentTimeMillis();
        int x = e.getX();
        int y = e.getY();
        
        
        if (maxMouseVelocity > 0) {
            long timeS = (lastMouseTime - prevTime);
            if (timeS == 0)
                return;
            // quick and dirty mouse speed test
            int dist = Math.abs(lastMouseX-prevX)+Math.abs(lastMouseY-prevY);
            long speed = dist*1000/timeS;
            if (speed >= maxMouseVelocity)
                return;
        }
        

        Sprite prevHover = hoverSprite;
        
        // skip the check if we are currently in the midst of a check
        if (searchingSprite)
            return;
        searchingSprite = true;
        try { hoverSprite = spriteAt(x,y); }
        finally { searchingSprite = false; }
        
        // still hovering over same sprite... do nothing
        if (hoverSprite == prevHover)
            return;
        
        // if sprite changed, but we are also still over the prevHover
        // if the prevHover is higher display priority than the new,
        // then do nothing. What is the point of this? If a fleet is in
        // the same area as a system (lower priority), then we want to
        // display the fleet until we mouse away from it because it is
        // drawn over the system.
        if ((prevHover != null) && prevHover.isSelectableAt(this,x,y)) {
            int prevPriority = prevHover.displayPriority();
            int hoverPriority = (hoverSprite == null) ? 0 : hoverSprite.displayPriority();
            if (hoverPriority < prevPriority) {
                hoverSprite = prevHover;
                return;
            }
        }
            
        // sprite has changed, so pass it to the parent UI for proper handling
        parent.hoveringOverSprite(hoverSprite);
    }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent arg0) { }
    @Override
    public void mouseExited(MouseEvent e) {
        if (parent.hoveringSprite() != null)
            parent.hoveringSprite().mouseExit(this);
        parent.hoveringOverSprite(null);
    }
    @Override
    public void mousePressed(MouseEvent e) {
        boolean rightClick = SwingUtilities.isRightMouseButton(e) && parent.allowsDragSelect();
        if (rightClick) {
            dragSelecting = true;
            selectX0 = selectX1 = e.getX();
            selectY0 = selectY1 = e.getY();
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        boolean shift = e.isShiftDown();
        if (dragSelecting) {
            if ((selectX0 != selectX1) && (selectY0 != selectY1))
                parent.dragSelect(selectX0, selectY0, selectX1, selectY1, shift);
            selectX0 = selectX1 = selectY0 = selectY1 = 0;
            dragSelecting = false;
            repaint();
            return;
        }

        if (e.getButton() > 3)
            return;
        
        int clicks = e.getClickCount();
        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        Sprite newSelection = hoverSprite;

        if (newSelection == null)
            parent.clickingNull(1, rightClick);
        else if ((clicks == 1) || newSelection.acceptDoubleClicks())
            parent.clickingOnSprite(newSelection, 1, rightClick, true);
            
        parent.hoveringOverSprite(newSelection);
    }
}

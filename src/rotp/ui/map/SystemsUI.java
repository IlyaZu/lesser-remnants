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
package rotp.ui.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JLayeredPane;
import javax.swing.border.Border;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.empires.SystemView;
import rotp.model.events.RandomEvent;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.tech.Tech;
import rotp.ui.BasePanel;
import rotp.ui.ExitButton;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.game.HelpUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.MapControlSprite;

public final class SystemsUI extends BasePanel implements IMapHandler, ActionListener, MouseWheelListener {
    private static final long serialVersionUID = 1L;
    private static final Color rallyBackHiC = new Color(192,139,105);
    private static final Color rallyBackLoC = new Color(77,55,34);
    private static final Color rallyBorderC = new Color(208,172,148);
    private static final Color unselectedTabC = new Color(112,85,68);
    private static final Color selectedTabC = new Color(178,124,87);

    private static final String expandTab = "Expand";
    private static final String exploitTab = "Exploit";
    private static final String exterminateTab = "Exterminate";
    private String selectedTab = expandTab;

    private MainTitlePanel titlePanel;

    private GalaxyMapPanel map;
    private LinearGradientPaint backGradient;
    private SystemInfoPanel displayPanel;
    private ExitSystemsButton exitButton;
    private final List<Sprite> controls = new ArrayList<>();
    private final Map<Integer,Integer> expandEnRouteSystems = new HashMap<>();
    private final Map<Integer,Integer> expandGuardedSystems = new HashMap<>();
    private Rectangle expandBox = new Rectangle();
    private Rectangle exploitBox = new Rectangle();
    private Rectangle exterminateBox = new Rectangle();

    private float colonyShipRange;

    private JLayeredPane layers = new JLayeredPane();
    public boolean animate = true;

    private int SIDE_PANE_W;
    private LinearGradientPaint grayBackC;
    private LinearGradientPaint redBackC;
    private LinearGradientPaint greenBackC;
    private LinearGradientPaint brownBackC;

    public SystemsUI() {
        int w, h;
        if (!UserPreferences.windowed()) {
            Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
            w = size.width;
            h = size.height;
        }
        else {
            w = scaled(Rotp.IMG_W);
            h = scaled(Rotp.IMG_H);
        }
        
        int rightPaneW = scaled(250);

        setBackground(Color.black);

        map = new GalaxyMapPanel(this);
        map.setBounds(0,0,w,h);

        titlePanel = new MainTitlePanel(this, "SYSTEMS_TITLE");
        titlePanel.setBounds(0,0,w-rightPaneW-s25, s45);
        
        displayPanel = new SystemInfoPanel(this);
        displayPanel.setBounds(w-rightPaneW-s5,s5,rightPaneW,scaled(673));
        
        exitButton = new ExitSystemsButton(rightPaneW, s60, s10, s2);
        exitButton.setBounds(w-rightPaneW-s5,h-s83,rightPaneW,s60);
        
        setLayout(new BorderLayout());
        add(layers, BorderLayout.CENTER);
        
        layers.add(titlePanel, JLayeredPane.PALETTE_LAYER);
        layers.add(displayPanel, JLayeredPane.PALETTE_LAYER);
        layers.add(exitButton, JLayeredPane.PALETTE_LAYER);
        layers.add(map, JLayeredPane.DEFAULT_LAYER);
        setOpaque(false);

        addMouseWheelListener(this);
    }
    public void init() {
        if (grayBackC == null)
            initGradients();

        // reset map everytime we open
        map.init();
        displayPanel.init();
        
        colonyShipRange = player().colonyShipRange();
        
        // on opening, build list of systems that we have colony ships
        // in transport to. This is too expensive to do real-time
        expandEnRouteSystems.clear();
        List<ShipFleet> allFleets = player().allFleets();
        for (ShipFleet fl: allFleets) {
            StarSystem sys = fl.destination();
            if (fl.canColonizeSystem(sys)) {
                int fleetTurns = fl.travelTurns(sys);
                if (expandEnRouteSystems.containsKey(sys.id)) {
                    int prevTurns = expandEnRouteSystems.get(sys.id);
                    if (fleetTurns < prevTurns)
                        expandEnRouteSystems.put(sys.id, fleetTurns);
                }
                else
                    expandEnRouteSystems.put(sys.id, fleetTurns);
            }
        }
        // on opening, build list of systems that we have colony ships
        // in transport to. This is too expensive to do real-time
        expandGuardedSystems.clear();
        Empire pl = player();
        for (Ship sh: pl.visibleShips()) {
            if (sh instanceof ShipFleet) {
                ShipFleet fl = (ShipFleet) sh;
                if (fl.inOrbit()) {
                    StarSystem sys = fl.system();
                    if (pl.sv.inShipRange(fl.sysId()) && !sys.isColonized() && pl.canColonize(fl.sysId())) {
                        if (fl.empire().aggressiveWith(pl, sys)) {
                            expandGuardedSystems.put(sys.id, fl.empId);
                        }
                    }
                }
            }
        }
        
    }
    public void clickSystem(StarSystem v, int count) {

    }
    public void clickFleet(ShipFleet fl) {
    }
    public void drawBrownButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, brownBackC, label, actionBox, hoverBox, y);
    }
    public void drawGrayButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, grayBackC, label, null, hoverBox, y);
    }
    public void drawGreenButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, greenBackC, label, actionBox, hoverBox, y);
    }
    public void drawRedButton(Graphics2D g, String label, Rectangle actionBox, Shape hoverBox, int y) {
        drawButton(g, redBackC, label, actionBox, hoverBox, y);
    }
    public void drawButton(Graphics2D g, LinearGradientPaint gradient, String label, Rectangle actionBox, Shape hoverBox, int y) {
        int buttonH = s27;
        int x1 = s3;
        int w1 = SIDE_PANE_W-s18;
        if (actionBox != null)
            actionBox.setBounds(x1,y,w1,buttonH);
        g.setColor(SystemPanel.buttonShadowC);
        g.fillRoundRect(x1+s1,y+s3,w1,buttonH,s8,s8);
        g.fillRoundRect(x1+s2,y+s4,w1,buttonH,s8,s8);

        g.setPaint(gradient);
        g.fillRoundRect(x1,y,w1,buttonH,s5,s5);

        boolean hovering = (actionBox != null) && (actionBox == hoverBox);
        Color c0 = (actionBox == null) ? SystemPanel.grayText : hovering ? SystemPanel.yellowText : SystemPanel.whiteText;

        g.setFont(narrowFont(18));
        int sw = g.getFontMetrics().stringWidth(label);
        int x0 = x1+((w1-sw)/2);
        drawShadowedString(g, label, 3, x0, y+buttonH-s7, SystemPanel.textShadowC, c0);

        g.setColor(c0);
        Stroke prev2 = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(x1+s1,y,w1-s2,buttonH,s5,s5);
        g.setStroke(prev2);
    }
    private void initGradients() {
        SIDE_PANE_W = scaled(250);
        int w = getWidth();
        int leftM = s2;
        int rightM = w-s2;
        Point2D start = new Point2D.Float(leftM, 0);
        Point2D end = new Point2D.Float(rightM, 0);
        float[] dist = {0.0f, 0.5f, 1.0f};

        Color brownEdgeC = new Color(100,70,50);
        Color brownMidC = new Color(161,110,76);
        Color[] brownColors = {brownEdgeC, brownMidC, brownEdgeC };

        Color grayEdgeC = new Color(59,59,59);
        Color grayMidC = new Color(92,92,92);
        Color[] grayColors = {grayEdgeC, grayMidC, grayEdgeC };

        Color greenEdgeC = new Color(44,59,30);
        Color greenMidC = new Color(71,93,48);
        Color[] greenColors = {greenEdgeC, greenMidC, greenEdgeC };

        Color redEdgeC = new Color(92,20,20);
        Color redMidC = new Color(117,42,42);
        Color[] redColors = {redEdgeC, redMidC, redEdgeC };

        brownBackC = new LinearGradientPaint(start, end, dist, brownColors);
        grayBackC = new LinearGradientPaint(start, end, dist, grayColors);
        greenBackC = new LinearGradientPaint(start, end, dist, greenColors);
        redBackC = new LinearGradientPaint(start, end, dist, redColors);
    }
    @Override
    public void paintOverMap(GalaxyMapPanel ui, Graphics2D g) {
        int w = ui.getWidth();
        int h = ui.getHeight();
                           
        if (backGradient == null) {
            Color c0 = Color.black;
            Color c1 = new Color(71,53,39);
            Point2D start = new Point2D.Float(s5, h-scaled(200));
            Point2D end = new Point2D.Float(s5, h-s20);
            float[] dist = {0.0f, 0.7f, 1.0f};
            Color[] colors = {c0, c0, c1 };
            backGradient = new LinearGradientPaint(start, end, dist, colors);
        }
        g.setPaint(backGradient);
        Area a = new Area(new Rectangle(0,0,w,h));
        a.subtract(new Area(new Rectangle(s5, s40,w-s20, h-s70)));
        g.fill(a);
    }
    @Override
    public Color shadeC()                          { return rallyBackLoC; }
    @Override
    public Color backC()                           { return rallyBackHiC; }
    @Override
    public Color lightC()                          { return rallyBorderC; }
    @Override
    public GalaxyMapPanel map()         { return map; }
    @Override
    public boolean animating()    { return animate; }
    public void clearMapSelections() {
        List<FlightPathSprite> paths = FlightPathSprite.workingPaths();
        paths.clear();
    }
    @Override
    public Color alertColor(SystemView sv) {
        switch(selectedTab) {
            case expandTab:      return expandAlertColor(sv);
            case exploitTab:     return exploitAlertColor(sv);
            case exterminateTab: return exterminateAlertColor(sv);
        }
        return null;
    }
    public StarSystem lastSystemSelected()    { return (StarSystem) sessionVar("MAINUI_SELECTED_SYSTEM"); }
    public void lastSystemSelected(Sprite s)  { sessionVar("MAINUI_SELECTED_SYSTEM", s); }
    public StarSystem systemToDisplay() {
        Sprite spr = hoveringSprite();
        if (spr instanceof StarSystem)
            return (StarSystem) spr;
        else
            return lastSystemSelected();
    }
    @Override
    public boolean canChangeMapScales()                 { return true; }
    @Override
    public void clickingNull(int cnt, boolean right) {
        displayPanel.useNullClick(cnt, right);
    }
    @Override
    public void clickingOnSprite(Sprite o, int count, boolean rightClick, boolean click) {
        if ((o instanceof ShipFleet) || (o instanceof Transport) || (o instanceof FlightPathSprite))
            return;
        
        boolean used = (displayPanel != null) && displayPanel.useClickedSprite(o, count, rightClick);
        hoveringOverSprite(null);
        if (!used)  {
            o.click(map, count, rightClick, click);
            if (o.persistOnClick()) {
                hoveringSprite(null);
                clickedSprite(o);
            }
            o.repaint(map);
        }
    }
    @Override
    public void hoveringOverSprite(Sprite o) {
        if (o == lastHoveringSprite())
            return;
        
        if (lastHoveringSprite() != null)
            lastHoveringSprite().mouseExit(map);

        if ((o == null) || (o instanceof StarSystem) || (o instanceof MapControlSprite))
            lastHoveringSprite(o);
        else
            return;
        
        boolean used = (displayPanel != null) && displayPanel.useHoveringSprite(o);
        if (!used) {
            if (hoveringSprite() != null)
                hoveringSprite().mouseExit(map);
            hoveringSprite(o);
            if (hoveringSprite() != null)
                hoveringSprite().mouseEnter(map);
        }
        repaint();
    }
    @Override
    public boolean shouldDrawSprite(Sprite s) {
        if (s == null)
            return false;
        if (s instanceof FlightPathSprite) {
            FlightPathSprite fp = (FlightPathSprite) s;
            Sprite fpShip = (Sprite) fp.ship();
            if (isClicked(fpShip) || isHovering(fpShip))
                return true;
            if (isClicked((Sprite) fp.destination()))
                return true;
            if (FlightPathSprite.workingPaths().contains(fp))
                return true;
            return fp.isPlayer() || fp.aggressiveToPlayer();
        }
        return true;
    }
    @Override
    public Location mapFocus() {
        Location loc = (Location) sessionVar("MAINUI_MAP_FOCUS");
        if (loc == null) {
            loc = new Location();
            sessionVar("MAINUI_MAP_FOCUS", loc);
        }
        return loc;
    }
    @Override
    public Sprite clickedSprite()            { return (Sprite) sessionVar("MAINUI_CLICKED_SPRITE"); }
    @Override
    public void clickedSprite(Sprite s)      {
        sessionVar("MAINUI_CLICKED_SPRITE", s);
        if (s instanceof StarSystem)
            lastSystemSelected(s);

    }
    @Override
    public Sprite hoveringSprite()           { return (Sprite) sessionVar("MAINUI_HOVERING_SPRITE"); }
    public void hoveringSprite(Sprite s)     { sessionVar("MAINUI_HOVERING_SPRITE", s); }
    public Sprite lastHoveringSprite()       { return (Sprite) sessionVar("MAINUI_LAST_HOVERING_SPRITE"); }
    public void lastHoveringSprite(Sprite s) { sessionVar("MAINUI_LAST_HOVERING_SPRITE", s); }
    @Override
    public List<Sprite> controlSprites()      { return controls; }
    @Override
    public Border mapBorder()       { return null; }
    @Override
    public float startingScalePct() {
        return (player().maxY()-player().minY()) / map().sizeY();
    }
    @Override
    public void animate() {
        if (animate)
            map.animate();
    }
    @Override
    public void cancelHelp() {
        RotPUI.helpUI().close();
    }
    @Override
    public void showHelp() {
        loadHelpUI();
        repaint();
    }
    @Override
    public void advanceHelp() {
        cancelHelp();
    }
    private void loadHelpUI() {
        HelpUI helpUI = RotPUI.helpUI();
        helpUI.clear();
        
        int w = getWidth();
        
        int x1 = scaled(150);
        int w1 = scaled(400);
        int y1 = scaled(300);
        helpUI.addBrownHelpText(x1, y1, w1, 4, text("SYSTEMS_HELP_1A"));

        int w2 = scaled(190);
        int y2 = s80;
        int y2a = s44;
        
        int x3 = expandBox.x-s60;
        int x3a = x3+(w2/2)+s30;
        HelpUI.HelpSpec sp3 = helpUI.addBrownHelpText(x3, y2, w2, 5, text("SYSTEMS_HELP_1C"));
        sp3.setLine(x3a, y2, x3a, y2a);
        
        int x4 = exploitBox.x-s30;
        int x4a = x4+(w2/2)+s15;
        HelpUI.HelpSpec sp4 = helpUI.addBrownHelpText(x4, y2, w2, 5, text("SYSTEMS_HELP_1D"));
        sp4.setLine(x4a, y2, x4a, y2a);
        
        int x5 = exterminateBox.x;
        HelpUI.HelpSpec sp5 = helpUI.addBrownHelpText(x5, y2, w2, 5, text("SYSTEMS_HELP_1E"));
        sp5.setLine(x5+(w2/2), y2, x5+(w2/2), y2a);
        
        int x6 = w-scaled(494);
        int x6a = w-scaled(245);
        int w6 = scaled(210);
        int y6 = scaled(220);
        HelpUI.HelpSpec sp6 = helpUI.addBrownHelpText(x6, y6, w6, 4, text("SYSTEMS_HELP_1F"));
        sp6.setLine(x6+w6, y6+(sp6.height()/2), x6a, y6+(sp6.height()/2));
        
        int y7 = scaled(335);
        HelpUI.HelpSpec sp7 = helpUI.addBrownHelpText(x6,y7,w6, 4, text("SYSTEMS_HELP_1G"));
        sp7.setLine(x6+w6, y7+(sp7.height()/2), x6a, y7+(sp7.height()/2));
        
        int y8 = scaled(470);
        HelpUI.HelpSpec sp8 = helpUI.addBrownHelpText(x6,y8,w6, 4, text("SYSTEMS_HELP_1H"));
        sp8.setLine(x6+w6, y8+(sp8.height()/2), x6a, y8+(sp8.height()/2));

        helpUI.open(this);
    }
    private String randomEventStatus(SystemView sv) {
        if (!sv.scouted())
            return "";
        StarSystem sys = sv.system();
        if (sys.hasEvent()) {
            RandomEvent ev = galaxy().events().activeEventForKey(sys.eventKey());
            if (ev != null)
                return ev.statusMessage();
        }
        return "";
    }
    private Color expandAlertColor(SystemView sv) {
        if (!sv.scouted())
            return null;
                
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
            if (sv.empire() == player())
                return MainUI.redAlertC;
        }

        if (sv.isColonized())
            return null;
        
        float sysDistance = sv.distance();
        Empire pl = player();
                 
        if ((sysDistance <= colonyShipRange) && pl.canColonize(sv.sysId)) {
            if (this.expandGuardedSystems.containsKey(sv.sysId))
                return MainUI.redAlertC;
            else if (expandEnRouteSystems.containsKey(sv.sysId))
                return null;
            else
                return MainUI.greenAlertC;
        }
        
        String rangeTech = pl.rangeTechNeededToReach(sv.sysId);
        String envTech = pl.environmentTechNeededToColonize(sv.sysId);
        if ((rangeTech != null) && (envTech != null))
            return MainUI.yellowAlertC;
        if ((envTech != null) && (sysDistance <= colonyShipRange))
            return MainUI.yellowAlertC;
        if ((rangeTech != null) && pl.canColonize(sv.sysId))
            return MainUI.yellowAlertC;
        return null;
    }
    private Color exploitAlertColor(SystemView sv) {
        if (sv.empire() != player())
            return null;
        
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
            if (sv.empire() == player())
                return MainUI.redAlertC;
        }
        
        Colony col = sv.system().colony();
        
        if (col.inRebellion())
            return MainUI.redAlertC;
        
        if (col.creatingWaste())
            return MainUI.redAlertC;
        
        int pct = (int) (100*col.currentProductionCapacity());
        if (pct < 34)
            return MainUI.redAlertC;
        else if (pct < 67)
            return MainUI.yellowAlertC;
        else if (pct < 100)
            return MainUI.greenAlertC;
        return null;
    }
    private Color exterminateAlertColor(SystemView sv)       {
        Empire pl = player();
        if (sv.distance() > pl.scoutRange())
            return null;
        Empire sysEmp = sv.empire();
        int sysEmpId = sv.empId();
        
        // deal with enemy fleets orbiting systems around us
        List<ShipFleet> fleets = sv.orbitingFleets();
        for (ShipFleet fl: fleets) {
            if (fl.isPotentiallyArmed(pl)) {
                if (pl.atWarWith(fl.empId())) {
                    if (sysEmp == null)
                        return MainUI.yellowAlertC; // enemy fleets around empty systems
                    if (sysEmp.isPlayer())
                        return MainUI.redAlertC;    // enemy fleets around player colonies
                    else if (pl.alliedWith(sysEmpId))
                        return MainUI.yellowAlertC; // enemy fleets around ally systems
                    else
                        return MainUI.yellowAlertC; // enemy fleets around other systems
                }
            }
        }
        
        // if we can see ship ETAs, look for armed enemy ships approaching
        // player or allied colonies
        if (pl.knowShipETA() && pl.alliedWith(sysEmpId)) {
            for (Ship sh: pl.visibleShips()) {
                if ((sh.destSysId() == sv.sysId) && pl.atWarWith(sh.empId()) && sh.isPotentiallyArmed(pl)) {
                    if (sysEmp.isPlayer())
                        return MainUI.redAlertC; // enemy fleets approaching player colonies
                    else
                        return MainUI.yellowAlertC; // enemy fleets approaching player allied colonies
                }
            }
        }
        
        if (sysEmp != pl) {
            int num = pl.transportsInTransit(sv.system());
            if (num > 0)
                return MainUI.yellowAlertC;
        }
        
        // enemy fleets approaching player allied colonies
        // for player systems, highlight those with no bases or insufficient shields
        if (sysEmp == null)
            return null;
        if (!sysEmp.isPlayer())
            return null;

        if (!sv.colony().defense().isCompleted())
            return MainUI.greenAlertC;
        
        return null;
    }
    public String alertDescription(SystemView sv) {
        switch(selectedTab) {
            case expandTab:      return expandAlertDescription(sv);
            case exploitTab:     return exploitAlertDescription(sv);
            case exterminateTab: return exterminateAlertDescription(sv);
        }
        return null;
    }
    private String expandAlertDescription(SystemView sv) {
        if (!sv.scouted())
            return null;
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
             if (sv.empire() == player())
                 return eventMessage;
        }

        if (sv.isColonized())
            return null;
        
        float sysDistance = sv.distance();
        Empire pl = player();
        if ((sysDistance <= colonyShipRange) && pl.canColonize(sv.sysId)) {
            if (expandGuardedSystems.containsKey(sv.sysId)) {
                Empire enemyEmp = galaxy().empire(expandGuardedSystems.get(sv.sysId));
                String s = text("SYSTEMS_CAN_COLONIZE_ENEMY");
                s = enemyEmp.replaceTokens(s, "alien");
                return s;
            }
            if (expandEnRouteSystems.containsKey(sv.sysId)) {
                int turns = expandEnRouteSystems.get(sv.sysId);
                return text("SYSTEMS_CAN_COLONIZE_EN_ROUTE", turns);
            }
            return text("SYSTEMS_CAN_COLONIZE");
        }
        
        String rangeTech = pl.rangeTechNeededToReach(sv.sysId);
        String envTech = pl.environmentTechNeededToColonize(sv.sysId);
        
        if ((rangeTech != null) && (envTech != null)) {
            Tech t1 = tech(envTech);
            Tech t2 = tech(rangeTech);
            return text("SYSTEMS_UNCOLONIZED_NEED_TECHS", t1.name(), t2.name());
        }
        if ((envTech != null) && (sysDistance <= colonyShipRange)){
            Tech t1 = tech(envTech);
            return text("SYSTEMS_UNCOLONIZED_NEED_TECH", t1.name());
        }
        if ((rangeTech != null) && pl.canColonize(sv.sysId)) {
            Tech t1 = tech(rangeTech);
            return text("SYSTEMS_UNCOLONIZED_NEED_TECH", t1.name());
        }
        return text("SYSTEMS_UNCOLONIZEABLE");
    }
    private String exploitAlertDescription(SystemView sv) {
        Empire sysEmp = sv.empire();
        if ((sysEmp == null) || !sysEmp.isPlayer())
            return null;
       
        String eventMessage = randomEventStatus(sv);
        if (!eventMessage.isEmpty()) {
             if (sv.empire() == player())
                 return eventMessage;
        }
        
        int num = player().transportsInTransit(sv.system());
        String transportMsg = num == 0 ? "" : " "+text("SYSTEMS_EXPLOIT_TRANSPORTS",str(num));

        Colony col = sv.system().colony();
        if (col.inRebellion())
            return concat(text("SYSTEMS_STATUS_REBELLION"),transportMsg);
        
        if (col.creatingWaste())
            return concat(text("SYSTEMS_EXPLOIT_WASTE"), transportMsg);
        
        int pct = (int) (100*col.currentProductionCapacity());
        String capMsg;
        if (pct < 34)
            capMsg = text("SYSTEMS_EXPLOIT_PCT", pct);
        else if (pct < 67)
            capMsg = text("SYSTEMS_EXPLOIT_PCT", pct);
        else if (pct < 100)
            capMsg = text("SYSTEMS_EXPLOIT_PCT", pct);
        else
            capMsg = text("SYSTEMS_EXPLOIT_COMPLETE");
        
        return concat(capMsg, transportMsg);
    }
    private String exterminateAlertDescription(SystemView sv) {
        Empire pl = player();
        if (sv.distance() > pl.scoutRange())
            return null;

        Empire sysEmp = sv.empire();
        int num = pl.transportsInTransit(sv.system());
        String troopMsg;
        if (num == 0)
            troopMsg = null;
        else if (sysEmp == pl)
            troopMsg = " "+text("SYSTEMS_EXPLOIT_TRANSPORTS",str(num));
        else {
            troopMsg = " "+text("SYSTEMS_EXT_INC_TRANSPORTS",str(num));
            troopMsg = pl.replaceTokens(troopMsg, "player");
        }
        
        if (pl.atWarWith(sv.empId())) {
            return troopMsg;
        }
            
        // deal with enemy fleets orbiting systems around us
        List<ShipFleet> fleets = sv.orbitingFleets();
        for (ShipFleet fl: fleets) {
            if (fl.isPotentiallyArmed(pl)) {
                if (pl.atWarWith(fl.empId())) {
                    String fleetMsg;
                    if (sysEmp == null)
                        fleetMsg = text("SYSTEMS_EXT_ENEMY_FLEET");
                    else if (sysEmp.isPlayer())
                        fleetMsg = text("SYSTEMS_EXT_ENEMY_FLEET_PLAYER");
                    else if (pl.alliedWith(sv.empId()))
                        fleetMsg = text("SYSTEMS_EXT_ENEMY_FLEET_ALLY");
                    else
                        fleetMsg = text("SYSTEMS_EXT_ENEMY_FLEET");
                    return concat(fleetMsg, troopMsg);
                }
            }
        }
        
        // if we can see ship ETAs, look for armed enemy ships approaching
        // player or allied colonies
        if (pl.knowShipETA() && pl.alliedWith(sv.empId())) {
            for (Ship sh: pl.visibleShips()) {
                if ((sh.destSysId() == sv.sysId) && pl.atWarWith(sh.empId()) && sh.isPotentiallyArmed(pl)) {
                    String fleetMsg;
                    if (sysEmp.isPlayer())
                        fleetMsg = text("SYSTEMS_EXT_INC_FLEET_PLAYER");
                    else
                        fleetMsg = text("SYSTEMS_EXT_INC_FLEET_ALLY");
                    return concat(fleetMsg, troopMsg);
                }
            }
        }
        
        if ((sysEmp != pl) && (num > 0))
            return troopMsg;
        
        // for player systems, highlight those with no bases or insufficient shields
        if ((sysEmp == null) || !sysEmp.isPlayer())
            return null;

        if (!sv.colony().defense().isCompleted())
            return concat(text("SYSTEMS_EXT_NEED_DEFENSE"), troopMsg);
        
        return null;
    }
    @Override
    public String systemLabel2(StarSystem sys) {
        Empire pl = player();
        SystemView sv = pl.sv.view(sys.id);
        switch(selectedTab) {
            case expandTab:
                if (!sv.scouted())
                    return "";
                else if (sv.currentSize() == 0)
                    return text("SYSTEMS_ENVIRONMENT_SIZE", sv.planetType().name(), "");
                else
                    return text("SYSTEMS_ENVIRONMENT_SIZE", sv.planetType().name(), str(sv.currentSize()));
            case exploitTab:
                return text("SYSTEMS_ENVIRONMENT_TYPE", text(sv.resourceType()), text(sv.ecologyType()));
            case exterminateTab:
                int bases = sv.bases();
                int shield = sv.shieldLevel();
                if ((bases == 0) && (shield == 0))
                    return "";
                String str1 = shield == 0 ? "" : text("SYSTEMS_SHIELD", str(shield));
                String str2 = bases == 0 ? "" : text("SYSTEMS_BASES", str(bases));
                return shield == 0 ? str2 : str1+" "+str2;
        }
        return "";
    }
    public void exit(boolean delay) {
        displayPanel.exit();
        RotPUI.instance().selectMainPanel(delay);
    }
    @Override
    public void keyPressed(KeyEvent e) {
        boolean shift = e.isShiftDown();
        if (e.getKeyChar() == '?') {
            showHelp();
            return;
        }
        switch(e.getKeyCode()) {
            case KeyEvent.VK_F1:
                showHelp();
                return;
            case KeyEvent.VK_TAB:
                if (!shift)
                    titlePanel.selectNextTab();
                else
                    titlePanel.selectPreviousTab();
                return;
            case KeyEvent.VK_ESCAPE:
                clearMapSelections();
                buttonClick();
                exit(false);
                return;
            case KeyEvent.VK_EQUALS:
                if (e.isShiftDown())  {
                    softClick();
                    map().adjustZoom(-1);
                }
                return;
            case KeyEvent.VK_MINUS:
                softClick();
                map().adjustZoom(1);
                return;
            case KeyEvent.VK_UP:
                softClick();
                map().dragMap(0, s40);
                return;
            case KeyEvent.VK_DOWN:
                softClick();
                map().dragMap(0, -s40);
                return;
            case KeyEvent.VK_LEFT:
                softClick();
                map().dragMap(s40, 0);
                return;
            case KeyEvent.VK_RIGHT:
                softClick();
                map().dragMap(-s40, 0);
                return;
            case KeyEvent.VK_F:
                displayPanel.toggleFlagColor(shift);
                return;
        }
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {

    }
    class MainTitlePanel extends BasePanel implements MouseMotionListener, MouseListener {
        private static final long serialVersionUID = 1L;
        SystemsUI parent;
        String titleKey;
        public MainTitlePanel(SystemsUI p, String s) {
            parent = p;
            titleKey = s;
            initModel();
        }
        Rectangle hoverBox;
        Rectangle helpBox = new Rectangle();

        private void initModel() {
            setOpaque(false);
            setPreferredSize(new Dimension(getWidth(),s45));
            addMouseListener(this);
            addMouseMotionListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;

            int w = getWidth();
            int h = getHeight();
            int gap = s10;
            int helpW = s30;
            int x0 = gap+helpW;
            int y0 = h - s12;
            String title = text(titleKey);
            String intLabel = text("SYSTEMS_TAB_EXPAND");
            String milLabel = text("SYSTEMS_TAB_EXPLOIT");
            String statusLabel = text("SYSTEMS_TAB_EXTERMINATE");

            drawHelpButton(g);
            
            g.setColor(SystemPanel.orangeText);
            g.setFont(narrowFont(32));
            int titleW = g.getFontMetrics().stringWidth(title);
            int titleSpacing = s60+s60;
            drawString(g,title, x0,y0);

            int tabW = (w-titleW-titleSpacing-(6*gap)-helpW)/4;
            int tabSpacing = tabW+gap;

            x0 += (titleW+titleSpacing);
            drawTab(g,x0,0,tabW,h,intLabel, expandBox, selectedTab.equals(expandTab));

            x0 += tabSpacing;
            drawTab(g,x0,0,tabW,h,milLabel, exploitBox, selectedTab.equals(exploitTab));

            x0 += tabSpacing;
            drawTab(g,x0,0,tabW,h,statusLabel, exterminateBox, selectedTab.equals(exterminateTab));
            
            g.setColor(selectedTabC);
            g.fillRect(s5, h-s5, w-s10, s5);
        }
        private void drawHelpButton(Graphics2D g) {
            helpBox.setBounds(s10,s10,s20,s25);
            g.setColor(unselectedTabC);
            g.fillOval(s10, s10, s20, s25);
            g.setFont(narrowFont(25));
            if (helpBox == hoverBox)
                g.setColor(Color.yellow);
            else
                g.setColor(Color.white);

            drawString(g,"?", s16, s30);
        }
        private void drawTab(Graphics2D g, int x, int y, int w, int h, String label, Rectangle box, boolean selected) {
            g.setFont(narrowFont(22));
            if (selected)
                g.setColor(selectedTabC);
            else
                g.setColor(unselectedTabC);

            box.setBounds(x, y+s10, w, h-s10);
            g.fillRoundRect(x, y+s10, w, h-s10, h/4, h/4);
            g.fillRect(x, h-s5, w, s5);

            if (box == hoverBox) {
                Stroke prev = g.getStroke();
                g.setStroke(stroke2);
                g.setColor(Color.yellow);
                g.setClip(x, y, w, h*2/3);
                g.drawRoundRect(x, y+s10, w, h-s10, h/4, h/4);
                g.setClip(x, y+h/2, w, h/2);
                g.drawRect(x, y+s10, w, h);
                g.setClip(null);
                g.setStroke(prev);
            }
            int sw = g.getFontMetrics().stringWidth(label);
            int x0 = x+((w-sw)/2);
            int y0 = y+h-s10;

            Color c0 = (box == hoverBox) ? Color.yellow : SystemPanel.whiteLabelText;
            drawShadowedString(g, label, 3, x0, y0, SystemPanel.textShadowC, c0);
        }
        public void selectNextTab() {
            switch(selectedTab) {
                case expandTab:      selectTab(exploitTab); break;
                case exploitTab:     selectTab(exterminateTab); break;
                case exterminateTab: selectTab(expandTab); break;
            }
        }
        public void selectPreviousTab() {
            switch(selectedTab) {
                case expandTab:      selectTab(exterminateTab); break;
                case exploitTab:     selectTab(expandTab); break;
                case exterminateTab: selectTab(exploitTab); break;
            }
        }
        private void selectTab(String s) {
            if (!selectedTab.equals(s)) {
                softClick();
                selectedTab = s;
                repaint();
            }
        }
        @Override
        public void mouseClicked(MouseEvent e) {}
        @Override
        public void mouseEntered(MouseEvent e) {}
        @Override
        public void mouseExited(MouseEvent e) {
            if (hoverBox != null) {
                hoverBox = null;
                repaint();
            }
        }
        @Override
        public void mousePressed(MouseEvent e) {}
        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() > 3)
                return;
            if (hoverBox == null)
                misClick();
            else {
                if (hoverBox == expandBox)
                    selectTab(expandTab);
                else if (hoverBox == exploitBox)
                    selectTab(exploitTab);
                else if (hoverBox == exterminateBox)
                    selectTab(exterminateTab);
                else if (hoverBox == helpBox)
                    parent.showHelp();
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) {}
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Rectangle prevHover = hoverBox;
            if (expandBox.contains(x,y))
                hoverBox = expandBox;
            else if (exploitBox.contains(x,y))
                hoverBox = exploitBox;
           else if (exterminateBox.contains(x,y))
                hoverBox = exterminateBox;
           else if (helpBox.contains(x,y))
                hoverBox = helpBox;

            if (hoverBox != prevHover)
                repaint();
        }
    }
    class ExitSystemsButton extends ExitButton {
        private static final long serialVersionUID = 1L;
        public ExitSystemsButton(int w, int h, int vMargin, int hMargin) {
            super(w, h, vMargin, hMargin);
        }
        @Override
        protected void clickAction(int numClicks) {
            // force recalcuate map bounds when returning
            buttonClick();
            exit(true);
        }
    }
}

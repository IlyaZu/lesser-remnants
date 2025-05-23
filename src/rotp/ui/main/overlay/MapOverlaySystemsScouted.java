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
package rotp.ui.main.overlay;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.TextButtonSprite;
import rotp.ui.sprites.MapSprite;

public class MapOverlaySystemsScouted extends MapOverlay {
    private static final Color maskC = new Color(40,40,40,160);
    
    private final TextButtonSprite prevSystemButton =
            new TextButtonSprite("MAIN_ALLOCATE_PREV_SYSTEM", false, this::previousSystem);
    private final TextButtonSprite nextSystemButton =
            new TextButtonSprite("MAIN_ALLOCATE_NEXT_SYSTEM", true, this::nextSystem);
    private final TextButtonSprite continueButton =
            new TextButtonSprite("MAIN_ALLOCATE_CLOSE", false, this::advanceMap);
    private final SystemFlagSprite flagButton = new SystemFlagSprite();
    private final MainUI parent;
    
    private Area mask;
    private BufferedImage planetImg;
    private List<StarSystem> scoutSystems = new ArrayList<>();
    private List<StarSystem> allySystems = new ArrayList<>();
    private List<StarSystem> astronomerSystems = new ArrayList<>();
    private List<StarSystem> orderedSystems = new ArrayList<>();
    private int systemIndex = 0;
    private boolean isOpen = false;
    
    public MapOverlaySystemsScouted(MainUI p) {
        parent = p;
    }
    
    public void init(HashMap<String, List<StarSystem>> newSystems) {
        parent.hideDisplayPanel();
        parent.map().setScale(20);
        systemIndex = 0;
        isOpen = true;
        orderedSystems.clear();
        flagButton.reset();
        if (newSystems.isEmpty())
            advanceMap();
        else {
            // create an alphabetized list of systems
            scoutSystems = newSystems.get("Scouts");
            allySystems = newSystems.get("Allies");
            astronomerSystems = newSystems.get("Astronomers");
            orderedSystems.addAll(scoutSystems);
            orderedSystems.addAll(astronomerSystems);
            orderedSystems.addAll(allySystems);
            Collections.sort(orderedSystems, StarSystem.NAME);
            mapSelectIndex(0);
        }
    }
    
    private void mapSelectIndex(int i) {
        mask = null;
        planetImg = null;
        StarSystem nextSystem = orderedSystems.get(i);
        parent.map().recenterMapOn(nextSystem);
        parent.mapFocus(nextSystem);
        parent.clickedSprite(nextSystem);
        parent.repaint();
    }
    private StarSystem starSystem() {
        return orderedSystems.get(systemIndex);
    }
    public void nextSystem() {
        systemIndex++;
        if (systemIndex >= orderedSystems.size())
            systemIndex = 0;
        mapSelectIndex(systemIndex);
    }
    public void previousSystem() {
        systemIndex--;
        if (systemIndex < 0)
            systemIndex = orderedSystems.size()-1;
        mapSelectIndex(systemIndex);
    }
    private void toggleFlagColor(boolean reverse) {
        StarSystem sys = orderedSystems.get(systemIndex);
        player().sv.toggleFlagColor(sys.id, reverse);
        parent.repaint();
    }
    private void resetFlagColor() {
        StarSystem sys = orderedSystems.get(systemIndex);
        player().sv.resetFlagColor(sys.id);
        parent.repaint();
    }
    @Override
    public void advanceMap() {
        if (isOpen) {
            isOpen = false;
            orderedSystems.clear();
            scoutSystems.clear();
            allySystems.clear();
            astronomerSystems.clear();

            if (session().performingTurn())
                parent.resumeTurn();
            else
                parent.resumeOutsideTurn();
        }
    }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        if (orderedSystems.isEmpty())
            return;
        StarSystem sys = orderedSystems.get(systemIndex);
        Empire pl = player();

        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s15 = BasePanel.s15;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s30 = BasePanel.s30;
        int s40 = BasePanel.s40;
        int s60 = BasePanel.s60;

        int w = ui.getWidth();
        int h = ui.getHeight();

        int bdrW = s7;
        int boxW = scaled(540);
        int boxH1 = BasePanel.s68;
        int boxH2 = scaled(172);
        int buttonPaneH = s40;
        int boxH = boxH1 + boxH2 + buttonPaneH;

        int boxX = -s40+(w/2);
        int boxY = -s40+(h-boxH)/2;
        
        // dimensions of the shade pane
        int x0 = boxX-bdrW;
        int y0 = boxY-bdrW;
        int w0 = boxW+bdrW+bdrW;
        int h0 = boxH+bdrW+bdrW;

        // draw map mask
        if (mask == null) {
            int r = s60;
            int centerX = w*2/5;
            int centerY = h*2/5;
            Ellipse2D window = new Ellipse2D.Float();
            window.setFrame(centerX-r, centerY-r, r+r, r+r);
            Area st1 = new Area(window);
            Rectangle blackout  = new Rectangle();
            blackout.setFrame(0,0,w,h);
            mask = new Area(blackout);
            mask.subtract(st1);
        }
        g.setColor(maskC);
        g.fill(mask);
        // draw border
        g.setColor(MainUI.paneShadeC);
        g.fillRect(x0, y0, w0, h0);

        // draw Box
        g.setColor(MainUI.paneBackground);
        g.fillRect(boxX, boxY, boxW, boxH1);

        // draw planet image
        if (planetImg == null) {
            if (sys.planet().type().isAsteroids()) {
                planetImg = newBufferedImage(boxW, boxH2);
                Graphics imgG = planetImg.getGraphics();
                imgG.setColor(Color.black);
                imgG.fillRect(0, 0, boxW, boxH2);
                drawBackgroundStars(imgG, boxW, boxH2);
                parent.drawStar((Graphics2D) imgG, sys.starType(), s60, boxW*4/5, boxH2/3);
                imgG.dispose();
            }
            else
                planetImg = sys.planet().type().panoramaImage();
        }
        g.drawImage(planetImg, boxX, boxY+boxH1, boxW, boxH2, null);

        // draw header info
        int x1 = boxX+s15;

        String scoutStr = text("MAIN_SCOUT_TITLE");
        int titleFontSize = scaledFont(g, scoutStr, x1-s30, 24, 14);
        g.setFont(narrowFont(titleFontSize));
        drawShadowedString(g, scoutStr, 4, x1, boxY+boxH1-s40, SystemPanel.textShadowC, Color.white);

        String detailStr = "";
        if (scoutSystems.contains(sys))
            detailStr = text("MAIN_SCOUT_SUBTITLE_1");
        else if (astronomerSystems.contains(sys))
            detailStr = text("MAIN_SCOUT_SUBTITLE_2");
        else if (allySystems.contains(sys))
            detailStr = text("MAIN_SCOUT_SUBTITLE_3");
            
        if (!detailStr.isEmpty()) {
            g.setColor(Color.darkGray);
            g.setFont(narrowFont(16));
            drawString(g,detailStr, x1, boxY+boxH1-s20);
        }

        // draw planet info, from bottom up
        int y1 = boxY+boxH-buttonPaneH-s10;
        int lineH = s20;
        int desiredFont = 18;

        if (pl.sv.isUltraPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_ULTRA_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isUltraRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ULTRA_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.sv.isOrionArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ANCIENTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ARTIFACTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.isEnvironmentHostile(sys)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_HOSTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentFertile(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_FERTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentGaia(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_GAIA_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        // classification line
        if (sys.planet().type().isAsteroids()) {
            String s1 = text("MAIN_SCOUT_NO_PLANET");
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else {
            String s1 = text("MAIN_SCOUT_TYPE", text(sys.planet().type().key()), (int)sys.planet().maxSize());
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.sv.isColonized(sys.id)) {
            g.setFont(narrowFont(24));
            String s1 = pl.sv.descriptiveName(sys.id);
            int fontSize = scaledFont(g, s1, boxW-x1-s10, 24, 18);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
            y1 -= scaled(5);
        }
        // planet name
        String sysName = pl.sv.name(sys.id);
        y1 -= scaled(5);
        g.setColor(SystemPanel.orangeText);
        g.setFont(narrowFont(40));
        drawBorderedString(g, sysName, 1, x1, y1, Color.darkGray, SystemPanel.orangeText);
        
        // planet flag
        parent.addNextTurnControl(flagButton);
        flagButton.init(this, g);
        flagButton.mapX(boxX+boxW-flagButton.width()+s10);
        flagButton.mapY(boxY+boxH-buttonPaneH-flagButton.height()+s10);
        flagButton.draw(parent.map(), g);

        // init and draw continue button sprite
        parent.addNextTurnControl(continueButton);
        continueButton.refreshSize(g);
        int continueX = x0+w0-continueButton.getWidth()-s10;
        int continueY = y0+h0-continueButton.getHeight()-s10;
        continueButton.setPosition(continueX, continueY);
        continueButton.draw(parent.map(), g);

        if (orderedSystems.size() > 1) {
            parent.addNextTurnControl(prevSystemButton);
            prevSystemButton.refreshSize(g);
            int prevSystemX = x0+s10;
            int prevSystemY = y0+h0-prevSystemButton.getHeight()-s10;
            prevSystemButton.setPosition(prevSystemX, prevSystemY);
            prevSystemButton.draw(parent.map(), g);

            // draw notice number
            String notice2Str = text("MAIN_ALLOCATE_BRIEF_NUMBER", str(systemIndex+1), str(orderedSystems.size()));
            g.setFont(narrowFont(16));
            int sw4 = g.getFontMetrics().stringWidth(notice2Str);
            int x4b = prevSystemX+prevSystemButton.getWidth()+s10;
            int y4b = prevSystemY+prevSystemButton.getHeight()-s10;
            g.setColor(SystemPanel.blackText);
            drawString(g,notice2Str, x4b, y4b);

            parent.addNextTurnControl(nextSystemButton);
            nextSystemButton.refreshSize(g);
            nextSystemButton.setPosition(x4b+sw4+s10, prevSystemY);
            nextSystemButton.draw(parent.map(), g);
        }
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        boolean shift = e.isShiftDown();
        switch(e.getKeyCode()) {
            case KeyEvent.VK_N:
                nextSystem();
                break;
            case KeyEvent.VK_P:
                previousSystem();
                break;
            case KeyEvent.VK_C:
            case KeyEvent.VK_ENTER:
            case KeyEvent.VK_ESCAPE:
                advanceMap();
                break;
            case KeyEvent.VK_F:
                toggleFlagColor(shift);
                break;
            default:
                misClick(); break;
        }
        return true;
    }
    class SystemFlagSprite extends MapSprite {
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlaySystemsScouted parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        public void reset()        { }

        public void init(MapOverlaySystemsScouted p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s70;
            buttonH = BasePanel.s70;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean acceptDoubleClicks()         { return true; }
        @Override
        public boolean acceptWheel()                { return true; }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            StarSystem sys = parent.starSystem();
            Image flagImage = parent.parent.flagImage(sys);
            Image flagHaze = parent.parent.flagHaze(sys);
            g.drawImage(flagHaze, mapX, mapY, buttonW, buttonH, null);
            if (hovering) {
                Image flagHover = parent.parent.flagHover(sys);
                g.drawImage(flagHover, mapX, mapY, buttonW, buttonH, null);
            }
            g.drawImage(flagImage, mapX, mapY, buttonW, buttonH, null);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            if (rightClick)
                parent.resetFlagColor();
            else
                parent.toggleFlagColor(false);
        }
        @Override
        public void wheel(GalaxyMapPanel map, int rotation, boolean click) {
            if (rotation < 0)
                parent.toggleFlagColor(true);
            else
                parent.toggleFlagColor(false);
        }
    }
}
    
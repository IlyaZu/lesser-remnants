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

import rotp.model.Sprite;
import rotp.model.combat.CombatManager;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.TextButtonSprite;
import rotp.ui.sprites.MapSprite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import rotp.ui.combat.ShipBattleUI;

public class MapOverlayShipCombatPrompt extends MapOverlay {
    private static final Color maskC  = new Color(40,40,40,160);
    
    private final TextButtonSprite resolveButton =
            new TextButtonSprite("SHIP_COMBAT_AUTO_RESOLVE", false, () -> startCombat(ShipBattleUI.AUTO_RESOLVE));
    private final TextButtonSprite retreatButton =
            new TextButtonSprite("SHIP_COMBAT_RETREAT_ALL", false, () -> startCombat(ShipBattleUI.RETREAT_ALL));
    private final TextButtonSprite battleButton =
            new TextButtonSprite("SHIP_COMBAT_ENTER_BATTLE", true, () -> startCombat(ShipBattleUI.ENTER_COMBAT));
    private final MainUI parent;
    
    private Area mask;
    private BufferedImage planetImg;
    private int sysId;
    private  CombatManager mgr;
    private SystemFlagSprite flagButton = new SystemFlagSprite();
    
    public MapOverlayShipCombatPrompt(MainUI p) {
        parent = p;
    }
    
    public void init(CombatManager m) {
        mgr = m;
        sysId = mgr.system().id;
        flagButton.reset();
        StarSystem sys = galaxy().system(sysId);
        planetImg = null;
        parent.hideDisplayPanel();
        parent.map().setScale(20);
        parent.map().recenterMapOn(sys);
        parent.mapFocus(sys);
        parent.clickedSprite(sys);
        parent.repaint();
    }
    
    public void startCombat(int combatFlag) {
        parent.clearOverlay();
        parent.repaintAllImmediately();
        RotPUI.instance().selectShipBattlePanel(mgr, combatFlag);
    }
    private StarSystem starSystem() {
        return galaxy().system(sysId);
    }
    private void toggleFlagColor(boolean reverse) {
        player().sv.toggleFlagColor(sysId, reverse);
        parent.repaint();
    }
    private void resetFlagColor() {
        player().sv.resetFlagColor(sysId);
        parent.repaint();
    }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        startCombat(ShipBattleUI.ENTER_COMBAT);
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        StarSystem sys = galaxy().system(sysId);
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
        int buttonPaneH = scaled(40);
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

        boolean scouted = player().sv.isScouted(sys.id);
        // draw planet image
        if (planetImg == null) {
            if (!scouted || sys.planet().type().isAsteroids()) {
                planetImg = newBufferedImage(boxW, boxH2);
                Graphics imgG = planetImg.getGraphics();
                imgG.setColor(Color.black);
                imgG.fillRect(0, 0, boxW, boxH2);
                drawBackgroundStars(imgG, boxW, boxH2);
                parent.drawStar((Graphics2D) imgG, sys.starType(), s60, boxW*4/5, boxH2/3);
                imgG.dispose();
            }
            else {
                planetImg = sys.planet().type().panoramaImage();
                int planetW = planetImg.getWidth();
                int planetH = planetImg.getHeight();
                Graphics imgG = planetImg.getGraphics();
                Empire emp = sys.empire();
                if (emp != null) {
                    BufferedImage fortImg = emp.race().fortress(sys.colony().fortressNum());
                    int fortW = scaled(fortImg.getWidth());
                    int fortH = scaled(fortImg.getHeight());
                    int fortScaleW = fortW*planetW/w;
                    int fortScaleH = fortH*planetW/w;
                    int fortX = planetImg.getWidth()-fortScaleW;
                    int fortY = planetImg.getHeight()-fortScaleH+(planetH/5);
                    imgG.drawImage(fortImg, fortX, fortY, fortX+fortScaleW, fortY+fortScaleH, 0, 0, fortImg.getWidth(), fortImg.getHeight(), null);
                    imgG.dispose();
                }
            }
        }
        g.drawImage(planetImg, boxX, boxY+boxH1, boxW, boxH2, null);

        // draw header info
        int x1 = boxX+s15;

        // print prompt string
        String sysName = player().sv.name(sys.id);
        String promptStr = scouted ? text("SHIP_COMBAT_TITLE_SYSTEM", sysName) : text("SHIP_COMBAT_TITLE_UNSCOUTED");
        int promptFontSize = scaledFont(g, promptStr, x1-s30, 24, 14);
        g.setFont(narrowFont(promptFontSize));
        drawShadowedString(g, promptStr, 4, x1, boxY+boxH1-s40, SystemPanel.textShadowC, Color.white);
        
        Empire aiEmpire = mgr.results().aiEmpire();
        String titleStr;
        if (aiEmpire == null)
            titleStr = text("SHIP_COMBAT_TITLE_MONSTER_DESC", mgr.results().aiRaceName());
        else {
            titleStr = text("SHIP_COMBAT_TITLE_DESC");
            titleStr = aiEmpire.replaceTokens(titleStr, "alien");
        }
        g.setColor(Color.darkGray);
        g.setFont(narrowFont(16));
        drawString(g,titleStr, x1, boxY+boxH1-s20);

        // init and draw battle and resolve buttons
        parent.addNextTurnControl(battleButton);
        battleButton.refreshSize(g);
        int buttonY = y0+h0-battleButton.getHeight()-s10;
        battleButton.setPosition(x0+w0-battleButton.getWidth()-s10, buttonY);
        battleButton.draw(parent.map(), g);
        
        if (aiEmpire != null) {
            parent.addNextTurnControl(resolveButton);
            resolveButton.refreshSize(g);
            int resolveX = x0+s10;
            resolveButton.setPosition(resolveX, buttonY);
            resolveButton.draw(parent.map(), g);

            parent.addNextTurnControl(retreatButton);
            retreatButton.refreshSize(g);
            retreatButton.setPosition(resolveX+resolveButton.getWidth()+s10, buttonY);
            retreatButton.draw(parent.map(), g);
        }
        // if unscouted, no planet info
        if (!scouted)
            return;
        
        // draw planet info, from bottom up
        int y1 = boxY+boxH1+boxH2-s10;
        int lineH = s20;
        int desiredFont = 18;

        if (pl.sv.isUltraPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_ULTRA_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isUltraRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ULTRA_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.sv.isOrionArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ANCIENTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ARTIFACTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.isEnvironmentHostile(sys)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_HOSTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentFertile(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_FERTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentGaia(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_GAIA_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
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

        // planet name
        y1 -= scaled(5);
        g.setFont(narrowFont(40));
        drawBorderedString(g, sysName, 1, x1, y1, Color.darkGray, SystemPanel.orangeText);
        
        // planet flag
        parent.addNextTurnControl(flagButton);
        flagButton.init(this, g);
        flagButton.mapX(boxX+boxW-flagButton.width()+s10);
        flagButton.mapY(boxY+boxH-buttonPaneH-flagButton.height()+s10);
        flagButton.draw(parent.map(), g);
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        boolean shift = e.isShiftDown();
        Empire aiEmpire = mgr.results().aiEmpire();
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_E:
                startCombat(ShipBattleUI.ENTER_COMBAT);
                break;
            case KeyEvent.VK_A:
                if (aiEmpire != null)
                    startCombat(ShipBattleUI.AUTO_RESOLVE);
                break;
            case KeyEvent.VK_R:
                if (aiEmpire != null)
                    startCombat(ShipBattleUI.RETREAT_ALL);
                break;
            case KeyEvent.VK_F:
                toggleFlagColor(shift);
                break;
            default:
                misClick();
                break;
        }
        return true;
    }
    class SystemFlagSprite extends MapSprite {
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        public void reset()       {  }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
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

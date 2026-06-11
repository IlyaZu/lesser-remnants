/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2026 Ilya Zushinskiy
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
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.TextButtonSprite;
import rotp.ui.sprites.SystemFlagSprite;

public class MapOverlayColonizePrompt extends MapOverlay {
    private static final int nameLengthLimit = 24;
    private static final Color dlgBox = new Color(123,123,123,192);
    private static final Color maskC = new Color(40,40,40,160);
    
    private final TextButtonSprite yesButton =
            new TextButtonSprite("MAIN_COLONIZE_YES", true, this::colonizeYes);
    private final TextButtonSprite noButton =
            new TextButtonSprite("MAIN_COLONIZE_NO", false, this::colonizeNo);
    private final SystemFlagSprite flagButton;
    private final MainUI parent;
    
    private Area mask;
    private BufferedImage planetImg;
    private int sysId;
    private String sysName;
    private ShipFleet fleet;
    private boolean isOpen = false;
    
    public MapOverlayColonizePrompt(MainUI p) {
        parent = p;
        flagButton = new SystemFlagSprite(p::repaint);
    }
    
    public void init(int systemId, ShipFleet fl) {
        StarSystem sys = galaxy().system(systemId);
        sysId = systemId;
        sysName = player().sv.name(sysId);
        fleet = fl;
        flagButton.setSystemId(systemId);
        isOpen = true;
        parent.hideDisplayPanel();
        parent.map().setScale(20);
        parent.map().recenterMapOn(sys);
        parent.mapFocus(sys);
        parent.clickedSprite(sys);
        parent.repaint();
    }
    
    public void colonizeYes() {
        if (isOpen) {
            isOpen = false;
            mask = null;
            planetImg = null;
            softClick();
            parent.clearOverlay();
            parent.repaintAllImmediately();
            
            player().sv.name(sysId, sysName);
            StarSystem system = galaxy().system(sysId);
            fleet.colonizeSystem(system);
            advanceMap();
        }
    }
    public void colonizeNo() {
        if (isOpen) {
            isOpen = false;
            mask = null;
            planetImg = null;
            advanceMap();
        }
    }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        parent.resumeTurn();
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
        
        // print prompt string
        String promptStr = text("MAIN_COLONIZE_PROMPT");
        int promptFontSize = scaledFont(g, promptStr, x1-s30, 24, 14);
        g.setFont(narrowFont(promptFontSize));
        drawShadowedString(g, promptStr, 4, x1, boxY+boxH1-s40, SystemPanel.textShadowC, Color.white);
        
        String scoutStr = text("MAIN_COLONIZE_TITLE", sysName);
        g.setColor(Color.darkGray);
        g.setFont(narrowFont(16));
        drawString(g,scoutStr, x1, boxY+boxH1-s20);

        // draw planet info, from bottom up
        int y1 = boxY+boxH-buttonPaneH-s10;
        int lineH = s20;
        int desiredFont = 18;

        if (pl.sv.isUltraPoor(sysId)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_ULTRA_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isPoor(sysId)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isRich(sysId)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isUltraRich(sysId)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ULTRA_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.sv.isOrionArtifact(sysId)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ANCIENTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isArtifact(sysId)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ARTIFACTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (player().isEnvironmentHostile(sys)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_HOSTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (player().isEnvironmentFertile(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_FERTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 14);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (player().isEnvironmentGaia(sys)) {
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
        
        // "Text box"
        y1 -= scaled(5);
        g.setColor(dlgBox);
        int textXPad = BasePanel.s4;
        int textTopPad = BasePanel.s4;
        int textBottomPad = BasePanel.s3;
        int textWidth = boxW - s15 - flagButton.getWidth();
        g.fillRect(x1 - textXPad, y1 - s30 - textTopPad,
                textWidth + textXPad * 2, s40 + textBottomPad);
        
        // planet name
        g.setFont(narrowFont(40));
        drawBorderedString(g, sysName, 1, x1, y1, Color.darkGray, SystemPanel.orangeText);

        // planet flag
        parent.addNextTurnControl(flagButton);
        flagButton.setPosition(boxX+boxW-flagButton.getWidth()+s10, boxY+boxH-buttonPaneH-flagButton.getHeight()+s10);
        flagButton.draw(parent.map(), g);
        
        // draw no button
        parent.addNextTurnControl(noButton);
        noButton.refreshSize(g);
        noButton.setPosition(x0+w0-noButton.getWidth()-s10, y0+h0-noButton.getHeight()-s10);
        noButton.draw(parent.map(), g);
        
        // draw yes button
        parent.addNextTurnControl(yesButton);
        yesButton.refreshSize(g);
        yesButton.setPosition(x0+s10, y0+h0-yesButton.getHeight()-s10);
        yesButton.draw(parent.map(), g);
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        return true;
    }
    @Override
    public boolean handleKeyTyped(KeyEvent e) {
        char keyChar = e.getKeyChar();
        if (Character.isLetterOrDigit(keyChar) || ' ' == keyChar) {
            if (sysName.length() < nameLengthLimit) {
                sysName += e.getKeyChar();
            }
        } else if ('\b' == keyChar && sysName.length() != 0) {
            sysName = sysName.substring(0, sysName.length()-1);
        }
        
        parent.repaint();
        return true;
    }
}

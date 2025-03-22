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
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import rotp.model.Sprite;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.TextButtonSprite;

public class MapOverlayAllocateSystems extends MapOverlay {
    private final TextButtonSprite prevSystemButton =
            new TextButtonSprite("MAIN_ALLOCATE_PREV_SYSTEM", false, this::previousSystem);
    private final TextButtonSprite nextSystemButton =
            new TextButtonSprite("MAIN_ALLOCATE_NEXT_SYSTEM", true, this::nextSystem);
    private final TextButtonSprite continueButton =
            new TextButtonSprite("MAIN_ALLOCATE_CLOSE", false, this::advanceMap);
    private final MainUI parent;
    
    private HashMap<StarSystem,List<String>> systemsToAllocate = new HashMap<>();
    private List<StarSystem> orderedSystems = new ArrayList<>();
    private LinearGradientPaint arrowBack;
    private int systemIndex = 0;
    private int x[] = new int[9];
    private int y[] = new int[9];
    private boolean drawSprites = false;

    public MapOverlayAllocateSystems(MainUI p) {
        parent = p;
    }
    
    public void init(HashMap<StarSystem,List<String>> newSystems) {
        drawSprites = true;
        systemsToAllocate = newSystems;
        orderedSystems.clear();
        systemIndex = 0;
        parent.hoveringOverSprite(null);
        if (newSystems.isEmpty())
            advanceMap();
        else {
            // create an alphabetized list of systems
            orderedSystems.addAll(newSystems.keySet());
            Collections.sort(orderedSystems, StarSystem.NAME);
            mapSelectIndex(0);
        }
    }
    
    private void mapSelectIndex(int i) {
        StarSystem nextSystem = orderedSystems.get(i);
        parent.map().recenterMapOn(nextSystem);
        parent.mapFocus(nextSystem);
        parent.clickedSprite(nextSystem);
        parent.showDisplayPanel();
        parent.repaint();
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
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        if (drawSprites) {
            drawSprites = false;
            if (!systemsToAllocate.isEmpty()) {
                systemsToAllocate.clear();
                orderedSystems.clear();
            }
            parent.hideDisplayPanel();
            parent.resumeTurn();
        }
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        int s3 = BasePanel.s3;
        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s30 = BasePanel.s30;

        int x0 = ui.getWidth()-scaled(680);
        int y0 = scaled(215);
        int w0 = scaled(420);
        int h0 = scaled(235);
        g.setColor(MainUI.paneShadeC2);
        g.fillRect(x0, y0, w0, h0);

        int x1 = x0 + s7;
        int y1 = y0 + s7;
        int w1 = w0 - s7 - s7;
        int h1 = scaled(50);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x1, y1, w1, h1);

        int x2 = x1;
        int y2 = y1+h1+s3;
        int w2 = w1;
        int h2 = scaled(127);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x2, y2, w2, h2);

        // draw title
        String titleStr = text("MAIN_ALLOCATE_TITLE");
        g.setFont(narrowFont(22));
        int x1b = x1+s10;
        drawShadowedString(g, titleStr, 3, x1b, y1+s30, SystemPanel.textShadowC, SystemPanel.whiteText);

        //draw arrow
        int r1 = x1+w1;
        int b1 = y2+h2;
        x[0]=r1;    x[1]=x[0]-s30;x[2]=x[1]-s25;x[3]=x[2]+s20;x[4]=x1;  x[5]=x[4];  x[6]=x[3];x[7]=x[6]-s10;x[8]=x[7]+s25;
        y[0]=b1-s20;y[1]=y[0]-s30;y[2]=y[1];    y[3]=b1-s30;  y[4]=y[3];y[5]=b1-s10;y[6]=y[5];y[7]=b1;      y[8]=b1;

        if (arrowBack == null) {
            float[] dist = {0.0f, 0.1f, 0.6f, 1.0f};
            Point2D arrowL = new Point2D.Float(x1, 0);
            Point2D arrowR = new Point2D.Float(r1, 0);
            Color[] arrowColors = {SystemPanel.orangeClear, SystemPanel.orangeClear, SystemPanel.orangeText, SystemPanel.orangeText };
            arrowBack = new LinearGradientPaint(arrowL, arrowR, dist, arrowColors);
        }
        g.setPaint(arrowBack);
        g.fillPolygon(x,y,x.length);

        //draw text in arrow
        String actionStr = text("MAIN_ALLOCATE_CHANGE_SPENDING");
        g.setFont(narrowFont(14));
        int sw2 = g.getFontMetrics().stringWidth(actionStr);
        int x1d = x[6]-sw2;
        int y1d = y[6]-scaled(5);
        g.setColor(SystemPanel.blackText);
        drawString(g,actionStr, x1d, y1d);

        // draw reasons info
        StarSystem sv = orderedSystems.get(systemIndex);
        List<String> reasons = systemsToAllocate.get(sv);
        int lineH = BasePanel.s15;
        int x2a = x2+s10;
        int y2a = y2+BasePanel.s15;

        int textW = w2+x2-x2a-s10;
        g.setFont(narrowFont(15));
        for (String reason: reasons) {
            List<String> lines = this.wrappedLines(g, reason, textW);
            for (String line: lines) {
                drawString(g,line, x2a, y2a);
                y2a += lineH;
            }
        }

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
            default:
                return false;
        }
        return true;
    }
}

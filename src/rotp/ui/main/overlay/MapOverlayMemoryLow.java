/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2025 Ilya Zushinskiy
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

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.TextButtonSprite;

public class MapOverlayMemoryLow extends MapOverlay {
    private final TextButtonSprite restartButton =
            new TextButtonSprite("MAIN_MEMORY_LOW_RESTART", true, this::restart);
    private final TextButtonSprite skipButton =
            new TextButtonSprite("MAIN_MEMORY_LOW_SKIP", true, this::skip);
    private final MainUI parent;
    
    public MapOverlayMemoryLow(MainUI p) {
        parent = p;
    }
    
    public void restart() {
        Rotp.restartFromLowMemory();
    }
    public void skip() {
        parent.clearOverlay();
    }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() { }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        int s3 = BasePanel.s3;
        int s7 = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s35 = BasePanel.s35;

        int x0 = scaled(330);
        int y0 = scaled(165);
        int w0 = scaled(420);
        int h0 = scaled(335);
        g.setColor(MainUI.paneShadeC2);
        g.fillRect(x0, y0, w0, h0);

        int x1 = x0 + s7;
        int y1 = y0 + s7;
        int w1 = w0 - s7 - s7;
        int h1 = scaled(65);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x1, y1, w1, h1);

        int x2 = x1;
        int y2 = y1+h1+s3;
        int w2 = w1;
        int h2 = scaled(212);
        g.setColor(MainUI.paneBackground);
        g.fillRect(x2, y2, w2, h2);

        // draw title
        String titleStr = text("MAIN_MEMORY_LOW_TITLE");
        g.setFont(narrowFont(22));
        int sw1 = g.getFontMetrics().stringWidth(titleStr);
        int x1a = x1+(w1-sw1)/2;
        drawShadowedString(g, titleStr, 3, x1a, y1+h1-s35, SystemPanel.textShadowC, SystemPanel.whiteText);

        int lineH = BasePanel.s18;
        int x2a = x2+s10;
        int y2a = y2+s20;

        int textW = w2+x2-x2a-s10;
        String desc1 = text("MAIN_MEMORY_LOW_DESC");
        g.setFont(narrowFont(16));
        List<String> lines = wrappedLines(g, desc1, textW);
        for (String line: lines) {
            drawString(g,line, x2a, y2a);
            y2a += lineH;
        }

        y2a += s10;
        String desc2 = text("MAIN_MEMORY_LOW_DESC_2");
        g.setFont(narrowFont(16));
        lines = wrappedLines(g, desc2, textW);
        for (String line: lines) {
            drawString(g,line, x2a, y2a);
            y2a += lineH;
        }


        y2a += s10;
        String desc3 = text("MAIN_MEMORY_LOW_DESC_3");
        g.setFont(narrowFont(16));
        lines = wrappedLines(g, desc3, textW);
        for (String line: lines) {
            drawString(g,line, x2a, y2a);
            y2a += lineH;
        }

        // init and draw continue button sprite
        parent.addNextTurnControl(restartButton);
        restartButton.refreshSize(g);
        int buttonY = y0+h0-restartButton.getHeight()-s10;
        restartButton.setPosition(x0+s10, buttonY);
        restartButton.draw(parent.map(), g);

        parent.addNextTurnControl(skipButton);
        skipButton.refreshSize(g);
        skipButton.setPosition(x0+s25+restartButton.getWidth(), buttonY);
        skipButton.draw(parent.map(), g);
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                //softClick();
                skip();
                break;
            default:
                misClick();
                break;
        }
        return true;
    }
}


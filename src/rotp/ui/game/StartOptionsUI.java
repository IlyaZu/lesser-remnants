/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023 Ilya Zushinskiy
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
package rotp.ui.game;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import rotp.ui.BasePanel;
import rotp.ui.BaseText;
import rotp.ui.main.SystemPanel;

public class StartOptionsUI extends BasePanel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final Color backgroundHaze = new Color(0,0,0,160);
    
    public static final Color lightBrown = new Color(178,124,87);
    public static final Color brown = new Color(141,101,76);
    public static final Color darkBrown = new Color(112,85,68);
    public static final Color darkerBrown = new Color(75,55,39);
    
    Rectangle hoverBox;
    Rectangle okBox = new Rectangle();
    Rectangle defaultBox = new Rectangle();
    BasePanel parent;
    BaseText galaxyAgeText;
    BaseText randomEventsText;
    
    public StartOptionsUI() {
        setOpaque(false);
        Color textC = SystemPanel.whiteText;
        galaxyAgeText = new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        randomEventsText = new BaseText(this, false, 20, 20,-78,  textC, textC, hoverC, depressedC, textC, 0, 0, 0);
        addMouseListener(this);
        addMouseMotionListener(this);
    }
    public void init() {
        galaxyAgeText.displayText(galaxyAgeStr());
        randomEventsText.displayText(randomEventsStr());
    }
    public void open(BasePanel p) {
        parent = p;
        init();
        enableGlassPane(this);
    }
    public void close() {
        disableGlassPane();
    }
    public void setToDefault() {
        newGameOptions().setToDefault();
        init();
        repaint();
    }
    @Override
    public void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        
        int w = getWidth();
        int h = getHeight();
        Graphics2D g = (Graphics2D) g0;
        
        
        // draw background "haze"
        g.setColor(backgroundHaze);
        g.fillRect(0, 0, w, h);
        
        int numColumns = 3;
        int columnPad = s20;
        int lineH = s17;
        Font descFont = narrowFont(15);
        int leftM = s100;
        int rightM = s100;
        int topM = s45;
        int w1 = w-leftM-rightM;
        int h1 = h-topM-s45;
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(leftM, topM, w1, h1);
        String title = text("SETTINGS_TITLE");
        g.setFont(narrowFont(30));
        int sw = g.getFontMetrics().stringWidth(title);
        int x1 = leftM+((w1-sw)/numColumns);
        int y1 = topM+s40;
        drawBorderedString(g, title, 1, x1, y1, Color.black, Color.white);
        
        g.setFont(narrowFont(18));
        String expl = text("SETTINGS_DESCRIPTION");
        g.setColor(SystemPanel.blackText);
        drawString(g,expl, leftM+s10, y1+s20);
        
        Stroke prev = g.getStroke();
        g.setStroke(stroke3);

        
        // left column
        int y2 = topM+scaled(110);
        int x2 = leftM+s10;
        int w2 = (w1/numColumns)-columnPad;
        int h2 = s90;
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, galaxyAgeText.stringWidth(g)+s10,s30);
        galaxyAgeText.setScaledXY(x2+s20, y2+s7);
        galaxyAgeText.draw(g);
        String desc = text("SETTINGS_GALAXY_AGE_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        List<String> lines = this.wrappedLines(g,desc, w2-s30);
        int y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        // Gap made by star density removal
        y2 += (h2+s20);      
       
        // Gap made by nebula frequency settings removal
        y2 += (h2+s20);
        
        // Gap made by planet quality settings removal
        y2 += (h2+s20);
        
        // Gap made by terraforming settings removal
        y2 += (h2+s20);
        
        // middle column
        y2 = topM+scaled(110);
        x2 = x2+w2+s20;
        h2 = s90;
        g.setColor(SystemPanel.blackText);
        g.drawRect(x2, y2, w2, h2);
        g.setPaint(GameUI.settingsSetupBackground(w));
        g.fillRect(x2+s10, y2-s10, randomEventsText.stringWidth(g)+s10,s30);
        randomEventsText.setScaledXY(x2+s20, y2+s7);
        randomEventsText.draw(g);
        desc = text("SETTINGS_RANDOM_EVENTS_DESC");
        g.setColor(SystemPanel.blackText);
        g.setFont(descFont);
        lines = this.wrappedLines(g,desc, w2-s30);
        y3 = y2+s10;
        for (String line: lines) {
            y3 += lineH;
            drawString(g,line, x2+s20, y3);
        }
        
        // Gap made by AI hostility settings removal
        y2 += (h2+s20);     
       
        // Gap made by Council rebellion removal
        y2 += (h2+s20);
        
        // Gap made by AI ability/personality randomisation options
        y2 += (h2+s20);
        
        // Gap made by autoplay removal
        y2 += (h2+s20);
        
        // right side
        // Gap made by research speed settings removal
        y2 = topM+scaled(110);
        h2 = s90;
        x2 = x2+w2+s20;
        
        // Gap made by warp speed settings removal
        y2 += (h2+s20);
        
        // Gap made by fuel range settings removal
        y2 += (h2+s20);

        // Gap made by tech trading settings removal
        y2 += (h2+s20);

        // Gap made by colonisation restriction settings removal
        y2 += (h2+s20);

        g.setStroke(prev);

        // draw settings button
        int y4 = scaled(690);
        int cnr = s5;
        int smallButtonH = s30;
        int smallButtonW = scaled(180);
        okBox.setBounds(w-scaled(289), y4, smallButtonW, smallButtonH);
        g.setColor(GameUI.buttonBackgroundColor());
        g.fillRoundRect(okBox.x, okBox.y, smallButtonW, smallButtonH, cnr, cnr);
        g.setFont(narrowFont(20));
        String text6 = text("SETTINGS_EXIT");
        int sw6 = g.getFontMetrics().stringWidth(text6);
        int x6 = okBox.x+((okBox.width-sw6)/2);
        int y6 = okBox.y+okBox.height-s8;
        Color c6 = hoverBox == okBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text6, 2, x6, y6, GameUI.borderDarkColor(), c6);
        prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(okBox.x, okBox.y, okBox.width, okBox.height, cnr, cnr);
        g.setStroke(prev);

        String text7 = text("SETTINGS_DEFAULT");
        int sw7 = g.getFontMetrics().stringWidth(text7);
        smallButtonW = sw7+s30;
        defaultBox.setBounds(okBox.x-smallButtonW-s30, y4, smallButtonW, smallButtonH);
        g.setColor(GameUI.buttonBackgroundColor());
        g.fillRoundRect(defaultBox.x, defaultBox.y, smallButtonW, smallButtonH, cnr, cnr);
        g.setFont(narrowFont(20));
        int x7 = defaultBox.x+((defaultBox.width-sw7)/2);
        int y7 = defaultBox.y+defaultBox.height-s8;
        Color c7 = hoverBox == defaultBox ? Color.yellow : GameUI.borderBrightColor();
        drawShadowedString(g, text7, 2, x7, y7, GameUI.borderDarkColor(), c7);
        prev = g.getStroke();
        g.setStroke(stroke1);
        g.drawRoundRect(defaultBox.x, defaultBox.y, defaultBox.width, defaultBox.height, cnr, cnr);
        g.setStroke(prev);
    }
    private String galaxyAgeStr() {
        String opt = text(newGameOptions().selectedGalaxyAge());
        return text("SETTINGS_GALAXY_AGE", opt)+"   ";
    }
    private String randomEventsStr() {
        String opt = text(newGameOptions().selectedRandomEventOption());
        return text("SETTINGS_RANDOM_EVENTS", opt)+"   ";
    }
    private void toggleGalaxyAge() {
        softClick();
        newGameOptions().selectedGalaxyAge(newGameOptions().nextGalaxyAge());
        galaxyAgeText.repaint(galaxyAgeStr());
    }
    private void toggleRandomEvents() {
        softClick();
        newGameOptions().selectedRandomEventOption(newGameOptions().nextRandomEventOption());
        randomEventsText.repaint(randomEventsStr());
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                close();
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_ENTER:
                parent.advanceHelp();
                break;
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {  }
    @Override
    public void mouseMoved(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Rectangle prevHover = hoverBox;
        hoverBox = null;
        if (galaxyAgeText.contains(x,y))
            hoverBox = galaxyAgeText.bounds();
        else if (randomEventsText.contains(x,y))
            hoverBox = randomEventsText.bounds();
        else if (okBox.contains(x,y))
            hoverBox = okBox;
        else if (defaultBox.contains(x,y))
            hoverBox = defaultBox;
		
        if (hoverBox != prevHover) {
            if (prevHover == galaxyAgeText.bounds())
                galaxyAgeText.mouseExit();
            else if (prevHover == randomEventsText.bounds())
                randomEventsText.mouseExit();
            if (hoverBox == galaxyAgeText.bounds())
                galaxyAgeText.mouseEnter();
            else if (hoverBox == randomEventsText.bounds())
                randomEventsText.mouseEnter();
            if (prevHover != null)
                repaint(prevHover);
            if (hoverBox != null)
                repaint(hoverBox);
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) { }
    @Override
    public void mousePressed(MouseEvent e) { }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        if (hoverBox == null)
            return;
        if (hoverBox == galaxyAgeText.bounds())
            toggleGalaxyAge();
        else if (hoverBox == randomEventsText.bounds())
            toggleRandomEvents();
        else if (hoverBox == okBox)
            close();
        else if (hoverBox == defaultBox)
            setToDefault();
    }
    @Override
    public void mouseEntered(MouseEvent e) { }
    @Override
    public void mouseExited(MouseEvent e) {
        if (hoverBox != null) {
            hoverBox = null;
            repaint();
        }
    }

}

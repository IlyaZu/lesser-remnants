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
package rotp.ui.main;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.swing.SwingUtilities;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;

public class AlienSystemPanel extends SystemPanel {
    private static final long serialVersionUID = 1L;
    static final Color textColor = new Color(20,20,20);
    static final Color dataBorders = new Color(160,160,160);

    public AlienSystemPanel(SpriteDisplayPanel p) {
        parentSpritePanel = p;
        initModel();
    }
    @Override
    public void animate()            { overviewPane.animate(); }
    @Override
    protected BasePanel topPane()    { return new SystemViewInfoPane(this); }
    @Override
    protected BasePanel bottomPane() { return new SystemRangePane(this); }
    @Override
    protected BasePanel detailPane() {
        return new DetailPane(this);
    }
    private class DetailPane extends BasePanel implements MouseMotionListener, MouseListener, MouseWheelListener {
        private static final long serialVersionUID = 1L;
        SystemPanel parent;
        Empire displayEmp;
        Rectangle nameBox = new Rectangle();
        Rectangle flagBox = new Rectangle();
        Shape hoverBox;

        DetailPane(SystemPanel p) {
            parent = p;
            init();
        }
        private void init() {
            setOpaque(false);
            addMouseMotionListener(this);
            addMouseListener(this);
            addMouseWheelListener(this);
        }
        @Override
        public void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0;
            nameBox.setBounds(0,0,0,0);
            displayEmp = null;
            StarSystem sys = parent.systemViewToDisplay();
            if (sys == null)
                return;

            int id = sys.id;
            Empire pl = player();
            displayEmp = pl.sv.empire(id);
            if (displayEmp == null)
                return;

            boolean spied = pl.sv.isSpied(id);

            super.paintComponent(g);
            int h = getHeight();
            int w = getWidth();

            int topH1 = s40;
            int topH = s90;
            // draw colony info box
            g.setColor(MainUI.paneBackground());
            g.fillRect(0, 0, w, topH-s5);
            GradientPaint back = new GradientPaint(0,0,displayEmp.color(),w, 0,MainUI.transC);
            g.setPaint(back);
            g.fillRect(0, 0, w, topH1-s5);
            g.setPaint(null);
            g.setColor(MainUI.shadeBorderC());
            g.fillRect(0, topH-s5, w, s6);

            //  colony name
            g.setFont(narrowFont(24));
            String empName = pl.sv.descriptiveName(id);
            int sw = g.getFontMetrics().stringWidth(empName);
            Color c0 = nameBox == hoverBox ? Color.yellow : SystemPanel.whiteLabelText;
            drawShadowedString(g, empName, 2, s10, topH1-s15, MainUI.shadeBorderC(), c0);
            nameBox.setBounds(s10, topH1-s40, sw+s5,s25);

            // draw system banner
            int sz = s70;
            Image flagImage = parentSpritePanel.parent.flagImage(sys);
            g.drawImage(flagImage, w-sz+s15, topH1-sz+s10, sz, sz, null);
            if (hoverBox == flagBox) {
                Image hoverImage = parentSpritePanel.parent.flagHover(sys);
                g.drawImage(hoverImage, w-sz+s15, topH1-sz+s10, sz, sz, null);
            }
            flagBox.setBounds(w-sz+s25,topH1-sz+s10,sz-s20,sz-s10);
            
            // colony data
            String unknown = text("RACES_UNKNOWN_DATA");
            String factLbl = text("MAIN_COLONY_FACTORIES");
            String baseLbl = text("MAIN_COLONY_BASES");
            String shieldLbl = text("MAIN_COLONY_SHIELD");
            String popLbl = text("MAIN_COLONY_POPULATION");

            int x0 = s5;
            int x1 = w/2;
            int y0 = topH-s37;
            int y1 = topH-s12;

            g.setFont(narrowFont(16));
            g.setColor(textColor);
            drawString(g,popLbl, x0, y0);
            drawString(g,factLbl, x1, y0);
            drawString(g,shieldLbl, x0, y1);
            drawString(g,baseLbl, x1, y1);

            String str = spied ? str(pl.sv.population(id)) : unknown;
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x1-sw-s10, y0);
            str = spied ? str(pl.sv.factories(id)) : unknown;
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, w-s10-sw, y0);
            str = spied ? str(pl.sv.shieldLevel(id)) : unknown;
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, x1-s10-sw, y1);
            str = spied ? str(pl.sv.bases(id)) : unknown;
            sw = g.getFontMetrics().stringWidth(str);
            drawString(g,str, w-s10-sw, y1);

            // draw borders around data
            g.setColor(dataBorders);
            Stroke prevStroke = g.getStroke();
            g.setStroke(stroke1);
            //g.drawLine(0, y0-s18, w, y0-s18);
            g.drawLine(0, y1-s18, w, y1-s18);
            g.drawLine(x1-s5, y0-s18, x1-s5, topH-s6);
            g.setStroke(prevStroke);

            // draw planet terrain background
            BufferedImage img = pl.sv.planetTerrain(id);
            g.drawImage(img, 0, topH, w, h, 0, 0, img.getWidth(), img.getHeight(), null);
            g.setFont(narrowFont(16));
            g.setColor(grayText);

            String desc = pl.sv.planetType(id).description(pl);
            List<String> descLines =  wrappedLines(g, text(desc), getWidth()-s12);

            int ydelta = s18;
            int y2=h-s8-(ydelta*(descLines.size()-1));
            for (String line: descLines) {
                drawBorderedString(g, line, s8, y2, Color.black, whiteText);
                y2 += ydelta;
            }
        }
        @Override
        public void mouseDragged(MouseEvent e) { }
        @Override
        public void mouseMoved(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            Shape prevHover = hoverBox;
            hoverBox = null;
            if (flagBox.contains(x,y))
                hoverBox = flagBox;
            else if (nameBox.contains(x,y))
                hoverBox = nameBox;

            if (prevHover != hoverBox)
                repaint();
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) {
            boolean rightClick = SwingUtilities.isRightMouseButton(e);
            if (hoverBox == flagBox) {
                StarSystem sys = parentSpritePanel.systemViewToDisplay();
                if (rightClick)
                    player().sv.resetFlagColor(sys.id);
                else
                    player().sv.toggleFlagColor(sys.id);
                parentSpritePanel.parent.repaint();
            }
            else if (hoverBox == nameBox) {
                RotPUI.instance().selectRacesPanel();
                RotPUI.instance().racesUI().selectDiplomacyTab();
                RotPUI.instance().racesUI().selectedEmpire(displayEmp);              
            }
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
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (hoverBox == flagBox) {
                StarSystem sys = parentSpritePanel.systemViewToDisplay();
                if (e.getWheelRotation() < 0)
                    player().sv.toggleFlagColor(sys.id, true);
                else
                    player().sv.toggleFlagColor(sys.id, false);
                parentSpritePanel.repaint();
            }
        }
    }
}

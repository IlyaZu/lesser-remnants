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
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import rotp.Rotp;
import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.empires.SabotageMission;
import rotp.model.empires.SpyReport;
import rotp.model.tech.Tech;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.sprites.TextButtonSprite;
import rotp.ui.sprites.MapSprite;

public class MapOverlaySpies extends MapOverlay {
    private static final Color maskC  = new Color(40,40,40,160);

    private final List<Empire> empires = new ArrayList<>();
    private final List<EmpireTabSprite> tabs = new ArrayList<>();
    private final TextButtonSprite closeButton =
            new TextButtonSprite("NOTICE_SPIES_CLOSE", false, this::advanceMap);
    private final TextButtonSprite intelButton =
            new TextButtonSprite("NOTICE_SPIES_MANAGE", false, this::manageSpies);
    private final MainUI parent;
    
    private BufferedImage labImg;
    private Empire selectedEmpire;
    
    public MapOverlaySpies(MainUI p) {
        parent = p;
    }
    
    public boolean shouldDisplay() {
        return !empires.isEmpty();
    }
    public void init() {
        labImg = null;
        empires.clear();
        tabs.clear();
        List<Empire> allEmpires = player().contactedEmpires();
        for (Empire emp: allEmpires) {
            EmpireView v = player().viewForEmpire(emp.id);
            SpyReport rpt = v.spies().report();
            if (rpt.hasActivity())
                empires.add(emp);
        }
        Collections.sort(empires, Empire.RACE_NAME);
        selectedEmpire = empires.isEmpty() ? null : empires.get(0);
        for (Empire e: empires)
            tabs.add(new EmpireTabSprite(this, e));
    }
    public void selectEmpire(Empire e) {
        selectedEmpire = e;
        parent.repaint();
    }
    public void manageSpies() {
        RotPUI.instance().selectRacesPanel();
        RotPUI.instance().racesUI().selectIntelligenceTab();
        RotPUI.instance().racesUI().selectedEmpire(selectedEmpire);
        parent.hoveringOverSprite(null);
    }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_C:
                advanceMap();
                break;
            case KeyEvent.VK_M:
                if (selectedEmpire != null)
                    manageSpies();
                break;
            case KeyEvent.VK_TAB:
                if (empires.size()> 1) {
                    int nextI = empires.indexOf(selectedEmpire);
                    if (e.isShiftDown()) {
                        nextI--;
                        if (nextI < 0)
                            nextI = empires.size()-1;
                    }
                    else {
                        nextI++;
                        if (nextI >= empires.size())
                            nextI = 0;
                    }
                    selectEmpire(empires.get(nextI));
                    break;
                }
            default:
                misClick(); break;
        }
        return true;
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        int h = ui.getHeight()-BasePanel.s30;
        Empire pl = player();
        
        int bdr = BasePanel.s7;
        int s10 = BasePanel.s10;

        parent.hideDisplayPanel();
        // draw map mask
        g.setColor(maskC);
        g.fillRect(0,0,ui.getWidth(),ui.getHeight());
        
        int extraEmps = min(0, max(6,24-empires.size()));
        int tabW = empires.isEmpty() ? 0 : scaled(100);
        int h0 = scaled(450) +(extraEmps*BasePanel.s24);
        int x0 = scaled(250)+(tabW/2);
        int y0 = (h-h0)/2;
        int w0 = scaled(580)+tabW;
        g.setColor(MainUI.paneShadeC2);
        g.fillRect(x0, y0, w0, h0);

        int leftW = scaled(250);

        int x1 = x0 + bdr;
        int y1 = y0 + bdr;
        int w1 = w0 - tabW - bdr - bdr;
        int h1 = h0 - bdr - bdr;
        int infoW = w1-leftW;

        int x2 = x1+leftW+s10;
        int x3 = x1+leftW+infoW;

        g.setColor(MainUI.paneBackground);
        g.fillRect(x1, y1, w1, h1);

        closeButton.refreshSize(g);
        int buttonY = y1+h1-closeButton.getHeight()-BasePanel.s5;
        int closeW = closeButton.getWidth();
        int closeX = x1+w1-closeW-BasePanel.s5;
        closeButton.setPosition(closeX, buttonY);
        
        intelButton.refreshSize(g);
        int intelW = intelButton.getWidth();
        int intelX = closeX-intelW-BasePanel.s5;
        intelButton.setPosition(intelX, buttonY);

        // draw title
        String subtitle = text("NOTICE_SPIES_TITLE");
        g.setFont(narrowFont(26));
        int sw = g.getFontMetrics().stringWidth(subtitle);
        int x1a = x1+((leftW-sw)/2);
        drawShadowedString(g, subtitle, 3, x1a, y1+BasePanel.s45, SystemPanel.textShadowC, Color.white);

        if (labImg == null) {
            labImg = asBufferedImage(pl.race().laboratory());
            Graphics imgG = labImg.getGraphics();
            BufferedImage spyImg = pl.race().spyQuiet();
            int w0a = labImg.getWidth();
            int h0a = labImg.getHeight();
            imgG.drawImage(spyImg, w0a/2, 0, w0a, h0a, 0, 0, (spyImg.getWidth()/2), spyImg.getHeight(), null);
            imgG.dispose();
        }

        int y1b = y1+BasePanel.s100;
        int h1b = h1-BasePanel.s100;
        int imgH = h1b;
        int imgW = imgH*Rotp.IMG_W/Rotp.IMG_H;
        g.setClip(x1+BasePanel.s5,y1b,leftW-BasePanel.s5,h1b-BasePanel.s5);
        g.drawImage(labImg, x1+leftW-imgW, y1b, x1+leftW, y1b+h1b, 0, 0, labImg.getWidth(), labImg.getHeight(), null);
        g.setClip(null);

        closeButton.draw(ui, g);
        parent.addNextTurnControl(closeButton);

        int descW = infoW-BasePanel.s20;
        int lineH = BasePanel.s18;

        if (selectedEmpire == null) {
            int y1a = y1+BasePanel.s100;
            String none = text("NOTICE_SPIES_NO_ACTIVITY");
            g.setColor(SystemPanel.whiteText);
            g.setFont(narrowFont(20));
            List<String> lines = wrappedLines(g, none, descW);
            for (String line: lines) {
                y1a += lineH;
                drawString(g,line, x2, y1a);
            }
            return;
        }
        
        intelButton.draw(ui, g);
        parent.addNextTurnControl(intelButton);

        EmpireView v = pl.viewForEmpire(selectedEmpire.id);
        
        // draw selected empire name
        int y2 = y1+BasePanel.s25;
        g.setFont(narrowFont(26));
        drawShadowedString(g, selectedEmpire.name(), 3, x2, y2, SystemPanel.textShadowC, Color.white);
        
        // draw treaty status
        y2 = y2 + BasePanel.s24;
        g.setFont(narrowFont(20));
        g.setColor(SystemPanel.blackText);
        drawString(g,v.embassy().treatyStatus(), x2, y2);

        g.setColor(SystemPanel.blackText);
        // draw spies caught
        SpyReport rpt = v.spies().report();
        int ourSpiesLost = rpt.spiesLost();
        if (ourSpiesLost > 0) {
            y2 += lineH;
            String desc = text("NOTICE_SPIES_LOST_DESC", str(ourSpiesLost));
            desc = selectedEmpire.replaceTokens(desc, "alien");
            if (rpt.confessedMission() != null) {
                switch(rpt.confessedMission()) {
                    case SABOTAGE: desc = concat(desc," ", text("NOTICE_SPIES_LOST_CONFESSED")); break;
                    case ESPIONAGE: desc = concat(desc," ", text("NOTICE_SPIES_LOST_CONFESSED2")); break;
                    case HIDE:
                        if (selectedEmpire.leader().isXenophobic())
                            desc = concat(desc, " ", text("NOTICE_SPIES_LOST_CONFESSED3")); break;
                }
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW);
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
        
        // show enemy spies that we caught
        int theirSpiesLost = rpt.spiesCaptured();
        if (theirSpiesLost > 0) {
            SpyReport theirRpt = v.otherView().spies().report();
            y2 += lineH;
            String desc = text("NOTICE_SPIES_CAUGHT_DESC", str(theirSpiesLost));
            desc = selectedEmpire.replaceTokens(desc, "alien");
            if (theirRpt.confessedMission() != null) {
                switch(theirRpt.confessedMission()) {
                    case SABOTAGE: desc = concat(desc," ", text("NOTICE_SPIES_CAUGHT_CONFESSED")); break;
                    case ESPIONAGE: desc = concat(desc," ", text("NOTICE_SPIES_CAUGHT_CONFESSED2")); break;
                }
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW);
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
        
        // show any sabotage
        if (rpt.sabotageCount() > 0) {
            y2 += lineH;
            String desc = "";
            switch (rpt.sabotageMission()) {
                case SabotageMission.BASES:
                    desc = text("NOTICE_SPIES_SABOTAGE_BASES", str(rpt.sabotageCount()), pl.sv.name(rpt.sabotageSystem));
                    break;
                case SabotageMission.FACTORIES:
                    desc = text("NOTICE_SPIES_SABOTAGE_FACTORIES", str(rpt.sabotageCount()), pl.sv.name(rpt.sabotageSystem));
                    break;
                case SabotageMission.REBELLION:
                    desc = text("NOTICE_SPIES_SABOTAGE_REBELS", str(rpt.sabotageCount()), pl.sv.name(rpt.sabotageSystem));
                    break;
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW);
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
                
        // show any techs we stole
        if (rpt.stolenTech() != null) {
            y2 += lineH;
            Tech t = tech(rpt.stolenTech());
            Empire framed = rpt.framedEmpire();
            String desc = text("NOTICE_SPIES_ESPIONAGE", t.name());
            desc = selectedEmpire.replaceTokens(desc, "alien");
            if (framed != null) {
                desc = concat(desc, " ", text("NOTICE_SPIES_ESPIONAGE_FRAME"));
                desc = framed.replaceTokens(desc, "framed");
            }
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW);
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
                
        // informs if we were framed
        if (rpt.wasFramed()) {
            y2 += lineH;
            String desc = text("NOTICE_SPIES_FRAMED");
            desc = selectedEmpire.replaceTokens(desc, "alien");
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW);
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }

        // show list of techs learned
        if (!rpt.techsLearned().isEmpty()) {
            y2 += lineH;
            List<String> techNames = new ArrayList<>();
            for (String tId : rpt.techsLearned())
                techNames.add(tech(tId).name());
            Collections.sort(techNames);
            String techList = techNames.get(0);
            if (techNames.size() > 1) {
                for (int i=1;i<techNames.size();i++)
                    techList = text("NOTICE_SPIES_MULTIPLE_TECHS", techList, techNames.get(i));
            }
            String desc = text("NOTICE_SPIES_LEARNED_TECH", techList);
            desc = selectedEmpire.replaceTokens(desc, "alien");
            g.setFont(narrowFont(15));
            List<String> lines = wrappedLines(g, desc, descW);
            for (String line: lines) {
                y2 += lineH;
                drawString(g,line, x2, y2);
            }
        }
        
        
        
        // draw tabs
        int y3 = y0+bdr;
        int tabSp = BasePanel.s2;
        for (EmpireTabSprite tab: tabs) {
            tab.setPosition(x3,y3);
            y3 = y3+tab.tabH+tabSp;
            tab.draw(ui, g);
        }
            
        
        for (EmpireTabSprite tab: tabs)
            parent.addNextTurnControl(tab);
    }
    @Override
    public void advanceMap() {
        parent.resumeOutsideTurn();
    }
    class EmpireTabSprite extends MapSprite {
        private int tabX, tabY, tabW, tabH;
        private int fontSize = 16;

        private final MapOverlaySpies parent;
        private final Empire empire;

        protected int mapX()      { return tabX; }
        protected int mapY()      { return tabY; }
        public void mapX(int i)   { tabX = i; }
        public void mapY(int i)   { tabY = i; }

        public int width()        { return tabW; }
        public int height()       { return tabH; }
        public void reset()       {  }

        public EmpireTabSprite(MapOverlaySpies p, Empire e)  {
            parent = p;
            empire = e;
            tabW = BasePanel.s100;
            fontSize = 11;
            int n = p.empires.size();
            if (n > 40)
                fontSize = 20;
            else if (n > 36)
                fontSize = 12;
            else if (n > 34)
                fontSize = 13;
            else if (n > 32)
                fontSize = 14;
            else if (n > 30)
                fontSize = 15;
            else if (n > 29)
                fontSize = 16;
            else if (n > 28)
                fontSize = 17;
            else if (n > 26)
                fontSize = 18;
            else if (n > 25)
                fontSize = 19;
            else
                fontSize = 20;
            tabH = scaled(fontSize+1);
        }
        public void setPosition(int x, int y) {
            tabX = x;
            tabY = y;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= tabX
                        && x <= tabX+tabW
                        && y >= tabY
                        && y <= tabY+tabH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            int cnr = scaled(fontSize/2);
            
            g.setFont(narrowFont(fontSize));
            if (empire == parent.selectedEmpire)
                g.setColor(MainUI.paneBackground);
            else
                g.setColor(MainUI.paneBackgroundDk);
            g.setClip(tabX,tabY,tabW,tabH);
            g.fillRoundRect(tabX-cnr,tabY,tabW+cnr,tabH,cnr,cnr);
            g.setClip(null);
            if (hovering)
                g.setColor(SystemPanel.yellowText);
            else
                g.setColor(SystemPanel.blackText);
            drawString(g,empire.raceName(), tabX+scaled(5), tabY+tabH-scaled(3));
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
            //if (click)
            //    softClick();
            parent.selectEmpire(empire);
        }
    }
}

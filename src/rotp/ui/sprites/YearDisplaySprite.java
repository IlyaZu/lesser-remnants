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
package rotp.ui.sprites;

import java.awt.Color;
import java.awt.Graphics2D;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;

public class YearDisplaySprite extends MapSprite {
    private int mapX, mapY, buttonW, buttonH;
    private int minMapX, maxButtonW;
    private final MainUI parent;

    public YearDisplaySprite(MainUI p)  { parent = p; }

    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
        if (session().currentAlert() != null)
            return false;
        hovering = x >= mapX
                    && x <= mapX+buttonW
                    && y >= mapY-buttonH-scaled(5)
                    && y <= mapY;
        return hovering;
    }
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g) {
        if (!parent.showYear())
            return;
        if (session().currentAlert() != null)
            return;

        String s = text("MAIN_YEAR_DISPLAY", galaxy().currentTurn());
        g.setFont(narrowFont(24));

        int s5 = scaled(5);
        int sw = g.getFontMetrics().stringWidth(s);
        mapX = map.getWidth()-sw-scaled(35);
        buttonW = sw+s5+s5;
        buttonH = scaled(20);
        mapY = map.getHeight()-scaled(75);
        Color textC = Color.gray;

        drawShadowedString(g, s, 2, mapX, mapY-s5, Color.black, textC);
    }
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (session().currentAlert() != null)
            return;
        minMapX = min(mapX, minMapX);
        maxButtonW = max(buttonW, maxButtonW);
        hovering = true;
    }
    @Override
    public void repaint(GalaxyMapPanel map)     {
        map.repaint(minMapX,mapY-buttonH,maxButtonW,buttonH);
    }
}
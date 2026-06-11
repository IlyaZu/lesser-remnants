/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2026 Ilya Zushinskiy
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

import java.awt.Graphics2D;
import java.awt.Image;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.util.ImageManager;

public class SystemFlagSprite extends MapSprite {
    private static final int WIDTH = BasePanel.s70;
    private static final int HEIGHT = BasePanel.s70;
    
    private final Runnable paintCallback;
    
    private int systemId;
    private int x, y;
    
    public SystemFlagSprite(Runnable paintCallback) {
        this.paintCallback = paintCallback;
    }
    
    public void setSystemId(int systemId) {
        this.systemId = systemId;
    }
    
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getWidth() {
        return WIDTH;
    }
    
    public int getHeight() {
        return HEIGHT;
    }
    
    @Override
    public boolean acceptDoubleClicks() {
        return true;
    }
    
    @Override
    public boolean acceptWheel() {
        return true;
    }
    
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
        hovering = x >= this.x && x <= this.x+WIDTH &&
                y >= this.y && y <= this.y+HEIGHT;

        return hovering;
    }
    
    @Override
    public void draw(GalaxyMapPanel map, Graphics2D graphic) {
        Image flagHaze = ImageManager.current().image("Flag_Haze");
        graphic.drawImage(flagHaze, x, y, WIDTH, HEIGHT, null);
        
        if (hovering) {
            Image flagHover = ImageManager.current().image("Flag_Hover");
            graphic.drawImage(flagHover, x, y, WIDTH, HEIGHT, null);
        }
        
        Image flagImage = player().sv.flagImage(systemId);
        graphic.drawImage(flagImage, x, y, WIDTH, HEIGHT, null);
    }
    
    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (rightClick) {
            player().sv.resetFlagColor(systemId);
        } else {
            player().sv.toggleFlagColor(systemId, false);
        }
        paintCallback.run();
    }
    
    @Override
    public void wheel(GalaxyMapPanel map, int rotation, boolean click) {
        player().sv.toggleFlagColor(systemId, rotation < 0);
        paintCallback.run();
    }
}
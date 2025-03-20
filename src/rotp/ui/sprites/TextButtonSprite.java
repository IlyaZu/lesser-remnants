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
package rotp.ui.sprites;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.SystemPanel;

public class TextButtonSprite extends MapSprite {
    private static final Color PRIME_BACKGROUND_COLOUR = new Color(70,93,48);
    private static final Color NON_PRIME_BACKGROUND_COLOUR = new Color(93,93,93);
    private static final int FONT_SIZE = 20;
    private static final int HEIGHT = BasePanel.s30;
    
    private final String textKey;
    private final boolean isPrime;
    private final Runnable clickCallback;
    
    private int x, y;
    private int spriteWidth, textWidth;
    
    public TextButtonSprite(String textKey, boolean isPrime, Runnable clickCallback) {
        this.textKey = textKey;
        this.isPrime = isPrime;
        this.clickCallback = clickCallback;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public void refreshSize(Graphics2D graphic) {
        graphic.setFont(narrowFont(FONT_SIZE));
        String text = text(textKey);
        textWidth = graphic.getFontMetrics().stringWidth(text);
        spriteWidth = BasePanel.s20 + textWidth;
    }
    
    public int getWidth() {
        return spriteWidth;
    }
    
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
        hovering = x >= this.x && x <= this.x+spriteWidth &&
                y >= this.y && y <= this.y+HEIGHT;

        return hovering;
    }

    @Override
    public void draw(GalaxyMapPanel map, Graphics2D graphic) {
        int s3 = BasePanel.s3;
        int s5 = BasePanel.s5;
        int s10 = BasePanel.s10;

        graphic.setColor(SystemPanel.blackText);
        graphic.fillRoundRect(x+s3, y+s3, spriteWidth, HEIGHT, s10, s10);
        
        graphic.setColor(isPrime ? PRIME_BACKGROUND_COLOUR : NON_PRIME_BACKGROUND_COLOUR);
        graphic.fillRoundRect(x, y, spriteWidth, HEIGHT, s5, s5);
        
        Color textColour = hovering ? SystemPanel.yellowText : Color.white;
        graphic.setColor(textColour);
        Stroke prevStr = graphic.getStroke();
        graphic.setStroke(BasePanel.stroke2);
        graphic.drawRoundRect(x, y, spriteWidth, HEIGHT, s5, s5);
        graphic.setStroke(prevStr);
        
        int textX = x+((spriteWidth-textWidth)/2);
        String text = text(textKey);
        graphic.setFont(narrowFont(FONT_SIZE));
        drawBorderedString(graphic, text, textX, y+HEIGHT-s10, SystemPanel.textShadowC, textColour);
    }

    @Override
    public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click) {
        if (click)
            softClick();
        clickCallback.run();
    }
}

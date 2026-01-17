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
package rotp.model.galaxy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import rotp.ui.sprites.RoundGradientPaint;
import rotp.util.Base;

public enum StarType implements Base {
    RED(Color.red, "RED_STAR_DESCRIPTION"),
    YELLOW(Color.yellow, "YELLOW_STAR_DESCRIPTION"),
    GREEN(new Color(0,255,128), "ORANGE_STAR_DESCRIPTION"),
    WHITE(Color.white, "WHITE_STAR_DESCRIPTION"),
    BLUE(Color.blue, "BLUE_STAR_DESCRIPTION"),
    PURPLE(Color.magenta, "PURPLE_STAR_DESCRIPTION");
    
    private static final RoundGradientPaint rgp = new RoundGradientPaint();

    private final Color color;
    private final String description;
    private final HashMap<Integer, BufferedImage> images = new HashMap<>();

    private StarType(Color color, String description) {
        this.color = color;
        this.description = description;
    }

    public Color color() {
        return color;
    }

    public String description() {
        return description;
    }

    private int maxRadius() {
        if (veryLowMemory())
            return 30;
        else if (lowMemory())
            return 45;
        else
            return 80;
    }

    public BufferedImage image(int r, int f) {
        int r0 = min(r,scaled(maxRadius()));
        int key = (r*200)+f;
        if (!images.containsKey(key))
            images.put(key, createStarImage(r0,f));
        return images.get(key);
    }

    private BufferedImage createStarImage(int r, int f) {
        Color c = color();
        Color c0 = new Color(c.getRed(), c.getGreen(), c.getBlue(), 0);
        int r1 = 127+(c0.getRed()/2);
        int g1 = 127+(c0.getGreen()/2);
        int b1 = 127+(c0.getBlue()/2);
        Color c1 = unscaled(r) > 70 ? Color.white : new Color(r1,g1,b1);
        int w = (r+f+f)*2;
        int x = w/2;
        int y = w/2;

        // draw star
        RoundRectangle2D rect = new RoundRectangle2D.Float(x-(w/2), y-(w/2), w, w, 0, 0);
        rgp.set(x, y, c1, new Point2D.Float(0, r), c0, f);

        BufferedImage img = newBufferedImage(w,w);
        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setPaint(rgp);
        g.fill(rect);
        g.dispose();
        return img;
    }
}

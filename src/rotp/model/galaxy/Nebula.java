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
package rotp.model.galaxy;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.sprites.MapSprite;
import rotp.ui.util.planets.PlanetImager;
import rotp.util.Base;
import rotp.util.FastImage;

public class Nebula extends MapSprite implements Base, IMappedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private Rectangle.Float shape;
    private Rectangle.Float innerShape;
    private int sysId = -1;
    private int numStars = 0;
    private float width, height;
    private float x, y;
    private transient BufferedImage image;

    public Rectangle.Float shape()        { return shape; }

    public Nebula copy() {
        Nebula neb = new Nebula();
        neb.width = width;
        neb.height = height;
        neb.image = image;
        return neb;
    }
    public boolean intersects(Nebula n)    { return shape.intersects(n.shape); }
    @Override
    public float x()                      { return x; }
    @Override
    public float y()                      { return y; }
    
    public float width()                { return width; }
    public float height()                { return height; }
    
    public float centerX()                { return x+(width()/2); }
    public float centerY()                { return y+(height()/2); }
    
    public boolean noStars()              { return numStars == 0; }
    
    public void setXY(float x1, float y1) {
        x = x1;
        y = y1;
        shape = new Rectangle.Float(x, y, width(), height());
        innerShape = new Rectangle.Float(x+1, y+1, width()-2, height()-2);
    }
    public Nebula() {
        
    }
    public Nebula(boolean buildImage) {
        width = random(8,14);
        height = random(8,14);
        if (buildImage)
            image = buildImage();
    }
    public void noteStarSystem(StarSystem sys) {
        numStars++;
        if (sysId < 0) {
            sysId = sys.id;
            return;
        }
        float centerX = centerX();
        float centerY = centerY();
        StarSystem currSys = galaxy().system(sysId);
        float currDist = distance(currSys.x(), currSys.y(), centerX, centerY);
        float newDist = distance(sys.x(), sys.y(), centerX, centerY);
        
        if (newDist < currDist)
            sysId = sys.id;
    }
    public void enrichCentralSystem() {
        if (numStars < 3)
            return;
        if (galaxy().numStarSystems() <= 100)
            return;
        
        StarSystem sys = galaxy().system(sysId);
        if (sys.planet().isEnvironmentNone())
            return;
        
        float ultraPct = numStars * .07f;
        if (random() < ultraPct)
            sys.planet().setResourceUltraRich();
        else if (!sys.planet().isResourceUltraRich())
            sys.planet().setResourceRich();
    }
    public boolean intersects(Line2D path) {
        return (innerShape != null) && path.intersects(innerShape);
    }
    public boolean contains(float x, float y) {
        return (innerShape != null) && innerShape.contains(x, y);
    }
    public BufferedImage image() {
        if (image == null)
            image = buildImage();
        return image;
    }
    private BufferedImage buildImage() {
        int w = (int) width*19;
        int h = (int) height*12;

        int nebR = roll(160,225);
        int nebG = 0;
        int nebB = roll(160,255);

        FastImage fImg = PlanetImager.current().getTerrainSubImage(w,h);

        int floor = 255;
        int ceiling = 0;
        for (int y=0;y<h;y++)    for (int x=0;x<w;x++) {
            int pixel = fImg.getRGB(x, y);
            floor = min(floor, pixel & 0xff);
            ceiling = max(ceiling, pixel & 0xff);
        }
        for (int x=0;x<w;x++)   for (int y=0;y<h;y++) {
            int pixel = fImg.getRGB(x, y);
            int landLevel = pixel & 0xff;
            landLevel = (int) (256*((float)(landLevel-floor)/(ceiling-floor)));
            int distFromEdgeX = min(x, w-x);
            int distFromEdgeY = min(y, h-y);
            int distFromEdge = min(distFromEdgeX, distFromEdgeY);
            float pctFromEdge = min((float)distFromEdgeX/w, (float)distFromEdgeY/h);
            //int distFromCenter = (int) Math.min(128,Math.sqrt(((x-centerX)*(x-centerX))+((y-centerY)*(y-centerY))));
            int alpha = min(distFromEdge/2, landLevel*3/5);
            alpha = (int) (pctFromEdge * landLevel);
            alpha = min(alpha*3/2, (alpha+255)/2);
           //alpha = Math.min(145-distFromCenter, landLevel/2);
            int newPixel = (alpha << 24) | (nebR << 16) | (nebG << 8) | nebB;
            fImg.setRGB(x, y, newPixel);
        }
        return fImg.image();
    }
    @Override
    public boolean isSelectableAt(GalaxyMapPanel map, int mapX, int mapY) { return false; }

    @Override
    public void draw(GalaxyMapPanel map, Graphics2D g2) {
        Rectangle mShape = mapShape(map);
        g2.drawImage(image(), mShape.x, mShape.y, mShape.x+mShape.width, mShape.y+mShape.height, 0, 0, image().getWidth(), image().getHeight(), map);
    }
    private Rectangle mapShape(GalaxyMapPanel map) {
        int x0 = map.mapX(x);
        int y0 = map.mapY(y);
        int x1 = map.mapX(x+(int)width());
        int y1 = map.mapY(y+(int)height());
        return new Rectangle(x0,y0, x1-x0, y1-y0);
    }
}

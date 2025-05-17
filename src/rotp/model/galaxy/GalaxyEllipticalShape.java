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

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import rotp.model.game.IGameOptions;

public class GalaxyEllipticalShape extends GalaxyShape {
    private static final List<String> options1 = new ArrayList<>();
    private static final List<String> options2 = new ArrayList<>();
    private static final long serialVersionUID = 1L;
    static {
        options1.add("SETUP_ELLIPSE_0");
        options1.add("SETUP_ELLIPSE_1");
        options1.add("SETUP_ELLIPSE_2");
        options1.add("SETUP_ELLIPSE_3");
        options1.add("SETUP_ELLIPSE_4");
        options2.add("SETUP_VOID_0");
        options2.add("SETUP_VOID_1");
        options2.add("SETUP_VOID_2");
        options2.add("SETUP_VOID_3");
        options2.add("SETUP_VOID_4");
    }

    private Shape ellipse, hole;
    private float ellipseRatio = 2.0f;
    private float voidSize = 0.0f;
    
    public GalaxyEllipticalShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public List<String> options1()  { return options1; }
    @Override
    public List<String> options2()  { return options2; }
    @Override
    public String defaultOption1()  { return options1.get(2); }
    @Override
    public String defaultOption2()  { return options2.get(0); }
    @Override
    public float maxScaleAdj()               { return 0.8f; }
    @Override
    public void init(int n) {
        super.init(n);

        int option1 = max(0, options1.indexOf(opts.selectedGalaxyShapeOption1()));
        int option2 = max(0, options2.indexOf(opts.selectedGalaxyShapeOption2()));
        
        switch(option1) {
            case 0: ellipseRatio = 1.0f; break;
            case 1: ellipseRatio = 1.5f; break;
            case 2: ellipseRatio = 2.0f; break;
            case 3: ellipseRatio = 3.0f; break;
            case 4: ellipseRatio = 5.0f; break;
            default: ellipseRatio = 2.0f; break;
        }
        
        switch(option2) {
            case 0: voidSize = 0.0f; break;
            case 1: voidSize = 0.2f; break;
            case 2: voidSize = 0.4f; break;
            case 3: voidSize = 0.6f; break;
            case 4: voidSize = 0.8f; break;
            default: voidSize = 0.0f; break;
        }
        // reset w/h vars since aspect ratio may have changed
        initWidthHeight();
        
        float gE = (float) galaxyEdgeBuffer();
        float gW = (float) galaxyWidthLY();
        float gH = (float) galaxyHeightLY();
        
        ellipse = new Ellipse2D.Float(gE,gE,gW,gH);
        
        hole = null;
        if (voidSize > 0) {
            float vW = voidSize*gW;
            float vH = voidSize*gH;
            float vX = gE+((gW-vW)/2);
            float vY = gE+((gH-vH)/2);
            hole = new Ellipse2D.Float(vX, vY,vW,vH);
        }
    }
    @Override
    protected int galaxyWidthLY() {
        return (int) (Math.sqrt(ellipseRatio*maxStars*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() {
        return (int) (Math.sqrt((1/ellipseRatio)*maxStars*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width, galaxyEdgeBuffer());
        pt.y = randomLocation(height, galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(float x, float y) {
        if (hole == null)
            return ellipse.contains(x, y);
        else
            return ellipse.contains(x, y) && !hole.contains(x, y);
    }
    private float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        switch (opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return 8;
            case IGameOptions.SIZE_SMALL:     return 10;
            case IGameOptions.SIZE_MEDIUM:    return 12;
            case IGameOptions.SIZE_LARGE:     return 13;
            case IGameOptions.SIZE_HUGE:      return 14;
            case IGameOptions.SIZE_MASSIVE:   return 16;
            default:                          return 19;
        }
    }
}

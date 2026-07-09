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

import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import rotp.model.game.IGameOptions;

public class GalaxyEllipticalShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;

    private Shape ellipse;
    
    public GalaxyEllipticalShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.8f; }
    @Override
    public void init(int numStars) {
        super.init(numStars);
        
        float gE = (float) EDGE_BUFFER;
        float gW = (float) galaxyWidthLY(numStars);
        float gH = (float) galaxyHeightLY(numStars);
        
        ellipse = new Ellipse2D.Float(gE,gE,gW,gH);
    }
    @Override
    protected int galaxyWidthLY(int numStars) {
        return (int) (Math.sqrt(numStars*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY(int numStars) {
        return (int) (Math.sqrt(numStars*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width(), EDGE_BUFFER);
        pt.y = randomLocation(height(), EDGE_BUFFER);
    }
    @Override
    public boolean valid(float x, float y) {
        return ellipse.contains(x, y);
    }
    private float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        switch (size) {
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

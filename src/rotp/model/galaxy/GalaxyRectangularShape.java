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
import rotp.model.game.IGameOptions;

public class GalaxyRectangularShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    
    public GalaxyRectangularShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.95f; }
    
    @Override
    protected int galaxyWidthLY() {
        return (int) (Math.sqrt(4.0/3.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    protected int galaxyHeightLY() {
        return (int) (Math.sqrt(3.0/4.0*opts.numberStarSystems()*adjustedSizeFactor()));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width(), galaxyEdgeBuffer());
        pt.y = randomLocation(height(), galaxyEdgeBuffer());
    }
    @Override
    public boolean valid(float x, float y) {
        float buff = galaxyEdgeBuffer();
        if (x > (width()-buff))
            return false;
        if (x < buff)
            return false;
        if (y > (height()-buff))
            return false;
        if (y < buff)
            return false;
        return true;
    }
    private float randomLocation(float max, float buff) {
        return buff + (random() * (max-buff-buff));
    }
    @Override
    protected float sizeFactor(String size) {
        switch (opts.selectedGalaxySize()) {
            case IGameOptions.SIZE_TINY:      return 10;
            case IGameOptions.SIZE_SMALL:     return 15;
            case IGameOptions.SIZE_MEDIUM:    return 17;
            case IGameOptions.SIZE_LARGE:     return 19;
            case IGameOptions.SIZE_HUGE:      return 20;
            case IGameOptions.SIZE_MASSIVE:   return 21;
            default:                          return 19;
        }
    }

}

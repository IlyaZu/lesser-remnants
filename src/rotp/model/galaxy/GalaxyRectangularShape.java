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
import rotp.model.game.IGameOptions;

public class GalaxyRectangularShape extends GalaxyShape {
    private static final long serialVersionUID = 1L;
    
    public GalaxyRectangularShape(IGameOptions options) {
        opts = options;
    }
    @Override
    public float maxScaleAdj()               { return 0.95f; }
    
    @Override
    protected int galaxyWidthLY(int numStars) {
        return (int) (Math.sqrt(4.0/3.0*numStars*sizeFactor(opts.selectedGalaxySize())));
    }
    @Override
    protected int galaxyHeightLY(int numStars) {
        return (int) (Math.sqrt(3.0/4.0*numStars*sizeFactor(opts.selectedGalaxySize())));
    }
    @Override
    public void setRandom(Point.Float pt) {
        pt.x = randomLocation(width(), EDGE_BUFFER);
        pt.y = randomLocation(height(), EDGE_BUFFER);
    }
    @Override
    public boolean valid(float x, float y) {
        float buff = EDGE_BUFFER;
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
    private float sizeFactor(String size) {
        int sizeFactor = switch (size) {
            case IGameOptions.SIZE_TINY    -> 10;
            case IGameOptions.SIZE_SMALL   -> 15;
            case IGameOptions.SIZE_MEDIUM  -> 17;
            case IGameOptions.SIZE_LARGE   -> 19;
            case IGameOptions.SIZE_HUGE    -> 20;
            case IGameOptions.SIZE_MASSIVE -> 21;
            default                        -> 19;
        };
        return sizeFactor + genAttempt()/3;
    }
}

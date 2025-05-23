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

import rotp.model.Sprite;
import rotp.model.galaxy.IMappedObject;
import rotp.util.Base;

public abstract class MapSprite implements Base, Sprite {
    private IMappedObject source;
    protected boolean hovering;

    @Override
    public boolean hovering()                   { return hovering; }
    @Override
    public void hovering(boolean b)             { hovering = b; }
    @Override
    public IMappedObject source()               { return source; }
    public void source(IMappedObject o)         { source = o; }
}

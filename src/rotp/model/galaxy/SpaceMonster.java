/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023 Ilya Zushinskiy
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

import java.awt.Image;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import rotp.model.combat.CombatEntity;
import rotp.model.empires.Empire;
import rotp.util.Base;

public class SpaceMonster implements Base, NamedObject, Serializable {
    private static final long serialVersionUID = 1L;
    protected final String nameKey;
    protected int lastAttackerId;
    private final List<Integer> path = new ArrayList<>();
    private transient List<CombatEntity> combatStacks = new ArrayList<>();
    
    public Empire lastAttacker()               { return galaxy().empire(lastAttackerId); }
    public void lastAttacker(Empire c)         { lastAttackerId = c.id; }
    public void visitSystem(int sysId)         { path.add(sysId); }
    public List<Integer> vistedSystems()       { return path; }
    public List<CombatEntity> combatStacks()    {
        if (combatStacks == null)
            combatStacks = new ArrayList<>();
        return combatStacks; 
    }
    public Image image()  { return image(nameKey); }
    public void initCombat() { }
    public void addCombatStack(CombatEntity c)  { combatStacks.add(c); }
    public SpaceMonster(String s) {
        nameKey = s;
    }
    @Override
    public String name()      { return text(nameKey);  }
    public boolean alive()    { 
        boolean alive = false;
        for (CombatEntity st: combatStacks) {
            if (!st.destroyed())
                return true;
        }
        return alive;
    }
    public void plunder() { }
}

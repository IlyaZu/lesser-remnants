/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2024 Ilya Zushinskiy
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
package rotp.model.ai;

import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.base.AIShipCaptain;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.combat.CombatEntity;
import rotp.model.combat.FlightPath;
import rotp.model.combat.CombatManager;
import rotp.model.galaxy.StarSystem;
import rotp.util.Base;

public class MonsterCaptain implements Base, ShipCaptain {
    @Override
    public StarSystem retreatSystem(StarSystem fr) { return null; }
    @Override
    public boolean wantToRetreat(CombatEntity stack) { return false; }
    @Override
    public void performTurn(CombatEntity stack)  {
        CombatManager mgr = galaxy().shipCombat();
        
        if (stack.destroyed()) {
            mgr.turnDone(stack);
            return;
        }

        CombatEntity prevTarget = null;
        while (stack.move > 0) {
            float prevMove = stack.move;
            prevTarget = stack.target;
            FlightPath bestPathToTarget = chooseTarget(stack);
            // if we need to move towards target, do it now
            if ((bestPathToTarget != null) && (bestPathToTarget.size() > 0))
                mgr.performMoveStackAlongPath(stack, bestPathToTarget);

            // if can attack target this turn, fire when ready
            if (stack.canAttack(stack.target))
                mgr.performAttackTarget(stack);
            
            // SANITY CHECK:
            // make sure we fall out if we haven't moved
            // and we are still picking the same target
            if ((prevMove == stack.move) && (prevTarget == stack.target))
                stack.move = 0;
        }
        mgr.turnDone(stack);
    }
    @Override
    public FlightPath pathTo(CombatEntity st, int x, int y) { return null; }
    private  FlightPath chooseTarget(CombatEntity stack) {
        CombatManager mgr = galaxy().shipCombat();

        List<CombatEntity> activeStacks = new ArrayList<>(mgr.activeStacks());
        List<CombatEntity> potentialTargets = new ArrayList<>();
        for (CombatEntity st: activeStacks) {
            if (st.isEmpireShip())
                potentialTargets.add(st);
        }
        FlightPath bestPath = null;
        CombatEntity bestTarget = null;
        int bestTurns = 9999;

        // can we eat any stacks? (range 0 weapon)
        for (CombatEntity target : potentialTargets) {
            FlightPath path = AIShipCaptain.findBestPathToAttack(stack, target);
            if (path != null) {  // can we even path to this target?
                if (bestPath == null) {
                    bestPath = path;
                    bestTarget = target;
                    bestTurns = (int) Math.ceil(path.size() / stack.maxMove());
                }
                else {
                    int turns = (int) Math.ceil(path.size() / stack.maxMove());
                    if (turns < bestTurns) {
                        bestPath = path;
                        bestTarget = target;
                        bestTurns = turns;
                    }
                    else if ((turns == bestTurns) && (target.totalHits() > bestTarget.totalHits())) {
                        bestPath = path;
                        bestTarget = target;
                        bestTurns = turns;
                    }
                }
            }
        }
        stack.target = bestTarget;
        return bestPath;
    }
}

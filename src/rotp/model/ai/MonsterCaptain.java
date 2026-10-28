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
package rotp.model.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
            FlightPath path = findBestPathToAttack(stack, target);
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
                    else if ((turns == bestTurns) && (totalHits(target) > totalHits(bestTarget))) {
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
    private float totalHits(CombatEntity entity) {
        return entity.maxHits() * entity.num;
    }
    private static FlightPath findBestPathToAttack(CombatEntity st, CombatEntity tgt) {
        if (!st.isArmed())
            return null;
        int range = st.optimalFiringRange(tgt);
        if (st.movePointsTo(tgt) <= range) {
            return new FlightPath();
        }
        if (tgt.isColony() && st.hasBombs())
            range = 1;

        List<FlightPath> validPaths = new ArrayList<>();
        FlightPath bestPath = null;
        
        if (st.x > tgt.x) {
            if (st.y > tgt.y) {
                for (int x1=tgt.x+range; x1>=tgt.x-range; x1--) {
                    for (int y1=tgt.y+range; y1>=tgt.y-range; y1--) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
            else {
                for (int x1=tgt.x+range; x1>=tgt.x-range; x1--) {
                    for (int y1=tgt.y-range; y1<=tgt.y+range; y1++) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
        }
        else {
            if (st.y > tgt.y) {
                for (int x1=tgt.x-range; x1<=tgt.x+range; x1++) {
                    for (int y1=tgt.y+range; y1>=tgt.y-range; y1--) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
            else {
                for (int x1=tgt.x-range; x1<=tgt.x+range; x1++) {
                    for (int y1=tgt.y-range; y1<=tgt.y+range; y1++) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
        }
            
         // there is no path to get in optimal firing range of target!
        if (validPaths.isEmpty()) {
            // are we within max firing range? if so, go with that
            if (st.movePointsTo(tgt) <= st.maxFiringRange(tgt))
                return new FlightPath();
            return null;
        }

        Collections.sort(validPaths,FlightPath.SORT);
        return validPaths.get(0);
    }
    private static FlightPath allValidPaths(int x0, int y0, int x1, int y1, int moves, CombatEntity stack, List<FlightPath> validPaths, FlightPath bestPath) {
        FlightPath updatedBestPath = bestPath;
        CombatManager mgr = stack.mgr;
        int gridW = CombatManager.maxX+3;

        // all squares containing ships, asteroids, etc or non-traversable
        // can also check for enemy repulsor beam effects
        boolean[] valid = mgr.validMoveMap(stack);

        int startX = x0 + 1;
        int startY = y0 + 1;
        int endX = x1 + 1;
        int endY = y1 + 1;

        // based on general direction to travel, find most straightforward path priority
        int[] pathDeltas = bestPathDeltas(startX, startY, endX, endY);

        int start = (startY*gridW)+startX;
        int end = (endY*gridW)+endX;

        List<Integer> path = new ArrayList<>();

        loadValidPaths(start, end, valid, moves, validPaths, path, pathDeltas, gridW, updatedBestPath);
        return updatedBestPath;
    }
    private static int[] bestPathDeltas(int c0, int c1) {
        int w = FlightPath.mapW;
        return bestPathDeltas(c0%w, c0/w, c1%w, c1/w);
    }
    private static int[] bestPathDeltas(int x0, int y0, int x1, int y1) {
        if (x1 < x0) {
            if (y1 < y0)
                return FlightPath.nwPathPriority;
            else if (y1 > y0)
                return FlightPath.swPathPriority;
            else
                return FlightPath.wPathPriority;
        }
        else if (x1 > x0) {
            if (y1 < y0)
                return FlightPath.nePathPriority;
            else if (y1 > y0)
                return FlightPath.sePathPriority;
            else
                return FlightPath.ePathPriority;
        }
        else {
            if (y1 < y0)
                return FlightPath.nPathPriority;
            else
                return FlightPath.sPathPriority;
        }
    }
    private static FlightPath loadValidPaths(int curr, int end, boolean[] valid, int moves, List<FlightPath> paths, List<Integer> currPath, int[] deltas, int gridW, FlightPath bestPath) {
        FlightPath updatedBestPath = bestPath;
        if (curr == end) {
            if (currPath.size() <= pathSize(bestPath)) {
                FlightPath newPath = new FlightPath(currPath, gridW);
                paths.add(newPath);
                updatedBestPath = newPath;
            }
            return updatedBestPath;
        }
        int[] basePaths = FlightPath.basePathPriority;

        int remainingMoves = moves - 1;
        for (int dir=0;dir<deltas.length;dir++) {
            int next = curr+deltas[dir];

            if (valid[next]) {
                // are we at the end? if so create FP and fall out
                if (next == end) {
                    currPath.add(next);
                    if (currPath.size() <= pathSize(bestPath)) {
                        FlightPath newPath = new FlightPath(currPath, gridW);
                        paths.add(newPath);
                        updatedBestPath = newPath;
                    }
                }
                else if (remainingMoves > 0) {
                    int minMovesReq = moveDistance(next,end,gridW);
                    int minPossibleMoves = minMovesReq + currPath.size() + 1;
                    int bestPathSize = pathSize(updatedBestPath);
                    if ((minPossibleMoves < bestPathSize) && (minMovesReq <= remainingMoves)) {
                        int baseDir = 0;
                        for (int i=0; i<basePaths.length;i++) {
                            if (basePaths[i] == deltas[dir]) {
                                baseDir = i;
                                break;
                            }
                        }
                        List<Integer> nextPath = new ArrayList<>(currPath);
                        nextPath.add(next);
                        boolean[] nextValid = Arrays.copyOf(valid, valid.length);
                        nextValid[curr] = false;
                        nextValid[curr + basePaths[(baseDir+1)%8]] = false;
                        nextValid[curr + basePaths[(baseDir+7)%8]] = false;
                        if (baseDir %2 == 0) {
                            nextValid[curr + basePaths[(baseDir+6)%8]] = false;
                            nextValid[curr + basePaths[(baseDir+2)%8]] = false;
                        }
                        int [] pathDeltas = bestPathDeltas(next, end);
                        updatedBestPath = loadValidPaths(next, end, nextValid, remainingMoves, paths, nextPath, pathDeltas, gridW, updatedBestPath);
                    }
                }
            }
        }
        return updatedBestPath;
    }
    private static int pathSize(FlightPath fp) {
        return fp == null ? 999 : fp.size();
    }
    private static int moveDistance(int pt0, int pt1, int w) {
        int x0 = pt0 % w;
        int y0 = pt0 / w;
        int x1 = pt1 % w;
        int y1 = pt1 / w;
        return Math.max(Math.abs(x0-x1), Math.abs(y0-y1));
    }
}

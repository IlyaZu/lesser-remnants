/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024-2025 Ilya Zushinskiy
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
package rotp.model.events;

import java.io.Serializable;
import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceCrystal;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.IGameOptions;
import rotp.ui.notifications.GNNNotification;
import rotp.util.Base;

public class RandomEventSpaceCrystal implements Base, Serializable, RandomEvent {
    private static final long serialVersionUID = 1L;
    public static SpaceCrystal monster = new SpaceCrystal();
    private int empId;
    private int sysId;
    private int turnCount = 0;

    @Override
    public String statusMessage()               { return text("SYSTEMS_STATUS_SPACE_CRYSTAL"); }
    @Override
    public String systemKey()                   { return "MAIN_PLANET_EVENT_CRYSTAL"; }
    @Override
    public boolean goodEvent()            { return false; }
    @Override
    public boolean repeatable()            { return false; }
    @Override
    public boolean monsterEvent()               { return true; }
    @Override
    public int minimumTurn()                    {
        // space monsters can be a challenge... delay their entry in the easier game settings
        switch (options().selectedGameDifficulty()) {
            case IGameOptions.DIFFICULTY_EASIEST:
                return RandomEvents.START_TURN + 400;
            case IGameOptions.DIFFICULTY_EASIER:
                return RandomEvents.START_TURN + 300;
            case IGameOptions.DIFFICULTY_EASY:
                return RandomEvents.START_TURN + 200;
            default:
                return RandomEvents.START_TURN + 100;
        }
    }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_SPACE_CRYSTAL");
        s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        return s1;
    }
    @Override
    public void trigger(Empire emp) {
        log("Starting Crystal event against: "+emp.raceName());
        StarSystem targetSystem = random(emp.allColonizedSystems());
        empId = emp.id;
        sysId = targetSystem.id;
        turnCount = 3;
        galaxy().events().addActiveEvent(this);
    }
    @Override
    public void nextTurn() {
        if (turnCount == 3)
            approachSystem();
        else if (turnCount == 0)
            enterSystem();
        turnCount--;
    }
    private void enterSystem() {
        monster.visitSystem(sysId);
        StarSystem targetSystem = galaxy().system(sysId);
        targetSystem.clearEvent();
        Colony col = targetSystem.colony();
        boolean isUnopposed = false;
        if (!targetSystem.orbitingFleets().isEmpty())
            galaxy().shipCombat().battle(targetSystem, monster);
        else if ((col != null) && col.defense().isArmed())
            galaxy().shipCombat().battle(targetSystem, monster);
        else
            // In this case the monster combat stacks are not initialised and
            // therefore monster.alive() may not return the correct output.
            isUnopposed = true;
        
        if (isUnopposed || monster.alive()) {
            degradePlanet(targetSystem);
            moveToNextSystem();
        }
        else
            crystalDestroyed();
    }
    private void approachSystem() {
        StarSystem targetSystem = galaxy().system(sysId);
        targetSystem.eventKey(systemKey());
        Empire pl = player();
        if (targetSystem.isColonized()) {
            if (pl.knowsOf(targetSystem.empire()) || !pl.sv.name(sysId).isEmpty())
                GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_CRYSTAL", targetSystem.empire()), "GNN_Event_Crystal");
        }
        else if (pl.sv.isScouted(sysId))
            GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_CRYSTAL_1", null), "GNN_Event_Crystal");
    }
    private void degradePlanet(StarSystem targetSystem) {
        Empire emp = targetSystem.empire();
        // colony may have already been destroyed in combat
        if (targetSystem.isColonized())
            monster.degradePlanet(targetSystem);
        
        if (emp == null)
            return;
        Empire pl = player();
        if (pl.knowsOf(emp) || !pl.sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_CRYSTAL_2", emp), "GNN_Event_Crystal");
    }
    private void crystalDestroyed() {
        galaxy().events().removeActiveEvent(this);
        
        monster.plunder();

        if (player().knowsOf(galaxy().empire(empId)) || !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_CRYSTAL_3", monster.lastAttacker()), "GNN_Event_Crystal");
    }
    private void moveToNextSystem() {
        StarSystem targetSystem = galaxy().system(sysId);
        // next system is one of the 10 nearest systems
        // more likely to go to new system (25%) than visited system (5%)
        int[] near = targetSystem.nearbySystems();
        boolean stopLooking = false;
        
        int nextSysId = -1;
        int loops = 0;
        if (near.length > 0) {
            while (!stopLooking) {
                loops++;
                for (int i=0;i<near.length;i++) {
                    float chance = monster.vistedSystems().contains(near[i]) ? 0.05f : 0.25f;
                    if (random() < chance) {
                        nextSysId = near[i];
                        stopLooking = true;
                        break;
                    }
                }
                if (loops > 10)
                    stopLooking = true;
            }
        }
        
        if (nextSysId < 0) {
            log("ERR: Could not find next system. Space Crystal removed.");
            galaxy().events().removeActiveEvent(this);
            return;
        }
    
        log("Space Crystal moving to system: "+nextSysId);
        StarSystem nextSys = galaxy().system(nextSysId);
        float slowdownEffect = max(1, 100.0f / galaxy().maxNumStarSystems());
        turnCount = (int) Math.ceil(1.5*slowdownEffect*nextSys.distanceTo(targetSystem));
        sysId = nextSys.id;
        if (turnCount <= 3)
            approachSystem();
    }
    private String notificationText(String key, Empire emp)    {
        String s1 = text(key);
        if (emp != null) {
            s1 = s1.replace("[system]", emp.sv.name(sysId));
            s1 = emp.replaceTokens(s1, "victim");
        }
        else
            s1 = s1.replace("[system]", player().sv.name(sysId));
        return s1;
    }
}

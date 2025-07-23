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
package rotp.model.empires;

import java.io.Serializable;
import rotp.ui.diplomacy.DialogueManager;
import rotp.util.Base;

public class Leader implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public enum Personality  {
        ERRATIC("LEADER_ERRATIC"),
        PACIFIST("LEADER_PACIFIST"),
        HONORABLE("LEADER_HONORABLE"),
        RUTHLESS("LEADER_RUTHLESS"),
        AGGRESSIVE("LEADER_AGGRESSIVE"),
        XENOPHOBIC("LEADER_XENOPHOBIC");
        private final String label;
        Personality(String s) { label = s; }
        @Override
        public String toString() { return label; }
    }
    public enum Objective {
        MILITARIST("LEADER_MILITARIST"),
        ECOLOGIST("LEADER_ECOLOGIST"),
        DIPLOMAT("LEADER_DIPLOMAT"),
        INDUSTRIALIST("LEADER_INDUSTRIALIST"),
        EXPANSIONIST("LEADER_EXPANSIONIST"),
        TECHNOLOGIST("LEADER_TECHNOLOGIST");
        private final String label;
        Objective(String s) { label = s; }
        @Override
        public String toString() { return label; }
    }
    private final String name;
    public final Personality personality;
    public final Objective objective;
    private final Empire empire;

    public String name()      { return name; }
    public Leader(Empire c) {
        this(c, c.race().randomLeaderName());
    }
    public Leader(Empire c, String s) {
        empire = c;
        name = s;
        personality = Personality.values()[empire.race().randomLeaderAttitude()];
        objective = Objective.values()[empire.race().randomLeaderObjective()];
    }
    public String objective()   { return text(objective.label); }
    public String personality() { return text(personality.label); }

    public boolean isErratic()     { return personality == Personality.ERRATIC; }
    public boolean isPacifist()    { return personality == Personality.PACIFIST; }
    public boolean isHonorable()   { return personality == Personality.HONORABLE; }
    public boolean isAggressive()  { return personality == Personality.AGGRESSIVE; }
    public boolean isRuthless()    { return personality == Personality.RUTHLESS; }
    public boolean isXenophobic()  { return personality == Personality.XENOPHOBIC; }

    public boolean isDiplomat()     { return objective == Objective.DIPLOMAT; }
    public boolean isMilitarist()   { return objective == Objective.MILITARIST; }
    public boolean isEcologist()    { return objective == Objective.ECOLOGIST; }
    public boolean isIndustrialist(){ return objective == Objective.INDUSTRIALIST; }
    public boolean isExpansionist() { return objective == Objective.EXPANSIONIST; }
    public boolean isTechnologist() { return objective == Objective.TECHNOLOGIST; }

    public String dialogueContactType() {
        switch(personality) {
            case PACIFIST:   return DialogueManager.CONTACT_PACIFIST;
            case HONORABLE:  return DialogueManager.CONTACT_HONORABLE;
            case RUTHLESS:   return DialogueManager.CONTACT_RUTHLESS;
            case AGGRESSIVE: return DialogueManager.CONTACT_AGGRESSIVE;
            case XENOPHOBIC: return DialogueManager.CONTACT_XENOPHOBIC;
            case ERRATIC:    return DialogueManager.CONTACT_ERRATIC;
            default:         return DialogueManager.CONTACT_ERRATIC;
        }
    }
    public float diplomacyAnnoyanceMod(EmpireView v) {
        // # of requests past the initial
        int addl = max(0, v.embassy().requestCount()-1);
        switch(personality) {
            case XENOPHOBIC: return -20*addl;
            case ERRATIC:    return -10*addl;
            case PACIFIST:   return -10*addl;
            case HONORABLE:  return -10*addl;
            case RUTHLESS:   return -10*addl;
            case AGGRESSIVE: return -10*addl;
            default:         return -10*addl;
        }
    }
    public float acceptPactMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = 20; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = 10; break;
            case RUTHLESS:      a = -10; break;
            case AGGRESSIVE:    a = -20; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 10; break;
            case MILITARIST:    b = -10; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 5; break;
            case EXPANSIONIST:  b = -5; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }
        return a+b;
    }
    public float acceptAllianceMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = 0; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = -20; break;
            case RUTHLESS:      a = -10; break;
            case AGGRESSIVE:    a = 0; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 10; break;
            case MILITARIST:    b = 10; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 10; break;
            case EXPANSIONIST:  b = 10; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }
        return a+b;
    }
    public float acceptTradeMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = 0; break;
            case HONORABLE:     a = 0; break;
            case XENOPHOBIC:    a = -20; break;
            case RUTHLESS:      a = 0; break;
            case AGGRESSIVE:    a = 0; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 0; break;
            case MILITARIST:    b = 0; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 10; break;
            case EXPANSIONIST:  b = 0; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }
        return a+b;
    }
    public float preserveTreatyMod() {
        int a, b;
        switch(personality) {
            case PACIFIST:      a = 0; break;
            case HONORABLE:     a = 40; break;
            case XENOPHOBIC:    a = 0; break;
            case RUTHLESS:      a = 0; break;
            case AGGRESSIVE:    a = 0; break;
            case ERRATIC:       a = 0; break;
            default:            a = 0; break;
        }
        switch(objective) {
            case DIPLOMAT:      b = 20; break;
            case MILITARIST:    b = 0; break;
            case ECOLOGIST:     b = 0; break;
            case INDUSTRIALIST: b = 0; break;
            case EXPANSIONIST:  b = 0; break;
            case TECHNOLOGIST:  b = 0; break;
            default:            b = 0; break;
        }
        return a+b;
    }
}

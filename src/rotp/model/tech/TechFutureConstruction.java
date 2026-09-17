/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024-2026 Ilya Zushinskiy
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
package rotp.model.tech;

import rotp.model.empires.Empire;

public final class TechFutureConstruction extends Tech {
    public TechFutureConstruction (int lv, int seq, boolean b, TechCategory c) {
        super(Tech.FUTURE_CONSTRUCTION, "FutureConstruction", seq, lv, c, "FUTURECONST_");
        free = b;
        switch (seq) {
            case 0 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_01";
            case 1 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_02";
            case 2 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_03";
            case 3 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_04";
            case 4 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_05";
            case 5 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_06";
            case 6 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_07";
            case 7 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_08";
            case 8 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_09";
            case 9 -> iconFilename = "TECH_FUTURE_CONSTRUCTION_10";
        }
    }
    @Override
    public boolean isFutureTech()  { return true; }
    @Override
    public int futureTechLevel()         { return typeSeq+1; } // number is zero-based
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
}

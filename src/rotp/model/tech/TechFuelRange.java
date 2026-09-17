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
package rotp.model.tech;

import rotp.model.empires.Empire;

public final class TechFuelRange extends Tech {
    private int range;
    public boolean unlimited = false;

    public float range()  { return range; }

    public TechFuelRange(int lv, int seq, boolean b, TechCategory c) {
        super(Tech.FUEL_RANGE, "FuelRange", seq, lv, c, "FUELRANGE_");
        free = b;
        switch(typeSeq) {
            case 0: range = 3; break;
            case 1: range = 4; iconFilename = "TECH_FUEL_HYDROGEN"; break;
            case 2: range = 5; iconFilename = "TECH_FUEL_DEUTERIUM"; break;
            case 3: range = 6; iconFilename = "TECH_FUEL_IRRIDIUM"; break;
            case 4: range = 7; iconFilename = "TECH_FUEL_DOTOMITE"; break;
            case 5: range = 8; iconFilename = "TECH_FUEL_URIDIUM"; break;
            case 6: range = 9; iconFilename = "TECH_FUEL_REAJAX"; break;
            case 7: range = 10; iconFilename = "TECH_FUEL_TRILITHIUM"; break;
            case 8: range = 99999;
                    unlimited = true;
                    iconFilename = "TECH_FUEL_THORIUM";
                    break;
        }
    }
    
    @Override
    public String detail() {
        return super.detail().replace("%1", ""+range);
    }
    @Override
    public boolean isFuelRangeTech()     { return true; }
    @Override
    public float expansionModeFactor()  { return 3; }
    @Override
    public boolean isObsolete(Empire c) {
        return range() < c.tech().shipRange();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (! isObsolete(c))
            c.tech().topFuelRangeTech(this);
    }
}

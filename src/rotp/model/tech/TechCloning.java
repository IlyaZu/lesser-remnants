/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024 Ilya Zushinskiy
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

public final class TechCloning extends Tech {
    public static final int BASE_POPULATION_COST = 20;
    public float growthCost;

    public TechCloning(String typeId, int lv, int seq, boolean b, TechCategory c) {
        super(c, Tech.CLONING, typeId, seq, lv);
        free = b;
        init();
    }
    private void init() {
        switch(typeSeq) {
            case 0:
                growthCost = 10;
                break;
            case 1:
                growthCost = 5;
                break;
        }
    }
    
    @Override
    public boolean isObsolete(Empire c) {
        return growthCost > c.tech().populationCost();
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        if (!isObsolete(c))
            c.tech().topCloningTech(this);
    }
}

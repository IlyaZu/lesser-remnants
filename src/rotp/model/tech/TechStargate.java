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

public final class TechStargate extends Tech {
    public static float MAINTENANCE = 300;
    public TechStargate(String typeId, int lv, int seq, boolean b, TechCategory c) {
        super(c, Tech.STARGATE, typeId, seq, lv);
        free = b;
        init();
    }
    private void init() {
        switch(typeSeq) {
            case 0:
                cost = 3000;
                break;
        }
    }
    
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);
        c.shipLab().stargateDesign().tech(this);
        c.tech().canBuildStargate(true);
    }
    @Override
    public float baseValue(Empire c) {
        return c.ai().scientist().baseValue(this);
    }
}

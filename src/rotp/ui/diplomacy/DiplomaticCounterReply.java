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
package rotp.ui.diplomacy;

import java.util.List;
import rotp.model.empires.Empire;

public class DiplomaticCounterReply extends DiplomaticReply {
    private final List<String> techs;
    private final int bribe;
    private final Empire targetEmp;

    public DiplomaticCounterReply(boolean accept, String remark,  Empire emp, List<String> techList, float bribeAmt) {
        super(accept, remark);
        targetEmp = emp;
        techs = techList;
        bribe = bribeAmt < 100 ? 0: ((int) bribeAmt/100)*100;
    }

    public List<String> techs() { return techs; }
    public int bribe() { return bribe; }
    public Empire target() { return targetEmp; }
}

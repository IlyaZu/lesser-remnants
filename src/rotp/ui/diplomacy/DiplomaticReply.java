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
package rotp.ui.diplomacy;

public class DiplomaticReply {

    private final boolean accepted;
    private String remark;
    private String returnMenu;
    private boolean resumeTurn;
    private boolean returnToMap;

    public DiplomaticReply(boolean accept, String remark) {
        this.accepted = accept;
        this.remark = remark;
    }
    
    public boolean accepted() { return accepted; }
    public String remark() { return remark; }
    public String returnMenu() { return returnMenu; }
    public void returnMenu(String s) { returnMenu = s; }
    public boolean resumeTurn(){ return resumeTurn; }
    public void resumeTurn(boolean b) { resumeTurn = b; }
    public boolean returnToMap() { return returnToMap; }
    public void returnToMap(boolean b) { returnToMap = b; }
    
    public void decode(String key, String value) {
        remark = remark.replace(key, value);
    }
}

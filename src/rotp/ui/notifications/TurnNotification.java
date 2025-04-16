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
package rotp.ui.notifications;

public interface TurnNotification extends Comparable<TurnNotification> {
    static final String RANDOM_EVENT     = "0002";  // before tech notifications
    static final String DISCOVER_TECH    = "0020";
    static final String PLUNDER_TECH     = "0021";
    static final String STEAL_TECH       = "0022";
    static final String SELECT_NEW_TECH  = "0030";  // after all tech discovery notifications
    static final String SYSTEMS_SCOUTED  = "0100";
    static final String PROMPT_BOMBARD   = "4000";  // must occur before colonize prompt
    static final String PROMPT_COLONIZE  = "4001";  // after system scans & ship combat
    static final String COUNCIL_NOTIFY   = "5000";
    static final String GNN_NOTIFY         = "8000";
    static final String DIPLOMATIC_MESSAGE = "8500";

    @Override
    public default int compareTo(TurnNotification notif) {
        return displayOrder().compareTo(notif.displayOrder());
    }
    public default String key()  { return ""; }
    abstract String displayOrder();
    abstract void notifyPlayer();
}

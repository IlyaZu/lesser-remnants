/*
 * Copyright 2024 Ilya Zushinskiy
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

import rotp.model.empires.EmpireView;

public class DiplomaticReplies {
	
	public static DiplomaticReply announceTrade(EmpireView view, int amount, int turnOccurred) {
		String remark = DialogueManager.current().randomMessage(DialogueManager.ANNOUNCE_TRADE, view);
		
		remark = remark.replace("[my_empire]", view.owner().name());
		remark = remark.replace("[your_empire]", view.empire().name());
		remark = remark.replace("[your_name]", view.empire().leader().name());
		remark = remark.replace("[amt]", Integer.toString(amount));
		remark = remark.replace("[year]", Integer.toString(turnOccurred));
		
		return new DiplomaticReply(true, remark);
	}
	
	public static DiplomaticReply acceptTrade(EmpireView view, int amount) {
		String remark = DialogueManager.current().randomMessage(DialogueManager.ACCEPT_TRADE, view);
		
		remark = remark.replace("[my_empire]", view.owner().name());
		remark = remark.replace("[your_empire]", view.empire().name());
		remark = remark.replace("[your_race]", view.empire().raceName());
		remark = remark.replace("[your_name]", view.empire().leader().name());
		remark = remark.replace("[amt]", Integer.toString(amount));
		
		return new DiplomaticReply(true, remark);
	}
}

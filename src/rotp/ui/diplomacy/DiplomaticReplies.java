/*
 * Copyright 2015-2020 Ray Fowler
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

import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.tech.Tech;

public class DiplomaticReplies {
	
	public static DiplomaticReply announceTrade(EmpireView view, int amount, int turnOccurred) {
		String remark = baseRemark(DialogueManager.ANNOUNCE_TRADE, view);
		
		remark = replaceEmpireTokens(remark, "my", view.owner());
		remark = replaceEmpireTokens(remark, "your", view.empire());
		remark = remark.replace("[amt]", Integer.toString(amount));
		remark = remark.replace("[year]", Integer.toString(turnOccurred));
		
		return new DiplomaticReply(true, remark);
	}
	
	public static DiplomaticReply acceptTrade(EmpireView view, int amount) {
		String remark = baseRemark(DialogueManager.ACCEPT_TRADE, view);
		
		remark = replaceEmpireTokens(remark, "my", view.owner());
		remark = replaceEmpireTokens(remark, "your", view.owner());
		remark = remark.replace("[amt]", Integer.toString(amount));
		
		return new DiplomaticReply(true, remark);
	}
	
	public static DiplomaticReply acceptFinancialAid(EmpireView view, int amount) {
		String remark = baseRemark(DialogueManager.ACCEPT_FINANCIAL_AID, view);
		
		remark = replaceEmpireTokens(remark, "my", view.owner());
		remark = remark.replace("[amt]", Integer.toString(amount));
		
		return new DiplomaticReply(true, remark);
	}
	
	public static DiplomaticReply acceptTechnologyAid(EmpireView view, Tech tech) {
		String remark = baseRemark(DialogueManager.ACCEPT_TECHNOLOGY_AID, view);
		
		remark = replaceEmpireTokens(remark, "my", view.owner());
		remark = remark.replace("[tech]", tech.name());
		
		return new DiplomaticReply(true, remark);
	}
	
	private static String baseRemark(String type, EmpireView view) {
		return DialogueManager.current().randomMessage(type, view);
	}
    
    private static String replaceEmpireTokens(String remark, String prefix, Empire empire) {
        String fullPrefix = "[" + prefix + "_";
        int fullPrefixSize = fullPrefix.length();
        int startIndex = remark.indexOf(fullPrefix, 0);
        while (startIndex != -1) {
            int endIndex = remark.indexOf(']', startIndex);
            if (endIndex == -1)
                break;
            String token = remark.substring(startIndex+fullPrefixSize-1, endIndex);
            remark = replaceEmpireToken(remark, prefix, token, empire);
            startIndex = remark.indexOf(fullPrefix, startIndex+1);
        }
        return remark;
    }
    
    private static String replaceEmpireToken(String remark, String prefix, String token, Empire empire) {
        String target = "[" + prefix + token +"]";
        if (token.equals("_name")) {
        	// leader name is special case, not in dictionary
            remark = remark.replace(target, empire.leader().name());
        }
        else if (token.equals("_home")) {
        	int capitalId = empire.capitalSysId();
            remark = remark.replace(target, empire.sv.name(capitalId));     
        }
        else {
            String value = empire.label(token);
            remark = remark.replace(target, value);
        }
        return remark;
    }
	
}

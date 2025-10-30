/*
 * Copyright 2015-2020 Ray Fowler
 * Copyright 2024-2025 Ilya Zushinskiy
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
        StringBuilder remark = baseRemark(DialogueManager.ANNOUNCE_TRADE, view);
        replaceToken(remark, "[amt]", Integer.toString(amount));
        replaceToken(remark, "[year]", Integer.toString(turnOccurred));
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply acceptTrade(EmpireView view, int amount) {
        StringBuilder remark = baseRemark(DialogueManager.ACCEPT_TRADE, view);
        replaceToken(remark, "[amt]", Integer.toString(amount));
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply announcePact(EmpireView view, int turnOccurred) {
        StringBuilder remark = baseRemark(DialogueManager.ANNOUNCE_PACT, view);
        replaceToken(remark, "[year]", Integer.toString(turnOccurred));
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply acceptPact(EmpireView view) {
        StringBuilder remark = baseRemark(DialogueManager.ACCEPT_PACT, view);
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply announceAlliance(EmpireView view, int turnOccurred) {
        StringBuilder remark = baseRemark(DialogueManager.ANNOUNCE_ALLIANCE, view);
        replaceToken(remark, "[year]", Integer.toString(turnOccurred));
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply acceptAlliance(EmpireView view) {
        StringBuilder remark = baseRemark(DialogueManager.ACCEPT_ALLIANCE, view);
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply acceptFinancialAid(EmpireView view, int amount) {
        StringBuilder remark = baseRemark(DialogueManager.ACCEPT_FINANCIAL_AID, view);
        replaceToken(remark, "[amt]", Integer.toString(amount));
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply acceptTechnologyAid(EmpireView view, Tech tech) {
        StringBuilder remark = baseRemark(DialogueManager.ACCEPT_TECHNOLOGY_AID, view);
        replaceToken(remark, "[tech]", tech.name());
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply announceHateWar(EmpireView view) {
        StringBuilder remark = baseRemark(DialogueManager.DECLARE_HATE_WAR, view);
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply acceptJointWar(EmpireView view, Empire target, int turnOccurred) {
        StringBuilder remark = baseRemark(DialogueManager.ACCEPT_JOINT_WAR, view);
        replaceEmpireTokens(remark, "other", target);
        replaceToken(remark, "[year]", Integer.toString(turnOccurred));
        return new DiplomaticReply(true, remark.toString());
    }
    
    public static DiplomaticReply declineOffer(EmpireView view) {
        StringBuilder remark = baseRemark(DialogueManager.DECLINE_OFFER, view);
        return new DiplomaticReply(false, remark.toString());
    }
    
    private static StringBuilder baseRemark(String type, EmpireView view) {
        String remarkString = DialogueManager.current().randomMessage(type, view);
        StringBuilder remark = new StringBuilder(remarkString);
        
        replaceEmpireTokens(remark, "my", view.owner());
        replaceEmpireTokens(remark, "your", view.empire());
        
        return remark;
    }
    
    private static void replaceEmpireTokens(StringBuilder remark, String prefix, Empire empire) {
        String fullPrefix = "[" + prefix + "_";
        int fullPrefixLength = fullPrefix.length();
        int startIndex = remark.indexOf(fullPrefix, 0);
        while (startIndex != -1) {
            int endIndex = remark.indexOf("]", startIndex);
            if (endIndex == -1)
                break;
            
            String token = remark.substring(startIndex + fullPrefixLength - 1, endIndex);
            String value = getEmpireTokenValue(token, empire);
            String fullToken = "[" + prefix + token + "]";
            remark.replace(startIndex, startIndex + fullToken.length(), value);
            
            startIndex = remark.indexOf(fullPrefix, startIndex+1);
        }
    }
    
    private static String getEmpireTokenValue(String token, Empire empire) {
        if (token.equals("_name")) {
            // leader name is special case, not in dictionary
            return empire.leader().name();
        }
        else if (token.equals("_home")) {
            int capitalId = empire.capitalSysId();
            return empire.sv.name(capitalId);
        }
        else {
            return empire.label(token);
        }
    }
    
    private static void replaceToken(StringBuilder remark, String token, String value) {
        int tokenLength = token.length();
        int startIndex = remark.indexOf(token, 0);
        while (startIndex != -1) {
            remark.replace(startIndex, startIndex + tokenLength, value);
            startIndex = remark.indexOf(token, startIndex);
        }
    }
    
}

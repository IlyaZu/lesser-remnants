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
package rotp.ui.diplomacy;

import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;

public class OfferPactMessage extends TurnNotificationMessage {
    public OfferPactMessage(String  s) {
        messageType = s;
    }
    @Override
    public void diplomat(Empire e)  {
        super.diplomat(e);
    }
    @Override
    public int numReplies()               { return 2; }
    @Override
    public boolean enabled(int i)       { return true; }
    @Override
    public String reply(int i)          {
        switch (i) {
            case 0 : return text("DIPLOMACY_ACCEPT_FORM_PACT");
            case 1 : return text("DIPLOMACY_DECLINE_OFFER");
        }
        return "";
    }
    @Override
    public void select(int i) {
        log("OfferPactMessage - selected: ", str(i));
        switch(i) {
        case 0:
            EmpireView view = player().viewForEmpire(diplomat());
            view.embassy().signPact();
            
            DiplomaticReply reply = DiplomaticReplies.announcePact(view, galaxy().currentTurn());
            reply.resumeTurn(true);
            DiplomaticMessage.reply(DiplomacyRequestReply.create(diplomat(), reply));
            break;
        case 1:
        default:
            escape(); break;
        }
    }
    @Override
    public void escape() {
        player().diplomatAI().refuseOfferPact(diplomat());
        session().resumeNextTurnProcessing();
    }
    @Override
    public String decode(String encodedMessage) {
        String s1 = super.decode(encodedMessage);
        return s1;
    }
}
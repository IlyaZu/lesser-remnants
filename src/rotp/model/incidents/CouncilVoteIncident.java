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
package rotp.model.incidents;

import rotp.model.empires.Empire;
import rotp.ui.diplomacy.DialogueManager;

public class CouncilVoteIncident extends DiplomaticIncident {
    private static final long serialVersionUID = 1L;
    
    private final int voteeId; // who was voted for... could be null on abstention
    private final int voterId; // the voter getting the praise/warning
    private final int candidateId; // the candidate giving the praise/warning
    private final int rivalId; // the candidate's rival

    public static void create(Empire candidate, Empire voter, Empire votee, Empire rival) {
        if (candidate == voter)
            return;
        candidate.diplomatAI().noticeIncident(new CouncilVoteIncident(candidate, voter, votee, rival), voter);
    }
    private CouncilVoteIncident(Empire candidate, Empire voter, Empire votee, Empire rival) {
        voteeId = votee == null ? Empire.NULL_ID : votee.id;
        voterId = voter.id;
        candidateId = candidate.id;
        rivalId = rival.id;
        severity = calculateSeverity();
        turnOccurred = galaxy().currentTurn();
        duration = 10;
    }
    @Override
    public String title()               { return text("INC_COUNCIL_VOTE_TITLE"); }
    @Override
    public String description() {
        if (candidateId == voteeId)
            return decode(text("INC_COUNCIL_VOTE_FOR_DESC"));
        else if (voteeId == Empire.NULL_ID)
            return decode(text("INC_COUNCIL_ABSTAIN_DESC"));
        else
            return decode(text("INC_COUNCIL_VOTE_AGAINST_DESC"));
    }
    private float calculateSeverity() {
        if (voteeId == Empire.NULL_ID) {
            // Abstain
        	return -10;
        } else if (voteeId == candidateId) {
        	// Voted for
        	return 25;
        } else {
        	// Voted against
        	return -20;
        }
    }
    @Override
    public String praiseMessageId()   { return severity > 0 ? DialogueManager.PRAISE_COUNCIL_VOTE : ""; }
    @Override
    public String warningMessageId() {  return severity < 0 ? DialogueManager.WARNING_COUNCIL_VOTE : ""; }
    @Override
    public String key() {
        return concat(str(turnOccurred), ":CouncilVote");
    }
    @Override
    public String decode(String s) {
        String s1 = super.decode(s);
        s1 = galaxy().empire(voterId).replaceTokens(s1, "voter");
        s1 = galaxy().empire(candidateId).replaceTokens(s1, "candidate");
        s1 = galaxy().empire(rivalId).replaceTokens(s1, "rival");
        s1 = galaxy().empire(rivalId).replaceTokens(s1, "other");  // sometimes used instead of rival
        return s1;
    }
}

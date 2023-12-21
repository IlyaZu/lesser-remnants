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
package rotp.model.empires;

import java.io.Serializable;
import rotp.model.incidents.TradeIncomeIncident;
import rotp.ui.notifications.TradeTreatyMaturedAlert;
import rotp.util.Base;

public class TradeRoute implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int UNIT = 25;

    private final int emp1;
    private final int emp2;
    private int level = 0;
    private float profit = 0;
    private float ownerProd = 0;
    private float civProd = 0;

    public TradeRoute (EmpireView view) {
        emp1 = view.owner().id;
        emp2 = view.empire().id;
    }

    public float profit() {
    	return profit;
    }

    public boolean atFullLevel() {
    	return profit >= level;
    }

    public int level() {
    	return level;
    }

    public int maxLevel() {
        float maxLevel = min(civProd,ownerProd) / 4;
        return maxLevel < UNIT ? 0 : ((int)(maxLevel / UNIT) * UNIT);
    }

    public boolean active() {
    	return level > 0;
    }

    private EmpireView view() {
    	return galaxy().empire(emp1).viewForEmpire(emp2);
    }

    public void assessTurn() {
        if (!view().inEconomicRange()) {
            stopRoute();
            return;
        }
        
        // anticipate maxLevel() potentially dropping as empires shrink
        civProd = view().empire().totalPlanetaryProduction();
        ownerProd = view().owner().totalPlanetaryProduction();
        if (level > maxLevel())
            level = maxLevel();
        
        float prevProfit = profit;
        
        float pct = (roll(1,200) + view().embassy().relations() + 25) / 6000.0f;
        profit = min(maxProfit(), profit + (pct * level) );
        if ((profit == level) && (profit > prevProfit)) {
            if (view().owner().isPlayer())
               TradeTreatyMaturedAlert.create(view().empId(), level);
        }
        if (active())
            TradeIncomeIncident.create(view(), profit, profit/ownerProd);
    }

    public void setContact() {
        civProd = view().empire().totalPlanetaryProduction();
        ownerProd = view().owner().totalPlanetaryProduction();
    }

    public void startRoute(int newLevel) {
        float newTrade = newLevel - level;
        if (newTrade <= 0)
            return;
        
        float newPct = ((profit/newTrade) + startPct()) * (newTrade/newLevel);

        profit = newPct * newLevel;
        level = newLevel;
        view().owner().flagColoniesToRecalcSpending();

        //if not done yet, increase the route on the "other" side of the relationship
        EmpireView otherView = view().otherView();
        if (otherView.trade().level() != newLevel)
            otherView.trade().startRoute(newLevel);
    }

    public void stopRoute() {
        level = 0;
        profit = 0;
        view().owner().flagColoniesToRecalcSpending();
        
        EmpireView otherView = view().otherView();
        if (otherView.trade().active())
            otherView.trade().stopRoute();
    }

    private float maxProfit() {
        return level * (1 + view().owner().tradePctBonus());
    }

    private float startPct() {
        return -.3f + view().owner().tradePctBonus();
    }
}
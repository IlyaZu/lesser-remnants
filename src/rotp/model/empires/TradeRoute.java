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
package rotp.model.empires;

import java.io.Serializable;
import rotp.model.incidents.TradeIncomeIncident;
import rotp.ui.notifications.TradeTreatyMaturedAlert;
import rotp.util.Base;

public class TradeRoute implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int UNIT = 25;
    private static final float INITIAL_PROFIT_FACTOR = -.3f;

    private final EmpireView view;
    private int profitLimit = 0;
    private float currentProfit = 0;
    private float ownerProd = 0;
    private float civProd = 0;

    public TradeRoute (EmpireView view) {
        this.view = view;
    }

    public float currentProfit() {
        return currentProfit;
    }

    public boolean atProfitLimit() {
        return currentProfit >= profitLimit;
    }

    public float profitLimit() {
        return profitLimit;
    }

    public int maxProfitLimit() {
        float tradeBonus = 1 + Math.max(view.owner().tradePctBonus(), view.empire().tradePctBonus());
        float maxProfitLimit = tradeBonus * Math.min(civProd, ownerProd) / 4;
        return maxProfitLimit < UNIT ? 0 : ((int)(maxProfitLimit / UNIT) * UNIT);
    }

    public boolean active() {
        return profitLimit > 0;
    }

    public void assessTurn() {
        if (!view.inEconomicRange()) {
            stopRoute();
            return;
        }
        
        // anticipate maxProfitLimit() potentially dropping as empires shrink
        civProd = view.empire().totalPlanetaryProduction();
        ownerProd = view.owner().totalPlanetaryProduction();
        if (profitLimit > maxProfitLimit())
            profitLimit = maxProfitLimit();
        
        float prevProfit = currentProfit;
        
        float pct = (roll(1,200) + view.embassy().relations() + 25) / 6000.0f;
        currentProfit = min(profitLimit, currentProfit + (pct * profitLimit) );
        if (atProfitLimit() && currentProfit > prevProfit) {
            if (view.owner().isPlayer())
               TradeTreatyMaturedAlert.create(view.empId(), profitLimit);
        }
        if (active())
            TradeIncomeIncident.create(view, currentProfit, currentProfit/ownerProd);
    }

    public void setContact() {
        civProd = view.empire().totalPlanetaryProduction();
        ownerProd = view.owner().totalPlanetaryProduction();
    }

    public void startRoute(int newProfitLimit) {
        float profitLimitDelta = newProfitLimit - profitLimit;
        if (profitLimitDelta <= 0)
            return;
        
        float newPct = ((currentProfit/profitLimitDelta) + INITIAL_PROFIT_FACTOR) * (profitLimitDelta/newProfitLimit);

        currentProfit = newPct * newProfitLimit;
        profitLimit = newProfitLimit;
        view.owner().flagColoniesToRecalcSpending();

        //if not done yet, increase the route on the "other" side of the relationship
        EmpireView otherView = view.otherView();
        if (otherView.trade().profitLimit() != newProfitLimit)
            otherView.trade().startRoute(newProfitLimit);
    }

    public void stopRoute() {
        profitLimit = 0;
        currentProfit = 0;
        view.owner().flagColoniesToRecalcSpending();
        
        EmpireView otherView = view.otherView();
        if (otherView.trade().active())
            otherView.trade().stopRoute();
    }
}
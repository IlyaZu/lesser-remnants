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
package rotp.model.combat;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.empires.Empire;
import rotp.model.empires.ShipView;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipWeapon;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.tech.TechCloaking;
import rotp.model.tech.TechStasisField;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public class CombatShip extends CombatEntity {
    private final int initiative;
    private final boolean isTeleporting;
    private final boolean isScanning;
    private final float missChance;
    private final float blackHoleDefence;
    private final int repulsorRange;
    private final int beamRangeBonus;
    private final float beamShieldMod;
    
    private final List<ShipSpecial> specials;

    private final List<ShipComponent> weapons = new ArrayList<>();
    private int selectedWeaponIndex = 0;
    
    private final int[] weaponCount = new int[7];
    public final int[] weaponAttacks = new int[7];
    private final int[] shotsRemaining = new int[7];
    public final int[] roundsRemaining = new int[7]; // how many rounds you can fire (i.e. missiles)
    private final int[] baseTurnsToFire = new int[7]; // how many turns to wait before you can fire again
    private final int[] wpnTurnsToFire = new int[7]; // how many turns to wait before you can fire again
    
    private CombatEntity ward;

    private final String name;
    
    public CombatShip(int count, float hits, float shield,
            int attack, int beamDefense, int missileDefense,
            int maneuverability, int move, int initiative,
            List<WeaponGroup> weaponGroups, List<ShipSpecial> specials,
            Image image, String name,
            ShipCaptain captian, CombatManager manager) {
        
        this.mgr = manager;
        this.captain = captian;
        this.origNum = this.num = count;
        this.startingMaxHits = this.maxHits = this.hits = hits;
        this.maxShield = this.shield = manager.system().inNebula() ? 0 : shield;
        this.attackLevel = attack;
        this.beamDefense = beamDefense;
        this.missileDefense = missileDefense;
        this.maneuverability = maneuverability;
        this.maxMove = this.move = move;
        this.initiative = initiative;
        this.image = image;
        this.name = name;

        for (WeaponGroup weaponGroup : weaponGroups) {
            ShipWeapon weapon = weaponGroup.getWeapon();
            this.weaponCount[this.weapons.size()] = weaponGroup.getCount();
            this.weaponAttacks[this.weapons.size()] = weapon.attacksPerRound();
            this.roundsRemaining[this.weapons.size()] = weapon.shots();
            this.baseTurnsToFire[this.weapons.size()] = weapon.turnsToFire();
            this.wpnTurnsToFire[this.weapons.size()] = 1;
            this.weapons.add(weaponGroup.getWeapon());
        }
        
        boolean isTeleporting = false;
        boolean isScanning = false;
        float missChance = 0.0f;
        float blackHoleDefence = 0.0f;
        int repulsorRange = 0;
        int beamRangeBonus = 0;
        float beamShieldMod = 1.0f;
        for (ShipSpecial special : specials) {
            if (special.isWeapon()) {
                this.weaponCount[this.weapons.size()] = 1;
                this.weaponAttacks[this.weapons.size()] = 1;
                this.roundsRemaining[this.weapons.size()] = 1;
                this.baseTurnsToFire[this.weapons.size()] = 1;
                this.wpnTurnsToFire[this.weapons.size()] = 1;
                this.weapons.add(special);
            }
            if (special.allowsCloaking()) {
                this.canCloak = true;
            }
            if (special.allowsTeleporting()) {
                isTeleporting = true;
            }
            if (special.allowsScanning()) {
                isScanning = true;
            }
            missChance = Math.max(missChance, special.missPct());
            repairPct = Math.max(repairPct, special.shipRepairPct());
            blackHoleDefence = Math.max(blackHoleDefence, special.blackHoleDef());
            repulsorRange = Math.max(repulsorRange, special.repulsorRange());
            beamRangeBonus += special.beamRangeBonus();
            beamShieldMod *= special.beamShieldMod();
        }
        this.isTeleporting = isTeleporting;
        this.isScanning = isScanning;
        this.missChance = missChance;
        this.blackHoleDefence = blackHoleDefence;
        this.repulsorRange = repulsorRange;
        this.beamRangeBonus = beamRangeBonus;
        this.beamShieldMod = beamShieldMod;
        
        this.specials = specials;

        System.arraycopy(weaponAttacks, 0, shotsRemaining, 0, shotsRemaining.length);
        
        cloak();
    }

    @Override
    public String toString() {
        if (target != null)
            return concat(shortString(), "  targeting: [", target.shortString(), "]");
        else
            return shortString();
    }
    
    @Override
    public String shortString() {
        return concat(name, " hp: ", str((int)hits), "/", str((int)maxHits), " at:", str(x), ",", str(y));
    }
    
    @Override
    public String name()             { return str(num)+":"+name; }
    @Override
    public CombatEntity ward()             { return ward; }
    @Override
    public boolean hasWard()              { return ward != null; }
    @Override
    public void ward(CombatEntity st)      { ward = st; }
    @Override
    public int repulsorRange()            { return repulsorRange; }
    @Override
    public int numWeapons()               { return weapons.size(); }
    @Override
    public ShipComponent weapon(int i)    { return weapons.get(i); }
    @Override
    public boolean hasTeleporting() { return isTeleporting; }
    @Override
    public boolean canScan()        { return isScanning; }
    @Override
    public float autoMissPct()      { return missChance; }
    @Override
    public float bombDamageMod()   { return 0; }
    @Override
    public float blackHoleDef()    { return blackHoleDefence; }
    @Override
    public boolean ignoreRepulsors()    { return cloaked || canTeleport(); }
    @Override
    public void becomeDestroyed()    {
        super.becomeDestroyed();
        for (ShipComponent c: weapons)
            c.becomeDestroyed();
    }
    @Override
    public boolean canFireWeapon() {
        for (CombatEntity st: mgr.activeStacks()) {
            if ((empire != st.empire) && canAttack(st))
                return true;
        }
        return false;
    }
    @Override
    public boolean canFireWeaponAtTarget(CombatEntity st) {
        if (st == null)
            return false;
        if (st.inStasis)
            return false;
        for (int i=0;i<weapons.size();i++) {
            ShipComponent comp = weapons.get(i);
            if (!comp.isSpecial() && shipComponentCanAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean hasBombs() {
        for (int i=0; i<weapons.size();i++) {
            ShipComponent comp = weapons.get(i);
            if (comp.groundAttacksOnly() && (roundsRemaining[i] > 0))
                return true;
        }
        return false;
    }
    @Override
    public int maxFiringRange(CombatEntity tgt) {
        int maxRange = 0;
        for (int i=0;i<weapons.size();i++) {
            ShipComponent wpn = weapons.get(i);
            if (wpn.groundAttacksOnly() && !tgt.isColony())
                continue;
            if (roundsRemaining[i]>0)
                maxRange = max(maxRange,weaponRange(wpn));
        }
        return maxRange;
    }
    @Override
    public int optimalFiringRange(CombatEntity tgt) {
        // if only missile weapons, use that range
        // else use beam weapon range;
        int missileRange = -1;
        int weaponRange = -1;
        
        for (int i=0;i<weapons.size();i++) {
            ShipComponent wpn = weapons.get(i);
            // if we are bombing a planet, ignore other weapons
            //ail: if we count specials as weapons, we'll never get close when we have long-range-specials but short range-weapons
            if(wpn.isSpecial())
                continue;
            if (tgt.isColony() && wpn.groundAttacksOnly())
                return 1;
            else if (wpn.isMissileWeapon())
            {
                if(roundsRemaining[i] > 0)
                {
                    float targetBackOffRange = 2 * tgt.maxMove();
                    if(distanceTo(0, 0) > tgt.distanceTo(0, 0))
                        targetBackOffRange = min(targetBackOffRange, tgt.distanceTo(0, 0));
                    if(distanceTo(0, CombatManager.maxY) > tgt.distanceTo(0, CombatManager.maxY))
                        targetBackOffRange = min(targetBackOffRange, tgt.distanceTo(0, CombatManager.maxY));
                    if(distanceTo(CombatManager.maxX, 0) > tgt.distanceTo(CombatManager.maxX, 0))
                        targetBackOffRange = min(targetBackOffRange, tgt.distanceTo(CombatManager.maxX, 0));
                    if(distanceTo(CombatManager.maxX, CombatManager.maxY) > tgt.distanceTo(CombatManager.maxX, CombatManager.maxY))
                        targetBackOffRange = min(targetBackOffRange, tgt.distanceTo(CombatManager.maxX, CombatManager.maxY));
                    int curr = (int)(max(1, (weaponRange(wpn) - targetBackOffRange) / sqrt(2) + 0.7f));
                    if(missileRange > 0)
                        missileRange = min(missileRange, curr);
                    else
                        missileRange = curr;
                }
            }
            else if(!wpn.groundAttacksOnly())
            {
                if(useSmartRangeForBeams())
                {
                    if(tgt.maxFiringRange(this) > repulsorRange()) //If our enemy has a bigger range than our repulsors we close in no matter what
                        weaponRange = 1;
                    else
                        weaponRange = min(repulsorRange() + 1, max(weaponRange, weaponRange(wpn))); //ail: optimal firing-range for beam-weapons should be as close as possible but still take advantage of repulsor
                }
                else
                    weaponRange = weaponRange(wpn); //Use longest range for base-AI as it otherwise can't deal with repulsor-beam-ships because it doesn't have a loop around it's path-finding trying bigger ranges when range 1 is blocked
            }
        }
        return max(missileRange, weaponRange);
    }
    protected boolean useSmartRangeForBeams() {
        return false;
    }
    @Override
    public float missileInterceptPct(ShipWeaponMissileType wpn)   {
        float maxIntercept = 0.0f;
        for (ShipSpecial special : specials) {
            maxIntercept = Math.max(maxIntercept, special.missileIntercept(wpn));
        }
        return maxIntercept;
    }
    @Override
    public int wpnCount(int i) { return weaponCount[i]; }
    @Override
    public int shotsRemaining(int i) { return shotsRemaining[i]; }
    @Override
    public void reloadWeapons() {
        System.arraycopy(weaponAttacks, 0, shotsRemaining, 0, shotsRemaining.length);
        
        for (ShipComponent c: weapons)
            c.reload();
        //ail: reset selectedWeaponIndex too, so that ship will consistently start from the same weapon each new turn
        selectedWeaponIndex = 0;
    }
    @Override
    public void endTurn() {
        super.endTurn();
        boolean anyWeaponFired = false;
        for (int i=0;i<shotsRemaining.length;i++) {
            boolean thisWeaponFired = shotsRemaining[i]<weaponAttacks[i];
            anyWeaponFired = anyWeaponFired || thisWeaponFired;
            wpnTurnsToFire[i] = thisWeaponFired ? baseTurnsToFire[i] : wpnTurnsToFire[i]-1;
        }
        
        if (!anyWeaponFired)
            cloak();
    }
    private void cloak() {
        if (!cloaked && canCloak) {
            cloaked = true;
            transparency = TechCloaking.TRANSPARENCY;
        }
    }
    private void uncloak() {
        if (cloaked) {
            cloaked = false;
            transparency = 1;
        }
    }
    @Override
    public int initiative() {
        if (cloaked)
            return 200+initiative;
        // modnar: replace canTeleport from this 'if' check
        // In ShipCombatManager.java, the CombatStack.INITIATIVE comparison/sort in setupBattle
        // is called before currentStack.beginTurn(). So while beginTurn() in this file
        // sets the correct value for canTeleport, it won't be used for initiative ordering.
        // This change correctly gives boosted turn/initiative order for ship stacks with teleporters.
        else if (hasTeleporting() && !mgr.interdiction())
            return 100+initiative;
        else
            return initiative;
    }
    @Override
    public boolean selectBestWeapon(CombatEntity target) {
        if (target.destroyed())
            return false;
        if (shipComponentCanAttack(target, selectedWeaponIndex))
            return true;

        rotateToUsableWeapon(target);
        return shipComponentCanAttack(target, selectedWeaponIndex);
    }
    private void rotateToUsableWeapon(CombatEntity target) {
        int i = selectedWeaponIndex;
        int j = i;
        boolean looking = true;
        
        while (looking) {
            j++;
            if (j == weapons.size())
                j = 0;
            selectedWeaponIndex = j;
            if ((j == i) || shipComponentCanAttack(target, j))
                looking = false;
        }
    }
    @Override
    public void fireWeapon(CombatEntity targetStack) {
        fireWeapon(targetStack, selectedWeaponIndex, false);
    }
    @Override
    public void fireWeapon(CombatEntity targetStack, int index, boolean allShots) {
        if (targetStack == null)
            return;

        if (targetStack.destroyed())
            return;
        selectedWeaponIndex = index;
        target = targetStack;
        target.damageSustained = 0;
        int shotsTaken = allShots ? shotsRemaining[index] : 1;

        // only fire if we have shots remaining... this is a missile concern
        if ((roundsRemaining[index] > 0) && (shotsRemaining[index] > 0)) {
            shotsRemaining[index] = shotsRemaining[index]-shotsTaken;
            uncloak();
            ShipComponent selectedWeapon = weapons.get(selectedWeaponIndex);
            // some weapons (beams) can fire multiple per round
            int count = num*shotsTaken*weaponCount[index];
            if (selectedWeapon.isMissileWeapon()) {
                CombatMissile missile = new CombatMissile(this, (ShipWeaponMissileType) selectedWeapon, count);
                mgr.addStackToCombat(missile);
            }
            else {
                selectedWeapon.fireUpon(this, target, count);
            }
            if (selectedWeapon.isLimitedShotWeapon())
                roundsRemaining[index] = max(0, roundsRemaining[index]-1);
            if (target == null) {
                return;
            }
        }

        if (shotsRemaining[index] == 0)
            rotateToUsableWeapon(targetStack);
        target.damageSustained = 0;
    }
    @Override
    public boolean canAttack(CombatEntity st) {
        if (st == null)
            return false;
        if (num <= 0)
            return false;
        if (st.inStasis)
            return false;
        for (int i=0;i<weapons.size();i++) {
            if (shipComponentCanAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean canPotentiallyAttack(CombatEntity st) {
        for (int i=0;i<weapons.size();i++) {
            if (shipComponentCanPotentiallyAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean isArmed() {
        for (int i=0;i<weapons.size();i++) {
            if (roundsRemaining[i] > 0) {
                int empireId = empire != null ? empire.id : Empire.NULL_ID;
                // armed if: weapons are not bombs or if not allied with planet (& can bomb it)
                if (!weapons.get(i).groundAttacksOnly())
                    return true;
                if (mgr.system().isColonized() && !mgr.system().empire().alliedWith(empireId))
                    return true;
            }
        }
        return false;
    }
    @Override
    public float estimatedKills(CombatEntity target) {
        float kills = 0;
        for (int i=0;i<weapons.size();i++) {
            ShipComponent comp = weapons.get(i);
            if (!comp.isLimitedShotWeapon() || (roundsRemaining[i] > 0))
            {
                //ail: take attack and defense into account
                float hitPct = 1.0f;
                if(comp.isBeamWeapon())
                    hitPct = (5 + attackLevel - target.beamDefense()) / 10;
                if(comp.isMissileWeapon())
                    hitPct = (5 + attackLevel - target.missileDefense()) / 10;
                hitPct = max(.05f, hitPct);
                hitPct = min(hitPct, 1.0f);
                //ail: we totally have to consider the weapon-count too!
                kills += hitPct * comp.estimatedKills(this, target, weaponCount[i] * num);
            }
        }
        return kills;
    }
    public boolean shipComponentCanAttack(CombatEntity target, int index) {
        if (target == null)
            return false;

        if (target.inStasis || target.isMissile())
            return false;

        if (index >= weapons.size())
            return false;
        
        ShipComponent shipWeapon = weapons.get(index);

        if ((shipWeapon == null) || !shipWeapon.isWeapon())
            return false;

        if (shotsRemaining[index] < 1)
            return false;
        
        if (wpnTurnsToFire[index] > 1)
            return false;

        if (shipWeapon.isLimitedShotWeapon() && (roundsRemaining[index] < 1))
            return false;

        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;

        int minMove = movePointsTo(target);
        if (weaponRange(shipWeapon) < minMove)
            return false;

        return true;
    }
    private boolean shipComponentCanPotentiallyAttack(CombatEntity target, int index) {
        if (target == null)
            return false;

        if (target.isMissile())
            return false;

        ShipComponent shipWeapon = weapons.get(index);

        if ((shipWeapon == null) || !shipWeapon.isWeapon())
            return false;

        if (shipWeapon.isLimitedShotWeapon() && (roundsRemaining[index] < 1))
            return false;

        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;

        return true;
    }
    @Override
    public int weaponNum(ShipComponent comp) {
        return weapons.indexOf(comp);
    }
    @Override
    public boolean shipComponentIsUsed(int index) {
        return shotsRemaining[index] < 1 || (roundsRemaining[index] < 1) || (wpnTurnsToFire[index] > 1);
    }
    @Override
    public boolean shipComponentIsOutOfMissiles(int index) {
        return weapon(index).isMissileWeapon() && roundsRemaining[index] == 0;
    }
    @Override
    public boolean shipComponentIsOutOfBombs(int index) {
        return weapon(index).groundAttacksOnly() && roundsRemaining[index] == 0;
    }
    @Override
    public String wpnName(int i) {
        ShipComponent wpn = weapons.get(i);
        if (wpn.isLimitedShotWeapon())
            return wpn.name()+":"+str(roundsRemaining[i]);
        else
            return wpn.name();
    }

    @Override
    public boolean shipComponentValidTarget(int index, CombatEntity target) {
        ShipComponent shipWeapon = weapons.get(index);
        if (target == null)
            return false;
        if (empire == target.empire)
            return false;
        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;
        return true;
    }
    @Override
    public boolean shipComponentInRange(int index, CombatEntity target) {
        ShipComponent shipWeapon = weapons.get(index);
        int minMove = movePointsTo(target);
        if (weaponRange(shipWeapon) < minMove)
            return false;
        return true;
    }
    @Override
    public float targetShieldMod(ShipComponent c) {
        if (c.isBeamWeapon()) {
            return beamShieldMod;
        }
        return 1.0f;
    }
    
    private int weaponRange(ShipComponent c) {
        if (!c.isBeamWeapon()) {
            return c.range();
        }
        return c.range()+beamRangeBonus;
    }
    
    @Override
    public void drawStack(ShipBattleUI ui, Graphics2D g, int origCount, int x, int y, int stackW, int stackH) {
        Image img = image;

        int w0 = img.getWidth(null);
        int h0 = img.getHeight(null);
        float scale0 = min((float)stackW/w0, (float)stackH/h0)*9/10;

        int x1 = x;
        int y1 = y;
        int w1 = (int)(scale0*w0);
        int h1 = (int)(scale0*h0);

        int s1 = scaled(1);
        int s2 = scaled(2);
        
        if (scale != 1.0f) {
            int prevW = w1;
            int prevH = h1;
            w1 = (int) (w1*scale);
            h1 = (int) (h1*scale);
            x1 = x1 +(prevW-w1)/2;
            y1 = y1 +(prevH-h1)/2;
        }

        Composite prevComp = g.getComposite();
        if (transparency < 1) {
            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,transparency);
            g.setComposite(ac);
        }
        if (reversed)  // XOR
            g.drawImage(img, x1, y1, x1+w1, y1+h1, w0, 0, 0, h0, ui);
        else
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, ui);

        if (transparency < 1)
            g.setComposite(prevComp);

        if (mgr.currentStack().isEmpireShip()) {
            CombatEmpireShip shipStack = (CombatEmpireShip) mgr.currentStack();
            if (!mgr.performingStackTurn) {
                if (shipStack == this) {
                    Stroke prev = g.getStroke();
                    g.setStroke(BasePanel.stroke2);
                    g.setColor(ShipBattleUI.currentBorderC);
                    g.drawRect(x1+s1, y1+s1, stackW-s2, stackH-s2);
                    g.setStroke(prev);
                }
            }
        }

        int iconW = BasePanel.s18;
        int y2 = y+stackH-BasePanel.s5;
        g.setFont(narrowFont(16));
        int nameMgn = ui.showTacticalInfo() ? iconW + BasePanel.s5 : BasePanel.s5;
        String name = ui.showTacticalInfo() ? this.name : text("SHIP_COMBAT_COUNT_NAME", str(num), this.name);
        scaledFont(g, name, stackW-nameMgn,16,8);
        int sw2 = g.getFontMetrics().stringWidth(name);
        int x1mgn = reversed || !ui.showTacticalInfo() ? x1 : x1+iconW;
        int x2 = max(x1mgn, x1mgn+((stackW-nameMgn-sw2)/2));

        g.setColor(Color.lightGray);
        drawString(g, name, x2, y2);
        
        if (inStasis) {
            g.setColor(TechStasisField.STASIS_COLOR);
            g.fillRect(x1,y1,stackW, stackH);
            String s = text("SHIP_COMBAT_STASIS");
            g.setFont(font(20));
            g.setColor(Color.white);
            int sw = g.getFontMetrics().stringWidth(s);
            int x3 = x1+(stackW-sw)/2;
            int y3 = y1+(stackH/2);
            drawBorderedString(g, s,x3,y3, Color.black, Color.white);
        }
        
        int mgn = BasePanel.s2;
        int x4 = x+mgn;
        int y4 = y+mgn;
        int w4 = stackW-mgn-mgn;
        int barH = BasePanel.s10;
        if (ui.showTacticalInfo()) {
            // draw health bar & hp
            g.setColor(healthBarBackC);
            g.fillRect(x4, y4, w4, barH);
            int w4a = (int)(w4*hits/maxHits);
            g.setColor(healthBarC);
            g.fillRect(x4, y4, w4a, barH);
            // draw ship count
            g.setColor(healthBarC);
            String numStr = str(num);
            g.setFont(narrowFont(20));
            int numW = g.getFontMetrics().stringWidth(numStr);
            int x6 = reversed ? x4: x4+w4-numW-BasePanel.s10;
            g.fillRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
            g.setColor(Color.white);
            Stroke prevStroke = g.getStroke();
            g.setStroke(BasePanel.stroke1);
            g.drawRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
            g.setStroke(prevStroke);
            g.drawString(numStr, x6+BasePanel.s5,y4+BasePanel.s18);
            // draw hit points
            g.setColor(Color.white);
            String hpStr = ""+(int)Math.ceil(hits)+"/"+(int)Math.ceil(maxHits);
            g.setFont(narrowFont(12));
            int hpW = g.getFontMetrics().stringWidth(hpStr);
            int x5 = reversed ? x4+((w4-hpW+numW)/2) : x4+((w4-hpW-numW)/2);
            g.drawString(hpStr, x5, y4+BasePanel.s9);
                
            
            ShipView view = player().shipViewFor(design());
            if (view != null) {
                // draw shield level
                g.setColor(shieldColor);
                int x4a = reversed ? x4+w4-iconW : x4;
                int y4a =y4+barH+BasePanel.s1;
                g.fillOval(x4a, y4a, iconW, iconW);
                if (view.shieldKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(shieldLevel()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4a+BasePanel.s14);
                }
                //draw attack level
                g.setColor(attackColor);
                int y4b =y4a+iconW+BasePanel.s2;
                g.fillOval(x4a, y4b, iconW, iconW);
                if (view.attackLevelKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(attackLevel()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4b+BasePanel.s14);
                }
                //draw beam defense level
                g.setColor(beamDefenseColor);
                int y4c =y4b+iconW+BasePanel.s1;
                g.fillOval(x4a, y4c, iconW, iconW);
                if (view.beamDefenseKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(beamDefense()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4c+BasePanel.s14);
                }
                //draw missie defense level
                g.setColor(missileDefenseColor);
                int y4d =y4c+iconW+BasePanel.s1;
                g.fillOval(x4a, y4d, iconW, iconW);
                if (view.missileDefenseKnown()) {
                    g.setColor(Color.white);
                    String valStr = str((int)Math.ceil(missileDefense()));
                    g.setFont(narrowFont(16));
                    int shldW = g.getFontMetrics().stringWidth(valStr);
                    g.drawString(valStr, x4a+((iconW-shldW)/2), y4d+BasePanel.s14);
                }
            }
        }
    }
    public void drawRetreat() {
        if (!mgr.showAnimations())
            return;

        ShipBattleUI ui = mgr.ui;
        Graphics2D g = (Graphics2D) ui.getGraphics();

        Color portalColor = Color.white;
        g.setColor(portalColor);

        Rectangle rect = ui.combatGrids[x][y];

        int x0 = rect.x;
        int y0 = rect.y;
        int h0 = rect.height;
        int w0 = rect.width;
        
        playAudioClip("ShipRetreat");

        // open portal
        for (int i=0; i<10; i++) {
            ui.paintCellImmediately(x,y);
           g.setColor(portalColor);
            if (reversed)
                g.fillOval(x0+w0-(w0/16), y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            else
                g.fillOval(x0, y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            sleep(20);
        }

        // reverse ship
        reverse();
        ui.paintCellImmediately(x,y);
        g.setColor(portalColor);
        if (reversed)
            g.fillOval(x0, y0, w0/16, h0);
        else
            g.fillOval(x0+w0-(w0/16), y0, w0/16, h0);

        sleep(50);

        // move ship through portal
        g.setClip(rect);
        for (int i=0;i<25;i++) {
            offsetX = reversed ? offsetX-.04f : offsetX+.04f;
            ui.paintCellImmediately(x,y);
            g.setColor(portalColor);
            if (reversed)
                g.fillOval(x0, y0, w0/16, h0);
            else
                g.fillOval(x0+w0-(w0/16), y0, w0/16, h0);
            sleep(30);
        }
        visible = false;
        g.setClip(null);

        // close portal
        for (int i=10; i>=0; i--) {
            ui.paintCellImmediately(x,y);
            g.setColor(portalColor);
            if (reversed)
                g.fillOval(x0, y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            else
                g.fillOval(x0+w0-(w0/16), y0+(h0/2)-(i*h0/20), w0*i/160, h0*i/10);
            sleep(20);
        }
        ui.paintCellImmediately(x,y);
    }
}

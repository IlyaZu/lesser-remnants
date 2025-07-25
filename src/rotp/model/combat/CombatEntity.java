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
package rotp.model.combat;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.model.tech.TechCloaking;
import rotp.model.tech.TechStasisField;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;
import rotp.util.Base;

public abstract class CombatEntity implements Base {
    private static final float MOVE_STEP = 0.1f;
    protected static final Color healthBarC = new Color(0,96,0);
    protected static final Color healthBarBackC = new Color(0,48,0);
    public static final Color shieldColor = new Color(0,0,128);
    public static final Color attackColor = new Color(128,0,0);
    public static final Color beamDefenseColor = new Color(0,128,0);
    public static final Color missileDefenseColor = new Color(64,160,64);
    public static final Color populationColor = new Color(0,128,0);
    public static final Color factoryColor = new Color(128,0,0);
    
    public Empire empire;
    public CombatManager mgr;
    public ShipCaptain captain;
    private final List<CombatMissile> targetingMissiles = new ArrayList<>();
    public int num = 0;
    public int origNum = 0;
    public int x = 0;
    public int y = 0;
    public float scale = 1.0f;
    public float brighten = 0.0f;
    public int attackLevel = 0;
    public float maneuverability = 0;
    public float missileDefense = 0;
    public float beamDefense = 0;
    public float offsetX = 0;
    public float offsetY = 0;
    public float startingMaxHits = 1;
    public float maxHits = 1;
    public float maxMove = 0;
    public float move = 0;
    public float maxShield = 0;
    public float shield = 0;
    public float hits = 0;
    public float repairPct = 0;
    public boolean inStasis = false;
    public boolean cloaked = false;
    public boolean canCloak = false;
    public boolean canTeleport = false;
    public float damageSustained = 0;
    public boolean attacked = false;
    public CombatEntity target;
    public int distance = 0;
    public Image image;
    public boolean reversed = false;
    public boolean visible = true;
    public float transparency = 1;

    public String shortString() {
        return concat(toString()," at:", str(x), ",", str(y));
    }
    public String fullName()            { return concat(str(num), ":", raceName(), " ", name()); }
    private String raceName()            { return empire != null ? empire.raceName() : name(); }
    public abstract String name();
    public int initiative()           { return 0; }
    public boolean isEmpireShip()       { return false; }
    public boolean isColony()           { return false; }
    public boolean isMonster()          { return false; }
    public boolean isPlayerControlled() { return (empire != null) && empire.isPlayerControlled(); }
    public boolean isMissile()          { return false; }
    public boolean destroyed()          { return ((num < 1) || (maxHits <= 0)); }
    public boolean isArmed()            { return false; }
    public CombatEntity ward()           { return null; }
    public boolean hasWard()            { return false; }
    public void ward(CombatEntity st)    { }
    public boolean hasBombs()           { return false; }
    public boolean canChangeTarget()    { return true; }
    public boolean canCollide()         { return false; }
    public boolean usingAI()            { return true; }

    public int repulsorRange()          { return 0; }
    public boolean ignoreRepulsors()    { return false; }
    public int weaponNum(ShipComponent w)  { return -1; }
    public boolean canRetreat()      { return false; }
    public boolean canTeleport()     { return canTeleport && !mgr.interdiction(); }
    public boolean hasTeleporting()  { return false; }
    public boolean canScan()         { return false; }
    public void becomeDestroyed()    { num = 0;}
    public int numWeapons()          { return 0; }
    public ShipComponent weapon(int i)   { return null; }
    public ShipDesign design()       { return null; }
    public float designCost()          { return 0; }

    public void performTurn()        { captain.performTurn(this); }
    public boolean wantToRetreat()   { return captain.wantToRetreat(this); }

    public float maxHits()          { return maxHits; }
    public float maxMove()          { return maxMove; }
    public float totalHits()        { return maxHits * num; }
    public boolean canMove()        { return (move > 0) || canTeleport(); }
    public boolean canFireWeapon()  { return false; }
    public boolean canFireWeaponAtTarget(CombatEntity st)  { return false; }
    public boolean immuneToStasis() { return false; }
    public float autoMissPct()      { return 0; }
    public boolean interceptsMissile(ShipWeaponMissileType wpn)  { return random() < missileInterceptPct(wpn);}
    public float missileInterceptPct(ShipWeaponMissileType wpn)  { return 0; }
    public float maneuverablity()     { return maneuverability; }
    public float missileDefense()     { return cloaked ? missileDefense +5 : missileDefense; }
    public float beamDefense()        { return cloaked ? beamDefense + 5 : beamDefense; }
    public int attackLevel()        { return attackLevel; }
    public float bombDefense()      { return 0; }
    public float bioweaponDefense() { return 0; }
    public boolean canEat(CombatEntity st)       { return false; }
    public boolean hostileTo(CombatEntity st, StarSystem sys)                  { return empire != st.empire; }
    public boolean selectBestWeapon(CombatEntity target)       { return false; }
    public boolean canAttack(CombatEntity target)              { return false; }
    public boolean canPotentiallyAttack(CombatEntity target)   { return false; }
    public float estimatedKills(CombatEntity target)           { return 0; }
    public float estimatedKillPct(CombatEntity target)         { return target.num == 0 ? 0 : estimatedKills(target) / target.num; }
    public void fireWeapon(CombatEntity target, int i, boolean shots) { }
    public void fireWeapon(CombatEntity target, int i) { fireWeapon(target,i,false); }
    public void fireWeapon(CombatEntity target)       {  }
    public int maxFiringRange(CombatEntity tgt)       { return 1; }
    public int optimalFiringRange(CombatEntity tgt)   { return 1; }
    public float maxDamage()                         { return 0; }
    public float shieldLevel()                       { return shield; }
    public float rotateRadians(CombatEntity target)   { return radiansTo(target) + ((float)Math.PI/2); }

    public float torpedoDamageMod()                  { return 1; }
    public float beamDamageMod()                     { return 1; }
    public float bombDamageMod()                     { return 1; }
    public float missileDamageMod()                  { return 1; }
    public float blackHoleDef()                      { return 0; }
    public void assignCollateralDamage(float damage) {  }
    public boolean retreat()                         { return retreatToSystem(captain.retreatSystem(mgr.system())); }
    public boolean retreatToSystem(StarSystem s)     { return false; }

    public boolean aggressiveWith(CombatEntity st)    { return empire.aggressiveWith(st.empire, mgr.system()); }

    public void reverse()                           { reversed = !reversed; }
    public List<CombatMissile> missiles()       { return targetingMissiles; }
    public void addMissile(CombatMissile miss)  { targetingMissiles.add(miss); }

    public boolean isTurnComplete() {
        if (inStasis)
            return true;

        if (canMove())
            return false;

        if (canFireWeapon())
            return false;

        return true;
    }
    public void beginTurn() {
        move = maxMove;
        canTeleport = hasTeleporting() && !mgr.interdiction();

        reloadWeapons();
        attemptToHeal();
        List<CombatMissile> missiles = new ArrayList<>(targetingMissiles);
        for (CombatMissile miss : missiles)
            miss.beginTurn();
    }
    public void reloadWeapons() { }
    public void attemptToHeal() {
        if (hits >= startingMaxHits)
            return;
        if (repairPct <= 0)
            return;
        float healAmt = startingMaxHits*repairPct;
        hits = min(startingMaxHits, hits+healAmt);
        maxHits = max(hits, maxHits);
    }
    public void endTurn() {
        if (!destroyed())
            finishMissileRemainingMoves();
        List<CombatMissile> missiles = new ArrayList<>(targetingMissiles);
        for (CombatMissile miss : missiles)
            miss.endTurn();
    }
    public int movePointsTo(CombatEntity target) {
        int distX = Math.abs(x - target.x);
        int distY = Math.abs(y - target.y);
        return max(distX, distY);
    }
    public int movePointsTo(int x1, int y1) {
        int distX = Math.abs(x - x1);
        int distY = Math.abs(y - y1);
        return max(distX, distY);
    }
    public int movePointsTo(int x0, int y0, int x1, int y1) {
        int distX = Math.abs(x0 - x1);
        int distY = Math.abs(y0 - y1);
        return max(distX, distY);
    }
    public float distanceTo(int x1, int y1) {
        return sqrt(((x-x1)*(x-x1)) + ((y-y1)*(y-y1)));
    }
    public float distanceTo(float x1, float y1) {
        return sqrt(((x()-x1)*(x()-x1)) + ((y()-y1)*(y()-y1)));
    }
    public boolean canMoveTo(int x, int y) {
        return (movePointsTo(x, y) <= move);
    }
    public void teleportTo(int x1, int y1, float amt) {
        int oldX = x;
        int oldY = y;
        drawFadeOut(amt);
        canTeleport = false;
        x = x1;
        y = y1;
        offsetX = 0;
        offsetY = 0;
        drawFadeIn(oldX, oldY);
    }
    public boolean moveTo(int x1, int y1) {
        float plannedDistance = movePointsTo(x1,y1);

        while (submoveTo(x1,y1))
            ;

        distance += plannedDistance;
        move -= plannedDistance;
        return !destroyed();
    }
    public boolean submoveTo(float x1, float y1) {
        boolean b = submoveTo(x1,y1, targetingMissiles);
        if (mgr.showAnimations())
            mgr.ui.paintAllImmediately(20);
        
        return b;
    }
    public boolean submoveTo(float x1, float y1, List<CombatMissile> missiles) {
        // this method performs one "sub-move" of a stack to its destination,
        // then allows each pursuing missile to perform a sub-move
        // the distance of the sub-move is dependent on the stack's maneuverability

        float x0 = x();
        float y0 = y();

        float totalDist = distance(x0,y0,x1,y1);
        float stepDist = MOVE_STEP;

        float stepPct = min(1,stepDist/totalDist);

        float distX = Math.abs(x0-x1);
        float distY = Math.abs(y0-y1);
        float xIncr = stepPct*distX;
        float yIncr = stepPct*distY;

        if (x1 < x())  xIncr = -xIncr;
        if (y1 < y())  yIncr = -yIncr;

        offsetY += yIncr;
        offsetX += xIncr;
        distY -= stepDist;
        if (distY <= 0) {
            y = (int) y1;
            offsetY = 0;
        }
        
        distX -= stepDist;
        if (distX <= 0) {
            x = (int) x1;
            offsetX = 0;
        }
        
        // allow missiles to pursue (check for cloaking). They may damage stack
        if (!missiles.isEmpty()) {
            List<CombatMissile> tempMissiles = new ArrayList<>(missiles);
            for (CombatMissile miss : tempMissiles)
                miss.pursue(stepDist);
        }

        // return true if still alive and haven't reached x1,y1
        return (((x != x1) || (y != y1)) && (!destroyed()));
    }
    public void finishMissileRemainingMoves() {
        while (!performMissileSubmove()) { }
    }
    public boolean performMissileSubmove() {
        boolean missilesFinished = true;
        List<CombatMissile> targetCopy = new ArrayList<>(targetingMissiles);
        for (CombatMissile miss : targetCopy)
            missilesFinished = miss.pursue(MOVE_STEP) && missilesFinished;
        
        if (mgr.showAnimations())
            mgr.ui.paintAllImmediately(20);

        return missilesFinished;
    }
    public float x() { return x + offsetX; }
    public float y() { return y + offsetY; }
    public boolean atGrid(int x1, int y1) {
        return (x == x1) && (y == y1);
    }
    public float radiansTo(CombatEntity target) {
        float dx = x() - target.x();
        float dy = y() - target.y();

        if (dy > 0)
            return (float)(Math.PI - Math.atan(dx/dy));
        else if (dy < 0) {
            if (dx > 0)
                return 0 - (float) Math.atan(dx/dy);
            else
                return (float)(Math.PI + Math.PI - Math.atan(dx/dy));
        }
        else {
            if (dx > 0)
                return (float)(Math.PI / 2);
            else
                return (float)(1.5 * Math.PI);
        }
    }
    public void takeBioweaponDamage(float damage) { }
    public float takeHullDamage(float damage) {
        if (inStasis)
            return 0;
        
        if (damage <= 0)
            return 0;
        attacked = true;
        maxHits = max(0,maxHits - damage);
        hits = min(hits,maxHits);

        if (hits <= 0)
            loseShip();

        assignCollateralDamage(damage);
        return damage;
    }
    protected float takeDamage(float damage, float shieldAdj) {
        if (inStasis)
            return 0;
        float damageTaken = 0;
        attacked = true;
        float dmg = max(0, damage - (shieldLevel() * shieldAdj));
        damageTaken += dmg;
        if (dmg == 0)
            return damageTaken;

        if (num > 0) {
            damageSustained += min(dmg, hits);
            hits -= dmg;
            if (hits <= 0)
               loseShip();
            if (destroyed() && (mgr != null))
                mgr.destroyStack(this);
        }
        assignCollateralDamage(dmg);
        return damageTaken;
    }
    public float takeMissileDamage(float damage, float shieldAdj) {
        return takeDamage(damage*missileDamageMod(), shieldAdj);
    }
    public float takeTorpedoDamage(float damage, float shieldAdj) {
        return takeDamage(damage*torpedoDamageMod(), shieldAdj);
    }
    public float takeBeamDamage(float damage, float shieldAdj) {
        return takeDamage(damage*beamDamageMod(), shieldAdj);
    }
    public float takeBombDamage(float damage, float shieldAdj) {
        return takeDamage(damage*bombDamageMod(), shieldAdj);
    }
    public float takePulsarDamage(float damage, float shieldAdj) {
        float adjDam = damage - (shieldLevel() * shieldAdj);
        return takeHullDamage(adjDam);
    }
    public float takeStreamingDamage(float damage, float shieldAdj) {
         if (inStasis)
            return 0;
        float damageTaken = 0;
        attacked = true;
        float damageLeft = damage*beamDamageMod();
        while ((damageLeft > 0) && !destroyed()) {
            float dmg = max(0, damageLeft - (shieldLevel() * shieldAdj));
            damageTaken += dmg;
            damageSustained += min(dmg, hits);
            hits -= dmg;
            if (hits <= 0) {
                damageLeft = 0 - hits;
                loseShip();
            }
            else
                damageLeft = 0;
        }
        assignCollateralDamage(damage);
        return damageTaken;
    }
    public float takeBlackHoleDamage(float pct) {
        if (inStasis)
            return 0;
        attacked = true;
        float pctLoss = pct - (shieldLevel() /50) - blackHoleDef();
        pctLoss = max(0,pctLoss);
        int kills = Math.round(num * pctLoss);
        for(int i = 0; i < kills; ++i)
            loseShip();
        return kills * maxHits;
    }
    public void loseShip() {
        int lost = maxHits > 0 ? 1 : num;
        hits = maxHits;
        shield = maxShield;
        num = max(0, num - lost);
        if (destroyed() && (mgr != null))
            mgr.destroyStack(this);
    }
    public boolean shipComponentIsUsed(int index)                          { return true; }
    public boolean shipComponentIsOutOfMissiles(int index)                 { return false; }
    public boolean shipComponentIsOutOfBombs(int index)                    { return false; }
    public boolean shipComponentValidTarget(int index, CombatEntity target) {
        return empire != target.empire ? false : weapon(index).validTarget(target);
    }
    public boolean shipComponentInRange(int index, CombatEntity target)     { return false; }
    public int wpnCount(int i)                                             { return 0; }
    public int shotsRemaining(int i)                                       { return 0; }
    public float targetShieldMod(ShipComponent c)                          { return 1.0f; }
    public String wpnName(int i) { return ""; }

    public FlightPath pathTo(int x, int y) {
        return captain.pathTo(this, x, y);
    }
    public void drawFadeOut(float amt) {
        if (!mgr.showAnimations())
            return;
        
        float maxTransparency = cloaked ? TechCloaking.TRANSPARENCY : 1.0f;
        ShipBattleUI ui = mgr.ui;

        // fade out
        for (float i=maxTransparency; i>=0; i-=amt) {
            transparency = i;
            long t0 = System.currentTimeMillis();
            ui.paintCellImmediately(x,y);
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 25)
                sleep(25-t1);
        }
    }
    public void drawFadeIn(int oldX, int oldY) {
        if (!mgr.showAnimations())
            return;
        
        float maxTransparency = cloaked ? TechCloaking.TRANSPARENCY : 1.0f;
        ShipBattleUI ui = mgr.ui;

        // fade in, but ensure old position is cleared out first
        ui.paintCellImmediately(oldX, oldY);
        for (float i = 0; i <= maxTransparency; i += .10f) {
            transparency = i;  // might already be cloaked!
            long t0 = System.currentTimeMillis();
            ui.paintCellImmediately(x, y);
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 25)
                sleep(25-t1);
        }
    }
    public void drawDamageTaken(float dmg, String result) {
        if (!mgr.showAnimations())
            return;
        
        int stW = mgr.ui.stackW();
        int stH = mgr.ui.stackH();
        int st1X = mgr.ui.stackX(this);
        int st1Y = mgr.ui.stackY(this);
        int x1 = st1X+stW/2;
        int y1 = st1Y+stH/2;
        Graphics2D g = (Graphics2D) mgr.ui.getGraphics();
        drawAttackResult(g,x1,y1,x1, dmg,result);
        mgr.ui.paintAllImmediately();
    }
    public void drawAttackResult(Graphics g, int x1, int y1, int x0, float dmg, String result) {
        if (!mgr.showAnimations())
            return;
         
        int xleft = x0 < x1 ? x : max(0, x-1);
        Rectangle rTopLeft = mgr.ui.combatGrids[xleft][max(0,y-1)];
        
        int FRAMES = mgr.autoComplete ? 12 : 12;
        int dx = x0 <= x1 ? BasePanel.s1 : -BasePanel.s1;
        int dy = -BasePanel.s1;
        int dFont = 1;
        int dAlpha = 255/ (FRAMES/4);

        Color[] cRed = new Color[FRAMES];
        Color[] cWhite = new Color[FRAMES];
        Font[] font = new Font[FRAMES];
        int alpha = 255;
        int fontsize = 20;
        for (int i=0;i<FRAMES;i++) {
            cWhite[i] =  new Color(255,255,255,alpha);
            cRed[i] = new Color(255,64,64,alpha);
            font[i] = narrowFont(fontsize);
            if (i > FRAMES*3/4)
                alpha -= dAlpha;
            fontsize += dFont;
        }

        int x2 = x1;
        int y2 = y1;
        String displayStr =  dmg > 0 ? "-" + (int) Math.ceil(dmg) : result;

        // set a clip to minimize delays cause by potential side-effect
        // repainting of the ui
        int clipX = rTopLeft.x+(int)((offsetX-0.2)*rTopLeft.width);
        int clipY = rTopLeft.y+(int)((offsetY-0.2)*rTopLeft.height);
        int clipW = 5*rTopLeft.width/2;
        int clipH = 5*rTopLeft.height/2;
        g.setClip(clipX, clipY, clipW, clipH);

        for (int i=0;i<FRAMES;i++) {
            long t0 = System.currentTimeMillis();
            if (!mgr.showAnimations())
                break;
            mgr.ui.paintImmediately(clipX,clipY,clipW,clipH);
            g.setFont(font[i]);
            Color c0;
            if (dmg != 0)
                c0 = cRed[i];
            else
                c0 = cWhite[i];
            g.setColor(c0);
            //drawBorderedString(g, displayStr, x2, y2, cWhite[i], c0);
            drawString(g,displayStr, x2, y2);
            x2 += dx;
            y2 += dy;
            fontsize += dFont;
            long dur = System.currentTimeMillis() - t0;
            if (dur < 50)
                sleep(50-dur);
        }
        g.setClip(null);
    }
    public void drawStack(ShipBattleUI ui, Graphics2D g, int origCount, int x, int y, int stackW, int stackH) {
        Image img = image;

        int w0 = img.getWidth(null);
        int h0 = img.getHeight(null);
        float scale0 = min((float)stackW/w0, (float)stackH/h0)*9/10;

        int x1 = x;
        int y1 = y;
        int w1 = (int)(scale0*w0);
        int h1 = (int)(scale0*h0);

        if (scale != 1.0f) {
            int prevW = w1;
            int prevH = h1;
            w1 = (int) (w1*scale);
            h1 = (int) (h1*scale);
            x1 = x1 +(prevW-w1)/2;
            y1 = y1 +(prevH-h1)/2;
        }
        
        Composite prevComp = g.getComposite();
        BufferedImage overlayImg = null;
        if (brighten > 0) {
            overlayImg = newBufferedImage(w1,h1);
            Graphics2D g0 = (Graphics2D) overlayImg.getGraphics();
            if (transparency < 1) {
                AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,max(0,transparency));
                g0.setComposite(ac);
            }
            if (reversed)  // XOR
                g0.drawImage(img, 0, 0, w1, h1, w0, 0, 0, h0, ui);
            else
                g0.drawImage(img, 0, 0, w1, h1, 0, 0, w0, h0, ui);
            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_IN,min(1,brighten));
            g0.setComposite(ac);
            g0.setColor(Color.white);
            g0.fillRect(0, 0, w1, h1);
            g0.setComposite(prevComp);
            g0.dispose();
        }
        
        if (transparency < 1) {
            AlphaComposite ac = java.awt.AlphaComposite.getInstance(AlphaComposite.SRC_OVER,max(0,transparency));
            g.setComposite(ac);
        }
        if (reversed)  // XOR
            g.drawImage(img, x1, y1, x1+w1, y1+h1, w0, 0, 0, h0, ui);
        else
            g.drawImage(img, x1, y1, x1+w1, y1+h1, 0, 0, w0, h0, ui);
        
        g.setComposite(prevComp);
        if (overlayImg != null)
            g.drawImage(overlayImg, x1, y1, ui);
            
        int y2 = y+stackH-BasePanel.s5;
        g.setFont(narrowFont(16));
        String s = text(name());
        int sw2 = g.getFontMetrics().stringWidth(s);
        int x2 = max(x1, x1+((stackW-sw2)/2));

        g.setColor(Color.lightGray);
        drawString(g,s, x2, y2);

        if (inStasis) {
            g.setColor(TechStasisField.STASIS_COLOR);
            g.fillRect(x1,y1,stackW, stackH);
            s = text("SHIP_COMBAT_STASIS");
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
            int numW = 0;
            // draw ship count
            if (num > 1) {
                g.setColor(healthBarC);
                String numStr = str(num);
                g.setFont(narrowFont(20));
                numW = g.getFontMetrics().stringWidth(numStr);
                int x6 = reversed ? x4: x4+w4-numW-BasePanel.s10;
                g.fillRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
                g.setColor(Color.white);
                Stroke prevStroke = g.getStroke();
                g.setStroke(BasePanel.stroke1);
                g.drawRect(x6, y4, numW+BasePanel.s10, BasePanel.s22);
                g.setStroke(prevStroke);
                g.drawString(numStr, x6+BasePanel.s5,y4+BasePanel.s18);
            }
            // draw hit points
            g.setColor(Color.white);
            String hpStr = ""+(int)Math.ceil(hits)+"/"+(int)Math.ceil(maxHits);
            g.setFont(narrowFont(12));
            int hpW = g.getFontMetrics().stringWidth(hpStr);
            int x5 = reversed ? x4+((w4-hpW+numW)/2) : x4+((w4-hpW-numW)/2);
            g.drawString(hpStr, x5, y4+BasePanel.s9);
        }
    }
}
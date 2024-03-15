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

import java.awt.*;
import java.awt.geom.AffineTransform;
import rotp.model.ships.ShipWeaponMissileType;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public class CombatMissile extends CombatEntity {
    private static final Color missileCountColour = new Color(255,240,78);
    private static final int MAX_TURNS = 10;
    public CombatEntity owner;
    private ShipWeaponMissileType missile;
    private int turnsLeft = 0;
    private Image missiles;
    private float moveRate = 0;
    private float range = 0;

    public CombatMissile(CombatEntity ship, ShipWeaponMissileType miss, int n) {
        missile = miss;
        range = ship.isColony() ? miss.range()*2 : miss.range();
        origNum = num = n*miss.scatterAttacks();
        owner = ship;
        mgr = ship.mgr;
        empire = ship.empire;
        target = ship.target;
        x = ship.x;
        y = ship.y;
        offsetX = ship.offsetX;
        offsetY = ship.offsetY;
        attackLevel = ship.attackLevel();
        Image missileImg = image(miss.tech().imageKey());

        int imgW = BasePanel.s60;
        int imgH = imgW*missileImg.getHeight(null)/missileImg.getWidth(null);
        missiles = newBufferedImage(imgW,imgH);
        Graphics g = missiles.getGraphics();
        g.drawImage(missileImg,0,0,imgW,imgH,null);
        g.dispose();

        maxMove = miss.speed();
        turnsLeft = MAX_TURNS;
        moveRate = target.maxMove() == 0 ? maxMove : maxMove / target.maxMove();
    }
    @Override
    public String fullName() {
        return concat(str(num), ":", missile.name(), "-", Integer.toHexString(hashCode()));
    }

    @Override
    public int maxFiringRange(CombatEntity tgt)           { return 0; }
    @Override
    public int optimalFiringRange(CombatEntity tgt)       { return 0; }
    @Override
    public boolean isMissile()            { return true; }
    @Override
    public String name()                  { return missile.name(); }
    @Override
    public boolean isArmed()              { return true; }
    @Override
    public boolean canChangeTarget()    { return false; }
    @Override
    public boolean canCollide()         { return true; }

    @Override
    public void endTurn() {
        turnsLeft--;
        if (selfDestruct()) {
            mgr.removeFromCombat(this);
        }
    }

    private float rotateRadians() {
        return radiansTo(target) + ((float)Math.PI/2);
    }
    @Override
    public boolean canAttack(CombatEntity target) {
        return (distanceTo(target.x(),target.y()) < .7);
    }
    @Override
    public void fireWeapon(CombatEntity target) {
        missile.fireUpon(this, target, num);

        mgr.destroyStack(this);
    }
    public boolean pursue(float tgtMoveDist) {
        if (!target.visible)
            return true;
        
        float targetDist = distanceTo(target.x(), target.y());
        float moveDist = min(move, moveRate * tgtMoveDist);

        float stepPct = min(1,moveDist/targetDist);

        float stepX = stepPct * (target.x()-x());
        float stepY = stepPct * (target.y()-y());

        offsetX += stepX;
        offsetY += stepY;

        if (canAttack(target))
            fireWeapon(target);

        move = max(0, move - moveDist);
        range -= moveDist;
        
        // return 'true' if missile is done
        return ((move <= 0) || destroyed());
    }
    private boolean selfDestruct() {
        if (turnsLeft < 1)
            return true;
        if (range <= 0)
            return true;
        if (target.destroyed())
            return true;
        if (owner.destroyed())
            return true;
        if (missile.maxDamage() <= missile.damageLoss(distance))
            return true;
        return false;
    }
    @Override
    public void drawStack(ShipBattleUI ui, Graphics2D g, int origCount, int x, int y, int stackW, int stackH) {
        int x0 = (int) ((x()+0.5f)*stackW);
        int y0 = (int) ((y()+0.5f)*stackH);
        AffineTransform tx = new AffineTransform();
        tx.translate(x0,y0);
        tx.rotate(rotateRadians(),0, 0);

        g.drawImage(missiles, tx, null);
        int y2 = y0+BasePanel.s6;
        int x1 = x0;
        g.setColor(missileCountColour);
        g.setFont(narrowFont(12));
        String cnt = str(num);
        drawBorderedString(g, cnt, 2, x1, y2, Color.black, Color.yellow);
    }
}

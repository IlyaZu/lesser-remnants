/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2023-2026 Ilya Zushinskiy
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
package rotp.model.galaxy;

import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import rotp.model.game.IGameOptions;
import rotp.util.Base;

public abstract class GalaxyShape implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    
    private static final float SYSTEM_BUFFER = 1.9f;
    public static final int EDGE_BUFFER = 1;
    
    private float orionBuffer = 8;
    private float empireBuffer = 6;
    private float[] x;
    private float[] y;
    private int width = 0;
    private int height = 0;
    private int maxStars = 0;
    private int num = 0;
    private int homeStars = 0;
    private int genAttempt = 0;
    private List<EmpireSystem> empSystems = new ArrayList<>();
    private Point.Float orionXY;
    IGameOptions opts;

    public int width()          { return width; }
    public int height()         { return height; }
    
    protected abstract int galaxyWidthLY(int numStars);
    protected abstract int galaxyHeightLY(int numStars);
    public abstract void setRandom(Point.Float p);
    public abstract boolean valid(float x, float y);
    protected abstract float sizeFactor(String size);
    public abstract float maxScaleAdj();

    public boolean valid(Point.Float p) { return valid(p.x, p.y); }
    public void coords(int n, Point.Float pt) {
        pt.x = x[n];
        pt.y = y[n];
    }
    public int numberStarSystems()            { return num; }
    public int totalStarSystems()             { return num+homeStars;}
    public List<EmpireSystem> empireSystems() { return empSystems; }
    public float adjustedSizeFactor()        { return sizeFactor(opts.selectedGalaxySize()) + (genAttempt/3); }

    public void init(int numStars) {
        maxStars = numStars;
        width = galaxyWidthLY(numStars) + (2 * EDGE_BUFFER);
        height = galaxyHeightLY(numStars) + (2 * EDGE_BUFFER);
        x = new float[maxStars];
        y = new float[maxStars];
    }
    public void generate(int numEmpires, int numStars) {
        log("Galaxy shape: "+maxStars+ " stars"+ "   emps:"+numEmpires);
        long tm0 = System.currentTimeMillis();
        genAttempt = 0;
        empSystems.clear();
        
        // SYSTEM_BUFFER is minimum distance between any two stars
        float minEmpireBuffer = 3*SYSTEM_BUFFER;
        float maxMinEmpireBuffer = 15*SYSTEM_BUFFER;
        float minOrionBuffer = 4*SYSTEM_BUFFER;
        
        // the stars/empires ratio for the most "densely" populated galaxy is about 8:1
        // we want to set the minimum distance between empires to half that in ly, with a minimum
        // of 6 ly... this means that it will not increase until there is at least a 12:1
        // ratio. However, the minimum buffer will never exceed the "MAX_MIN", to ensure that
        // massive maps don't always GUARANTEE hundreds of light-years of space to expand uncontested
        empireBuffer = min(maxMinEmpireBuffer, max(minEmpireBuffer, (maxStars/(numEmpires*2))));
        // Orion buffer is 50% greater with minimum of 8 ly.
        orionBuffer = max(minOrionBuffer, empireBuffer*3/2);

        // add systems needed for empires
        while (empSystems.size() < numEmpires) {
            init(numStars);
            genAttempt++;
            empSystems.clear();
            homeStars = 0;
            num = 0;
            orionXY = addOrion();
            for (int i=0;i<numEmpires;i++) {
                EmpireSystem sys = new EmpireSystem(this);
                if (sys.valid) {
                    empSystems.add(sys);
                    homeStars += sys.numSystems();
                }
            }
        }

        // add other systems to fill out galaxy
        int attempts = addUncolonizedSystems();
        long tm1 = System.currentTimeMillis();
        log("Galaxy generation: "+(tm1-tm0)+"ms  Attempts: ", str(attempts), "  stars:", str(num), "/", str(maxStars));
    }
    private Point.Float addOrion() {
        Point.Float pt = new Point.Float();
        findAnyValidLocation(pt);
        addSystem(pt);
        return pt;
    }
    private int addUncolonizedSystems() {
        int maxAttempts = maxStars * 10;
        
        // we've already generated 3 stars for every empire so reduce their
        // total from the count of remaining stars to create ("too many stars" bug)
        int nonEmpireStars = maxStars - (empSystems.size() *3);
         int attempts = 0;
        Point.Float pt = new Point.Float();
        while ((num < nonEmpireStars) && (attempts++ < maxAttempts)) {
            findAnyValidLocation(pt);
            if (!isTooNearExistingSystem(pt.x,pt.y,false))
                addSystem(pt);
        }
        return attempts;
    }
    private Point.Float findAnyValidLocation(Point.Float p) {
        setRandom(p);
        while (!valid(p))
            setRandom(p);
        
        return p;
    }
    private void addSystem(Point.Float pt) {
        x[num] = pt.x;
        y[num] = pt.y;
        num++;
    }
    private boolean isTooNearExistingSystem(float x0, float y0, boolean isHomeworld) {
        if (isHomeworld) {
            if (distance(x0,y0,orionXY.x,orionXY.y) <= orionBuffer)
                return true;
            for (EmpireSystem emp: empSystems) {
                if (distance(x0,y0,emp.colonyX(),emp.colonyY()) <= empireBuffer)
                    return true;
            }
        }
        // not too close to other systems in galaxy
        if (isTooNearSystemsInEntireGalaxy(x0,y0))
            return true;
        // not too close to other systems in any empire system
        for (EmpireSystem emp: empSystems) {
            for (int i=0;i<emp.num;i++) {
                if (distance(x0,y0,emp.x(i),emp.y(i)) <= SYSTEM_BUFFER)
                    return true;
            }
        }
        return false;
    }
    private boolean isTooNearSystemsInEntireGalaxy(float x0, float y0) {
        for (int i=0;i<num;i++) {
            if (distance(x0,y0,x[i],y[i]) <= SYSTEM_BUFFER)
                return true;
        }
        return false;
    }
    public final class EmpireSystem implements Serializable {
        private float[] x = new float[3];
        private float[] y = new float[3];
        private int num = 0;
        private boolean valid = false;

        private EmpireSystem(GalaxyShape sp) {
            // empire is valid if it can create a valid home system
            // and two valid nearby stars
            valid = addNewHomeSystem(sp);
            valid = valid && addNearbySystem(sp, colonyX(), colonyY(), 3.0f);
            valid = valid && addNearbySystem(sp, colonyX(), colonyY(), 3.0f);
        }
        public int numSystems()   { return num; }
        public float x(int i)    { return x[i]; }
        public float y(int i)    { return y[i]; }
        public float colonyX()   { return x[0]; }
        public float colonyY()   { return y[0]; }

        private boolean addNewHomeSystem(GalaxyShape sp) {
            int attempts = 0;
            Point.Float pt = new Point.Float();
            while (attempts++ < 100) {
                findAnyValidLocation(pt);
                if (!sp.isTooNearExistingSystem(pt.x,pt.y,true)) {
                    addSystem(pt.x,pt.y);
                    return true;
                }
            }
            return false;
        }
        private boolean addNearbySystem(GalaxyShape sh, float x0, float y0, float maxDistance) {
            float x1 = x0-maxDistance;
            float x2 = x0+maxDistance;
            float y1 = y0-maxDistance;
            float y2 = y0+maxDistance;
            int attempts = 0;
            Point.Float pt = new Point.Float();
            while (attempts < 100) {
                attempts++;
                pt.x = random(x1, x2);
                pt.y = random(y1, y2);
                if (sh.valid(pt)) {
                    boolean tooCloseToAny = isTooNearExistingSystem(sh,pt.x,pt.y);
                    boolean tooFarFromRef = distance(x0, y0, pt.x,pt.y) >= maxDistance;
                    if (!tooCloseToAny && !tooFarFromRef) {
                        addSystem(pt.x,pt.y);
                        return true;
                    }
                }
            }
            return false;
        }
        private boolean isTooNearExistingSystem(GalaxyShape sh, float x0, float y0) {
            for (int i=0;i<num;i++) {
                if (distance(x0,y0,x[i],y[i]) <= SYSTEM_BUFFER)
                    return true;
            }
            return sh.isTooNearExistingSystem(x0,y0,false);
        }
        private void addSystem(float x0, float y0) {
            x[num] = x0;
            y[num] = y0;
            num++;
        }
    }
}

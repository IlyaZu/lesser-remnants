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
package rotp.ui.util.planets;

import java.util.HashMap;
import java.util.Map;
import rotp.model.planet.Planet;
import rotp.util.Base;
import rotp.util.FastImage;

public class Sphere2D implements Base {
    private final int radius;
    private final FastImage mapImage;
    public static final int FAST_PLANET_R = 50;
    public static final int SMALL_PLANET_R = 100;
    private static Map<Integer, FastImage> cachedFastOvalImg = new HashMap<>();
    private static Map<Integer, FastImage> cachedFastGlobeImg = new HashMap<>();

    public Sphere2D(FastImage terrainImg, FastImage cloudImg, Planet p) {
        FastImage compositeImg = PlanetImager.createCompositeImage(terrainImg, p);
        radius = terrainImg.getHeight()/2;
        mapImage = compositeImg;
    }

    private FastImage largeVisibleSphere(float pct, int r) {
        return visibleSphere(mapImage, radius, pct);
    }
    private FastImage visibleSphere(FastImage baseImg, int r, float pct) {
        float p0 = pct - (int) pct;

        int h = 2*r;
        int w = (int) (r*Math.PI);
        if (!cachedFastOvalImg.containsKey(w))
            cachedFastOvalImg.put(w,FastImage.sized(w, h));
        FastImage img = cachedFastOvalImg.get(w);

        int x1Mid = (int) (0.5*r*Math.PI);
        int x0Mid = (int) (r*Math.PI);

        int imgMaxW = img.getWidth()-1;
        int baseImgMaxW = baseImg.getWidth()-1;

        for (int y=0;y<h;y++) {
            int xInt = xIntercept(y,p0,r);
            int x0Min = xIntercept(y,0,r);
            int x0Max = xIntercept(y,1,r);
            int x1Min = xIntercept(y,0.25f,r);
            int x1Max = xIntercept(y,0.75f,r)+1;
            for (int x=x1Min;x<x1Max;x++) {
                int x1 = x+xInt;   // ostensibly the pixel we need from the full image
                if (x1<x0Min) {
                    x1=x0Max+x1-x0Min;
                } // check if in the fullImage oval
                else if (x1>=x0Max) {
                    x1=x1+x0Min-x0Max;
                }

                int xb = x+x1Mid;
                if (xb < 0) xb = 0;
                else if (xb > imgMaxW) xb = imgMaxW;

                int x1b = x1+x0Mid;
                if (x1b < 0) x1b = 0;
                else if (x1b > baseImgMaxW) x1b = baseImgMaxW;

                img.setRGB(xb, y, baseImg.getRGB(x1b, y));
            }
        }
        return img;
    }
    public FastImage image(float pct) {
        return image(pct, radius);
    }
    private FastImage image(float pct, int r) {
        float p0 = pct - (int) pct;

        FastImage img0 = largeVisibleSphere(p0, r);
        int x0Mid = img0.getWidth()/2;

        int h = 2*r;
        int w = h;

        if (!cachedFastGlobeImg.containsKey(w))
            cachedFastGlobeImg.put(w,FastImage.sized(w, h));
        FastImage img =  cachedFastGlobeImg.get(w);

        float pi = (float) Math.PI;
        float x1MinPct = 0.5f-(1/(2*pi));
        float x1MaxPct = 0.5f+(1/(2*pi));
        int x1Mid = r;

        for (int y=0;y<h;y++) {
            int x0Max = xIntercept(y,0.75f, r);
            int x1Min = xIntercept(y,x1MinPct, r);
            int x1Max = xIntercept(y,x1MaxPct, r);
            int prevx0 = -1;
            for (int x1=x1Min;x1<x1Max;x1++) {
                float x1Pct = (float)x1/x1Max;
                float x0Radians = asin(x1Pct);
                float x0Pct = (pi-(2*x0Radians))/pi;
                int x0 = x0Max-(int)(x0Pct*x0Max);
                int numpx = (prevx0 < 0) ? 1 : x0-prevx0;
                img.setRGB(x1+x1Mid, y, mergedRGB(img0,x0+x0Mid+1-numpx, y, numpx));
                prevx0 = x0;
            }
        }

        return img;
    }

    private int xIntercept(int y1, float pct, int r) {
        // returns an x DELTA from the center line

        if (pct == 0.5)
            return 0;

        float p0 = (2*pct)-1;

        // returns the x-intercept for a given y value, when the center is set to pct

        //   horizontal radius of the arc
        float a = p0*r*(float)Math.PI;

        // vertical radius of arc
        float tmp = (float)(y1-r)/r;
        int x1 = (int)(a*Math.sqrt(1-(tmp*tmp)));
        return x1;
    }
    
    private int mergedRGB(FastImage img, int x, int y, int numX) {
        //if (numX == 0)
        //    return 0;
        if (numX < 2)
            return img.getRGB(x,y);

        int a1 = 0;
        int r1 = 0;
        int g1 = 0;
        int b1 = 0;

        for (int i=0;i<numX;i++) {
            int p0 = img.getRGB(x+i,y);
            a1 += (p0 >> 24 & 0xff);
            r1 += (p0 >> 16 & 0xff);
            g1 += (p0 >> 8 & 0xff);
            b1 += (p0 >> 0 & 0xff);
        }
        return (a1/numX << 24)+(r1/numX << 16)+(g1/numX << 8)+b1/numX;
    }
}

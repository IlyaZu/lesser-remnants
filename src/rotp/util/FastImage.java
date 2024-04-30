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
package rotp.util;

import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.PixelGrabber;
import java.awt.image.Raster;
import rotp.model.planet.PlanetHeightMap;

public class FastImage {
    private static final BufferedImage PROTOTYPE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    private int w;
    private int h;
    private int[] pixels;
    public BufferedImage image() {
        if (pixels == null)
            return null;
        return new BufferedImage(
                PROTOTYPE.getColorModel(),
                Raster.createWritableRaster(
                        PROTOTYPE.getSampleModel().createCompatibleSampleModel(w, h),
                        new DataBufferInt(pixels, pixels.length),
                        new Point()),
                false,
                null);
    }
    public void image(Image img)  {
        if (img == null)
            pixels = null;
        else {
            w = img.getWidth(null);
            h = img.getHeight(null);
            pixels = new int[w*h];
            try {
                new PixelGrabber(img, 0, 0, w, h, pixels, 0, w).grabPixels();
            }
            catch (InterruptedException localInterruptedException) { }
        }
    }
    public static FastImage from(Image img) {
        return img == null ? null : new FastImage(img);
    }
    public static FastImage sized(int w, int h) {
        return new FastImage(w,h);
    }
    public boolean equals(FastImage img) {
        if (img == this)
            return true;
        if (img == null)
            return false;
        if (getHeight() != img.getHeight())
            return false;
        if (getWidth() != img.getWidth())
            return false;
        for (int x=0;x<getWidth();x++) {
            for (int y=0;y<getHeight();y++) {
                if (getRGB(x, y) != img.getRGB(x,y))
                    return false;
            }
        }
        return true;
    }
    public static FastImage fromHeightMap(PlanetHeightMap map) {
        int w = map.width();
        int h = map.height();
        int[] px = new int[w*h];
        // convert each byte value (-128 to 127)
        // to integers representing grayscale (0-255 for r,g,b)
        for (int x=0;x<w;x++) {
            for (int y=0;y<h;y++) {
                int index = indexPosn(x,y,w); // find 1D index
                int v = map.col(x, y);
                int alpha = 255;
                if (v == Byte.MIN_VALUE)
                    alpha = 0;
                int pxV = v-Byte.MIN_VALUE;    // convert to 0-255
                int newPixel = (alpha << 24)+(pxV << 16)+(pxV << 8)+pxV;
                px[index] = newPixel;
            }
        }
        return new FastImage(px, w, h);
    }
    public int getWidth()                    { return w; }
    public int getHeight()                   { return h; }
    public int getRGB(int x, int y)          { return ((y*w)+x < 0) ? 0 : pixels[(y*w)+x]; }
    public int getAlpha(int x, int y)        { return getRGB(x,y) >> 24 & 0xff;    }
    
    public void setRGB(int x, int y, int px) {
        if ((x < w) && (y < h))
            pixels[(y*w)+x] = px;
    }
    private static int indexPosn(int x, int y, int w0) {
        return (y * w0) + x;
    }
    private FastImage(Image img) {
        image(img);
    }
    private FastImage(int w0, int h0) {
        w = w0;
        h = h0;
        pixels = new int[w*h];
    }
    private FastImage(int[] px, int w0, int h0) {
        w = w0;
        h = h0;
        pixels = px;
    }
}

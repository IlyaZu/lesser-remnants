/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2024-2026 Ilya Zushinskiy
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

import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;

public final class ImageColorizer implements Base {
    private int avgGrayLevel = 0;
    private int minGrayLevel = 0;
    private int maxGrayLevel = 0;
    private boolean onlyWhite = false;
    private boolean onlyBlack = false;
    private boolean onlyYellow = false;
    private boolean onlyRed = false;
    private boolean onlyGreen = false;
    private boolean onlyBlue = false;
    private Color specificColor;
    private Rectangle scope = null;
    private HashMap<Integer,Integer> transforms = new HashMap<>();

    private static final Color COLOR_AQUA = new Color(37,239,210);
    private static final Color COLOR_PURPLE = new Color(255,128,255);
    private static final Color COLOR_LIGHT_BLUE = new Color(128,128,255);
    private static final Color COLOR_ORANGE = new Color(239,127,34);
    private static final Color COLOR_DK_GREEN = new Color(0,127,0);
    private static final Color COLOR_BLUE = new Color(34,97,239);
    private static final Color COLOR_GREEN = new Color(57,181,84);
    private static final Color COLOR_YELLOW = new Color(247,186,79);

    public static final int YELLOW = 1;
    public static final int ORANGE = 2;
    public static final int PURPLE = 3;
    public static final int AQUA = 4;
    public static final int WHITE = 5;
    public static final int GRAY = 6;
    public static final int BLACK = 7;
    public static final int RED = 8;
    public static final int GREEN = 9;
    public static final int BLUE = 10;
    public static final int LIGHT_BLUE = 11;
    public static final int DARK_GREEN = 12;

    public static Color color(int id) {
        switch(id) {
            case WHITE:      return Color.white;
            case GRAY:       return Color.gray;
            case BLACK:      return Color.black;
            case RED:        return Color.red;
            case GREEN:      return COLOR_GREEN;
            case BLUE:       return COLOR_BLUE;
            case YELLOW:     return COLOR_YELLOW;
            case ORANGE:     return COLOR_ORANGE;
            case PURPLE:     return COLOR_PURPLE;
            case DARK_GREEN: return COLOR_DK_GREEN;
            case LIGHT_BLUE: return COLOR_LIGHT_BLUE;
            case AQUA:   return COLOR_AQUA;
            default: throw new RuntimeException("Unknown color id: "+id);
        }
    }
    public void reset() {
        avgGrayLevel = 0;
        onlyWhite = false;
        onlyBlack = false;
        onlyYellow = false;
        onlyRed = false;
        onlyGreen = false;
        onlyBlue = false;
        scope = null;
        specificColor = null;
        transforms.clear();
    }

    public boolean modify(int r, int g, int b) {
        int sum = r+g+b;
        int min = Math.min(r, Math.min(g, b));
        int max = Math.max(r, Math.max(g, b));

        if ((specificColor != null)
        && (r == specificColor.getRed()
        && (g == specificColor.getGreen())
        && (b == specificColor.getBlue())))
            return true;
        if (onlyRed && (r > sum/3))
            return true;
        if (onlyGreen && (g > sum/3))
            return true;
        if (onlyBlue && (b > sum/3))
            return true;
        if (onlyYellow && (r > b) && (r > g))
            return true;
        if (onlyBlack
        && ((avgGrayLevel > sum/3)
        ||  (minGrayLevel > min)
        ||  (maxGrayLevel > max)))
            return true;
        if (onlyWhite
        && ((avgGrayLevel < sum/3)
        ||  (minGrayLevel < min)
        ||  (maxGrayLevel < max)))
            return true;

        return false;
    }
    public BufferedImage makeColor(int id, Image img) {
        switch(id) {
            case WHITE:  return makeWhite(img);
            case GRAY:   return makeGray(img);
            case RED:    return makeRed(img);
            case GREEN:  return makeGreen(img);
            case BLUE:   return makeBlue(img);
            case PURPLE: return makeMagenta(img);
            case AQUA:   return makeAqua(img);
            case YELLOW: return makeYellow(img);
            case BLACK:  return makeBlack(img);
            case ORANGE: return makeOrange(img);
            case DARK_GREEN:  return makeDarkGreen(img);
            case LIGHT_BLUE:  return makeLightBlue(img);
            default: throw new RuntimeException("Unknown color id: " + id);
        }
    }

    private BufferedImage makeGray(Image base) {
        BufferedImage img = newBufferedImage(base);

        //Rectangle rect = getScope(img);
        int w = img.getWidth();
        int h = img.getHeight();

        for (int j = 0; j <h; j++) {
            for (int i = 0; i < w; i++) {
                int pixel = img.getRGB(i,j);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel & 0xff);

                int sum = r0+g0+b0;
                int r1 = sum / 3;
                int g1 = r1;
                int b1 = r1;

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(i,j,newPixel);
            }
        }
        reset();
        return img;
    }
    private BufferedImage makeRed(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel & 0xff);

                int sum = r0 + g0 + b0;
                int max = max(r0,g0,b0);
                int r1 = max;
                int g1 = (sum - max) / 2;
                int b1 = g1;
                
                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    private BufferedImage makeGreen(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel & 0xff);

                int sum = r0 + g0 + b0;
                int max = max(r0,g0,b0);
                int r1 = (sum - max) / 2;
                int g1 = max;
                int b1 = r1;

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeBlue(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel & 0xff);

                int sum = r0 + g0 + b0;
                int max = max(r0,g0,b0);
                int r1 = (sum - max) / 2;
                int g1 = r1;
                int b1 = max;

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeLightBlue(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel & 0xff);

                int sum = r0 + g0 + b0;
                int max = max(r0,g0,b0);
                int min = min((r0+g0+b0)/3, 63);
                int r1 = min+((sum - max)/2)*3/4;
                int g1 = r1;
                int b1 = min+max*3/4;
 
                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeDarkGreen(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel >> 0 & 0xff);

                int r1 = r0;
                int g1 = g0;
                int b1 = b0;

                int max = max(r0,g0,b0);
                int min = min(r0,g0,b0);
                r1 = min / 2;
                g1 = max / 2;
                b1 = min / 2;

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeYellow(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel & 0xff);

                int sum = r0 + g0 + b0;
                int min = min(r0,g0,b0);
                int minR = min((r0+g0+b0)/3, 191);

                int r1 = minR + ((sum - min) / 12);
                int g1 = r1;
                int b1 = min;
 
                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeOrange(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel & 0xff);

                int sum = r0 + g0 + b0;
                int minR = min((r0+g0+b0)/3, 191);
                int minG = min((r0+g0+b0)/6, 90);

                int min = min(r0,g0,b0);
                int r1 = minR + ((sum - min) / 12);
                int g1 = minG + ((sum - min) / 12);
                int b1 = g1;

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeBlack(Image base) {
        BufferedImage img = newBufferedImage(base);

        Rectangle rect = getScope(img);
        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel >> 0 & 0xff);

                int sum = r0 + g0 + b0;
                int r1 = sum/6;
                int g1 = sum/6;
                int b1 = sum/6;

                if (rect.contains(x,y) && modify(r1,g1,b1)) {
                    int min = min(r0,g0,b0);
                    r1 = 191 + ((sum - min) / 12);
                    g1 = r1 / 2;
                    b1 = min / 2;
                }

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    private BufferedImage makeMagenta(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel >> 0 & 0xff);

                int sum = r0 + g0 + b0;
                int r1 = r0;
                int g1 = g0;
                int b1 = b0;

                int min = min(r0,g0,b0);
                r1 = (sum - min) / 2;
                g1 = min;
                b1 = r1;

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeAqua(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel >> 0 & 0xff);

                int sum = r0 + g0 + b0;
                int r1 = r0;
                int g1 = g0;
                int b1 = b0;

                int min = min(r0,g0,b0);
                r1 = min;
                g1 = (sum - min) / 2;
                b1 = g1;

                int newPixel = (a0 << 24)+(r1 << 16)+(g1 << 8)+b1;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }
    public BufferedImage makeWhite(Image base) {
        BufferedImage img = newBufferedImage(base);

        int w = img.getWidth();
        int h = img.getHeight();

        for (int y = 0; y <h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = img.getRGB(x,y);
                int a0 = pixel >> 24 & 0xff;
                int r0 = (pixel >> 16 & 0xff);
                int g0 = (pixel >> 8 & 0xff);
                int b0 = (pixel >> 0 & 0xff);

                int max = max(r0,g0,b0);
                int newPixel = (a0 << 24)+(max << 16)+(max << 8)+max;
                img.setRGB(x,y,newPixel);
            }
        }
        reset();
        return img;
    }

    public Rectangle getScope(BufferedImage img) {
        if (scope != null)
                return scope;

        return new Rectangle(0,0,img.getWidth(), img.getHeight());
    }
}

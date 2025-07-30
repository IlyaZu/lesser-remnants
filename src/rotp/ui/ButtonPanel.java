/*
 * Copyright 2015-2020 Ray Fowler
 * Modifications Copyright 2025 Ilya Zushinskiy
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
package rotp.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.border.Border;
import rotp.util.ThickBevelBorder;

public abstract class ButtonPanel extends BasePanel implements MouseListener {
    private static final long serialVersionUID = 1L;

    // BLUE THEME
    private static Color buttonLighter = new Color(163,162,204);
    private static Color buttonLight = new Color(143,142,184);
    private static Color buttonColor = new Color(100,98,145);
    private static Color buttonDepressed = new Color(96,96,96);
    private static Color buttonDark = new Color(81,79,126);
    private static Color buttonDarker = new Color(63,63,63);

    private static Border buttonBevelBorder, buttonDepressedBorder;

    protected Shape boundingShape;
    protected boolean hovering = false;
    protected boolean depressed = false;
    private Border buttonBevelBorder() {
        if (buttonBevelBorder == null)
            buttonBevelBorder =  new ThickBevelBorder(4, 1, buttonLighter, buttonLight, buttonDarker, buttonDark, buttonDark, buttonDarker, buttonLight, buttonLighter);
        return buttonBevelBorder;
    }
    private Border buttonDepressedBorder() {
        if (buttonDepressedBorder == null)
            buttonDepressedBorder = new ThickBevelBorder(4, 1, buttonDarker, buttonDark, buttonLighter, buttonLight, buttonLight, buttonLighter, buttonDark, buttonDarker);
        return buttonDepressedBorder;
    }
    public boolean isDepressed() { return depressed; }
    private boolean isHovering()  { return hovering; }
    private void depressed(boolean b) {
        if (depressed != b) {
            depressed = b;
            repaint();
        }
    }
    abstract public boolean isButtonEnabled();
    abstract public String buttonLabel();
    abstract public Font textFont();
    abstract public void buttonClicked(int cnt);

    public ButtonPanel() {
        setBackground(new Color(0,0,0,0));
        addMouseListener(this);
    }
    public Shape boundingShape() {
        if (boundingShape == null)
            boundingShape = new Rectangle(0,0,getWidth(),getHeight());
        return boundingShape;
    }
    public void paintBackground(Graphics2D g) {
        g.setColor(backgroundColor());
        g.fill(boundingShape());
    }
    public void paintBorder(Graphics2D g) {
        if (depressed)
            buttonDepressedBorder().paintBorder(this,g,0,0,getWidth(),getHeight());
        else
            buttonBevelBorder().paintBorder(this,g,0,0,getWidth(),getHeight());
    }
    @Override
    public void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        super.paintComponent(g);

        paintBackground(g);
        paintBorder(g);
        g.setFont(textFont());
        g.setColor(textColor());
        String text = buttonLabel();
        int sw = g.getFontMetrics().stringWidth(text);
        int sh = g.getFontMetrics().getHeight();
        int x0 = (getWidth() - sw) / 2;
        int y0 = getHeight() - bottomMargin(sh);
        drawString(g,text, x0, y0);
    }
    private int bottomMargin(int fontHeight) {
        return s4+(getHeight()-(fontHeight*9/10))/2;
    }
    public Color backgroundColor() {
        if (depressed)
            return buttonDepressed;
        else
            return buttonColor;
    }
    public Color textColor() {
        if (isButtonEnabled()) {
            if (isHovering())
                return hoveringTextColor();
            else
                return enabledTextColor();
        }
        else
            return disabledTextColor();
    }
    private Color hoveringTextColor()    { return Color.white; }
    public Color enabledTextColor()     { return Color.black; }
    private Color disabledTextColor()    { return Color.darkGray; }
    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {
        hovering = true;
        repaint();
    }
    @Override
    public void mouseExited(MouseEvent e) {
        hovering = false;
        depressed(false);
    }
    @Override
    public void mousePressed(MouseEvent e) {
        if (isButtonEnabled())
            depressed(true);
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() > 3)
            return;
        if (depressed && isButtonEnabled()) {
            depressed(false);
            buttonClicked(e.getClickCount());
        }
    }
}

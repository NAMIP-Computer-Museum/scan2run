package net.sourceforge.javaocr.ocrPlugins.learningGUI;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;

public class ImageArea extends JLabel{

	private BufferedImage img;
	int cx, cy, cw, ch;
	double zoom;
	
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = getPreferredSize();
        g.drawImage(img, 0, 0, dim.width, dim.height, this);
        g.setColor(Color.red);
        g.drawRect((int)(cx*zoom), (int)(cy*zoom), (int)(cw*zoom), (int)(ch*zoom));
    }

    public void setZoom(double zoom) {
    	this.zoom=zoom;
        int w = (int) (zoom * img.getWidth());
        int h = (int) (zoom * img.getHeight());
        setPreferredSize(new Dimension(w, h));
        revalidate();
        repaint();
    }
    
    public void setSelection(int x, int y, int w, int h) {
    	cx=x;
    	cy=y;
    	cw=w;
    	ch=h;
        repaint();
    }
	
    public void setImage(BufferedImage image) {
    	this.img=image;
    	setZoom(1.0);
    	repaint();
    }
    
    public Image getImage() {
    	return img;
    }
    
}
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
	
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = getPreferredSize();
        g.drawImage(img, 0, 0, dim.width, dim.height, this);
        g.setColor(Color.red);
        g.drawRect(cx, cy, cw, ch);
    }

    public void setZoom(double zoom) {
        int w = (int) (zoom * img.getWidth());
        int h = (int) (zoom * img.getHeight());
        setPreferredSize(new Dimension(w, h));
        revalidate();
        repaint();
    }
    
    public void setSelection(int x1, int y1, int x2, int y2) {
    	cx=x1;
    	cy=y1; 
    	cw=x2-x1;
    	ch=y2-y1;
    }
	
	/*
	Image image;
    int width, height;

    public void paint(Graphics g) {
        int x, y;
        //this is to center the image
        x = (this.getWidth() - width) < 0 ? 0 : (this.getWidth() - width);
        y = (this.getHeight() - width) < 0 ? 0 : (this.getHeight() - width);

        g.drawImage(image, x, y, width, height, null);
    }

    public void setDimensions(int width, int height) {
        this.height = height;
        this.width = width;

        image = image.getScaledInstance(width, height, Image.SCALE_FAST);
        Container parent = this.getParent();
        if (parent != null) {
            parent.repaint();
        }
        this.repaint();
    } */
    
    public void setImage(BufferedImage image) {
    	this.img=image;
    	setZoom(1.0);
    	repaint();
    }
    
    public Image getImage() {
    	return img;
    }
    
}
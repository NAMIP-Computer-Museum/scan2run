// OCRScanner.java
// Copyright (c) 2003-2010 Ronald B. Cemer
// Modified by William Whitney
// All rights reserved.
// This software is released under the BSD license.
// Please see the accompanying LICENSE.txt for details.
package net.sourceforge.javaocr.ocrPlugins.mseOCR;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.sourceforge.javaocr.scanner.DocumentScanner;
import net.sourceforge.javaocr.scanner.DocumentScannerListenerAdaptor;
import net.sourceforge.javaocr.scanner.PixelImage;
import net.sourceforge.javaocr.scanner.accuracy.AccuracyListenerInterface;
import net.sourceforge.javaocr.scanner.accuracy.AccuracyProviderInterface;
import net.sourceforge.javaocr.scanner.accuracy.OCRComp;
import net.sourceforge.javaocr.scanner.accuracy.OCRIdentification;

/**
 * OCR document scanner.
 * @author Ronald B. Cemer
 */
public class OCRScanner extends DocumentScannerListenerAdaptor implements AccuracyProviderInterface
{
	private BufferedImage source;
	private OCRListener listener;
	private double askThreshold;
	private double confirmThreshold;	
	private int trainingThreshold;

    private static final int BEST_MATCH_STORE_COUNT = 8;
    private StringBuffer decodeBuffer = new StringBuffer();
    private CharacterRange[] acceptableChars;
    private boolean beginningOfRow = false;
    private boolean firstRow = false;
    private String newline = System.getProperty("line.separator");
    private HashMap<Character, ArrayList<TrainingImage>> trainingImages = new HashMap<Character, ArrayList<TrainingImage>>();
    private Character[] bestChars = new Character[BEST_MATCH_STORE_COUNT];
    private double[] bestMSEs = new double[BEST_MATCH_STORE_COUNT];
    private DocumentScanner documentScanner = new DocumentScanner();
    private AccuracyListenerInterface accListener;

    private static List<Character> MINSETXX;
    static {
    	String s="ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567989=+-*/()<>$:;.";
    	MINSETXX=new ArrayList<Character>();
    	for(char c: s.toCharArray())
    		MINSETXX.add(c);
    }
    
    public void addOCRListener(OCRListener l) {
    	listener=l;
    }
    
    public void setAskThreshold(double t) {
    	askThreshold=t;
    }

    public void setConfirmThreshold(double t) {
    	confirmThreshold=t;
    }
    
    public void setTrainingThreshold(int t) {
    	trainingThreshold=t;
    }
    
    public void acceptAccuracyListener(AccuracyListenerInterface listener)
    {
        accListener = listener;
    }

    /**
     * @return The <code>DocumentScanner</code> instance that is used to scan the document(s).
     * This is useful if the caller wants to adjust some of the scanner's parameters.
     */
    public DocumentScanner getDocumentScanner()
    {
        return documentScanner;
    }

    /**
     * Remove all training images from the training set.
     */
    public void clearTrainingImages()
    {
        trainingImages.clear();
    }

    /**
     * Add training images to the training set.
     * @param images A <code>HashMap</code> using <code>Character</code>s for
     * the keys.  Each value is an <code>ArrayList</code> of
     * <code>TrainingImages</code> for the specified character.  The training
     * images are added to any that may already have been loaded.
     */
    public void addTrainingImages(HashMap<Character, ArrayList<TrainingImage>> images)
    {
        for (Iterator<Character> it = images.keySet().iterator(); it.hasNext();)
        {
            Character key = it.next();
            ArrayList<TrainingImage> al = images.get(key);
            ArrayList<TrainingImage> oldAl = trainingImages.get(key);
            if (oldAl == null)
            {
                oldAl = new ArrayList<TrainingImage>();
                trainingImages.put(key, oldAl);
            }
            for (int i = 0; i < al.size(); i++)
            {
                oldAl.add(al.get(i));
            }
        }
    }

    /**
     * Scan an image and return the decoded text.
     * @param image The <code>Image</code> to be scanned.
     * @param x1 The leftmost pixel position of the area to be scanned, or
     * <code>0</code> to start scanning at the left boundary of the image.
     * @param y1 The topmost pixel position of the area to be scanned, or
     * <code>0</code> to start scanning at the top boundary of the image.
     * @param x2 The rightmost pixel position of the area to be scanned, or
     * <code>0</code> to stop scanning at the right boundary of the image.
     * @param y2 The bottommost pixel position of the area to be scanned, or
     * <code>0</code> to stop scanning at the bottom boundary of the image.
     * @param acceptableChars An array of <code>CharacterRange</code> objects
     * representing the ranges of characters which are allowed to be decoded,
     * or <code>null</code> to not limit which characters can be decoded.
     * @return The decoded text.
     */
    public String scan(
            Image image,
            int x1,
            int y1,
            int x2,
            int y2,
            CharacterRange[] acceptableChars)
    {

        this.acceptableChars = acceptableChars;
        this.source=(BufferedImage)image; // CP unsafe check
        PixelImage pixelImage = new PixelImage(image);
        pixelImage.toGrayScale(true);
        pixelImage.filter();
        decodeBuffer.setLength(0);
        firstRow = true;
        documentScanner.scan(pixelImage, this, x1, y1, x2, y2);
        String result = decodeBuffer.toString();
        decodeBuffer.setLength(0);
        return result;
    }

    @Override
    public void endRow(PixelImage pixelImage, int y1, int y2)
    {
        //Send accuracy of this identification to the listener
        if (accListener != null)
        {
            OCRIdentification identAccuracy = new OCRIdentification(OCRComp.MSE);
            identAccuracy.addChar('\n', 0.0);
            accListener.processCharOrSpace(identAccuracy);
        }
    }

    @Override
    public void beginRow(PixelImage pixelImage, int y1, int y2)
    {
        beginningOfRow = true;
        if (firstRow)
        {
            firstRow = false;
        }
        else
        {
            decodeBuffer.append(newline);
        }
    }

    @Override
    public void processChar(
            PixelImage pixelImage,
            int x1,
            int y1,
            int x2,
            int y2,
            int rowY1,
            int rowY2)
    {	
        int[] pixels = pixelImage.pixels;
        int w = pixelImage.width;
        int h = pixelImage.height;
        int areaW = x2 - x1, areaH = y2 - y1;
       	if (listener!=null) listener.selectionUpdated(x1, y1, areaW, areaH);
        
        float aspectRatio = ((float) areaW) / ((float) areaH);
        int rowHeight = rowY2 - rowY1;
        float topWhiteSpaceFraction = (float) (y1 - rowY1) / (float) rowHeight;
        float bottomWhiteSpaceFraction = (float) (rowY2 - y2) / (float) rowHeight;
        Iterator<Character> it;
        if (acceptableChars != null) // CP not used with training
        {
            ArrayList<Character> al = new ArrayList<Character>();
            for (int cs = 0; cs < acceptableChars.length; cs++)
            {
                CharacterRange cr = acceptableChars[cs];
                for (int c = cr.min; c <= cr.max; c++)
                {
                    Character ch = new Character((char) c);
                    if (al.indexOf(ch) < 0)
                    {
                        al.add(ch);
                    }
                }
            }
            it = al.iterator();
        }
        else
        {
            it = trainingImages.keySet().iterator();
        }
        int bestCount = 0;
        while (it.hasNext())
        {
            Character ch = it.next();
            ArrayList<TrainingImage> al = trainingImages.get(ch);
            int nimg = al.size();
            if (nimg > 0)
            {
                double mse = 0.0;
                boolean gotAny = false;
                for (int i = 0; i < nimg; i++)
                {
                    TrainingImage ti = al.get(i);
                    if (isTrainingImageACandidate(
                            aspectRatio,
                            areaW,
                            areaH,
                            topWhiteSpaceFraction,
                            bottomWhiteSpaceFraction,
                            ti))
                    {
                        double thisMSE = ti.calcMSE(pixels, w, h, x1, y1, x2, y2);
                        if ((!gotAny) || (thisMSE < mse))
                        {
                            gotAny = true;
                            mse = thisMSE;
                        }
                    }
                }
/// Maybe mse should be required to be below a certain threshold before we store it.
/// That would help us to handle things like welded characters, and characters that get improperly
/// split into two or more characters.
                if (gotAny)
                {
                    boolean inserted = false;
                    for (int i = 0; i < bestCount; i++)
                    {
                        if (mse < bestMSEs[i])
                        {
                            for (int j = Math.min(bestCount, BEST_MATCH_STORE_COUNT - 1); j > i; j--)
                            {
                                int k = j - 1;
                                bestChars[j] = bestChars[k];
                                bestMSEs[j] = bestMSEs[k];
                            }
                            bestChars[i] = ch;
                            bestMSEs[i] = mse;
                            if (bestCount < BEST_MATCH_STORE_COUNT)
                            {
                                bestCount++;
                            }
                            inserted = true;
                            break;
                        }
                    }
                    if ((!inserted) && (bestCount < BEST_MATCH_STORE_COUNT))
                    {
                        bestChars[bestCount] = ch;
                        bestMSEs[bestCount] = mse;
                        bestCount++;
                    }
                }
            }
        }
/// We could also put some aspect ratio range checking into the page scanning logic (but only when
/// decoding; not when loading training images) so that the aspect ratio of a non-empty character
/// block is limited to within the min and max of the aspect ratios in the training set.
        if (bestCount > 0)
        {
//        	System.out.println("BEST COUNT: "+bestCount);
//        	for(int i=0; i<bestCount; i++) System.out.print(bestMSEs[i]+"   ");
      	    double diff=1000.0;
      	    boolean ambiguity=false;
      	    if (bestCount>1) diff=bestMSEs[1]-bestMSEs[0]; // check for possible ambiguity 
      	    if (diff<0.3) ambiguity=true;
        	System.out.println(decodeBuffer.length()+" "+bestChars[0].charValue()+"  "+bestMSEs[0]+"  "+diff);
        	System.out.println("TS SIZE: "+trainingImages.keySet().size());
        	if (isTraining() || (bestMSEs[0]>askThreshold) || ambiguity) {
        	  char ch=bestChars[0].charValue();
        	  if ((bestMSEs[0]>confirmThreshold) || ambiguity) ch=(char)0;
        	  String s=fromUser(pixelImage, x1, y1, x2, y2, rowY1, rowY2, ch);
        	  decodeBuffer.append(s);
        	} else {
              decodeBuffer.append(bestChars[0].charValue());
        	}
        	System.out.println(decodeBuffer.toString());
        	if (listener!=null) listener.textUpdated(decodeBuffer.toString());


            //Send accuracy of this identification to the listener
            if (accListener != null)
            {
                OCRIdentification identAccuracy = new OCRIdentification(OCRComp.MSE);
                for (int i = 0; i < bestCount; i++)
                {
                    identAccuracy.addChar((char) bestChars[i], bestMSEs[i]);
                }
                accListener.processCharOrSpace(identAccuracy);
            }

        }
        else
        {
        	System.out.println("NO BEST GUESS "+x1+" "+y1+" "+x2+" "+y2);
        	String s=fromUser(pixelImage, x1, y1, x2, y2, rowY1, rowY2);
        	decodeBuffer.append(s);
        	if (listener!=null) listener.textUpdated(decodeBuffer.toString());
        	
            if (accListener != null)
            {
                OCRIdentification identAccuracy = new OCRIdentification(OCRComp.MSE);
                accListener.processCharOrSpace(identAccuracy);
            }
        }
    }
    
    public boolean isTraining() {
    	return trainingImages.keySet().size()<trainingThreshold;
    }
    
    // NOT USED
    private BufferedImage normalise(BufferedImage img, int x1, int y1, int x2, int y2) {
    	int std_width=100;
    	int std_height=100;
        int areaW = x2 - x1;
        int areaH = y2 - y1;

        //Extract the character
        BufferedImage characterImage = img.getSubimage(x1, y1, areaW, areaH);

        //Scale image so that both the height and width are less than std size
        if (characterImage.getWidth() > std_width)
        {
            //Make image always std_width wide
            double scaleAmount = (double) std_width / (double) characterImage.getWidth();
            AffineTransform tx = new AffineTransform();
            tx.scale(scaleAmount, scaleAmount);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
            characterImage = op.filter(characterImage, null);
        }

        if (characterImage.getHeight() > std_height)
        {
            //Make image always std_height tall
            double scaleAmount = (double) std_height / (double) characterImage.getHeight();
            AffineTransform tx = new AffineTransform();
            tx.scale(scaleAmount, scaleAmount);
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
            characterImage = op.filter(characterImage, null);
        }

        //Paint the scaled image on a white background
        BufferedImage normalizedImage = new BufferedImage(std_width, std_height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = normalizedImage.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, std_width, std_height);

        //Center scaled image on new canvas
        int x_offset = (std_width - characterImage.getWidth()) / 2;
        int y_offset = (std_height - characterImage.getHeight()) / 2;

        g.drawImage(characterImage, x_offset, y_offset, null);
        g.dispose();

        return normalizedImage;
    }
    
    private String fromUser(PixelImage img, int x1, int y1, int x2, int y2, int rowY1, int rowY2) {
    	return fromUser(img, x1,y1,x2,y2, rowY1, rowY2, (char)0);
    }
    
    private String fromUser(PixelImage img, int x1, int y1, int x2, int y2, int rowY1, int rowY2, char c) {    
    	listener.userRequested(c);
        //Extract the character
    	/*
        BufferedImage crop = new BufferedImage(x2-x1, y2-y1, BufferedImage.TYPE_INT_RGB);
    	Graphics g = crop.getGraphics();
//    	g.setColor(Color.BLACK);
//		g.fillRect(5,5,20,20);
    	g.drawImage(img.source,0,0,x2-x1,y2-y1,x1,y1,x2,y2,null);
    	g.dispose(); */
    	
    	//BufferedImage crop=normalise(source, x1, y1, x2, y2); // CP unchecked cast
    	BufferedImage source=(BufferedImage)(img.source);
    	BufferedImage crop = source.getSubimage(x1, y1, x2-x1, y2-y1);
        ImageIcon icon=new ImageIcon(crop);
    
        // ask to confirm if there is a guess
        if ((int)c>0) {
           int res=JOptionPane.showConfirmDialog(null,"Is this: "+c, "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon);
           if (res==JOptionPane.YES_OPTION) {
        	  return ""+c;
           }
        }
        
        // ask a guess if not confirmed
        String input="";
        while(input.length()==0) {
        	input = (String)JOptionPane.showInputDialog(null,"What is this character ?","User Input", JOptionPane.QUESTION_MESSAGE, icon, null, null);
        }
        System.out.println("GOT: "+input);
        
        // learn
        if ((input.length()==1) && (!input.equals(" "))) {
        	System.out.println("ADDING TO TS");
        	ArrayList<TrainingImage> tab=trainingImages.get(input.charAt(0));

        	// extract pixel array
            int w = x2 - x1;
            int h = y2 - y1;
            int[] pixels = new int[w * h];
            for (int y = y1, destY = 0; y < y2; y++, destY++)
            {   // arraycopy is from SRC to DST
                System.arraycopy(img.pixels, (y * img.width) + x1, pixels, destY * w, w);
            }
        	TrainingImage timg=new TrainingImage(pixels, w, h, y1-rowY1, rowY2-y2); // CP NOT SURE
        	if (tab==null) {
        		tab=new ArrayList<TrainingImage>();
        		trainingImages.put(input.charAt(0), tab);
        	} 
    		tab.add(timg); // maybe replace ??
    		System.out.println("TS SIZE: "+trainingImages.keySet().size());
    		//displayTrainingSet();
        }
        
        return input;
    }
    
    public static BufferedImage getImageFromPixelImage(PixelImage img) {
        BufferedImage image = new BufferedImage(img.width, img.height, BufferedImage.TYPE_INT_RGB);
        int[] rgb=new int[img.pixels.length*3];
        for(int i=0; i<img.pixels.length; i++) {
        	int p=img.pixels[i];
        	rgb[3*i]=p;
        	rgb[3*i+1]=p;
        	rgb[3*i+2]=p;
        }
        WritableRaster raster = (WritableRaster) image.getData();
        raster.setPixels(0,0,img.width,img.height,rgb);
        return image;
    }

    private void displayTrainingSet() {
    	if (trainingImages.keySet().size()==0) return;
    	
    	int MAX=15;

    	TrainingImage img=trainingImages.get('0').get(0); // requires "0" as sample !
    	BufferedImage sample=getImageFromPixelImage(img);
    	System.out.println("Sample: "+sample);
        BufferedImage combined = new BufferedImage(sample.getWidth()*10, sample.getHeight(), sample.getType());
    	Graphics g = combined.getGraphics();
    	
    	int i=0;
    	for(Character ch:trainingImages.keySet()) {
        	TrainingImage timg=trainingImages.get(ch).get(0);
        	BufferedImage bimg=getImageFromPixelImage(img);
            g.drawImage(bimg, i*sample.getWidth(), 0, null);
            i++;
            if (i==MAX) break;
        }
    	g.dispose();
        
    	ImageIcon icon=new ImageIcon(combined);
        JOptionPane.showConfirmDialog(null,"","Training Set", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE, icon);
    }
    
    private boolean isTrainingImageACandidate(
            float aspectRatio,
            int w,
            int h,
            float topWhiteSpaceFraction,
            float bottomWhiteSpaceFraction,
            TrainingImage ti)
    {
        // The aspect ratios must be within tolerance.
        if (((aspectRatio / ti.aspectRatio) - 1.0f) > TrainingImage.ASPECT_RATIO_TOLERANCE)
        {
            return false;
        }
        if (((ti.aspectRatio / aspectRatio) - 1.0f) > TrainingImage.ASPECT_RATIO_TOLERANCE)
        {
            return false;
        }
        // The top whitespace fractions must be within tolerance.
        if (Math.abs(topWhiteSpaceFraction - ti.topWhiteSpaceFraction)
                > TrainingImage.TOP_WHITE_SPACE_FRACTION_TOLERANCE)
        {
            return false;
        }
        // The bottom whitespace fractions must be within tolerance.
        if (Math.abs(bottomWhiteSpaceFraction - ti.bottomWhiteSpaceFraction)
                > TrainingImage.BOTTOM_WHITE_SPACE_FRACTION_TOLERANCE)
        {
            return false;
        }
        // If the area being scanned is really small and we
        // are about to crunch down a training image by a huge
        // factor in order to compare to it, then don't do that.
        if ((w <= 4) && (ti.width >= (w * 10)))
        {
            return false;
        }
        if ((h <= 4) && (ti.height >= (h * 10)))
        {
            return false;
        }
        // If the area being scanned is really large and we
        // are about to expand a training image by a huge
        // factor in order to compare to it, then don't do that.
        if ((ti.width <= 4) && (w >= (ti.width * 10)))
        {
            return false;
        }
        if ((ti.height <= 4) && (h >= (ti.height * 10)))
        {
            return false;
        }
        return true;
    }

    @Override
    public void processSpace(PixelImage pixelImage, int x1, int y1, int x2, int y2)
    {
        decodeBuffer.append(' ');
        //Send accuracy of this identification to the listener
        if (accListener != null)
        {
            OCRIdentification identAccuracy = new OCRIdentification(OCRComp.MSE);
            identAccuracy.addChar(' ', 0.0);
            accListener.processCharOrSpace(identAccuracy);
        }
    }
    private static final Logger LOG = Logger.getLogger(OCRScanner.class.getName());
}

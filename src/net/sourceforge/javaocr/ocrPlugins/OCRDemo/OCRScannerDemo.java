// OCRScannerDemo.java
// Copyright (c) 2003-2010 Ronald B. Cemer
// Modified by William Whitney
// All rights reserved.
// This software is released under the BSD license.
// Please see the accompanying LICENSE.txt for details.
package net.sourceforge.javaocr.ocrPlugins.OCRDemo;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.CharacterRange;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.OCRScanner;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImage;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImageLoader;
import net.sourceforge.javaocr.scanner.PixelImage;

/**
 * Demo application to demonstrate OCR document scanning and decoding.
 * @author Ronald B. Cemer
 */
public class OCRScannerDemo
{

    private static final long serialVersionUID = 1L;
    private boolean debug = true;
    private Image image;
    private OCRScanner scanner;

    public OCRScannerDemo()
    {
        scanner = new OCRScanner();
    }

    /**
     * Load demo training images.
     * @param trainingImageDir The directory from which to load the images.
     */
    public void loadTrainingImages(String trainingImageDir)
    {
        if (debug)
        {
            System.err.println("loadTrainingImages(" + trainingImageDir + ")");
        }
        if (!trainingImageDir.endsWith(File.separator))
        {
            trainingImageDir += File.separator;
        }
        try
        {
            scanner.clearTrainingImages();
            TrainingImageLoader loader = new TrainingImageLoader();
            HashMap<Character, ArrayList<TrainingImage>> trainingImageMap = new HashMap<Character, ArrayList<TrainingImage>>();
            System.out.println("Loading DAINAMIC data set from "+trainingImageDir);
            File folder = new File(trainingImageDir);
            File[] listOfFiles = folder.listFiles();
            for(File file:listOfFiles) {
            	String name=file.getName();
            	if (name.startsWith("num")) continue;
            	int p=name.lastIndexOf(".png");
            	if (p>0) {
            		char c;
            		if (name.startsWith("ASC")) {
            			c=(char)Integer.parseInt(name.substring(3,p));
            		}
            		else {
            			c=name.charAt(0);
            		}
            		System.out.println("LOADING: "+c);
            		loader.load(
                            file.getAbsolutePath(),
                            new CharacterRange(c,c),
                            trainingImageMap);
            	}
            }
            /*
            loader.load(
                    trainingImageDir + "ascii.png",
                    new CharacterRange('!', 'Z'),
                    trainingImageMap);
            */            
            /*
            if (debug)
            {
                System.err.println("ascii.png");
            }
            loader.load(
                    trainingImageDir + "ascii.png",
                    new CharacterRange('!', '~'),
                    trainingImageMap);
            if (debug)
            {
                System.err.println("hpljPica.jpg");
            }
            loader.load(
                    trainingImageDir + "hpljPica.jpg",
                    new CharacterRange('!', '~'),
                    trainingImageMap);
            if (debug)
            {
                System.err.println("digits.jpg");
            }
            loader.load(
                    trainingImageDir + "digits.jpg",
                    new CharacterRange('0', '9'),
                    trainingImageMap);
            */
            if (debug)
            {
                System.err.println("adding images");
            }
            // cleanTrainingImage(trainingImageMap); // TODO maybe here too
            scanner.addTrainingImages(trainingImageMap);
            if (debug)
            {
                System.err.println("loadTrainingImages() done");
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            System.exit(2);
        }
    }

    public void process(String imageFilename)
    {
        if (debug)
        {
            System.err.println("process(" + imageFilename + ")");
        }
        try
        {
            image = ImageIO.read(new File(imageFilename));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if (image == null)
        {
            System.err.println("Cannot find image file: " + imageFilename);
            return;
        }

        if (debug)
        {
            System.err.println("constructing new PixelImage");
        }

        PixelImage pixelImage = new PixelImage(image);
        if (debug)
        {
            System.err.println("converting PixelImage to grayScale");
        }
        pixelImage.toGrayScale(true);
        if (debug)
        {
            System.err.println("filtering");
        }
        pixelImage.filter();
        if (debug)
        {
            System.err.println("setting image for display");
        }
       
        System.out.println(imageFilename + ":");
        String text = scanner.scan(image, 0, 0, 0, 0, null);
        System.out.println("[" + text + "]");
    }

    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("Please specify one or more image filenames.");
            System.exit(1);
        }
        String trainingImageDir = System.getProperty("TRAINING_IMAGE_DIR");
        if (trainingImageDir == null)
        {
            System.err.println("Please specify -DTRAINING_IMAGE_DIR=<dir> on "
                    + "the java command line.");
            return;
        }
        OCRScannerDemo demo = new OCRScannerDemo();
        demo.loadTrainingImages(trainingImageDir);
        for (int i = 0; i < args.length; i++)
        {
            demo.process(args[i]);
        }
        System.out.println("done.");
    }
    private static final Logger LOG = Logger.getLogger(OCRScannerDemo.class.getName());
}

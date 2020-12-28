// OCRScannerDemo.java
// Copyright (c) 2003-2010 Ronald B. Cemer
// Modified by William Whitney
// All rights reserved.
// This software is released under the BSD license.
// Please see the accompanying LICENSE.txt for details.
package net.sourceforge.javaocr.ocrPlugins.learningGUI;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sourceforge.javaocr.ocrPlugins.mseOCR.CharacterRange;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.OCRListener;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.OCRScanner;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImage;
import net.sourceforge.javaocr.ocrPlugins.mseOCR.TrainingImageLoader;
import net.sourceforge.javaocr.scanner.PixelImage;

/**
 * Learning GUI
 * @author C. Ponsard
 */
public class LearningGUI extends JFrame
{

    private static final long serialVersionUID = 1L;
    private boolean debug = true;
    BufferedImage image;
    ImageArea img_area;
    JTextArea text_area;
    OCRScanner scanner;

    public LearningGUI()
    {
    	// GUI config
        setTitle("Scan2Run Learning GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // GUI menu
		JMenuBar menuBar = new JMenuBar();
		
		JMenu menu_file = new JMenu("File"); 
		JMenuItem load = new JMenuItem("Load Image");
		menu_file.add(load);
		JMenuItem save = new JMenuItem("Save Text");
		menu_file.add(save);
		JMenuItem exit = new JMenuItem("Exit");
		menu_file.add(exit);
		menuBar.add(menu_file);
		
		JMenu menu_scan = new JMenu("Scan"); 
		JMenuItem start = new JMenuItem("Start");
		menu_scan.add(start);
		start.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				process(image);
			}
		});
		
		JMenuItem char_load = new JMenuItem("Load learned profile");
		menu_scan.add(char_load);		
		JMenuItem char_check = new JMenuItem("Check learned profile");
		menu_scan.add(char_check);
		JMenuItem char_save = new JMenuItem("Save learned profile");
		menu_scan.add(char_save);
		menuBar.add(menu_scan);
		 
		JMenu menu_help = new JMenu("Help"); 
		JMenuItem help = new JMenuItem("Instructions");
		menu_help.add(help);
		JMenuItem about = new JMenuItem("About");
		menu_help.add(about);
		menuBar.add(menu_help);
		
		setJMenuBar(menuBar);
		
		// panels
		getContentPane().setLayout(new BorderLayout());

		JSlider slider = new JSlider(0, 1000, 500);
	    slider.addChangeListener(new ChangeListener() {
	        @Override
	        public void stateChanged(ChangeEvent e) {
	            img_area.setZoom(2. * slider.getValue() / slider.getMaximum());
	        }
	    });
	    add(slider,BorderLayout.PAGE_START);

		img_area=new ImageArea();
        JScrollPane scrollPane = new JScrollPane(img_area);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPane,BorderLayout.CENTER);
		text_area=new JTextArea();
		text_area.setRows(5);
		add(text_area,BorderLayout.PAGE_END);
		
        // scanner
    	scanner = new OCRScanner();
    }
    
    public void setImage(String path) {
        try {
            image = ImageIO.read(new File(path));
        }
        catch (IOException e) {
            LOG.warning("Image "+path+" not found - ignoring");
            return;
        }
        img_area.setImage(image);
        img_area.setZoom(0.33);  // TODO compute
        // img_area.setSelection(246,88,8,12);
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
            if (debug)
            {
                System.err.println("ascii.png");
            }
            loader.load(
                    trainingImageDir + "ascii.png",
                    new CharacterRange('!', 'Z'),
                    trainingImageMap);
            */
            if (debug)
            {
                System.err.println("adding images");
            }
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

    public void process(Image image)
    {
        if (image == null)
        {
            System.err.println("No image");
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

        scanner.addOCRListener(new OCRListener() {
			
			@Override
			public void selectionUpdated(int x, int y, int w, int h) {
				System.out.println("SEL "+x+" "+y+" "+w+" "+h);
				img_area.setSelection(x, y, w, h);
			}
			
			@Override
			public void textUpdated(String text) {
				text_area.setText(text);
			}

		});
        scanner.scan(image,0,0,0,0,null);
//        String text = scanner.scan(image, 0, 0, 0, 0, null);
//        System.out.println("[" + text + "]");
    }

    public static void main(String[] args)
    {
    	// TODO USAGE
        {
            System.err.println("Please specify one or more image filenames.");
            System.err.println("Use -demo to load demo image");
        } 
        String trainingImageDir = System.getProperty("TRAINING_IMAGE_DIR");
        
        String img_path=null;
        if (args.length>0) {
        	if (args[0].equals("-demo")) {
        		img_path=".\\ListingTests\\LISTING-P1.png";
        	  trainingImageDir=".\\ListingTests\\trainingDAINAMIC";
        	} else {
        		img_path=args[0];
        	}		
        }     
        if (trainingImageDir == null)
        {
            System.err.println("Please specify -DTRAINING_IMAGE_DIR=<dir> on "
                    + "the java command line.");
            return;
        }
        
        LearningGUI gui = new LearningGUI();
        gui.setImage(img_path);
        gui.loadTrainingImages(trainingImageDir);
        gui.pack();
        gui.setSize(800,600);
        gui.setVisible(true);
    }
    private static final Logger LOG = Logger.getLogger(LearningGUI.class.getName());
}

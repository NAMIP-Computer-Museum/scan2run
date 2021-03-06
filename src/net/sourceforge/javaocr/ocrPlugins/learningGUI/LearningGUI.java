// OCRScannerDemo.java
// Copyright (c) 2003-2010 Ronald B. Cemer
// Modified by William Whitney
// All rights reserved.
// This software is released under the BSD license.
// Please see the accompanying LICENSE.txt for details.
package net.sourceforge.javaocr.ocrPlugins.learningGUI;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultCaret;

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
    private static final String APP_NAME="Scan2Run Learning GUI";
    private boolean debug = true;
    BufferedImage image;
    File currentFile;
    ImageArea img_area;
    JTextArea txt_area;
    JPanel pan_inter;
    JLabel lab_problem, lab_question;
    JTextField txt_answer;
    boolean validated=false;
    OCRScanner scanner;
    int nRequested;
    int nAsked;
    int nConfirmed;

    public LearningGUI()
    {
    	// GUI config
        setTitle(APP_NAME);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // GUI menu
		JMenuBar menuBar = new JMenuBar();
		
		JMenu menu_file = new JMenu("File"); 
		JMenuItem load = new JMenuItem("Load Image");
		load.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				loadImage();
			}
		});		
		
		
		menu_file.add(load);
		JMenuItem save = new JMenuItem("Save Text");
		menu_file.add(save);
		save.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				saveText();
			}
		});		
		
		JMenuItem exit = new JMenuItem("Exit");
		menu_file.add(exit);
		exit.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});		
		menuBar.add(menu_file);
		
		JMenu menu_scan = new JMenu("Scan");
		JMenuItem start = new JMenuItem("Start");		
		JMenuItem stop = new JMenuItem("Stop");		
		stop.setEnabled(false);

		menu_scan.add(start);
		start.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				start.setEnabled(false);
				stop.setEnabled(true);
				txt_area.setText("");
				process(image);
			}
		});

		menu_scan.add(stop);
		stop.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("STOP requested");
				scanner.getDocumentScanner().stop();
				start.setEnabled(true);
				stop.setEnabled(false);
			}
		});
		menuBar.add(menu_scan);
		
		JMenuItem params = new JMenuItem("Parameters");
		menu_scan.add(params);
				
		JMenu menu_training = new JMenu("Training");
		JMenuItem char_clear = new JMenuItem("Clear learned profile");
		char_clear.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("CLEARING");
				scanner.clearTrainingImages();
			}
		});
		menu_training.add(char_clear);
		JMenuItem char_load = new JMenuItem("Load learned profile");
		char_load.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("LOADING");
			}
		});
		menu_training.add(char_load);		
		JMenuItem char_check = new JMenuItem("Check learned profile");
		menu_training.add(char_check);
		char_check.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("CHECKING");
				checkProfile();
			}
		});
		JMenuItem char_save = new JMenuItem("Save learned profile");
		char_save.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("SAVING");
				saveProfile();
			}
		});
		menu_training.add(char_save);
		menuBar.add(menu_training);
		 
		JMenu menu_help = new JMenu("Help"); 
		JMenuItem help = new JMenuItem("Instructions");
		menu_help.add(help);
		JMenuItem about = new JMenuItem("About");
		menu_help.add(about);
		about.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(LearningGUI.this,"Scan2Run Learning GUI");  
			}
		});
		menuBar.add(menu_help);
		setJMenuBar(menuBar);
		
		// panels
		getContentPane().setLayout(new BorderLayout());

		final JSlider slider = new JSlider(0, 1000, 500);
	    slider.addChangeListener(new ChangeListener() {
	        @Override
	        public void stateChanged(ChangeEvent e) {
	            img_area.setZoom(2. * slider.getValue() / slider.getMaximum());
	        }
	    });
	    add(slider,BorderLayout.PAGE_START);

	    // scanned image
		img_area=new ImageArea();
        JScrollPane scrollPaneImg = new JScrollPane(img_area);
        scrollPaneImg.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneImg.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPaneImg,BorderLayout.CENTER);
		
		// interaction panel
		JPanel pan_inter=new JPanel();
		pan_inter.setPreferredSize(new Dimension(220,300));
		pan_inter.setLayout(new BoxLayout(pan_inter, BoxLayout.Y_AXIS));

		lab_problem=new JLabel();
		lab_question=new JLabel("TEST");

		txt_answer=new JTextField(10);
		txt_answer.setMaximumSize(new Dimension(200,20));
//		txt_answer.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel lab_title=new JLabel("Feedback (hit enter to validate)");
		lab_title.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan_inter.add(lab_title);
		pan_inter.add(Box.createRigidArea(new Dimension(0,5)));
		pan_inter.add(lab_question);
		lab_question.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan_inter.add(Box.createRigidArea(new Dimension(0,5)));
		pan_inter.add(lab_problem);
		lab_problem.setMinimumSize(new Dimension(100,100));
		lab_problem.setAlignmentX(Component.CENTER_ALIGNMENT);
		pan_inter.add(Box.createRigidArea(new Dimension(0,5)));
		pan_inter.add(txt_answer);
		txt_answer.addActionListener(new ActionListener() {
			
			@Override
			public  void actionPerformed(ActionEvent e) {
				System.out.println("TEXT IS:"+txt_answer.getText());
				synchronized(txt_answer) {
					validated=true;
					txt_answer.notifyAll();
				}
			}
		});
		add(pan_inter,BorderLayout.LINE_END);
		
		// text recognised
		txt_area=new JTextArea();
		txt_area.setRows(15);
		DefaultCaret caret = (DefaultCaret) txt_area.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);    
        JScrollPane scrollPaneTxt = new JScrollPane(txt_area);
        scrollPaneTxt.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPaneTxt.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		add(scrollPaneTxt,BorderLayout.PAGE_END);
		
        // scanner
    	scanner = new OCRScanner();
    }
    
    public void setImage(File file) {
        try {
            image = ImageIO.read(file);
            setTitle(APP_NAME+" - "+file.getName());
        }
        catch (IOException e) {
            LOG.warning("Image "+file+" not found - ignoring");
            return;
        }
        img_area.setImage(image);
        img_area.setZoom(0.33);  // TODO compute
        currentFile=file;
        // img_area.setSelection(246,88,8,12);
    }
    
    public void loadImage() {
		final JFileChooser fc = new JFileChooser();
		fc.setCurrentDirectory(currentFile);
		int returnVal = fc.showOpenDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) return;

		File file=fc.getSelectedFile();
		setImage(file);;
    }
    
    public void saveText() {
		String txt=txt_area.getText();
		final JFileChooser fc = new JFileChooser();
		int returnVal = fc.showSaveDialog(this);
		if (returnVal != JFileChooser.APPROVE_OPTION) return;
		
		File file=fc.getSelectedFile();
		try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
		    out.print(txt);
		} catch (FileNotFoundException e1) {
			LOG.warning("Could not save to "+file);
		}
    }

    /**
     * Load demo training images.
     * @param trainingImageDir The directory from which to load the images.
     */
    public void loadTrainingImages(File folder)
    {
        if (debug)
        {
            System.err.println("loadTrainingImages(" + folder.getAbsolutePath() + ")");
        }
        try
        {
            scanner.clearTrainingImages();
            TrainingImageLoader loader = new TrainingImageLoader();
            HashMap<Character, ArrayList<TrainingImage>> trainingImageMap = new HashMap<Character, ArrayList<TrainingImage>>();
            LOG.info("Loading DAINAMIC data set from "+folder.getAbsolutePath());
            File[] listOfFiles = folder.listFiles();
            for(File file:listOfFiles) {
            	String name=file.getName();
            	if (name.startsWith("num")) continue;
            	int p=name.lastIndexOf(".png");
            	if (p>0) {
            		char c1,c2;
            		if (name.startsWith("RAN")) {
            			int p1=name.indexOf("-");
            			int p2=name.indexOf(".");
            			if (p1<0) break;
            			if (p2<0) break;
            			if (p2<p1) break;
            			System.out.println(name.substring(3,p1)+" "+name.substring(p1+1,p2));
            			c1=(char)Integer.parseInt(name.substring(3,p1));
            			c2=(char)Integer.parseInt(name.substring(p1+1,p2));
                		System.out.println("LOADING: "+c1+".."+c2);
            		} else if (name.startsWith("ASC")) {
            			c1=(char)Integer.parseInt(name.substring(3,p));
            			c2=c1;
                		System.out.println("LOADING: "+c1);
            		} else {
            			c1=name.charAt(0);
            			c2=c1;
                		System.out.println("LOADING: "+c1);
            		}
            		loader.load(
                            file.getAbsolutePath(),
                            new CharacterRange(c1,c2),
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
            HashMap<Character, ArrayList<TrainingImage>> cleanedImageMap=cleanTrainingImage(trainingImageMap);
            scanner.addTrainingImages(cleanedImageMap);
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
    
    private void updateStatistics(char c) {
		nRequested++;
		if (c==(char)0) 
			nAsked++;
		else 
			nConfirmed++;
		int txt_l=txt_area.getText().length();
        System.out.println(txt_l+" "+nRequested+" "+nAsked+" "+nConfirmed+" "+(nRequested*1.0)/txt_l);
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

        nRequested=0;
        nAsked=0;
        nConfirmed=0;
        scanner.setAskThreshold(2.0);
        scanner.setConfirmThreshold(2.5);
        scanner.setTrainingThreshold(0);
        scanner.addOCRListener(new OCRListener() {
			
			@Override
			public void selectionUpdated(int x, int y, int w, int h) {
				System.out.println("SEL "+x+" "+y+" "+w+" "+h);
				img_area.setSelection(x, y, w, h);
			}
			
			@Override
			public void textUpdated(String text) {
				txt_area.setText(text);
			}

			@Override
			public String userRequested(String question, ImageIcon icon, char candidate) {
				// request
				lab_question.setText(question);
				lab_problem.setIcon(icon);
				txt_answer.setText("");
				validated=false;
				txt_answer.requestFocus();
				
				// wait for input
				synchronized (txt_answer) {
				while(!validated) {
					try {
						txt_answer.wait();
					} catch (InterruptedException e) {
						System.out.println("ERROR");
					}
				};
				}
				System.out.println("ICI");
				
				// statistics
				updateStatistics(candidate);
				
				// return value
				return txt_answer.getText();
			}

		});
        
        Thread t_scan=new Thread() {
        	public void run() {
                String text=scanner.scan(image,0,0,0,0,null);
                System.out.println(text.length()+" "+nRequested+" "+nAsked+" "+nConfirmed+" "+(nRequested*1.0)/text.length());                
        	}
        };
        t_scan.start();
        
//        String text = scanner.scan(image, 0, 0, 0, 0, null);
//        System.out.println("[" + text + "]");
    }
    
    public void loadProfile() {
    	
    }
    
    public void checkProfile() {
    	BufferedImage combined=scanner.getTrainingSetImage();
    	ImageIcon icon=new ImageIcon(combined);
        JOptionPane.showMessageDialog(null,"","Training Set", JOptionPane.INFORMATION_MESSAGE, icon);
    }
        
    public void saveProfile() {
    	// ask directory
    	JFileChooser fileChooser = new JFileChooser();
    	fileChooser.setDialogTitle("Specify a destination folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    	int sel=fileChooser.showSaveDialog(this);
    	if (sel != JFileChooser.APPROVE_OPTION) return;
    	
    	File dir = fileChooser.getSelectedFile();
    	System.out.println("Save to: " + dir.getAbsolutePath());
    	
    	// TODO check dir empty
    	
    	// range
    	BufferedImage combined=scanner.getTrainingSetImage();
    	String name=scanner.getTrainingSetName();
    	File output=new File(dir,name);
    	try {
			ImageIO.write(combined, "png", output);
		} catch (IOException e) {
			System.err.println("Could not write: "+output);
		}

        /*
    	for(Character c:trainingImages.keySet()) {
    		ArrayList<TrainingImage> list=trainingImages.get(c);
    		for(TrainingImage img:list) {
    			File file=new File(dir,"ASC"+(int)c.charValue()+".png");
    			BufferedImage bimg=OCRScanner.getImageFromPixelImage(img);
    			try {
					ImageIO.write(bimg, "png", file);
				} catch (IOException e) {
					System.err.println("Could not write: "+file);
				}
    			System.out.println(file);
    		}
    	}
    	*/
    	
    }
    
    private static boolean isBlack(TrainingImage img) {
   // 	BufferedImage bimg=OCRScanner.getImageFromPixelImage(img);
   //     JOptionPane.showMessageDialog(null,"","Training Set", JOptionPane.INFORMATION_MESSAGE, new ImageIcon(bimg));
    	System.out.println("IMG "+img.width+" "+img.height);
    	int[] tab=img.pixels;
    	long tot=0;
    	for(int i=0; i<tab.length; i++) {
    		tot+=tab[i];
    	}
    	return (tot<100*tab.length);
    }
    
    // remove black images
    private HashMap<Character, ArrayList<TrainingImage>> cleanTrainingImage(HashMap<Character, ArrayList<TrainingImage>> map) {
    	HashMap<Character, ArrayList<TrainingImage>> nmap=new HashMap<Character, ArrayList<TrainingImage>>();
    	for(Character c:map.keySet()) {
    		ArrayList<TrainingImage> list=map.get(c);    		
    		ArrayList<TrainingImage> nlist=new ArrayList<TrainingImage>();
    		for(TrainingImage img:list) {
    			System.out.println("CHAR "+c);
    			if (!isBlack(img)) {
    				nlist.add(img);
    			} else {
    				System.out.println("CLEANING BLACK "+c);
    			}
    		}
    		if (!nlist.isEmpty()) nmap.put(c,nlist);
    	}
    	for(Character c:nmap.keySet()) {
    		System.out.println("**** "+c);
    	}
    	return nmap;
    }

    public static void main(String[] args)
    {
    	File workdir=new File(System.getProperty("user.dir"));
    	LOG.info("Working directory: "+workdir.getAbsolutePath());
    	
        String trainingImageDir = System.getProperty("TRAINING_IMAGE_DIR");
        
        String img_path=null;
        if (args.length>0) {
        	if (args[0].equals("-demo")) {
        		img_path="ListingTests/LISTING-P1b.png";
        	  trainingImageDir="ListingTests/trainingSet";
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
        gui.setImage(new File(workdir, img_path));
        gui.loadTrainingImages(new File(workdir,trainingImageDir));
        gui.pack();
        gui.setSize(800,600);
        gui.setVisible(true);
    }
    private static final Logger LOG = Logger.getLogger(LearningGUI.class.getName());
}

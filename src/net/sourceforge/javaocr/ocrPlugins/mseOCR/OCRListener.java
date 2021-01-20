package net.sourceforge.javaocr.ocrPlugins.mseOCR;

import javax.swing.ImageIcon;

public interface OCRListener {
	// called when cursor is moved to new scan position
	public void selectionUpdated(int x, int y, int w, int h);

	// called when text is updated with new character
	public void textUpdated(String text);
	
	// called when user input is requested
	public String userRequested(String question, ImageIcon icon, char candidate);	
	
}

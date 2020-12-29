# Scan2Run

## Goal and Scope

This project focus on the digital preservation of computer heritage distributed in paper form (e.g. old magasines)
Such material can be the only available form (no electronic support available) but can be found scanned format or can be scanned.
However in order to transform such a listing in a running computer programs and sharing the experience requires quite a few steps:
* retyping the program
* loading it into the device or some emulator
* capturing some result in textual, image or even video format

The main scope is to be able to run listing of 80's programs (e.g. BASIC) into an emulator with MAME as primary target. Our use case are emulator such as the Amstrad CPC (quite widespread) and the DAI In-DATA Imagination Machine (very rare computer).

# Features

This projet currently aims at gathering and sharing minimal useful toolset supporting the above goals through
* simple OCRing of listings with some learning support
* injection in emulator through available means, at minimum keyboard injection simulating retyping

Complementary Open Source tools such as gimp and ODS can be used to support digital capture of the resulting execution.

## Status

The code is in alpha stage and include the following
* OCR adapted from Java OCR (see https://roncemer.com/software-development/java-ocr/ and sourceforge) with improvements for interactive learning phase
* Lua scripts for MAME injection either on a line basis or character per frame basis (depending on what the machine can accept as input)

## Running the code

Quick instructions for listing OCR
* clone the project
* compile using ANT task or using compile.sh/bat script in the scripts directory and run demoDAI.sh/.bat for an automated listing recognition process
* alternately you can also compile using JDT and run net.sourceforge.javaocr.ocrPlugins.learningGUI.LearningGUI with -demo argument
* once the GUI is started, goto the scan menu and select "start"
* answer the occasional question to enter unknown character or confirm some possibily unreliable characters
* current progress is visible in UI (red rectangle) and bottom text area is continuouslu updated
* limitation: modal window blocks access to sliders

![Scan in progress](https://github.com/NAMIP-Computer-Museum/scan2run/blob/main/screenshots/Scan2Run-scap02.png?raw=true)

Quick instruction for listing injection into MAME
* launch MAME in console mode with the following option 
  -console -autoboot_script import.lua
* start your emulator with basic prompt
* type inject(<path_to_listing> in the MAME prompt
* check the import at the console, use F10 full speed more to speed up import
* checkout the video at: https://www.youtube.com/watch?v=L2JlUjgxQHo

## Troubleshooting/Limitations

For OCR:
* modal dialogs block the UI so the controls (sliders for zoom, image and text area are not accessible once the process is started)
* different training parameters can be used to force more validation (error level, number of occurences, coverage of coding characters...)
* quality of the training set cannot yet be checked but is planned, possibly removing poor entries or learning errors ;-)

For injection
* make sure the character set matches the target machine, use some preprocessing if necessary
* if carriage returns are missed, add extra carriage returns between lines (or edit the script to inject extra ones)

## Future work

* manage learned profiles, e.g. for magasines using the same font
* training set editing
* improved documentation, video tutorial

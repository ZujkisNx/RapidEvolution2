package com.mixshare.rapid_evolution.ui.swing.animation;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;

import org.apache.log4j.Logger;

/**
 * When I tried this it didn't work that well at all...
 */
public class Dissolver extends JComponent implements Runnable {

    private static Logger log = Logger.getLogger(Dissolver.class);
    
    Frame frame;
    Window fullscreen;
    int count;
    BufferedImage frame_buffer;
    BufferedImage screen_buffer;
    
    public Dissolver() { }
    
    public void dissolveExit(JFrame frame) {
        try {
            this.frame = frame;
            Robot robot = new Robot();
            
            // cap screen w/ frame to frame buffer
            Rectangle frame_rect = frame.getBounds();
            frame_buffer = robot.createScreenCapture(frame_rect);
            
            // hide frame
            frame.setVisible(false);
            
            // cap screen w/o frame
            Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screen_rect = new Rectangle(0, 0, screensize.width, screensize.height);
            screen_buffer = robot.createScreenCapture(screen_rect);
            
            // create big window w/o decorations
            fullscreen = new Window(new JFrame());
            fullscreen.setSize(screensize);
            fullscreen.add(this);
            this.setSize(screensize);
            fullscreen.setVisible(true);
            
            // start animation
            new Thread(this).start();        
        } catch (Exception e) {
            log.error("dissolveExit(): error Exception", e);
        }
    }
    
    public void run() {
        try {
            count = 0;
            Thread.currentThread().sleep(100);
            for (int i = 0; i < 20; ++i) {
                count = i;
                fullscreen.repaint();
                Thread.currentThread().sleep(100);
            }
        } catch (Exception e) {
            log.error("run(): error Exception", e);
        }
    }
    
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        // draw the screen, offset in case the window isn't at 0,0
        g.drawImage(screen_buffer, -fullscreen.getX(), -fullscreen.getY(), null);
        
        // draw the frame
        Composite old_comp = g2.getComposite();
        Composite fade = AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, 1.0f - ((float)count) / 20.0f);
        g2.setComposite(fade);
        g2.drawImage(frame_buffer, frame.getX(), frame.getY(), null);
        g2.setComposite(old_comp);
    }
    
}

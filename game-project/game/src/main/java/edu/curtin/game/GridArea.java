// WARNING: don't modify this file, unless you're sure you know what you're doing!

package edu.curtin.game;

import java.awt.*;                                          //SOurced from Assignment 1 starter code
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;
import java.util.*;
import java.util.List; // To use java.util.List (and not java.awt.List).

/**
 * A Swing GUI element that implements an automatically-scaling on-screen area, on which you can
 * position captioned icons, represented as GridAreaIcon objects.
 *
 * You can add images like this:
 *
 * GridArea area = new GridArea(100, 100);
 * area.getIcons().add(new GridAreaIcon(...));
 * area.repaint();
 *
 * You can move (or otherwise change) images like this:
 *
 * GridAreaIcon icon = ...;
 * icon.setPosition(5.0, 6.0);
 * icon.setRotation(45.0);     // degrees
 * area.repaint();
 *
 * Remember to call area.repaint() to ask the GUI to redraw the panel after you've modified
 * something. If you're making several changes at once, only call it once at the end.
 */
public class GridArea extends JPanel
{
    private double gridWidth;
    private double gridHeight;
    private double gridSquareSize = 1.0; // Re-calculated
    private boolean gridLines = true;
    private Color captionColour = Color.WHITE;
    private List<GridAreaIcon> icons = new ArrayList<>();

    public GridArea(double gridWidth, double gridHeight)
    {
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }
    
    /**
     * Changes whether grid lines are visible.
     */
    public void setGridLines(boolean gridLines)
    {
        this.gridLines = gridLines;
    }
    
    /**
     * Retrieves a modifiable list of GridAreaIcons. Use this to _add_ new icon objects; e.g.:
     *
     * GridArea area = ...;
     * area.getIcons().add(new GridAreaIcon(...));
     * area.repaint();
     */
    public List<GridAreaIcon> getIcons()
    {
        return icons;
    }

    /**
     * Sets the colour used to display the caption text for each icon.
     */
    public void setCaptionColour(Color captionColour)
    {
        this.captionColour = captionColour;
    }
    
    /**
     * Redraws the grid area, either because the user is manipulating the window, OR because you've
     * called 'repaint()'.
     */
    @Override
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D gfx = (Graphics2D) g;
        gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
                             RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        
        // First, calculate how big each grid cell should be, in pixels. (We must redo this
        // every time, because the size can change.)
        gridSquareSize = Math.min(getWidth() / gridWidth,
                                  getHeight() / gridHeight);

        if(gridLines)
        {
            // Draw the grid lines. This may help for debugging purposes, and just generally
            // to see what's going on.
            gfx.setColor(Color.GRAY);

            for(double gridX = 0.0; gridX < gridWidth; gridX++) // Internal vertical grid lines
            {
                int x = (int) ((gridX + 0.5) * gridSquareSize);
                gfx.drawLine(x, (int)(gridSquareSize / 2.0),
                             x, (int)((gridHeight - 0.5) * gridSquareSize));
            }
            
            for(double gridY = 0.0; gridY < gridHeight; gridY++) // Internal horizontal grid lines
            {
                int y = (int) ((gridY + 0.5) * gridSquareSize);
                gfx.drawLine((int)(gridSquareSize / 2.0), y,
                             (int)((gridWidth - 0.5) * gridSquareSize), y);
            }
        }
        
        // Draw all the images and their captions.
        for(var icon : icons)
        {
            if(icon.isShown())
            {
                drawIcon(gfx, icon);
            }
        }
    }
    
    
    /** 
     * Draw a GridAreaIcon -- its image and caption -- at their proper location. Only
     * to be called from within paintComponent().
     */
    private void drawIcon(Graphics2D gfx, GridAreaIcon icon)
    {
        // Put the actual image on the screen.
        var transform = new AffineTransform();
        transform.translate(0.5, 0.5);
        transform.scale(gridSquareSize, gridSquareSize);
        transform.concatenate(icon.getTransform());
        gfx.drawImage(icon.getImage(), transform, null);

        // Draw the caption below the image.
        gfx.setColor(captionColour);
        FontMetrics fm = gfx.getFontMetrics();
        String caption = icon.getCaption();
        gfx.drawString(
            caption,
            (int) ((icon.getX() + 0.5) * gridSquareSize - fm.stringWidth(caption) / 2.0),
            (int) ((icon.getY() + 1.0) * gridSquareSize) + fm.getHeight());
    }
}

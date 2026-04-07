package edu.curtin.game;

import java.awt.*;                                          //SOurced from Assignment 1 starter code
import java.awt.geom.AffineTransform;

public class GridAreaIcon
{
    private AffineTransform transform = new AffineTransform();
    private boolean redoTransform = true;
    private double x, y, rotation, scale;
    private Image image;
    private String caption;
    private boolean shown = true;

    public GridAreaIcon(double x, double y, double rotation, double scale, Image image, String caption)
    {
        this.x = x; this.y = y; this.rotation = rotation; this.scale = scale;
        this.image = image; this.caption = caption;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public boolean isShown() { return shown; }

    public AffineTransform getTransform()
    {
        if (redoTransform)
        {
            transform.setToIdentity();
            transform.translate(x, y);
            double w = image.getWidth(null);
            double h = image.getHeight(null);
            double rad = rotation*Math.PI/180.0;

            if (w>h) {
                transform.translate(0,(1-h/w)/2.0);
                transform.rotate(rad,0.5,0.5*h/w);
                transform.scale(scale/w, scale/w);
            } else {
                transform.translate((1-w/h)/2.0,0);
                transform.rotate(rad,0.5*w/h,0.5);
                transform.scale(scale/h, scale/h);
            }
            redoTransform=false;
        }
        return transform;
    }

    public Image getImage() { return image; }
    public String getCaption() { return caption; }
}

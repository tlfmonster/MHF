package com.mhframework.platform.graphics;

import com.mhframework.math.MHVector;
import com.mhframework.math.geom.MHRectangle;


public abstract class MHGraphicsCanvas
{
    private MHVector size;
    protected MHFont font;
    
    public void setSize(int width, int height)
    {
        if (size == null)
            size = new MHVector(width, height);
        else
            size.setVector(width, height);
    }

    
    
    
    public int getWidth()
    {
        return (int)size.getX();
    }

    
    public int getHeight()
    {
        return (int)size.getY();
    }
    
    
    public abstract void setFont(MHFont font);
    public abstract void setColor(MHColor color);
    public abstract void drawString(String text, int x, int y);
    public abstract void fill(MHColor bgColor);
    public abstract void drawImage(MHBitmapImage image, int x, int y);
    public abstract void drawImage(MHBitmapImage image, int x, int y, int w, int h);
    public abstract void fillRect(MHRectangle rect);
    public abstract void fillRect(int x, int y, int width, int height);
    public abstract void drawRect(MHRectangle rect);
    public abstract void drawRect(int x, int y, int width, int height);
    public abstract void drawLine(int x1, int y1, int x2, int y2);

    
    //public MHFont getFont()
    //{
    //    return font;
    //}
}

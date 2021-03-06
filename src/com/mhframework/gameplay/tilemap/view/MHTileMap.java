package com.mhframework.gameplay.tilemap.view;


import com.mhframework.MHRenderable;
import com.mhframework.MHScreenManager;
import com.mhframework.gameplay.actor.MHActor;
import com.mhframework.gameplay.tilemap.MHMapCellAddress;
import com.mhframework.gameplay.tilemap.MHMapGrid;
import com.mhframework.gameplay.tilemap.MHObjectFactory;
import com.mhframework.gameplay.tilemap.MHTileMapDirection;
import com.mhframework.math.MHVector;
import com.mhframework.math.geom.MHRectangle;
import com.mhframework.platform.event.MHMouseTouchEvent;
import com.mhframework.platform.graphics.MHGraphicsCanvas;

/********************************************************************
 * Base class for deriving tile-based map objects.  It may be used by
 * itself for creating rectangular tile maps and extended to make
 * isometric and hexagonal maps.  This class and its subclasses
 * handle the presentation of and user interaction with the data
 * contained in an MHMapGrid object.
 * 
 * @author Michael Henson
 */
public class MHTileMap implements MHRenderable
{
    ////////////////////////////
    ////    Data Members    ////
    ////////////////////////////

    /** Object containing the map data. */
    private final MHMapGrid mapData;

    /** The width of a standard base tile. */
    private int tileWidth;

    /** A rectangle containing the screen coordinates. */
    protected MHRectangle screenSpace;

        /** A rectangle containing all of the coordinates of the
         * entire virtual world. */
    protected MHRectangle worldSpace;

        /** A rectangle defining the difference between the screen
         * space and the world space which is used for clipping
     * and stuff.
     */
    protected MHRectangle anchorSpace;

        /** The upper-left corner of the visible portion of
         * the map.
         */
    protected MHVector screenAnchor = new MHVector(0, 0);

    protected MHMapCellAddress cursorAddress = new MHMapCellAddress();
    protected MHVector cursorPoint = new MHVector();
    protected MHVector cursorAnchor = new MHVector();

    private boolean mouseScroll = false;


    ////////////////////////////
    ////      Methods       ////
    ////////////////////////////

        /****************************************************************
         * Constructor.  Creates the map data structure and sets up the
         * virtual spaces (screen space, world space, and anchor space).
         *
         * @param data      A reference to the application's data model.
         * @param filename  The name of the map file to be loaded into
         *                  this tile map.
         * @param vendor    A reference to an existing MHObjectVendor
         *                  for instantiating interactive actor objects.
         */
    public MHTileMap(final String filename, final MHObjectFactory vendor)
    {
        mapData = new MHMapGrid(filename);//, vendor);

        // set a default tile width
        setTileWidth(128);

        //set screen space
        setScreenSpace(0, 0, MHScreenManager.getDisplayWidth(), MHScreenManager.getDisplayHeight());
    }


    /****************************************************************
     * Tile plotter.  Converts map coordinates to screen coordinates.
     *
     * Remember, this version is intended for rectangular tile maps.
     * It must be overridden to work for isometric and hex maps.
     *
     * @param mapRow  The row of the map whose pixel position is
     *                 being calculated.
     * @param mapCol  The column of the map whose pixel position is
     *                 being calculated.
     *
     * @return The pixel position on screen where the base tile is
     *          to be rendered.
     */
    public MHVector plotTile(final int mapRow, final int mapCol)
    {
        MHVector plotPoint;

                // Calculate world coordinates
        final int x = mapCol * getTileWidth();
        final int y = mapRow * getTileHeight();

                // Convert world coordinates to screen coordinates
                plotPoint = worldToScreen(new MHVector(x, y));

                return plotPoint;
    }

    
    public MHVector calculateBasePoint(MHActor actor)
    {
        return actor.getPosition();
    }

        /****************************************************************
     * Scrolls the map in the distance specified by the input
     * parameters.
     *
     * @param scrollX  The horizontal distance in pixels to scroll
     *                 the map
     * @param scrollY  The vertical distance in pixels to scroll
     *                 the map
     */
    public void scrollMap(final int scrollX, final int scrollY)
    {
                // Move the screen anchor.
                screenAnchor.setX(screenAnchor.getX() + scrollX);
                screenAnchor.setY(screenAnchor.getY() + scrollY);

                // Clip the screen anchor
                clipScreenAnchor();

    }


        protected void clipScreenAnchor()
        {
                // Right edge
                if (screenAnchor.getX() >= anchorSpace.width)
                    screenAnchor.setX(anchorSpace.width - 1);

        // Bottom edge
                if (screenAnchor.getY() >= anchorSpace.height)
                    screenAnchor.setY(anchorSpace.height - 1);

        // Left edge
                if (screenAnchor.getX() < anchorSpace.x)
                    screenAnchor.setX(anchorSpace.x);

        // Top edge
                if (screenAnchor.getY() < anchorSpace.y)
                    screenAnchor.setY(anchorSpace.y);
        }


        /****************************************************************
     * Convert screen coordinates to world coordinates.
     *
     * @param screen  A point in screen coordinates.
     *
     * @return  The input screen point translated into world
     *           coordinates.
     */
    public MHVector screenToWorld(final MHVector screen)
    {
                final MHVector world = screen.clone();

        // translate into plotspace coordinates
        world.setX(world.getX() - screenSpace.x);
        world.setY(world.getY() - screenSpace.y);

        // translate into world coordinates
                world.setX(world.getX() + screenAnchor.getX());
                world.setY(world.getY() + screenAnchor.getY());

        return world;
    }


        /****************************************************************
     * Convert world coordinates to screen coordinates.
     *
     * @param location  A point in world space.
     *
     * @return A point in screen space.
     */
    public MHVector worldToScreen(final MHVector location)
    {
        int screenX, screenY;

        // translate into plotspace coordinates
        screenX = (int)(location.getX() - screenAnchor.getX());
        screenY = (int)(location.getY() - screenAnchor.getY());

        // translate into screen coordinates
            screenX += screenSpace.x;
            screenY += screenSpace.y;

        return new MHVector(screenX, screenY);
    }


        /****************************************************************
         * Calculate the anchor space for this tile map.
         */
    protected void calculateAnchorSpace()
    {
        int width = worldSpace.width - screenSpace.width;
        int height = worldSpace.height - screenSpace.height;

            if(width <= 0)
                width = 1;

            if(height <= 0)
            height = 1;

                anchorSpace = new MHRectangle();
                anchorSpace.setRect(worldSpace.x, worldSpace.y, width, height);
    }


        /****************************************************************
         * Calculate the world space for this tile map.
         */
    protected void calculateWorldSpace()
    {
        //set worldspace rect to empty
        worldSpace = new MHRectangle();

        //point for plotting
        MHVector ptPlot;

                // Parameters for world space rectangle
                int wx = 0,
                        wy = 0,
                        width = 0,
                        height = 0;

        //loop through map positions
        for(int row = 0; row < mapData.getHeight(); row++)
        {
                for(int col = 0; col < mapData.getWidth(); col++)
                {
                        //plot the map point
                        ptPlot = plotTile(row, col);

                        //expand the boundaries of worldspace
                        //left
                        wx = (int) Math.min(ptPlot.getX(), worldSpace.x);

                        //top
                        wy = (int) Math.min(ptPlot.getY(), worldSpace.y);

                        //right
                        width = (int) Math.max(ptPlot.getX() + getTileWidth(),
                                         worldSpace.width);

                        //bottom
                        height = (int) Math.max(ptPlot.getY() + getTileHeight(),
                                  worldSpace.height);

                            worldSpace.setRect(wx, wy, width, height);

                    } // for (int col...
            } // for (int row...
    }


        /****************************************************************
         * Returns the width of a base tile in this tile map.
         */
    public int getTileWidth()
    {
        return tileWidth;
    }


        /****************************************************************
         * Returns the height of a base tile in this tile map.  For
         * normal rectangular maps, the tile height is usually the same
         * as the tile width because the tiles are square.  Override
         * this method in subclasses requiring something different, such
         * as isometric maps.
         */
    public int getTileHeight()
    {
        return tileWidth;
    }


        /****************************************************************
         * Determines if the given point is a valid world coordinate.
         *
         * @return  True if the given point is a valid point in world
         *          space; false otherwise.
         */
    public boolean isWorldCoordinate(final MHVector p)
    {
        return worldSpace.contains(p);
    }


        /****************************************************************
         * Determines if the given point is a valid screen coordinate.
         *
         * @return  True if the given point is a valid point in screen
         *          space; false otherwise.
         */
    public boolean isScreenCoordinate(final MHVector p)
    {
        return screenSpace.contains(p);
    }


        /****************************************************************
         * Determines if the given point is a valid anchor coordinate.
         *
         * @return  True if the given point is a valid point in anchor
         *          space; false otherwise.
         */
    public boolean isAnchorCoordinate(final MHVector p)
    {
        return anchorSpace.contains(p);
    }


        /****************************************************************
         * Defines the bounds of the screen space.  Screen space is the
         * area on the screen where the game world is visible.
         */
    public void setScreenSpace(final int x, final int y, final int width, final int height)
    {
        screenSpace = new MHRectangle(x, y, width, height);

        //set world space
        calculateWorldSpace();

        //set anchor space
        calculateAnchorSpace();
    }


    /****************************************************************
     * Converts mouse coordinates into a map cell address.
     *
     * @param mousePoint  A Point indicating the current physical
     *                    location of the mouse cursor on screen.
     *
     * @return  The address of the map cell containing the mouse
     *           cursor.
     */
    public MHMapCellAddress mapMouse(MHVector mousePoint)
    {
        final MHMapCellAddress mapPoint = new MHMapCellAddress();
        final MHVector refPoint = plotTile(0,0);

        //convert from screen to world
        mousePoint = screenToWorld(mousePoint);

        // calculate reference point
        refPoint.translate(screenSpace.x, screenSpace.y);

        //subtract reference point
        mousePoint.translate( (int) -refPoint.getX(),
                              (int) -refPoint.getY() );

        mapPoint.column=(int)(mousePoint.getX()/mapData.getWidth());
        mapPoint.row=(int)(mousePoint.getY()/mapData.getHeight());

        //return map coordinate
        return(mapPoint);
    }


        /****************************************************************
         * Returns a reference to the MHMapGrid object serving as the map's
         * data structure.
         *
         * @return A reference to the map data object
         */
        public MHMapGrid getMapData()
        {
                return mapData;
        }


        /****************************************************************
         * Sets the tile width value
         * .
         * @param width  The value to use as the tile width.
         */
        public void setTileWidth(final int width)
        {
                tileWidth = width;
        }

        
        public void setMouseScroll(boolean mouseScroll)
        {
            this.mouseScroll  = mouseScroll;
        }
        

        public void update(long elapsedTime)
        {
            for (int row = 0; row < mapData.getHeight(); row++)
                for (int col = 0; col < mapData.getWidth(); col++)
//                    for (int layer = 0; layer < MHMapCell.NUM_LAYERS; layer++)
//                    {
//                        MHActor a = mapData.getMapCell(row, col).getLayer(layer);
//                        if (a != null)
//                        {
//                            a.advance();
//                        }
//                    }
            
                if (isScreenCoordinate(cursorPoint) && mouseScroll)
                {
                    final int scrollArea = 16;
                if (cursorPoint.getX() < screenSpace.x + scrollArea)
                {
                        scrollMap((int) (cursorPoint.getX() - (int)(screenSpace.x + scrollArea)), 0);
                }

                if (cursorPoint.getX() > screenSpace.width - scrollArea)
                {
                        scrollMap((int) (cursorPoint.getX() - (int)(screenSpace.width - scrollArea)), 0);
                }

                if (cursorPoint.getY() < screenSpace.y + scrollArea)
                {
                        scrollMap(0, (int) (cursorPoint.getY() - (int)(screenSpace.y + scrollArea)));
                }

                if (cursorPoint.getY() > screenSpace.height - scrollArea)
                {
                        scrollMap(0, (int) (cursorPoint.getY() - (int)(screenSpace.height - scrollArea)));
                }
                }
                
        }


        public void render(final MHGraphicsCanvas g)
        {
        }


        /****************************************************************
         * Performs validation on the cursor's map cell address to ensure
         * that it is within the bounds of the map.
         */
        protected void clipCursorAddress()
        {
        // clip cursor to tile map
        if (cursorAddress.column < 0)
            cursorAddress.column = 0;

        if (cursorAddress.row < 0)
            cursorAddress.row = 0;

        if (cursorAddress.column > mapData.getWidth() - 1)
            cursorAddress.column = mapData.getWidth() - 1;

        if (cursorAddress.row > mapData.getHeight() - 1)
            cursorAddress.row = mapData.getHeight() - 1;
        }


    public void mouseMoved(final MHMouseTouchEvent e)
    {
        // grab mouse coordinate
        cursorPoint = e.getPoint();
        //cursorPoint.x -= MHDisplayModeChooser.DISPLAY_X;
        //cursorPoint.y -= MHDisplayModeChooser.DISPLAY_Y;

        // map the mouse coordinates
        cursorAddress = mapMouse(cursorPoint);

        clipCursorAddress();

    }

    /****************************************************************
     * Calculates the next map position to which an actor would walk
     * if it were in position (<i>row</i>, <i>column</i>) and
     * traveled in the direction specified by <i>direction</i>.
     *
     * @param row
     *            The actor's current row position.
     * @param column
     *            The actor's current column position.
     * @param direction
     *            The direction in which to walk.
     *
     * @return A point indicating the actor's new column and row
     *         position after walking from its original position.
     */
    public MHMapCellAddress tileWalk(int row, int column,
            MHTileMapDirection direction)
    {
        MHMapCellAddress a = new MHMapCellAddress(row, column);
        
        switch (direction)
        {
            case NORTH:     a.row--;             break;
            case NORTHEAST: a.row--; a.column++; break;
            case EAST:               a.column++; break;
            case SOUTHEAST: a.row++; a.column++; break;
            case SOUTH:     a.row++;             break;
            case SOUTHWEST: a.row++; a.column--; break;
            case WEST:               a.column--; break;
            case NORTHWEST: a.row--; a.column--; break;
            case CENTER:                         break;
            default:
                break;
        }
        
        return a;
    }


    /****************************************************************
     * Calculates the next map position to which an actor would walk
     * if it were in the position specified by <i>origin</i> and
     * travelled in the direction specified by <i>direction</i>.
     *
     * @param origin
     *            The actor's current map cell position.
     * @param direction
     *            The direction in which to walk.
     *
     * @return A point indicating the actor's new column and row
     *         position after walking from its original position.
     */
    public MHMapCellAddress tileWalk(
            final MHMapCellAddress origin, final MHTileMapDirection direction)
    {
        return tileWalk(origin.row, origin.column, direction);
    }
        /**
         * Returns the screenSpace.
         * @return Rectangle2D
         */
        public MHRectangle getScreenSpace()
        {
                return screenSpace;
        }


        public MHMapCellAddress getCursorAddress()
        {
                return cursorAddress;
        }
        /**
         * Returns the screenAnchor.
         * @return Point
         */
        public MHVector getScreenAnchor()
        {
                return screenAnchor;
        }

        /**
         * Sets the screenAnchor.
         * @param screenAnchor The screenAnchor to set
         */
        public void setScreenAnchor(final MHVector screenAnchor)
        {
            this.screenAnchor = screenAnchor.clone();
        }


        /**
         * Sets the screen anchor.
         *
         * @param x The x coordinate of the screen anchor
         * @param y The y coordinate of the screen anchor
         */
        public void setScreenAnchor(final int x, final int y)
        {
                screenAnchor.setX(x);
                screenAnchor.setY(y);
        }

        /**
         * Returns the cursorPoint.
         * @return Point
         */
        public MHVector getCursorPoint()
        {
                return cursorPoint;
        }

        /**
         * Sets the cursorPoint.
         * @param cursorPoint The cursorPoint to set
         */
        public void setCursorPoint(final MHVector cursorPoint)
        {
                this.cursorPoint = cursorPoint;
        }

        
        public void render(MHGraphicsCanvas g, int x, int y)
        {
            // TODO Auto-generated method stub
            
        }

}


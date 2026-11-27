package gfc.widgets;

import gfc.microedition.lcdui.*;
import gfc.graphics.*;

import java.util.Vector;


//Horiz, Vert: createHorzTableLayout(), createVertTableLayout()
//TODO: padding

/**
 * 
 * class Layout
 * 
 * @author Alex Knorr
 * 
 * Created on 25.10.2004
 */
public final class Layout {

    /** Layout types */
    private static final int TABLE = 0;
    private static final int TABLE_HORZ = 1;
    private static final int TABLE_VERT = 2;
    private static final int STACK = 3;

    /** type of the Layout */
    private int type;

    /** global stack-layout class */
    private static Layout stackLayout = null;

    /** Layout parameters depending on <code>type</code> */
    private int param1;

    private int param2;

    /**
     * Various Layout creators
     */
    public static Layout createTableLayout(int columns, int rows) {
        return new Layout(TABLE, columns, rows);
    }

    public static Layout createHorzTableLayout(int columns) {
        return new Layout(TABLE_HORZ, columns, 0);
    }

    public static Layout createVertTableLayout(int rows) {
        return new Layout(TABLE_VERT, 0, rows);
    }

    public static Layout createStackLayout() {
        if (stackLayout == null) {
            stackLayout = new Layout(STACK, 0, 0);
        }

        return stackLayout;
    }

    /**
     * Construct a new Layout
     * 
     * @param type -
     *            type of Layout
     * @param param1 -
     *            meaning depends on <code>type</code>
     * @param param2 -
     *            meaning depends on <code>type</code>
     * @param param3 -
     *            meaning depends on <code>type</code>
     */
    private Layout(int type, int param1, int param2) {

        this.type = type;
        this.param1 = param1;
        this.param2 = param2;
    }

    /**
     * Return the Layout-Type
     */
    public int getType() {
        return type;
    }

    /**
     * Layout a list of Widgets. Widgets are positioned depending on the type of
     * this Layout.
     * 
     * @param widgets -
     *            list of widgets.
     */
    public void layoutWidgets(Vector widgets) {
        switch (type) {
        case TABLE:
        case TABLE_HORZ:
        case TABLE_VERT:
            tableLayout(widgets);
            break;
        case STACK:
            stackLayout(widgets);
            break;
        }
    }

    /**
     * Position Widgets in a Table.
     * <p>
     * Meaning of Layout parameters:
     * <p>
     * <code>param1</code>- max number of columns. <code>param2</code>-
     * max number of rows. <code>param3</code>- padding between table cells.
     */
    private void tableLayout(Vector widgets) {
        int nWidget = widgets.size();
        if (nWidget == 0) {
            return;
        }

        // number of columns and rows
        //   TABLE:      param1 columns, param2 rows, widgets fill row by row, extra widgets are not placed
        //   TABLE_HORZ: param1 columns, as many rows as needed, filled row by row
        //   TABLE_VERT: param2 rows, as many columns as needed, filled column by column
        int cols, rows;
        if (type == TABLE_VERT) {
            rows = Math.max(1, param2);
            cols = (nWidget + rows - 1) / rows;
        } else if (type == TABLE_HORZ) {
            cols = Math.max(1, param1);
            rows = (nWidget + cols - 1) / cols;
        } else {
            cols = Math.max(1, param1);
            rows = Math.max(1, param2);
        }

        int n = Math.min(nWidget, cols * rows);

        // width of each column, height of each row
        int[] colWidth = new int[cols];
        int[] rowHeight = new int[rows];
        for (int i = 0; i < n; i++) {
            Widget w = (Widget) widgets.elementAt(i);
            int col = column(i, cols, rows);
            int row = row(i, cols, rows);
            if (w.getWidth() > colWidth[col]) {
                colWidth[col] = w.getWidth();
            }
            if (w.getHeight() > rowHeight[row]) {
                rowHeight[row] = w.getHeight();
            }
        }

        // start positions of columns and rows
        int[] colX = new int[cols];
        for (int c = 1; c < cols; c++) {
            colX[c] = colX[c - 1] + colWidth[c - 1];
        }
        int[] rowY = new int[rows];
        for (int r = 1; r < rows; r++) {
            rowY[r] = rowY[r - 1] + rowHeight[r - 1];
        }

        // place every widget in its cell
        for (int i = 0; i < n; i++) {
            Widget w = (Widget) widgets.elementAt(i);
            int col = column(i, cols, rows);
            int row = row(i, cols, rows);
            alignWidgetToRect(w, colX[col], rowY[row], colWidth[col], rowHeight[row]);
        }
    }

    private int column(int i, int cols, int rows) {
        return (type == TABLE_VERT) ? (i / rows) : (i % cols);
    }

    private int row(int i, int cols, int rows) {
        return (type == TABLE_VERT) ? (i % rows) : (i / cols);
    }

    /**
     * Stack widgets one on another.
     * <p>
     * At first a rectangle of (0, 0, longest_widget_width,
     * highest_widget_height) is assumed. All widgets are then aligned to this
     * rectangle.
     */
    private void stackLayout(Vector widgets) {

        int xExtend = 0;
        int yExtend = 0;

        // find out horz. & vert. extension of the stack
        for (int i = widgets.size() - 1; i >= 0; i--) {

            Widget w = (Widget) widgets.elementAt(i);

            if (w.getWidth() > xExtend) {
                xExtend = w.getWidth();
            }
            if (w.getHeight() > yExtend) {
                yExtend = w.getHeight();
            }
        }

        // align all Widgets to rectangle (0, 0, xExtend, yExtend)
        for (int i = widgets.size() - 1; i >= 0; i--) {
            alignWidgetToRect((Widget) widgets.elementAt(i), 0, 0, xExtend,
                    yExtend);
        }
    }

    /**
     * Align a Widget to a rectangle.
     * 
     * @param w -
     *            Widget to align
     * @param x -
     *            x position of rectangle to align to
     * @param y -
     *            y position of rectangle to align to
     * @param width -
     *            width of rectangle to align to
     * @param height -
     *            height of rectangle to align to
     */
    private void alignWidgetToRect(Widget w, int x, int y, int width, int height) {

        int alignment = w.getAlignment();

        if ((alignment & Graphics.HCENTER) != 0) {
            x = x + (width / 2 - w.getWidth() / 2);
        } else if ((alignment & Graphics.RIGHT) != 0) {
            x = x + width - w.getWidth();
        }

        if ((alignment & Graphics.VCENTER) != 0) {
            y = y + (height / 2 - w.getHeight() / 2);
        } else if ((alignment & Graphics.BOTTOM) != 0) {
            y = y + height - w.getHeight();
        }

        w.setPosition(x, y);
    }
}
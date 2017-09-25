package com.harium.etyl.filechooser;

import com.harium.etyl.commons.layer.GeometricLayer;
import com.harium.etyl.core.graphics.Graphics;


public class Folder {

    FolderType type;
    String path;
    GeometricLayer layer;

    private static final int OFFSET_TEXT_X = 38;
    private static final int OFFSET_TEXT_Y = -16;

    public Folder() {

    }

    public Folder(String path, FolderType type, int x, int y, int w, int h) {
        this.path = path;
        this.type = type;
        this.layer = new GeometricLayer(x, y, w, h);
    }

    public Folder(String path, int x, int y, int w, int h) {
        this(path, FolderType.FOLDER, x, y, w, h);
    }

    public void draw(Graphics g, int offsetX, int offsetY) {
        g.drawString(path, offsetX + layer.getX() + OFFSET_TEXT_X, offsetY + layer.getY() + layer.getH() + OFFSET_TEXT_Y);
    }

    public boolean updateMouse(int mx, int my) {
        if (layer.colideRectPoint(mx, my)) {
            return true;
        }
        return false;
    }

    public FolderType getType() {
        return type;
    }
}

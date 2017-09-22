package com.harium.etyl.filechooser;


import com.harium.etyl.commons.event.KeyEvent;
import com.harium.etyl.commons.event.MouseEvent;
import com.harium.etyl.commons.event.PointerEvent;
import com.harium.etyl.commons.event.PointerState;
import com.harium.etyl.commons.graphics.Color;
import com.harium.etyl.core.graphics.Graphics;
import com.harium.etyl.layer.ImageLayer;
import com.harium.etyl.util.PathHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileChooser {

    private static final Color HARIUM_PRIMARY = new Color(0, 0x71, 0xBC);
    private static final Color HARIUM_GRAY = new Color(0xDC, 0xDC, 0xDC);

    private Color primaryColor = HARIUM_PRIMARY;
    private Color secondaryColor = HARIUM_GRAY;
    private Color backgroundColor = Color.WHITE;

    public static final String ERROR_LIST_FILES = "Can't list files, please check if you have such permissions";

    private String title = "Choose file";

    int w, h;
    int titleH = 44;
    int footerH = 44;

    // File Item h
    int fileItemHeight = 42;
    int dirH = fileItemHeight - 10;

    int px = 10;
    int py = 40;
    int rectWidth = 280;
    int rectHeight = 444;

    String dir = "";
    String currentDir = ".";
    String upperDir = "..";

    Folder onMouse = NULL_FOLDER;
    Folder selectedFile = NULL_FOLDER;

    List<Folder> folders = new ArrayList<>();
    Set<String> extensions = new HashSet<>();

    // File Chooser
    boolean chooseFile = true;

    // Control
    boolean visible = false;
    boolean clicked = false;
    boolean dragged = false;
    int ey = 0;
    int offsetY = 0;

    int visibleIndex = 0;
    int visibleFolders = 8;

    // Footer Buttons
    boolean overCancel = false;
    boolean overOk = false;

    private static final Folder NULL_FOLDER = new Folder();

    private ImageLayer folderIcon;
    private ImageLayer upIcon;

    String path;
    private ChooseFileListener listener;

    public FileChooser(int w, int h, String path) {
        this.path = path;
        this.w = w;
        this.h = h;
        init();
    }

    private void init() {
        folderIcon = new ImageLayer("ui/icon/mfolder.png");
        upIcon = new ImageLayer("ui/icon/mup.png");

        rectWidth = w / 3;
        updateRectWidth();

        // Check
        rectHeight = h - 60 - titleH - footerH;

        visibleFolders = (rectHeight / fileItemHeight) - 1; //Header and Sub-Header

        openFolder(path);
    }

    private void drawHeader(Graphics g) {
        //Draw Title Bar
        g.setColor(primaryColor);
        g.fillRect(px, py, rectWidth, titleH);
        g.setColor(backgroundColor);
        g.drawString(title, px, py, rectWidth, titleH);

        //Draw Current Dir
        g.setColor(secondaryColor);
        g.fillRect(px, py + titleH, rectWidth, dirH);
        g.setColor(backgroundColor);
        g.drawString(currentDir, px, py + titleH, rectWidth, dirH);
    }

    private void drawFooter(Graphics g) {
        //Draw Button Bar
        int fx = px;
        int fy = py + rectHeight;

        int hw = rectWidth / 2;

        drawButton(fx, fy, "Cancel", overCancel, g);
        drawButton(fx + hw, fy, "OK", overOk, g);

        g.setColor(HARIUM_GRAY);
        g.drawRect(fx, fy, hw, footerH);
        g.drawRect(fx + hw, fy, hw - 1, footerH);
    }

    private void drawButton(int bx, int by, String text, boolean active, Graphics g) {
        int hw = rectWidth / 2;

        Color background = backgroundColor, foreground = primaryColor;
        if (active) {
            background = primaryColor;
            foreground = backgroundColor;
        }

        g.setColor(background);
        g.fillRect(bx, by, hw, footerH);
        g.setColor(foreground);
        g.drawString(text, bx, by, hw, footerH);
    }

    private void openFolder(String path) {
        dir = path;
        if (dir.endsWith(File.separator)) {
            dir = dir.substring(0, dir.length() - 1);
        }

        currentDir = currentFolder(dir);
        upperDir = currentFolder(upperFolder(dir, currentDir));

        offsetY = 0;
        visibleIndex = 0;
        loadFolders(path);
    }

    private String upperFolder(String directory, String currentFolder) {
        return directory.substring(0, directory.length() - (currentFolder.length() + 1));
    }

    private String currentFolder(String directory) {
        String folder = directory;
        if (directory.endsWith(File.separator)) {
            folder = directory.substring(0, directory.length() - 1);
        }

        return folder.substring(folder.lastIndexOf(File.separator) + 1, folder.length());
    }

    private void loadFolders(String root) {
        folders.clear();

        int fy = py + titleH + dirH;

        // Add Up folder
        Folder upFolder = new Folder("Up to " + upperDir, FolderType.UP_FOLDER, px, fy, rectWidth, fileItemHeight);
        folders.add(upFolder);

        // Add the real folders
        File rootFolder = new File(root);
        File[] listOfFiles = rootFolder.listFiles();

        if (listOfFiles == null) {
            System.err.println(ERROR_LIST_FILES);
            return;
        }

        List<String> files = new ArrayList<>();
        List<String> sorted = new ArrayList<>();

        // Add folders
        for (File folder : listOfFiles) {
            if (folder.isDirectory()) {
                sorted.add(folder.getName());
            } else if (folder.isFile()) {
                if (!extensions.isEmpty()) {
                    String extension = PathHelper.getExtension(folder.getName());
                    if (!extensions.contains(extension)) {
                        continue;
                    }
                }
                files.add(folder.getName());
            }
        }

        // Sort Folders
        Collections.sort(sorted);
        Collections.sort(files);

        int i = 0;
        for (String f : sorted) {
            folders.add(new Folder(f, px, fy + fileItemHeight * (i + 1), rectWidth, fileItemHeight));
            i++;
        }

        for (String f : files) {
            folders.add(new Folder(f, FolderType.FILE, px, fy + fileItemHeight * (i + 1), rectWidth, fileItemHeight));
            i++;
        }
    }

    public void updateMouse(PointerEvent event) {
        int mx = event.getX();
        int my = event.getY();

        if (!visible || mx < px || mx > px + rectWidth) {
            return;
        }

        onMouse = NULL_FOLDER;

        if (my > py + rectHeight) {
            // verify footer
            if (mx < px + rectWidth / 2) {
                overCancel = true;
                overOk = false;
                if (event.isButtonDown(MouseEvent.MOUSE_BUTTON_LEFT)) {
                    closeDialog();
                }
            } else {
                overCancel = false;
                overOk = true;
                if (event.isButtonDown(MouseEvent.MOUSE_BUTTON_LEFT)) {
                    if (chooseFile) {
                        if (selectedFile != NULL_FOLDER) {
                            if (FolderType.FILE == selectedFile.type) {
                                openFile(dir + File.separator + selectedFile.path);
                            }
                        }
                    } else {
                        openFile(dir);
                    }
                }
            }
        } else {
            disableFooterButtons();

            int all = visibleFolders();
            for (int i = 0; i < all; i++) {
                Folder folder = folders.get(visibleIndex + i);
                if (folder.updateMouse(mx, my - offsetY)) {
                    onMouse = folder;
                }
            }

            if (event.isButtonDown(MouseEvent.MOUSE_BUTTON_LEFT)) {
                if (PointerState.DRAGGED == event.getState()) {
                    dragged = true;
                }

                if (folders.size() > visibleFolders) {
                    //7 => rectHeight / fileItemHeight
                    if (!clicked) {
                        clicked = true;
                        if (mx > px && mx < px + rectWidth &&
                                my > py && my < py + rectHeight) {
                            ey = my - offsetY;
                        }
                    } else {
                        // Change offset
                        changeOffset(event);
                    }
                }
            }

            if (event.isButtonUp(MouseEvent.MOUSE_BUTTON_LEFT)) {
                clicked = false;

                if (dragged) {
                    dragged = false;
                    return;
                }

                if (onMouse != NULL_FOLDER) {
                    if (FolderType.UP_FOLDER == onMouse.type) {
                        String path = upperFolder(dir, currentDir);
                        openFolder(path);
                    } else if (FolderType.FOLDER == onMouse.type) {
                        openFolder(dir + File.separator + onMouse.path);
                    } else if (FolderType.FILE == onMouse.type) {
                        selectedFile = onMouse;
                    }
                }
            }
        }
    }

    private void disableFooterButtons() {
        overCancel = false;
        overOk = false;
    }

    private void closeDialog() {
        visible = false;
        // Reset Path?
    }

    private void openFile(String path) {

        System.out.println("Open File: " + path);
        if (listener != null) {
            listener.onFileChoosed(path);
        }

        closeDialog();
    }

    private void changeOffset(PointerEvent event) {
        offsetY = event.getY() - ey;
        int maxOffset = (folders.size() - visibleFolders) * fileItemHeight;
        if (offsetY > 0) {
            offsetY = 0;
            ey = event.getY();
        } else if (offsetY < -maxOffset) {
            offsetY = -maxOffset;
            ey = event.getY() + maxOffset;
        }

        visibleIndex = -offsetY / fileItemHeight;
    }

    public void draw(Graphics g) {
        if (!visible) {
            return;
        }

        // Draw Dark Background
        drawDarkBackground(g);

        g.setColor(Color.WHITE);
        g.fillRect(px, py, rectWidth, rectHeight);

        drawFolders(g);
        drawHeader(g);
        drawFooter(g);
    }

    private void drawDarkBackground(Graphics g) {
        g.setAlpha(50);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        g.resetAlpha();
    }

    private void drawFolders(Graphics g) {
        int all = visibleFolders();

        for (int i = 0; i < all; i++) {
            Folder folder = folders.get(visibleIndex + i);

            int fx = folder.layer.getX();
            int fy = folder.layer.getY() + offsetY;

            g.setColor(HARIUM_GRAY);
            int lineY = fy + folder.layer.getH();
            g.drawLine(px, lineY, px + rectWidth - 1, lineY);

            g.setColor(HARIUM_PRIMARY);

            if (folder == onMouse || folder == selectedFile) {
                g.fillRect(fx, fy, folder.layer.getW(), folder.layer.getH());
                g.setColor(Color.WHITE);
                folder.draw(g, 0, offsetY);
                g.setColor(HARIUM_PRIMARY);
            } else {
                folder.draw(g, 0, offsetY);
            }

            //Draw Icon
            if (FolderType.FOLDER == folder.type) {
                folderIcon.simpleDraw(g, fx + 3, fy + 9);
            } else if (FolderType.UP_FOLDER == folder.type) {
                upIcon.simpleDraw(g, fx + 3, fy + 6);
            }
        }
    }

    private int visibleFolders() {
        int all = visibleFolders;
        if (visibleFolders > folders.size()) {
            all = folders.size();
        }
        return all;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = true;
    }

    private void updateRectWidth() {
        px = w / 2 - rectWidth / 2;
        for (Folder folder : folders) {
            folder.layer.setX(px);
            folder.layer.setW(rectWidth);
        }
    }

    private void updateRectHeight() {
        py = h / 2 - rectHeight / 2;
    }

    public void setListener(ChooseFileListener listener) {
        this.listener = listener;
    }

    public ChooseFileListener getListener() {
        return listener;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public void addExtension(String extension) {
        this.extensions.add(extension);
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void updateKeyboard(KeyEvent event) {
        if (event.isKeyDown(KeyEvent.VK_ESC)) {
            visible = false;
        }
    }

    public void setChooseFile(boolean chooseFile) {
        this.chooseFile = chooseFile;
    }

    public Color getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(Color primaryColor) {
        this.primaryColor = primaryColor;
    }

    public Color getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(Color secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getRectWidth() {
        return rectWidth;
    }

    public void setRectWidth(int pw) {
        this.rectWidth = pw;
        updateRectWidth();
    }

    public int getRectHeight() {
        return rectHeight;
    }

    public void setRectHeight(int rectHeight) {
        this.rectHeight = rectHeight;
        updateRectHeight();
    }

    public int getFileItemHeight() {
        return fileItemHeight;
    }

    public void setFileItemHeight(int fileItemHeight) {
        this.fileItemHeight = fileItemHeight;
    }
}

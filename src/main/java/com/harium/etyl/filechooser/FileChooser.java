package com.harium.etyl.filechooser;


import com.harium.etyl.commons.event.KeyEvent;
import com.harium.etyl.commons.event.MouseEvent;
import com.harium.etyl.commons.event.PointerEvent;
import com.harium.etyl.commons.event.PointerState;
import com.harium.etyl.commons.graphics.Color;
import com.harium.etyl.core.graphics.Graphics;
import com.harium.etyl.layer.ImageLayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileChooser {

    private static final Color HARIUM_PRIMARY = new Color(0, 0x71, 0xBC);
    private static final Color HARIUM_GRAY = new Color(0xDC, 0xDC, 0xDC);

    private String title = "Choose file";

    int w, h;
    int titleH = 36;
    int footerH = 36;

    int fileH = 42;
    int dirH = fileH - 10;

    int px = 10;
    int py = 40;
    int pw = 260;
    int ph = 444;

    String dir = "";
    String currentDir = ".";
    String upperDir = "..";

    Folder onMouse = NULL_FOLDER;
    Folder selectedFile = NULL_FOLDER;

    List<Folder> folders = new ArrayList<>();
    String extension = "";

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

        px = w / 2 - pw / 2;

        // Check
        ph = h - 100 - titleH - footerH;

        visibleFolders = ph / fileH - 1; //Header and Sub-Header

        openFolder(path);
    }

    private void drawHeader(Graphics g) {
        //Draw Title Bar
        g.setColor(HARIUM_PRIMARY);
        g.fillRect(px, py, pw, titleH);
        g.setColor(Color.WHITE);
        g.drawString(title, px, py, pw, titleH);

        //Draw Current Dir
        g.setColor(HARIUM_GRAY);
        g.fillRect(px, py + titleH, pw, dirH);
        g.setColor(Color.BLACK);
        g.drawString(currentDir, px, py + titleH, pw, dirH);
    }

    private void drawFooter(Graphics g) {
        //Draw Button Bar
        int fx = px;
        int fy = py + ph;

        int hw = pw / 2;

        drawButton(fx, fy, "Cancel", overCancel, g);
        drawButton(fx + hw, fy, "OK", overOk, g);

        g.setColor(HARIUM_GRAY);
        g.drawRect(fx, fy, hw, footerH);
        g.drawRect(fx + hw, fy, hw - 1, footerH);
    }

    private void drawButton(int bx, int by, String text, boolean active, Graphics g) {
        int hw = pw / 2;

        Color background = Color.WHITE, foreground = HARIUM_PRIMARY;
        if (active) {
            background = HARIUM_PRIMARY;
            foreground = Color.WHITE;
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
        Folder upFolder = new Folder("Up to " + upperDir, FolderType.UP_FOLDER, px, fy, pw, fileH);
        folders.add(upFolder);

        // Add the real folders
        File rootFolder = new File(root);
        File[] listOfFiles = rootFolder.listFiles();

        List<String> files = new ArrayList<>();
        List<String> sorted = new ArrayList<>();

        // Add folders
        for (File folder : listOfFiles) {
            if (folder.isDirectory()) {
                sorted.add(folder.getName());
            } else if (folder.isFile()) {
                if (!extension.isEmpty()) {
                    if (!folder.getName().endsWith(extension)) {
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
            folders.add(new Folder(f, px, fy + fileH * (i + 1), pw, fileH));
            i++;
        }

        for (String f : files) {
            folders.add(new Folder(f, FolderType.FILE, px, fy + fileH * (i + 1), pw, fileH));
            i++;
        }
    }

    public void updateMouse(PointerEvent event) {
        int mx = event.getX();
        int my = event.getY();

        if (!visible || mx < px || mx > px + pw) {
            return;
        }

        onMouse = NULL_FOLDER;

        if (my > py + ph) {
            // verify footer
            if (mx < px + pw / 2) {
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
                    //7 => ph / fileH
                    if (!clicked) {
                        clicked = true;
                        if (mx > px && mx < px + pw &&
                                my > py && my < py + ph) {
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
        int maxOffset = (folders.size() - visibleFolders) * fileH;
        if (offsetY > 0) {
            offsetY = 0;
            ey = event.getY();
        } else if (offsetY < -maxOffset) {
            offsetY = -maxOffset;
            ey = event.getY() + maxOffset;
        }

        visibleIndex = -offsetY / fileH;
    }

    public void draw(Graphics g) {
        if (!visible) {
            return;
        }

        // Draw Dark Background
        drawDarkBackground(g);

        g.setColor(Color.WHITE);
        g.fillRect(px, py, pw, ph);

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
            g.drawLine(px, lineY, px + pw - 1, lineY);

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

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
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
}

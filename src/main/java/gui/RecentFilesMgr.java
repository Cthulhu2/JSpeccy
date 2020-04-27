package gui;

import configuration.RecentFilesType;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;
import java.util.ArrayList;


/**
 * @author jsanchez
 */
public class RecentFilesMgr {

    private final JMenuItem[] itemList;
    private final ArrayList<File> filesList;

    RecentFilesMgr(RecentFilesType cfgFiles, JMenu menu) {
        itemList = new JMenuItem[menu.getItemCount()];
        filesList = new ArrayList<>();

        for (int idx = 0; idx < menu.getItemCount(); idx++) {
            itemList[idx] = menu.getItem(idx);
        }

        int idx = 0;
        for (String name : cfgFiles.getRecentFile()) {
            if (name == null || name.length() == 0) {
                continue;
            }

            File file = new File(name);
            if (file.exists()) {
                filesList.add(file);
                itemList[idx].setText(file.getName());
                itemList[idx].setToolTipText(file.getAbsolutePath());
                itemList[idx].setEnabled(true);
                idx++;
            }
        }
    }

    public File getRecentFile(int idx) {
        File fd = filesList.get(idx);

        if (idx > 0) {
            filesList.remove(idx);
            filesList.add(0, fd);
        }

        updateRecentMenu();
        return fd;
    }

    public String getAbsolutePath(int index) {
        return filesList.get(index).getAbsolutePath();
    }

    public void addRecentFile(File fd) {
        filesList.remove(fd);
        filesList.add(0, fd);

        if (filesList.size() > itemList.length) {
            filesList.remove(itemList.length);
        }

        updateRecentMenu();
    }

    public int size() {
        return filesList.size();
    }

    private void updateRecentMenu() {
        int idx = 0;

        for (File file : filesList) {
            itemList[idx].setText(file.getName());
            itemList[idx].setToolTipText(file.getAbsolutePath());
            itemList[idx].setEnabled(true);
            idx++;
        }
    }
}

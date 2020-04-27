package gui;

import configuration.RecentFilesType;
import org.junit.Test;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class RecentFilesMgrTest {

    @Test
    public void getRecentFile() {
        JMenu menu = new JMenu();
        JMenuItem mItem1 = new JMenuItem();
        JMenuItem mItem2 = new JMenuItem();
        menu.add(mItem1);
        menu.add(mItem2);

        RecentFilesMgr mgr = new RecentFilesMgr(new RecentFilesType(), menu);
        File file1 = new File("file1");
        File file2 = new File("file2");
        mgr.addRecentFile(file1);
        mgr.addRecentFile(file2);

        assertEquals(file2.getAbsolutePath(), mgr.getAbsolutePath(0));
        assertEquals(file1.getAbsolutePath(), mgr.getAbsolutePath(1));

        mgr.getRecentFile(1); //file1

        assertEquals(file1.getAbsolutePath(), mgr.getAbsolutePath(0));
        assertEquals(file2.getAbsolutePath(), mgr.getAbsolutePath(1));
    }

    @Test
    public void getAbsolutePath() {
        JMenu menu = new JMenu();
        JMenuItem mItem1 = new JMenuItem();
        JMenuItem mItem2 = new JMenuItem();
        menu.add(mItem1);
        menu.add(mItem2);

        RecentFilesMgr mgr = new RecentFilesMgr(new RecentFilesType(), menu);
        File file1 = new File("file1");
        mgr.addRecentFile(file1);

        assertEquals(file1.getAbsolutePath(), mgr.getAbsolutePath(0));
    }

    @Test
    public void addRecentFile() {
        JMenu menu = new JMenu();
        JMenuItem mItem1 = new JMenuItem();
        JMenuItem mItem2 = new JMenuItem();
        menu.add(mItem1);
        menu.add(mItem2);

        RecentFilesMgr mgr = new RecentFilesMgr(new RecentFilesType(), menu);
        mgr.addRecentFile(new File("file1"));
        File file2 = new File("file2");
        File file3 = new File("file3");
        mgr.addRecentFile(file2);
        mgr.addRecentFile(file3);

        assertEquals(file3.getAbsolutePath(), mgr.getAbsolutePath(0));
        assertEquals(file2.getAbsolutePath(), mgr.getAbsolutePath(1));
        assertEquals(mItem1.getText(), file3.getName());
        assertEquals(mItem2.getText(), file2.getName());
    }

    @Test
    public void addRecentFileSameName() {
        JMenu menu = new JMenu();
        JMenuItem mItem1 = new JMenuItem();
        JMenuItem mItem2 = new JMenuItem();
        JMenuItem mItem3 = new JMenuItem();
        menu.add(mItem1);
        menu.add(mItem2);
        menu.add(mItem3);

        RecentFilesMgr mgr = new RecentFilesMgr(new RecentFilesType(), menu);
        mgr.addRecentFile(new File("file1"));
        mgr.addRecentFile(new File("file2"));
        mgr.addRecentFile(new File("file1"));

        assertEquals(2, mgr.size());
        assertEquals(mItem1.getText(), "file1");
        assertEquals(mItem2.getText(), "file2");
        assertTrue(mItem3.getText().isEmpty());
    }

    @Test
    public void size() {
        JMenu menu = new JMenu();
        menu.add(new JMenuItem());
        menu.add(new JMenuItem());

        RecentFilesMgr mgr = new RecentFilesMgr(new RecentFilesType(), menu);
        assertEquals(0, mgr.size());

        mgr.addRecentFile(new File("file1"));
        assertEquals(1, mgr.size());

        mgr.addRecentFile(new File("file2"));
        assertEquals(2, mgr.size());

        mgr.addRecentFile(new File("file3"));
        assertEquals(2, mgr.size());
    }
}

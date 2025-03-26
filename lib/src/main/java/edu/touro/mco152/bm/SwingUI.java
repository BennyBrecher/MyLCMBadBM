package edu.touro.mco152.bm;

import edu.touro.mco152.bm.persist.DiskRun;
import edu.touro.mco152.bm.ui.Gui;

import javax.swing.*;

public class SwingUI implements GeneralUI{
    @Override
    public void updateLegend() {
        Gui.updateLegend();
    }

    @Override
    public void resetTestData() {
        Gui.resetTestData();
    }

    @Override
    public void addWriteMark(DiskMark mark) {
        Gui.addWriteMark(mark);
    }

    @Override
    public void addReadMark(DiskMark mark) {
        Gui.addReadMark(mark);
    }

    @Override
    public void updateTitle(String text) {
        Gui.chartPanel.getChart().getTitle().setVisible(true);
        Gui.chartPanel.getChart().getTitle().setText(text);
    }

    @Override
    public void adjustSensitivity() {
        Gui.mainFrame.adjustSensitivity();
    }

    @Override
    public void showPlainMessageDialog(String message, String title) {
        JOptionPane.showMessageDialog(Gui.mainFrame, message, title, JOptionPane.PLAIN_MESSAGE);
    }

    @Override
    public void showErrorMessageDialog(String message, String title){
        JOptionPane.showMessageDialog(Gui.mainFrame, message, title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void addRun(DiskRun run) {
        Gui.runPanel.addRun(run);
    }
}

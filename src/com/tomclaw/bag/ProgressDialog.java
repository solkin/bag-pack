package com.tomclaw.bag;

import javax.swing.*;

public class ProgressDialog extends JDialog {
    private JPanel contentPane;
    private JProgressBar progressBar1;
    private JButton buttonOK;

    public ProgressDialog() {
        setContentPane(contentPane);
        getRootPane().setDefaultButton(buttonOK);
    }

    public void setProgress(int percent) {
        progressBar1.setValue(percent);
    }
}

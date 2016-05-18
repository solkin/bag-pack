package com.tomclaw.bag;

import com.alee.laf.WebLookAndFeel;

import javax.swing.*;

public class Main {

    public static void main(final String[] args) {
        // You should work with UI (including installing L&F) inside Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater ( new Runnable ()
        {
            public void run ()
            {
                // Install WebLaF as application L&F
                WebLookAndFeel.install ();

                // Create you Swing application here
                MainForm.main(args);
            }
        } );
    }
}

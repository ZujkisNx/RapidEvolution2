package rapid_evolution.ui.main;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.JButton;
import rapid_evolution.ui.RapidEvolutionUI;
import rapid_evolution.audio.AudioEngine;
import rapid_evolution.audio.adddetectfilethread;
import rapid_evolution.audio.PitchShift;
import rapid_evolution.ui.REDialog;

import com.mixshare.rapid_evolution.ui.swing.button.REButton;

public class TimePitchShiftProgressUI extends REDialog implements ActionListener {
    public TimePitchShiftProgressUI(String id) {
      super(id);
        instance = this;
        setupDialog();
        setupActionListeners();
    }

    public static TimePitchShiftProgressUI instance = null;

    public JProgressBar progressbar2 = new JProgressBar(0, 100);
    public JButton cancelbutton = new REButton();

    private void setupDialog() {
    }


    private void setupActionListeners() {
        cancelbutton.addActionListener(this);
    }

    public PitchShift shifter;

    public void actionPerformed(ActionEvent ae) {
      if (ae.getSource() == cancelbutton) {
           setVisible(false);
           shifter.stopshifting = true;
      }
    }

    public void PostDisplay() {
      progressbar2.setValue(0);
    }
}
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package DSLM;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.CMMCore;
import org.micromanager.MMStudioMainFrame;
import org.micromanager.api.AcquisitionEngine;
import org.micromanager.api.ScriptInterface;
/**
 *
 * @author Gualda
 */


public class DSLMcontrolsPlugin implements org.micromanager.api.MMPlugin{

    public static String menuName = "OPEN SPIN MICROSCOPY CONTROL";
    
    private CMMCore core_;
    private MMStudioMainFrame gui_;
    private AcquisitionEngine acq_;
    private DSLM_controlsDialog dialog_;

    
    public String getDescription() {
        return "The DSLM Control plugin allows the user to control the DSLM microscope";
    }

    public String getInfo() {
        return null;
    }

    public String getVersion() {
        return "0.1";
    }

    public String getCopyright() {
        return "Instituto Gulbenkian de Ciência, Portugal, 2011. Author: Emilio J. Gualda";
    }
    
    public void configurationChanged() {
    }
   
    public void dispose() {
        if (dialog_ != null) {
            dialog_.setVisible(false);
            dialog_.dispose();
            dialog_ = null;
        }
    }
    
    public void setApp(ScriptInterface app) {
        gui_ = (MMStudioMainFrame) app;
        core_ = gui_.getMMCore();
        acq_=gui_.getAcquisitionEngine();
    }

    public void show() {
        if (dialog_ == null) {
            try {
                dialog_ = new DSLM_controlsDialog(this, core_,acq_,gui_);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(DSLMcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(DSLMcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
         dialog_.setVisible(true);
      } else {
         dialog_.setPlugin(this);
         dialog_.toFront();
        }

    }
   
}


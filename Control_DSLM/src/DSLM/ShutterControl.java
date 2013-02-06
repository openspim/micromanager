/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DSLM;

import java.util.logging.Level;
import java.util.logging.Logger;
import mmcorej.CMMCore;

/**
 *
 * @author pcadmin
 */
public class ShutterControl {
        private CMMCore core_;
        private String StateDev_;
        private String ShutterDev_;
            public ShutterControl(CMMCore core, String StateDev, String ShutterDev){
            core_ = core;
            StateDev_=StateDev;
            ShutterDev_=ShutterDev;
    }
    void openshutter(){
        try {
            core_.setProperty(StateDev_,"State","32");
            core_.setProperty(ShutterDev_,"OnOff","1");
            
        } catch (Exception ex) {
            Logger.getLogger(ShutterControl.class.getName()).log(Level.SEVERE, null, ex);
        }
			
            }
    
        void closeshutter(){
        try {
            core_.setProperty(StateDev_,"State","32");
            core_.setProperty(ShutterDev_,"OnOff","0");
            
        } catch (Exception ex) {
            Logger.getLogger(ShutterControl.class.getName()).log(Level.SEVERE, null, ex);
        }
			
            }
}

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
 * @author Gualda
 */
public class RotationControl {
    private CMMCore core_;
 
    public RotationControl(CMMCore core){
    core_ = core;
    }
    String leftMove(String Step, String PositionIni) {
       int aux=Integer.parseInt(Step);
       int aux2=Integer.parseInt(PositionIni);
       int STEP=aux/45;
       aux2=aux2+(STEP*45);
       if (aux2>=360){aux2=aux2-360;}
       String PositionFinal=Integer.toString(aux2);
       move(STEP, 20, 1);
       return PositionFinal;
    }
    
    String rightMove(String Step, String PositionIni) {
       int aux=Integer.parseInt(Step);
       int aux2=Integer.parseInt(PositionIni);
       int STEP=aux/45;
       aux2=aux2-(STEP*45);
       if (aux2<0){aux2=aux2+360;}
       String PositionFinal=Integer.toString(aux2);
       move(STEP, 20, 0);
       return PositionFinal;
    }
    
    void move(int nsteps, int sleeptime, int direction) {                   
           if (direction == 0){
            try {	
		for (int n = 0; n < nsteps; n++) {
                    
                        core_.setProperty("Arduino-Switch","State","1");                   
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","3"); //soma de bits antes e depois
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","2");
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","6"); //soma de bits antes e depois
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","4");
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","5"); //soma de bits antes e depois
                        core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","1");
			core_.setProperty("Arduino-Shutter","OnOff","1");

		}
                
            } catch (Exception ex) {
                        Logger.getLogger(DSLMcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                    }
    }
	
	else if (direction == 1) {
            try {
		for (int n = 0; n < nsteps; n++) {
			core_.setProperty("Arduino-Switch","State","1");
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","5"); //soma de bits antes e depois
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","4");
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","7"); //soma de bits antes e depois
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","2");
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
			core_.setProperty("Arduino-Switch","State","3"); //soma de bits antes e depois
			core_.setProperty("Arduino-Shutter","OnOff","1");
			core_.sleep(sleeptime);
                        core_.setProperty("Arduino-Switch","State","1"); 
			core_.setProperty("Arduino-Shutter","OnOff","1");
			
		}
            } catch (Exception ex) {
                        Logger.getLogger(DSLMcontrolsPlugin.class.getName()).log(Level.SEVERE, null, ex);
                    }
	}
}
   
}

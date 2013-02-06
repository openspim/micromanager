/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package DSLM;

import mmcorej.CMMCore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.*;
        
/**
 *
 * @author Gualda
 */
public class StackControl {
        private CMMCore core_;
 
    public StackControl(CMMCore core){
    core_ = core;
    }
    
    String getPos(){
        try {
           String StackPosition=core_.getProperty("ThorlabsDCStage2","Set position (microns)");
           return StackPosition;
           
        } catch (Exception ex) {
            Logger.getLogger(StackControl.class.getName()).log(Level.SEVERE, null, ex);
            return "Error in get position";
        }
    }
    
        String setPos(String Position){           
        try {
           core_.setProperty("ThorlabsDCStage2","Set position (microns)",Position);
           String StackPosition=core_.getProperty("ThorlabsDCStage2","Set position (microns)");
           return StackPosition;
           
        } catch (Exception ex) {
            Logger.getLogger(StackControl.class.getName()).log(Level.SEVERE, null, ex);
            return "Error in set position";
        }
    }
        
        String setPosUp(String OldPosition,String Step){
            NumberFormat formatter = new DecimalFormat("#,00000");                      
            double aux2=Double.parseDouble(OldPosition);
            double STEP=Double.parseDouble(Step);
            double NewPOS=aux2+STEP;//+STEP;
            String NewPosition = formatter.format(NewPOS);
           
        try {
           core_.setProperty("ThorlabsDCStage2","Set position (microns)",NewPosition);
           String StackPosition=core_.getProperty("ThorlabsDCStage2","Set position (microns)");
           return StackPosition;
           
        } catch (Exception ex) {
            Logger.getLogger(StackControl.class.getName()).log(Level.SEVERE, null, ex);
            return "Error UP";
        }
    }
        
        
       String setPosDown(String OldPosition,String Step){
            NumberFormat formatter = new DecimalFormat("#,00000");                      
            double aux2=Double.parseDouble(OldPosition);            
            double STEP=Double.parseDouble(Step);
            double NewPOS=aux2-STEP;//+STEP;
            String NewPosition = formatter.format(NewPOS);
           
        try {
           core_.setProperty("ThorlabsDCStage2","Set position (microns)",NewPosition);
           String StackPosition=core_.getProperty("ThorlabsDCStage2","Set position (microns)");
           return StackPosition;
           
        } catch (Exception ex) {
            Logger.getLogger(StackControl.class.getName()).log(Level.SEVERE, null, ex);
            return "Error DOWN";
        }
    }
    
}

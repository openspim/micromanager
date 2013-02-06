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
 * @author User
 */
public class FilterControl {
          private CMMCore core_;
          private String StateDev_FW_;
          private String ShutterDev_FW_;
 
    public FilterControl(CMMCore core, String StateDev_FW, String ShutterDev_FW){
    core_ = core;
    StateDev_FW_=StateDev_FW;
    ShutterDev_FW_=ShutterDev_FW;
    }
    
    String ChangeExcitationFilter(int Current_Item){
          int Move_Item=Current_Item;//-Global.Exci_Previous_Item;  
          //if (Move_Item<0) {Move_Item=Move_Item+6;}
          String CI=Integer.toString(Current_Item);        
          Global.Exci_Previous_Item=Current_Item;
          try{
            if (Move_Item==0) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","15");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
             if (Move_Item==1) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","17");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
              if (Move_Item==2) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","19");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
              if (Move_Item==3) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","21");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
                if (Move_Item==4) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","23");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }                    
                if (Move_Item==5) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","25");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");	
                       
            }   
 
           return CI;
      }
          
      catch (Exception ex) {
                        Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                        return("Error in Excitation Filter"+Integer.toString(Move_Item));
                    }
    }
       String ChangeEmissionFilter(int Current_Item){
          int Move_Item=Current_Item;//-Global.Emi_Previous_Item;  
          //if (Move_Item<0) {Move_Item=Move_Item+6;}
          String CI=Integer.toString(Current_Item);        
          //Global.Emi_Previous_Item=Current_Item;
          try{
            if (Move_Item==0) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","14");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
             if (Move_Item==1) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","32");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
              if (Move_Item==2) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","18");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
              if (Move_Item==3) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","20");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            }
            if (Move_Item==4) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","22");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            } 
            if (Move_Item==5) {
                        core_.setProperty(ShutterDev_FW_,"OnOff","0");
                        core_.setProperty(StateDev_FW_,"State","24");                   
                        core_.setProperty(ShutterDev_FW_,"OnOff","1");		
            } 

           return CI;
      }
          
      catch (Exception ex) {
                        Logger.getLogger(FilterControl.class.getName()).log(Level.SEVERE, null, ex);
                        return("Error in Excitation Filter"+Integer.toString(Move_Item));
                    }
    }
    
}

///////////////////////////////////////////////////////////////////////////////
// FILE:          OpenSPIM.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   The drivers required for the OpenSPIM project
//                
// AUTHOR:        Johannes Schindelin, 2011
//
// COPYRIGHT:     Johannes Schindelin, 2011
// LICENSE:       This file is distributed under the BSD license.
//                License text is included with the source distribution.
//
//                This file is distributed in the hope that it will be useful,
//                but WITHOUT ANY WARRANTY; without even the implied warranty
//                of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//
//                IN NO EVENT SHALL THE COPYRIGHT OWNER OR
//                CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
//                INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES.

#include "OpenSPIM.h"
#include "../../MMDevice/ModuleInterface.h"
#include "../../../3rdpartypublic/picard/PiUsb.h"

using namespace std;

// External names used used by the rest of the system
// to load particular device from the "OpenSPIM.dll" library
const char* g_TwisterDeviceName = "SPIMTwister";
const char* g_StageDeviceName = "SPIMZStage";
const char* g_XYStageDeviceName = "DXYStage";

// windows DLL entry code
#ifdef WIN32
BOOL APIENTRY DllMain( HANDLE /*hModule*/, 
                      DWORD  ul_reason_for_call, 
                      LPVOID /*lpReserved*/
                      )
{
   switch (ul_reason_for_call)
   {
   case DLL_PROCESS_ATTACH:
   case DLL_THREAD_ATTACH:
   case DLL_THREAD_DETACH:
   case DLL_PROCESS_DETACH:
      break;
   }
   return TRUE;
}
#endif





///////////////////////////////////////////////////////////////////////////////
// Exported MMDevice API
///////////////////////////////////////////////////////////////////////////////

/**
 * List all supported hardware devices here Do not discover devices at runtime.
 * To avoid warnings about missing DLLs, Micro-Manager maintains a list of
 * supported device (MMDeviceList.txt).  This list is generated using
 * information supplied by this function, so runtime discovery will create
 * problems.
 */
MODULE_API void InitializeModuleData()
{
   AddAvailableDeviceName(g_TwisterDeviceName, "Twister");
   AddAvailableDeviceName(g_StageDeviceName, "Z stage");
   AddAvailableDeviceName(g_XYStageDeviceName, "XY stage");

   if (DiscoverabilityTest())
   {
      SetDeviceIsDiscoverable(g_TwisterDeviceName, true); 
      SetDeviceIsDiscoverable(g_StageDeviceName, true); 
      SetDeviceIsDiscoverable(g_XYStageDeviceName, true);
   }
}

MODULE_API MM::Device* CreateDevice(const char* deviceName)
{
   if (deviceName == 0)
      return 0;

   // decide which device class to create based on the deviceName parameter
   if (strcmp(deviceName, g_TwisterDeviceName) == 0)
   {
      // create stage
      return new CSIABTwister();
   }
   else if (strcmp(deviceName, g_StageDeviceName) == 0)
   {
      // create stage
      return new CSIABStage();
   }
   else if (strcmp(deviceName, g_XYStageDeviceName) == 0)
   {
      // create stage
      return new CSIABXYStage();
   }

   // ...supplied name not recognized
   return 0;
}

MODULE_API void DeleteDevice(MM::Device* pDevice)
{
   delete pDevice;
}

// The twister

CSIABTwister::CSIABTwister()
{
}

CSIABTwister::~CSIABTwister()
{
}

bool CSIABTwister::Busy()
{
	return false;
}

double CSIABTwister::GetDelayMs() const
{
	return 0;
}

void CSIABTwister::SetDelayMs(double delay)
{
}

bool CSIABTwister::UsesDelay()
{
	return false;
}

int CSIABTwister::Initialize()
{
	int error = -1;
	handle_ = piConnectMotor(&error, serial_);
	return 0;
}

int CSIABTwister::Shutdown()
{
	return 0;
}

void CSIABTwister::GetName(char* name) const
{
}

int CSIABTwister::SetPositionUm(double pos)
{
	return 0;
}

int CSIABTwister::SetRelativePositionUm(double d)
{
	return 0;
}

int CSIABTwister::Move(double velocity)
{
	return 0;
}

int CSIABTwister::SetAdapterOriginUm(double d)
{
	return 0;
}

int CSIABTwister::GetPositionUm(double& pos)
{
	return 0;
}

int CSIABTwister::SetPositionSteps(long steps)
{
	return 0;
}

int CSIABTwister::GetPositionSteps(long& steps)
{
	return 0;
}

int CSIABTwister::SetOrigin()
{
	return 0;
}

int CSIABTwister::GetLimits(double& lower, double& upper)
{
	return 0;
}

int CSIABTwister::IsStageSequenceable(bool& isSequenceable) const
{
	return 0;
}

int CSIABTwister::GetStageSequenceMaxLength(long& nrEvents) const
{
	return 0;
}

int CSIABTwister::StartStageSequence() const
{
	return 0;
}

int CSIABTwister::StopStageSequence() const
{
	return 0;
}

int CSIABTwister::ClearStageSequence()
{
	return 0;
}

int CSIABTwister::AddToStageSequence(double position)
{
	return 0;
}

int CSIABTwister::SendStageSequence() const
{
	return 0;
}

bool CSIABTwister::IsContinuousFocusDrive() const
{
	return false;
}

// The Stage

CSIABStage::CSIABStage()
{
}
CSIABStage::~CSIABStage()
{
}

bool CSIABStage::Busy()
{
	return false;
}

double CSIABStage::GetDelayMs() const
{
	return 0;
}

void CSIABStage::SetDelayMs(double delay)
{
}

bool CSIABStage::UsesDelay()
{
	return false;
}

int CSIABStage::Initialize()
{
	return 0;
}

int CSIABStage::Shutdown()
{
	return 0;
}

void CSIABStage::GetName(char* name) const
{
}

int CSIABStage::SetPositionUm(double pos)
{
	return 0;
}

int CSIABStage::SetRelativePositionUm(double d)
{
	return 0;
}

int CSIABStage::Move(double velocity)
{
	return 0;
}

int CSIABStage::SetAdapterOriginUm(double d)
{
	return 0;
}

int CSIABStage::GetPositionUm(double& pos)
{
	return 0;
}

int CSIABStage::SetPositionSteps(long steps)
{
	return 0;
}

int CSIABStage::GetPositionSteps(long& steps)
{
	return 0;
}

int CSIABStage::SetOrigin()
{
	return 0;
}

int CSIABStage::GetLimits(double& lower, double& upper)
{
	return 0;
}

int CSIABStage::IsStageSequenceable(bool& isSequenceable) const
{
	return 0;
}

int CSIABStage::GetStageSequenceMaxLength(long& nrEvents) const
{
	return 0;
}

int CSIABStage::StartStageSequence() const
{
	return 0;
}

int CSIABStage::StopStageSequence() const
{
	return 0;
}

int CSIABStage::ClearStageSequence()
{
	return 0;
}

int CSIABStage::AddToStageSequence(double position)
{
	return 0;
}

int CSIABStage::SendStageSequence() const
{
	return 0;
}

bool CSIABStage::IsContinuousFocusDrive() const
{
	return false;
}

// The XY Stage

CSIABXYStage::CSIABXYStage()
{
}
CSIABXYStage::~CSIABXYStage()
{
}

bool CSIABXYStage::Busy()
{
	return false;
}

double CSIABXYStage::GetDelayMs() const
{
	return 0;
}

void CSIABXYStage::SetDelayMs(double delay)
{
}

bool CSIABXYStage::UsesDelay()
{
	return false;
}

int CSIABXYStage::Initialize()
{
	return 0;
}

int CSIABXYStage::Shutdown()
{
	return 0;
}

void CSIABXYStage::GetName(char* name) const
{
}

int CSIABXYStage::SetPositionUm(double x, double y)
{
	return 0;
}

int CSIABXYStage::SetRelativePositionUm(double dx, double dy)
{
	return 0;
}

int CSIABXYStage::SetAdapterOriginUm(double x, double y)
{
	return 0;
}

int CSIABXYStage::GetPositionUm(double& x, double& y)
{
	return 0;
}

int CSIABXYStage::GetLimitsUm(double& xMin, double& xMax, double& yMin, double& yMax)
{
	return 0;
}

int CSIABXYStage::Move(double vx, double vy)
{
	return 0;
}

int CSIABXYStage::SetPositionSteps(long x, long y)
{
	return 0;
}

int CSIABXYStage::GetPositionSteps(long& x, long& y)
{
	return 0;
}

int CSIABXYStage::SetRelativePositionSteps(long x, long y)
{
	return 0;
}

int CSIABXYStage::Home()
{
	return 0;
}

int CSIABXYStage::Stop()
{
	return 0;
}

int CSIABXYStage::SetOrigin()
{
	return 0;
}

int CSIABXYStage::GetStepLimits(long& xMin, long& xMax, long& yMin, long& yMax)
{
	return 0;
}

double CSIABXYStage::GetStepSizeXUm()
{
	return 0;
}

double CSIABXYStage::GetStepSizeYUm()
{
	return 0;
}

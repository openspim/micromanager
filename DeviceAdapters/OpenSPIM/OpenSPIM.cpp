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

#include <iostream>

#include "OpenSPIM.h"
#include "../../MMDevice/ModuleInterface.h"
#include "../../../3rdpartypublic/picard/PiUsb.h"

// We have a lot of stub implementations in here...
#pragma warning(disable: 4100)

using namespace std;

// External names used used by the rest of the system
// to load particular device from the "OpenSPIM.dll" library
const char* g_TwisterDeviceName = "Picard Twister";
const char* g_StageDeviceName = "Picard Z Stage";
const char* g_XYStageDeviceName = "Picard XY Stage";
const char* g_Keyword_SerialNumber = "Serial Number";
const char* g_Keyword_SerialNumberX = "Serial Number (X)";
const char* g_Keyword_SerialNumberY = "Serial Number (Y)";
const char* g_Keyword_MinX = "X-Min";
const char* g_Keyword_MaxX = "X-Max";
const char* g_Keyword_MinY = "Y-Min";
const char* g_Keyword_MaxY = "Y-Max";
const char* g_Keyword_Velocity = "Velocity";
const char* g_Keyword_VelocityX = "X-Velocity";
const char* g_Keyword_VelocityY = "Y-Velocity";

#define MAX_WAIT 0.3 // Maximum time to wait for the motors to begin motion, in seconds.

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

#define MAX_IDX 500

class CPiDetector
{
	public:
	CPiDetector()
	{
		std::cout << "Pinging motors..." << endl;

		m_pMotorList = new int[16];
		m_pTwisterList = new int[4];

		int error = PingDevices(&piConnectMotor, &piDisconnectMotor, m_pMotorList, 16, &m_iMotorCount);
		if(error > 1)
			std::cout << " Error detecting motors: " << error << endl;

		error = PingDevices(&piConnectTwister, &piDisconnectTwister, m_pTwisterList, 4, &m_iTwisterCount);
		if(error > 1)
			std::cout << " Error detecting twisters: " << error << endl;

		std::cout << "Found " << m_iMotorCount << " motors and " << m_iTwisterCount << " twisters." << endl;
	}

	int GetMotorSerial(int idx)
	{
		if(idx < m_iMotorCount)
			return m_pMotorList[idx];

		return -1;
	}

	int GetTwisterSerial(int idx)
	{
		if(idx < m_iTwisterCount)
			return m_pTwisterList[idx];

		return -1;
	}

	~CPiDetector()
	{
		delete[] m_pMotorList;
		delete[] m_pTwisterList;
	}

	private:
	int PingDevices(void* (__stdcall* connfn)(int*, int), void (__stdcall* discfn)(void*), int* pOutArray, const int iMax, int* pOutCount)
	{
		void* handle = NULL;
		int error = 0;
		int count = 0;
		for(int idx = 0; idx < MAX_IDX && count < iMax; ++idx)
		{
			if((handle = (*connfn)(&error, idx)) != NULL && error <= 1)
			{
				pOutArray[count++] = idx;
				(*discfn)(handle);
				handle = NULL;
			}
			else if(error > 1)
			{
				std::cout << "Error scanning index " << idx << ": " << error << "" << endl;
				*pOutCount = count;
				return error;
			}
		}

		*pOutCount = count;
		return 0;
	}

	int*	m_pMotorList;
	int		m_iMotorCount;

	int*	m_pTwisterList;
	int		m_iTwisterCount;
};

static CPiDetector* g_pPiDetector = new CPiDetector();

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
	if(g_pPiDetector != NULL)
		delete g_pPiDetector;

	g_pPiDetector = new CPiDetector();

   AddAvailableDeviceName(g_TwisterDeviceName, "Twister");
   AddAvailableDeviceName(g_StageDeviceName, "Z stage");
   AddAvailableDeviceName(g_XYStageDeviceName, "XY stage");
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
: serial_(g_pPiDetector->GetTwisterSerial(0)), handle_(NULL)
{
	char buf[16];
	itoa(serial_, buf, 10);

	CPropertyAction* pAct = new CPropertyAction (this, &CSIABTwister::OnSerialNumber);
	CreateProperty(g_Keyword_SerialNumber, buf, MM::String, false, pAct, true);
	SetErrorText(1, "Could not initialize twister");
}

CSIABTwister::~CSIABTwister()
{
}

int CSIABTwister::OnSerialNumber(MM::PropertyBase* pProp, MM::ActionType eAct)
{
   if (eAct == MM::BeforeGet)
   {
      // instead of relying on stored state we could actually query the device
      pProp->Set((long)serial_);
   }
   else if (eAct == MM::AfterSet)
   {
      long serial;
      pProp->Get(serial);
      serial_ = (int)serial;

	  return Initialize();
   }
   return DEVICE_OK;
}

bool CSIABTwister::Busy()
{
	BOOL moving;
	if (handle_ && !piGetTwisterMovingStatus(&moving, handle_))
		return moving != 0;
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
	handle_ = piConnectTwister(&error, serial_);
	if (handle_)
		piGetTwisterVelocity(&velocity_, handle_);
	else {
		std::ostringstream buffer;
		buffer << "Could not initialize twister " << serial_ << " (error code " << error << ")";
		LogMessage(buffer.str().c_str(), false);
	}
	return handle_ ? 0 : 1;
}

int CSIABTwister::Shutdown()
{
	if (handle_) {
		piDisconnectTwister(handle_);
		handle_ = NULL;
	}
	return 0;
}

void CSIABTwister::GetName(char* name) const
{
	CDeviceUtils::CopyLimitedString(name, g_TwisterDeviceName);
}

int CSIABTwister::SetPositionUm(double pos)
{
	int moveret = piRunTwisterToPosition((int)pos, velocity_, handle_);

	double at = 0;
	if(GetPositionUm(at))
		return DEVICE_ERR;

	if((int)at != (int)pos) {
		time_t start = time(NULL);
		while(!Busy() && (int)at != (int)pos && difftime(time(NULL),start) < MAX_WAIT) {
			CDeviceUtils::SleepMs(1);

			if(GetPositionUm(at) != DEVICE_OK)
				return DEVICE_ERR;
		};

		if(difftime(time(NULL),start) >= MAX_WAIT/2)
			LogMessage("Long wait (twister)...");
	};

	return moveret;
}

int CSIABTwister::Move(double velocity)
{
	velocity_ = (int)velocity;
	return DEVICE_ERR;
}

int CSIABTwister::SetAdapterOriginUm(double d)
{
	return DEVICE_ERR;
}

int CSIABTwister::GetPositionUm(double& pos)
{
	int position;
	if (piGetTwisterPosition(&position, handle_))
		return DEVICE_ERR;
	pos = position;
	return DEVICE_OK;
}

int CSIABTwister::SetPositionSteps(long steps)
{
	return DEVICE_ERR;
}

int CSIABTwister::GetPositionSteps(long& steps)
{
	return DEVICE_ERR;
}

int CSIABTwister::SetOrigin()
{
	return DEVICE_ERR;
}

int CSIABTwister::GetLimits(double& lower, double& upper)
{
	lower = -32767;
	upper = +32767;
	return 0;
}

int CSIABTwister::IsStageSequenceable(bool& isSequenceable) const
{
	isSequenceable = false;
	return DEVICE_OK;
}

int CSIABTwister::GetStageSequenceMaxLength(long& nrEvents) const
{
	nrEvents = 0;
	return DEVICE_OK;
}

int CSIABTwister::StartStageSequence() const
{
	return DEVICE_OK;
}

int CSIABTwister::StopStageSequence() const
{
	return DEVICE_OK;
}

int CSIABTwister::ClearStageSequence()
{
	return DEVICE_OK;
}

int CSIABTwister::AddToStageSequence(double position)
{
	return DEVICE_OK;
}

int CSIABTwister::SendStageSequence() const
{
	return DEVICE_OK;
}

bool CSIABTwister::IsContinuousFocusDrive() const
{
	return false;
}

// The Stage

CSIABStage::CSIABStage()
: serial_(g_pPiDetector->GetMotorSerial(2)), handle_(NULL)
{
	char buf[16];
	itoa(serial_, buf, 10);

	CPropertyAction* pAct = new CPropertyAction (this, &CSIABStage::OnSerialNumber);
	CreateProperty(g_Keyword_SerialNumber, buf, MM::Integer, false, pAct, true);

	pAct = new CPropertyAction (this, &CSIABStage::OnVelocity);
	CreateProperty(g_Keyword_Velocity, "10", MM::Integer, false, pAct);
	std::vector<std::string> allowed_velocities = std::vector<std::string>();
	char buffer[20];
	for (int i = 1; i <= 10; i++) {
		sprintf(buffer, "%d", i);
		allowed_velocities.push_back(buffer);
	}
	SetAllowedValues(g_Keyword_Velocity, allowed_velocities);

	SetErrorText(1, "Could not initialize motor (Z stage)");
}

CSIABStage::~CSIABStage()
{
}

int CSIABStage::OnSerialNumber(MM::PropertyBase* pProp, MM::ActionType eAct)
{
   if (eAct == MM::BeforeGet)
   {
      // instead of relying on stored state we could actually query the device
      pProp->Set((long)serial_);
   }
   else if (eAct == MM::AfterSet)
   {
      long serial;
      pProp->Get(serial);
      serial_ = (int)serial;

	  return Initialize();
   }
   return DEVICE_OK;
}

int CSIABStage::OnVelocity(MM::PropertyBase* pProp, MM::ActionType eAct)
{
   if (eAct == MM::BeforeGet)
   {
      // instead of relying on stored state we could actually query the device
      pProp->Set((long)velocity_);
   }
   else if (eAct == MM::AfterSet)
   {
      long velocity;
      pProp->Get(velocity);
      velocity_ = (int)velocity;
   }
   return DEVICE_OK;
}

bool CSIABStage::Busy()
{
	BOOL moving;
	if (handle_ && !piGetMotorMovingStatus(&moving, handle_))
		return moving != 0;
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
	int error = -1;
	handle_ = piConnectMotor(&error, serial_);
	if (handle_)
		piGetMotorVelocity(&velocity_, handle_);
	else {
		std::ostringstream buffer;
		buffer << "Could not initialize motor " << serial_ << " (error code " << error << ")";
		LogMessage(buffer.str().c_str(), false);
	}
	return handle_ ? 0 : 1;
}

int CSIABStage::Shutdown()
{
	if (handle_) {
		piDisconnectMotor(handle_);
		handle_ = NULL;
	}
	return 0;
}

void CSIABStage::GetName(char* name) const
{
	CDeviceUtils::CopyLimitedString(name, g_StageDeviceName);
}

int CSIABStage::SetPositionUm(double pos)
{
	int moveret = piRunMotorToPosition((int)pos, velocity_, handle_);

	double at = 0;
	if(GetPositionUm(at) != DEVICE_OK)
		return DEVICE_ERR;

	// WORKAROUND: piRunMotorToPosition doesn't wait for the motor to get
	// underway. Wait a bit here.
	if((int)at != (int)pos) {
		time_t start = time(NULL);
		while(!Busy() && (int)at != (int)pos && difftime(time(NULL),start) < MAX_WAIT) {
			CDeviceUtils::SleepMs(1);

			if(GetPositionUm(at) != DEVICE_OK)
				return DEVICE_ERR;
		};

		if(difftime(time(NULL),start) >= MAX_WAIT/2)
			LogMessage("Long wait...");
	};

	return moveret;
}

int CSIABStage::SetRelativePositionUm(double d)
{
	double position;
	int err = GetPositionUm(position);
	if(err != DEVICE_OK)
		return err;

	return SetPositionUm(position + d);
}

int CSIABStage::Move(double velocity)
{
	velocity_ = (int)velocity;
	return DEVICE_ERR;
}

int CSIABStage::SetAdapterOriginUm(double d)
{
	return DEVICE_ERR;
}

int CSIABStage::GetPositionUm(double& pos)
{
	int position;
	if (piGetMotorPosition(&position, handle_))
		return DEVICE_ERR;
	pos = position;
	return DEVICE_OK;
}

int CSIABStage::SetPositionSteps(long steps)
{
	return DEVICE_ERR;
}

int CSIABStage::GetPositionSteps(long& steps)
{
	return DEVICE_ERR;
}

int CSIABStage::SetOrigin()
{
	return DEVICE_ERR;
}

int CSIABStage::GetLimits(double& lower, double& upper)
{
	lower = 1;
	// TODO: make this a property; the USB Motor I has an upper limit of 2000
	upper = 8000;
	return 0;
}

int CSIABStage::IsStageSequenceable(bool& isSequenceable) const
{
	return false;
}

int CSIABStage::GetStageSequenceMaxLength(long& nrEvents) const
{
	nrEvents = 0;
	return DEVICE_OK;
}

int CSIABStage::StartStageSequence() const
{
	return DEVICE_OK;
}

int CSIABStage::StopStageSequence() const
{
	return DEVICE_OK;
}

int CSIABStage::ClearStageSequence()
{
	return DEVICE_OK;
}

int CSIABStage::AddToStageSequence(double position)
{
	return DEVICE_OK;
}

int CSIABStage::SendStageSequence() const
{
	return DEVICE_OK;
}

bool CSIABStage::IsContinuousFocusDrive() const
{
	return false;
}

// The XY Stage
enum XYSTAGE_ERRORS {
	XYERR_INIT_X = 1347,
	XYERR_INIT_Y,
	XYERR_MOVE_X,
	XYERR_MOVE_Y
};

CSIABXYStage::CSIABXYStage()
: serialX_(g_pPiDetector->GetMotorSerial(1)), serialY_(g_pPiDetector->GetMotorSerial(0)),
  handleX_(NULL), handleY_(NULL), minX_(1), minY_(1), maxX_(8000), maxY_(8000)
{
	char buf[16];
	itoa(serialX_, buf, 10);

	CPropertyAction* pActX = new CPropertyAction (this, &CSIABXYStage::OnSerialNumberX);
	CreateProperty(g_Keyword_SerialNumberX, buf, MM::Integer, false, pActX, true);

	itoa(serialY_, buf, 10);
	CPropertyAction* pActY = new CPropertyAction (this, &CSIABXYStage::OnSerialNumberY);
	CreateProperty(g_Keyword_SerialNumberY, buf, MM::Integer, false, pActY, true);

	SetErrorText(XYERR_INIT_X, "Could not initialize motor (X stage)");
	SetErrorText(XYERR_INIT_Y, "Could not initialize motor (Y stage)");
	SetErrorText(XYERR_MOVE_X, "X stage out of range.");
	SetErrorText(XYERR_MOVE_Y, "Y stage out of range.");

	CPropertyAction* pVelX = new CPropertyAction(this, &CSIABXYStage::OnVelocityX);
	CPropertyAction* pVelY = new CPropertyAction(this, &CSIABXYStage::OnVelocityY);
	CreateProperty(g_Keyword_VelocityX, "10", MM::Integer, false, pVelX, false);
	CreateProperty(g_Keyword_VelocityY, "10", MM::Integer, false, pVelY, false);

	char buffer[3];
	std::vector<std::string> allowed_values = std::vector<std::string>();
	for(int i=1; i <= 10; ++i) {
		sprintf(buffer, "%i", i);
		allowed_values.push_back(buffer);
	};
	SetAllowedValues(g_Keyword_VelocityX, allowed_values);
	SetAllowedValues(g_Keyword_VelocityY, allowed_values);

	CPropertyAction* pActMinX = new CPropertyAction(this, &CSIABXYStage::OnMinX);
	CPropertyAction* pActMaxX = new CPropertyAction(this, &CSIABXYStage::OnMaxX);
	CPropertyAction* pActMinY = new CPropertyAction(this, &CSIABXYStage::OnMinY);
	CPropertyAction* pActMaxY = new CPropertyAction(this, &CSIABXYStage::OnMaxY);
	CreateProperty(g_Keyword_MinX, "0", MM::Integer, false, pActMinX, true);
	CreateProperty(g_Keyword_MaxX, "8000", MM::Integer, false, pActMaxX, true);
	CreateProperty(g_Keyword_MinY, "0", MM::Integer, false, pActMinY, true);
	CreateProperty(g_Keyword_MaxY, "8000", MM::Integer, false, pActMaxY, true);
}

CSIABXYStage::~CSIABXYStage()
{
}

int CSIABXYStage::OnSerialNumberX(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		// instead of relying on stored state we could actually query the device
		pProp->Set((long)serialX_);
	}
	else if (eAct == MM::AfterSet)
	{
		long serial;
		pProp->Get(serial);
		serialX_ = (int)serial;

		int errorX = -1;
		handleX_ = piConnectMotor(&errorX, serialX_);
		if (handleX_)
		{
			piGetMotorVelocity(&velocityX_, handleX_);
		}
		else
		{
			std::ostringstream buffer;
			buffer << "Could not initialize X motor " << serialX_ << " (error code " << errorX << ")";
			LogMessage(buffer.str().c_str(), false);
			return XYERR_INIT_X;
		}
	}
	return DEVICE_OK;
}

int CSIABXYStage::OnSerialNumberY(MM::PropertyBase* pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
	{
		// instead of relying on stored state we could actually query the device
		pProp->Set((long)serialY_);
	}
	else if (eAct == MM::AfterSet)
	{
		long serial;
		pProp->Get(serial);
		serialY_ = (int)serial;

		int errorY = -1;
		handleY_ = piConnectMotor(&errorY, serialY_);
		if (handleY_)
		{
			piGetMotorVelocity(&velocityY_, handleY_);
		}
		else
		{
			std::ostringstream buffer;
			buffer << "Could not initialize Y motor " << serialY_ << " (error code " << errorY << ")";
			LogMessage(buffer.str().c_str(), false);
			return XYERR_INIT_Y;
		}
	}
	return DEVICE_OK;
}

int CSIABXYStage::OnMinX(MM::PropertyBase *pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
		pProp->Set((long)minX_);
	else if (eAct == MM::AfterSet)
		pProp->Get((long&)minX_);

	return DEVICE_OK;
}

int CSIABXYStage::OnMaxX(MM::PropertyBase *pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
		pProp->Set((long)maxX_);
	else if (eAct == MM::AfterSet)
		pProp->Get((long&)maxX_);

	return DEVICE_OK;
}

int CSIABXYStage::OnMinY(MM::PropertyBase *pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
		pProp->Set((long)minY_);
	else if (eAct == MM::AfterSet)
		pProp->Get((long&)minY_);

	return DEVICE_OK;
}

int CSIABXYStage::OnMaxY(MM::PropertyBase *pProp, MM::ActionType eAct)
{
	if (eAct == MM::BeforeGet)
		pProp->Set((long)maxY_);
	else if (eAct == MM::AfterSet)
		pProp->Get((long&)maxY_);

	return DEVICE_OK;
}

int CSIABXYStage::OnVelocityX(MM::PropertyBase *pProp, MM::ActionType eAct)
{
	if(handleX_ == NULL)
		return (eAct == MM::BeforeGet ? DEVICE_OK : DEVICE_ERR);

	int value = -1;
	if(eAct == MM::BeforeGet)
	{
		if(piGetMotorVelocity(&value, handleX_) != 0)
			return DEVICE_ERR;

		pProp->Set((long)value);
	}
	else if(eAct == MM::AfterSet)
	{
		pProp->Get((long&)value);

		return piSetMotorVelocity(value, handleX_);
	};

	return DEVICE_OK;
}

int CSIABXYStage::OnVelocityY(MM::PropertyBase *pProp, MM::ActionType eAct)
{
	if(handleY_ == NULL)
		return (eAct == MM::BeforeGet ? DEVICE_OK : DEVICE_ERR);

	int value = -1;
	if(eAct == MM::BeforeGet)
	{
		if(piGetMotorVelocity(&value, handleY_) != 0)
			return DEVICE_ERR;

		pProp->Set((long)value);
	}
	else if(eAct == MM::AfterSet)
	{
		pProp->Get((long&)value);

		return piSetMotorVelocity(value, handleY_);
	};

	return DEVICE_OK;
}

bool CSIABXYStage::Busy()
{
	BOOL movingX = FALSE, movingY = FALSE;
	if (handleX_)
		piGetMotorMovingStatus(&movingX, handleX_);
	if (handleY_)
		piGetMotorMovingStatus(&movingY, handleY_);
	return movingX != FALSE || movingY != FALSE;
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
	int errorX = -1, errorY = -1;
	handleX_ = piConnectMotor(&errorX, serialX_);
	if (handleX_)
		piGetMotorVelocity(&velocityX_, handleX_);
	else {
		std::ostringstream buffer;
		buffer << "Could not initialize X motor " << serialX_ << " (error code " << errorX << ")";
		LogMessage(buffer.str().c_str(), false);
	}
	handleY_ = piConnectMotor(&errorY, serialY_);
	if (handleY_)
		piGetMotorVelocity(&velocityY_, handleY_);
	else {
		std::ostringstream buffer;
		buffer << "Could not initialize Y motor " << serialY_ << " (error code " << errorY << ")";
		LogMessage(buffer.str().c_str(), false);
	}
	return handleX_ ? (handleY_ ? 0 : 2) : 1;
}

int CSIABXYStage::Shutdown()
{
	if (handleX_) {
		piDisconnectMotor(handleX_);
		handleX_ = NULL;
	}
	if (handleY_) {
		piDisconnectMotor(handleY_);
		handleY_ = NULL;
	}
	return 0;
}

void CSIABXYStage::GetName(char* name) const
{
	CDeviceUtils::CopyLimitedString(name, g_XYStageDeviceName);
}

void CSIABXYStage::GetOrientation(bool& mirrorX, bool& mirrorY)
{
	long x = 0, y = 0;
	assert(GetProperty(MM::g_Keyword_Transpose_MirrorX, x) == DEVICE_OK);
	assert(GetProperty(MM::g_Keyword_Transpose_MirrorY, y) == DEVICE_OK);

//	mirrorX = (x != 1);
//	mirrorY = (y != 1);
	mirrorX = false;
	mirrorY = false;
}

int CSIABXYStage::SetPositionUm(double x, double y)
{
	if(handleX_ == NULL || handleY_ == NULL)
		return DEVICE_ERR;

	bool flipX, flipY;
	GetOrientation(flipX, flipY);

	if(x < minX_ || x > maxX_)
		x = min(maxX_, max(x, minX_));
	if(y < minY_ || y > maxY_)
		y = min(maxY_, max(y, minY_));

	int toX = flipX ? (maxX_ - (int)x) + minX_ : (int)x;
	int toY = flipY ? (maxY_ - (int)y) + minY_ : (int)y;

	std::ostringstream str;
	str << "SetPositionUm(" << x << ", " << y << "): ";
	str << "sn X/Y={" << serialX_ << ", " << serialY_ << "}, ";
	str << "flip X/Y={" << (flipX ? "true" : "false") << ", " << (flipY ? "true" : "false") << "}, ";
	str << "min/max X=[" << minX_ << ", " << maxX_ << "], ";
	str << "min/max Y=[" << minY_ << ", " << maxY_ << "], ";
	str << "to X/Y={" << toX << ", " << toY << "}";
	LogMessage(str.str().c_str(), true);

	int moveX = piRunMotorToPosition(toX, velocityX_, handleX_);
	int moveY = piRunMotorToPosition(toY, velocityY_, handleY_) << 1;

	double atX, atY;

	if(int ret = GetPositionUm(atX, atY))
		return ret;

	if((int)atX != (int)x || (int)atY != (int)y) {
		time_t start = time(NULL);
		while(!Busy() && ((int)atX != (int)x || (int)atY != (int)y) && difftime(time(NULL),start) < MAX_WAIT) {
			CDeviceUtils::SleepMs(1);

			if(GetPositionUm(atX, atY) != DEVICE_OK)
				return DEVICE_ERR;
		};

		if(difftime(time(NULL),start) >= MAX_WAIT/2)
			LogMessage("Long wait (X/Y)...");
	};

	return moveX | moveY;
}

int CSIABXYStage::SetRelativePositionUm(double dx, double dy)
{
	double positionX, positionY;
	int err = GetPositionUm(positionX, positionY);
	if(err != DEVICE_OK)
		return err;

	return SetPositionUm(positionX + dx, positionY + dy);
}

int CSIABXYStage::SetAdapterOriginUm(double x, double y)
{
	return 0;
}

int CSIABXYStage::GetPositionUm(double& x, double& y)
{
	if(handleX_ == NULL || handleY_ == NULL)
		return DEVICE_ERR;

	bool flipX, flipY;
	GetOrientation(flipX, flipY);

	int positionX, positionY;
	if (piGetMotorPosition(&positionX, handleX_) ||
			piGetMotorPosition(&positionY, handleY_))
		return DEVICE_ERR;

	x = flipX ? (maxX_ - positionX) + minX_ : positionX;
	y = flipY ? (maxY_ - positionY) + minY_ : positionY;

	x = (x < minX_ ? minX_ : (x > maxX_ ? maxX_ : x));
	y = (y < minY_ ? minY_ : (y > maxY_ ? maxY_ : y));

	return DEVICE_OK;
}

int CSIABXYStage::GetLimitsUm(double& xMin, double& xMax, double& yMin, double& yMax)
{
	xMin = minX_;
	xMax = maxX_;
	yMin = minY_;
	yMax = maxY_;

	return 0;
}

int CSIABXYStage::Move(double vx, double vy)
{
	velocityX_ = (int)vx;
	velocityY_ = (int)vy;
	return 0;
}

int CSIABXYStage::SetPositionSteps(long x, long y)
{
	return DEVICE_ERR;
}

int CSIABXYStage::GetPositionSteps(long& x, long& y)
{
	return DEVICE_ERR;
}

int CSIABXYStage::SetRelativePositionSteps(long x, long y)
{
	return DEVICE_ERR;
}

int CSIABXYStage::Home()
{
	return DEVICE_ERR;
}

int CSIABXYStage::Stop()
{
	return DEVICE_ERR;
}

int CSIABXYStage::SetOrigin()
{
	return DEVICE_ERR;
}

int CSIABXYStage::GetStepLimits(long& xMin, long& xMax, long& yMin, long& yMax)
{
	return DEVICE_ERR;
}

double CSIABXYStage::GetStepSizeXUm()
{
	return DEVICE_ERR;
}

double CSIABXYStage::GetStepSizeYUm()
{
	return DEVICE_ERR;
}

int CSIABXYStage::IsXYStageSequenceable(bool& isSequenceable) const
{
	isSequenceable = false;
	return DEVICE_OK;
}

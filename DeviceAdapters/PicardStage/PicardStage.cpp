///////////////////////////////////////////////////////////////////////////////
// FILE:          PicardStage.cpp
// PROJECT:       Micro-Manager
// SUBSYSTEM:     DeviceAdapters
//-----------------------------------------------------------------------------
// DESCRIPTION:   The drivers required for the Picard Industries USB stages
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

#include "ModuleInterface.h"
#include "PiUsb.h"

#include "PicardStage.h"

// We have a lot of stub implementations in here...
#pragma warning(disable: 4100)

using namespace std;

// External names used used by the rest of the system
// to load particular device from the "PicardStage.dll" library
const char* g_TwisterDeviceName = "Picard Twister";
const char* g_StageDeviceName = "Picard Z Stage";
const char* g_XYStageDeviceName = "Picard XY Stage";
const char* g_XYAdapterDeviceName = "Picard XY Stage Adapter";
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
const char* g_Keyword_StepSize = "StepSize";
const char* g_Keyword_StepSizeX = "X-StepSize";
const char* g_Keyword_StepSizeY = "Y-StepSize";

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

#define MAX_IDX 250

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
   AddAvailableDeviceName(g_TwisterDeviceName, "Twister");
   AddAvailableDeviceName(g_StageDeviceName, "Z stage");
   AddAvailableDeviceName(g_XYStageDeviceName, "XY stage");
//   AddAvailableDeviceName(g_XYAdapterDeviceName, "XY stage adapter");
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
/*   else if (strcmp(deviceName, g_XYAdapterDeviceName) == 0)
   {
	   return new CPicardXYStageAdapter();
   };*/

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
	if(handle_ == NULL)
		return false;

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
	if(handle_ == NULL)
		return DEVICE_ERR;

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
	if(handle_ == NULL)
		return DEVICE_ERR;

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

	CreateProperty(g_Keyword_StepSize, "1.5", MM::Float, false);

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
	if(handle_ == NULL)
		return false;

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

double CSIABStage::GetStepSizeUm()
{
	double out = 0;

	if(GetProperty(g_Keyword_StepSize, out) != DEVICE_OK)
		return 0;

	return out;
}

int CSIABStage::SetPositionUm(double pos)
{
	if(handle_ == NULL)
		return DEVICE_ERR;

	int moveret = piRunMotorToPosition((int)(pos / GetStepSizeUm()), velocity_, handle_);

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
	if(handle_ == NULL)
		return DEVICE_ERR;

	int position;
	if (piGetMotorPosition(&position, handle_))
		return DEVICE_ERR;
	pos = position * GetStepSizeUm();
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

	CreateProperty(g_Keyword_StepSizeX, "1.5", MM::Float, false);
	CreateProperty(g_Keyword_StepSizeY, "1.5", MM::Float, false);
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

	int toX = (int)((flipX ? (maxX_ - x) + minX_ : x) / GetStepSizeXUm());
	int toY = (int)((flipY ? (maxY_ - y) + minY_ : y) / GetStepSizeYUm());

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

	x = (x < minX_ ? minX_ : (x > maxX_ ? maxX_ : x)) * GetStepSizeXUm();
	y = (y < minY_ ? minY_ : (y > maxY_ ? maxY_ : y)) * GetStepSizeYUm();

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
	double out = 0;
	if(GetProperty(g_Keyword_StepSizeX, out) != DEVICE_OK)
		return 0;
	return out;
}

double CSIABXYStage::GetStepSizeYUm()
{
	double out = 0;
	if(GetProperty(g_Keyword_StepSizeY, out) != DEVICE_OK)
		return 0;
	return out;
}

int CSIABXYStage::IsXYStageSequenceable(bool& isSequenceable) const
{
	isSequenceable = false;
	return DEVICE_OK;
}

#if 0
///////////////////////////////////////////////////////////////////////////////
// XY Metastage
// (L.S. early June 2013)
//
// The class to follow will hopefully, eventually, supersede CSIABXYStage. It
// functions as an X/Y stage by instantiating two CSIABStage objects and
// passing them every request it receives from MM. A few serious things to
// think about yet:
//
// - The whole concept is hackish.
// - It only works for our stages (dscho suggests instead getting the stage
//   objects as an init-time parameter)
// - Error text handling is dodgy, esp. if error numbers exceed 15 bits.
//   (bit 32 is set if the metastage had an internal error, so Y errors can't
//   be higher than 0x7FFF, though X stages have the full short to work with.)
//
// Basically, this is experimental; use at your own risk. It isn't yet on the
// OpenSPIM update site, and may never be.

CPicardXYStageAdapter::CPicardXYStageAdapter() : m_szLabel(NULL), m_szDesc(NULL), m_szParentId(NULL), m_hModule(NULL), m_szModName(NULL), m_pStageX(new CSIABStage()), m_pStageY(new CSIABStage()), m_pCallback(NULL)
{
};

CPicardXYStageAdapter::~CPicardXYStageAdapter()
{
	delete m_pStageX, m_pStageY;
	delete m_szLabel, m_szDesc, m_szParentId, m_szModName;
};

unsigned CPicardXYStageAdapter::GetNumberOfProperties() const
{
	return m_pStageX->GetNumberOfProperties() + m_pStageY->GetNumberOfProperties();
};

#define QUICKXY(name) ((name)[0] == 'X' ? m_pStageX : m_pStageY)

int CPicardXYStageAdapter::GetProperty(const char* name, char* value) const
{
	return QUICKXY(name)->GetProperty(name + 2, value);
};

int CPicardXYStageAdapter::SetProperty(const char* name, const char* value)
{
	return QUICKXY(name)->SetProperty(name + 2, value);
};

bool CPicardXYStageAdapter::HasProperty(const char* name) const
{
	return QUICKXY(name)->HasProperty(name + 2);
};

bool CPicardXYStageAdapter::GetPropertyName(unsigned idx, char* name) const
{
	bool x = idx < m_pStageX->GetNumberOfProperties();

	if(!x)
		idx -= m_pStageX->GetNumberOfProperties();

	if(!(x ? m_pStageX : m_pStageY)->GetPropertyName(idx, name))
		return false;

	// reverse memcpy to shift memory right by 2...
	for(int i = min(strlen(name), MM::MaxStrLength - 3); i >= 0; --i)
		name[i + 2] = name[i];

	// ...to make room for the axis tag.
	if(x)
	{
		name[0] = 'X';
		name[1] = '-';
		return true;
	}
	else
	{
		name[0] = 'Y';
		name[1] = '-';
		return true;
	};
};

int CPicardXYStageAdapter::GetPropertyReadOnly(const char* name, bool& readOnly) const
{
	return QUICKXY(name)->GetPropertyReadOnly(name + 2, readOnly);
};

int CPicardXYStageAdapter::GetPropertyInitStatus(const char* name, bool& preInit) const
{
	return QUICKXY(name)->GetPropertyInitStatus(name + 2, preInit);
};

int CPicardXYStageAdapter::HasPropertyLimits(const char* name, bool& hasLimits) const
{
	return QUICKXY(name)->HasPropertyLimits(name + 2, hasLimits);
};

int CPicardXYStageAdapter::GetPropertyLowerLimit(const char* name, double& lowLimit) const
{
	return QUICKXY(name)->GetPropertyLowerLimit(name + 2, lowLimit);
};

int CPicardXYStageAdapter::GetPropertyUpperLimit(const char* name, double& hiLimit) const
{
	return QUICKXY(name)->GetPropertyUpperLimit(name + 2, hiLimit);
};

int CPicardXYStageAdapter::GetPropertyType(const char* name, MM::PropertyType& pt) const
{
	return QUICKXY(name)->GetPropertyType(name + 2, pt);
};

unsigned CPicardXYStageAdapter::GetNumberOfPropertyValues(const char* propertyName) const
{
	return QUICKXY(propertyName)->GetNumberOfPropertyValues(propertyName + 2);
};

bool CPicardXYStageAdapter::GetPropertyValueAt(const char* propertyName, unsigned index, char* value) const
{
	return QUICKXY(propertyName)->GetPropertyValueAt(propertyName + 2, index, value);
};

int CPicardXYStageAdapter::IsPropertySequenceable(const char* name, bool& isSequenceable) const
{
	return QUICKXY(name)->IsPropertySequenceable(name + 2, isSequenceable);
};

int CPicardXYStageAdapter::GetPropertySequenceMaxLength(const char* propertyName, long& nrEvents) const
{
	return QUICKXY(propertyName)->GetPropertySequenceMaxLength(propertyName + 2, nrEvents);
};

int CPicardXYStageAdapter::StartPropertySequence(const char* propertyName)
{
	return QUICKXY(propertyName)->StartPropertySequence(propertyName + 2);
};

int CPicardXYStageAdapter::StopPropertySequence(const char* propertyName)
{
	return QUICKXY(propertyName)->StopPropertySequence(propertyName + 2);
};

int CPicardXYStageAdapter::ClearPropertySequence(const char* propertyName)
{
	return QUICKXY(propertyName)->ClearPropertySequence(propertyName + 2);
};

int CPicardXYStageAdapter::AddToPropertySequence(const char* propertyName, const char* value)
{
	return QUICKXY(propertyName)->AddToPropertySequence(propertyName + 2, value);
};

int CPicardXYStageAdapter::SendPropertySequence(const char* propertyName)
{
	return QUICKXY(propertyName)->SendPropertySequence(propertyName + 2);
};

#define XYERR_GENERIC 0x80000000;

bool CPicardXYStageAdapter::GetErrorText(int errorCode, char* errMessage) const
{
	if(errorCode & 0x80000000)
	{
		strcpy(errMessage, "Unknown metastage error occurred."); // might get their own values eventually
		return true;
	};

	int errx = errorCode & 0xFFFF;
	int erry = (errorCode >> 16) & 0x7FFF;
	char tmp[MM::MaxStrLength];

	string tmp2 = "X stage: ";
	m_pStageX->GetErrorText(errx, tmp); tmp2.append(tmp);
	tmp2.append("\nY stage: ");
	m_pStageY->GetErrorText(erry, tmp); tmp2.append(tmp);

	CDeviceUtils::CopyLimitedString(errMessage, tmp2.c_str());

	return true;
};

bool CPicardXYStageAdapter::Busy()
{
	return m_pStageX->Busy() || m_pStageY->Busy();
};

double CPicardXYStageAdapter::GetDelayMs() const
{
	return max(m_pStageX->GetDelayMs(), m_pStageY->GetDelayMs());
};

void CPicardXYStageAdapter::SetDelayMs(double delay)
{
	m_pStageX->SetDelayMs(delay);
	m_pStageY->SetDelayMs(delay);
};

bool CPicardXYStageAdapter::UsesDelay()
{
	return m_pStageX->UsesDelay() || m_pStageY->UsesDelay();
};

HDEVMODULE CPicardXYStageAdapter::GetModuleHandle() const
{
	return m_hModule;
};

void CPicardXYStageAdapter::SetModuleHandle(HDEVMODULE hLibraryHandle)
{
	m_hModule = hLibraryHandle;
};

void CPicardXYStageAdapter::SetLabel(const char* label)
{
	delete m_szLabel;
	m_szLabel = new char[strlen(label) + 1];
	strcpy(m_szLabel, label);
};

void CPicardXYStageAdapter::GetLabel(char* name) const
{
	CDeviceUtils::CopyLimitedString(name, m_szLabel == NULL ? "" : m_szLabel);
};

void CPicardXYStageAdapter::SetModuleName(const char* moduleName)
{
	delete m_szModName;
	m_szModName = new char[strlen(moduleName) + 1];
	strcpy(m_szModName, moduleName);
};

void CPicardXYStageAdapter::GetModuleName(char* moduleName) const
{
	CDeviceUtils::CopyLimitedString(moduleName, m_szModName == NULL ? "" : m_szModName);
};

void CPicardXYStageAdapter::SetDescription(const char* description)
{
	delete m_szDesc;
	m_szDesc = new char[strlen(description) + 1];
	strcpy(m_szDesc, description);
};

void CPicardXYStageAdapter::GetDescription(char* description) const
{
	CDeviceUtils::CopyLimitedString(description, m_szDesc == NULL ? "" : m_szDesc);
};

int CPicardXYStageAdapter::Initialize()
{
	return m_pStageX->Initialize() | (m_pStageY->Initialize() << 16);
};

int CPicardXYStageAdapter::Shutdown()
{
	return m_pStageX->Shutdown() | (m_pStageY->Shutdown() << 16);
};

void CPicardXYStageAdapter::GetName(char* name) const
{
	CDeviceUtils::CopyLimitedString(name, "PicardXYStageAdapter");
};

void CPicardXYStageAdapter::SetCallback(MM::Core* callback)
{
	m_pCallback = callback;
};

int CPicardXYStageAdapter::AcqBefore()
{
	return m_pStageX->AcqBefore() | (m_pStageX->AcqBefore() << 16);
};

int CPicardXYStageAdapter::AcqAfter()
{
	return m_pStageX->AcqAfter() | (m_pStageX->AcqAfter() << 16);
};

int CPicardXYStageAdapter::AcqBeforeFrame()
{
	return m_pStageX->AcqBeforeFrame() | (m_pStageX->AcqBeforeFrame() << 16);
};

int CPicardXYStageAdapter::AcqAfterFrame()
{
	return m_pStageX->AcqAfterFrame() | (m_pStageX->AcqAfterFrame() << 16);
};

int CPicardXYStageAdapter::AcqBeforeStack()
{
	return m_pStageX->AcqBeforeStack() | (m_pStageX->AcqBeforeStack() << 16);
};

int CPicardXYStageAdapter::AcqAfterStack()
{
	return m_pStageX->AcqAfterStack() | (m_pStageX->AcqAfterStack() << 16);
};

MM::DeviceDetectionStatus CPicardXYStageAdapter::DetectDevice(void)
{
	return (MM::DeviceDetectionStatus)(min(m_pStageX->DetectDevice(), m_pStageY->DetectDevice()));
};

void CPicardXYStageAdapter::SetParentID(const char* parentId)
{
	delete m_szParentId;
	m_szParentId = new char[strlen(parentId) + 1];
	strcpy(m_szParentId, parentId);
};

void CPicardXYStageAdapter::GetParentID(char* parentID) const
{
	CDeviceUtils::CopyLimitedString(parentID, m_szParentId == NULL ? "" : m_szParentId);
};

int CPicardXYStageAdapter::SetPositionUm(double x, double y)
{
	return m_pStageX->SetPositionUm(x) | (m_pStageY->SetPositionUm(y) << 16);
};

int CPicardXYStageAdapter::SetRelativePositionUm(double dx, double dy)
{
	return m_pStageX->SetRelativePositionUm(dx) | (m_pStageY->SetRelativePositionUm(dy) << 16);
};

int CPicardXYStageAdapter::SetAdapterOriginUm(double x, double y)
{
	return m_pStageX->SetAdapterOriginUm(x) | (m_pStageY->SetAdapterOriginUm(y) << 16);
};

int CPicardXYStageAdapter::GetPositionUm(double& x, double& y)
{
	return m_pStageX->GetPositionUm(x) | (m_pStageY->GetPositionUm(y) << 16);
};

int CPicardXYStageAdapter::GetLimitsUm(double& xMin, double& xMax, double& yMin, double& yMax)
{
	return m_pStageX->GetLimits(xMin, xMax) | (m_pStageY->GetLimits(yMin, yMax) << 16);
};

int CPicardXYStageAdapter::Move(double vx, double vy)
{
	return m_pStageX->Move(vx) | (m_pStageY->Move(vy) << 16);
};

int CPicardXYStageAdapter::SetPositionSteps(long x, long y)
{
	return m_pStageX->SetPositionSteps(x) | (m_pStageY->SetPositionSteps(y) << 16);
};

int CPicardXYStageAdapter::GetPositionSteps(long& x, long& y)
{
	return m_pStageX->GetPositionSteps(x) | (m_pStageY->GetPositionSteps(y) << 16);
};

int CPicardXYStageAdapter::SetRelativePositionSteps(long x, long y)
{
	long cx, cy;
	int r = m_pStageX->GetPositionSteps(cx) | (m_pStageY->GetPositionSteps(cy) << 16);

	if(r != 0)
		return r;

	return m_pStageX->SetPositionSteps(cx + x) | (m_pStageY->SetPositionSteps(cy + y) << 16);
};

int CPicardXYStageAdapter::Home()
{
	// Not sure...
	// This *might* be ->SetPositionUm(0) for both.
	return XYERR_GENERIC;
};

int CPicardXYStageAdapter::Stop()
{
	// This *might* be ->SetPositionUm(GetPositionUm) for both.
	return XYERR_GENERIC;
};

int CPicardXYStageAdapter::SetOrigin()
{
	return m_pStageX->SetOrigin() | (m_pStageY->SetOrigin() << 16);
};

int CPicardXYStageAdapter::GetStepLimits(long& xMin, long& xMax, long& yMin, long& yMax)
{
	return XYERR_GENERIC;
};

double CPicardXYStageAdapter::GetStepSizeXUm()
{
	return 1.0;
};

double CPicardXYStageAdapter::GetStepSizeYUm()
{
	return 1.0;
};

int CPicardXYStageAdapter::IsXYStageSequenceable(bool& isSequenceable) const
{
	int r = m_pStageX->IsStageSequenceable(isSequenceable);

	if(r == 0 && isSequenceable)
		r |= (m_pStageY->IsStageSequenceable(isSequenceable) << 16);

	return r;
};

int CPicardXYStageAdapter::GetXYStageSequenceMaxLength(long& nrEvents) const
{
	long tmp;
	int r = m_pStageX->GetStageSequenceMaxLength(tmp) | (m_pStageY->GetStageSequenceMaxLength(nrEvents) << 16);

	nrEvents = min(nrEvents, tmp);

	return r;
};

int CPicardXYStageAdapter::StartXYStageSequence()
{
	return m_pStageX->StartStageSequence() | (m_pStageY->StartStageSequence() << 16);
};

int CPicardXYStageAdapter::StopXYStageSequence()
{
	return m_pStageX->StopStageSequence() | (m_pStageY->StopStageSequence() << 16);
};

int CPicardXYStageAdapter::ClearXYStageSequence()
{
	return m_pStageX->ClearStageSequence() | (m_pStageY->ClearStageSequence() << 16);
};

int CPicardXYStageAdapter::AddToXYStageSequence(double positionX, double positionY)
{
	return m_pStageX->AddToStageSequence(positionX) | (m_pStageY->AddToStageSequence(positionY) << 16);
};

int CPicardXYStageAdapter::SendXYStageSequence()
{
	return m_pStageX->SendStageSequence() | (m_pStageY->SendStageSequence() << 16);
};
#endif

/*
 * Created on Apr 16, 2004
 * Created by Paul Gardner
 * Copyright (C) 2004 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SARL au capital de 30,000 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


#include "stdafx.h"
#include "aereg.h"
#include "stdio.h"
#include "stdlib.h"
#include "windows.h"
#include "shlwapi.h"
#include "process.h"


#include "org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface.h"


#define VERSION "1.4"


HMODULE	application_module;
bool	non_unicode			= false;

BOOL APIENTRY 
DllMain( 
	HANDLE hModule, 
    DWORD  ul_reason_for_call, 
    LPVOID lpReserved )
{
    switch (ul_reason_for_call)
	{
		case DLL_PROCESS_ATTACH:
		{
			OSVERSIONINFOA	osvi;

			application_module = (HMODULE)hModule;

			osvi.dwOSVersionInfoSize = sizeof(OSVERSIONINFOA);

			GetVersionExA(&osvi);

			non_unicode = ( osvi.dwPlatformId == VER_PLATFORM_WIN32_WINDOWS );

			break;
		}
		case DLL_THREAD_ATTACH:
		case DLL_THREAD_DETACH:
		case DLL_PROCESS_DETACH:
			break;
    }
    return TRUE;
}




CAereg::CAereg()
{ 
	return; 
}


void
throwException(
	JNIEnv*			env,
	char*			operation,
	char*			message )
{
	jclass except = env->FindClass( "org/gudy/azureus2/platform/win32/access/impl/AEWin32AccessExceptionImpl" );
	
	bool	ok = false;

	if ( except != NULL ){

		jmethodID method = env->GetMethodID( except, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V" );
	
		if ( method != NULL ){

	
			jobject new_object =
					env->NewObject( except, 
									method, 
									env->NewStringUTF((const char*)operation), 
									env->NewStringUTF((const char*)message ));

			if ( new_object != NULL ){

				env->Throw( (jthrowable)new_object );

				ok = true;
			}
		}
	}

	if ( !ok ){

		fprintf( stderr, "AEWin32AccessInterface: failed to throw exception %s: %s\n", operation, message );
	}
}

void
throwException(
	JNIEnv*			env,
	char*			operation,
	char*			message,
	int				error_code )
{
	char	buffer[4096];

	sprintf( buffer, "%s (error_code=%d)", message, error_code );

	throwException( env, operation, buffer );
}

HKEY
mapHKEY(
	JNIEnv*		env,
	jint		_type )
{
	if ( _type == org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_HKEY_CLASSES_ROOT ){

		return( HKEY_CLASSES_ROOT );

	}else if ( _type == org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_HKEY_CURRENT_CONFIG ){

		return( HKEY_CURRENT_CONFIG );

	}else if ( _type == org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_HKEY_LOCAL_MACHINE ){

		return( HKEY_LOCAL_MACHINE );

	}else if ( _type == org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_HKEY_CURRENT_USER ){

		return( HKEY_CURRENT_USER );

	}else{

		throwException( env, "readValue", "unsupported type" );

		return( NULL );
	}
}


// ******************************

bool
jstringToCharsW(
	JNIEnv		*env,
	jstring		jstr,
	WCHAR		*chars,
	int			chars_len )
{
	if ( jstr == NULL ){

		chars[0] = 0;

	}else{

		int	jdata_len = env->GetStringLength(jstr);

		if ( jdata_len >= chars_len ){

			throwException( env, "jstringToChars", "jstring truncation occurred" );

			return( false );
		}

		const jchar *jdata = env->GetStringChars( jstr, NULL );

		for (int i=0;i<jdata_len;i++){

			chars[i] = (char)jdata[i];
		}

		chars[jdata_len]=0;

		env->ReleaseStringChars( jstr, jdata );
	}

	return( true );
}

// UNICODE METHODS

JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getModuleNameW(
	JNIEnv		*env,
	jclass		cla )
{
	WCHAR	buffer[2048];

	if ( !GetModuleFileName(application_module, buffer, sizeof( buffer ))){


		throwException( env, "getModuleName", "GetModuleFileName fails" );

		return( NULL );
	}

	return( env->NewString(buffer, wcslen(buffer)));
}



JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getVersionW(
	JNIEnv		*env,
	jclass		cla )
{
	jstring	result = env->NewStringUTF((char *)VERSION);

	return( result );
}


JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readStringValueW(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	HKEY		key;
	HKEY		subkey;
	WCHAR		subkey_name[1024];
	WCHAR		value_name[1024];

	jstring		result	= NULL;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return( NULL );
	}

	if ( !jstringToCharsW( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return( NULL );
	}

	if ( !jstringToCharsW( env, _value_name, value_name, sizeof( value_name ))){

		return( NULL );
	}

	if ( RegOpenKey( key, subkey_name, &subkey ) == ERROR_SUCCESS ){

		BYTE	value[1024];
		DWORD	value_length	= sizeof( value );
		DWORD	type;

		if ( RegQueryValueEx( subkey, value_name, NULL, &type, (unsigned char*)value, &value_length ) == ERROR_SUCCESS){

			if ( type == REG_SZ || type == REG_EXPAND_SZ || type == REG_MULTI_SZ ){

				if ( type == REG_EXPAND_SZ ){

					WCHAR	expanded_value[2048];

					ExpandEnvironmentStrings((const WCHAR*)value, expanded_value, sizeof( expanded_value ));
			
					result = env->NewString(expanded_value,wcslen(expanded_value));

				}else{


					result = env->NewString((const WCHAR*)value,wcslen((WCHAR *)value));
				}			

			}else{

				throwException( env, "readValue", "type mismach" );
			}
		}else{

			throwException( env, "readStringValue", "RegQueryValueEx failed" );
		}

		RegCloseKey(subkey);

	}else{

		throwException( env, "readStringValue", "RegOpenKey failed" );
	}

	return( result );
}

JNIEXPORT jint JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readWordValueW(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	HKEY		key;
	HKEY		subkey;
	WCHAR		subkey_name[1024];
	WCHAR		value_name[1024];

	jint		result	= 0;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return( NULL );
	}

	if ( !jstringToCharsW( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return( NULL );
	}

	if ( !jstringToCharsW( env, _value_name, value_name, sizeof( value_name ))){

		return( NULL );
	}

	if ( RegOpenKey( key, subkey_name, &subkey ) == ERROR_SUCCESS ){

		BYTE	value[1024];
		DWORD	value_length	= sizeof( value );
		DWORD	type;

		if ( RegQueryValueEx( subkey, value_name, NULL, &type, (unsigned char*)value, &value_length ) == ERROR_SUCCESS){

			if ( type == REG_DWORD ){

				result = (LONG)value[0];

			}else{

				throwException( env, "readValue", "type mismach" );
			}
		}else{

			throwException( env, "readStringValue", "RegQueryValueEx failed" );
		}

		RegCloseKey(subkey);

	}else{

		throwException( env, "readStringValue", "RegOpenKey failed" );
	}

	return(result);
}


JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_writeStringValueW(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name,
	jstring		_value_value )
{
	HKEY		key;
	HKEY		subkey;
	WCHAR		subkey_name[1024];
	WCHAR		value_name[1024];
	WCHAR		value_value[1024];

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return;
	}

	if ( !jstringToCharsW( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return;
	}

	if ( !jstringToCharsW( env, _value_name, value_name, sizeof( value_name ))){

		return;
	}


	if ( !jstringToCharsW( env, _value_value, value_value, sizeof( value_value ))){

		return;
	}


	if ( RegCreateKeyEx( key, subkey_name, 0, REG_NONE, 0, KEY_ALL_ACCESS, NULL, &subkey, NULL ) == ERROR_SUCCESS ){


		if ( RegSetValueEx( subkey, value_name, 0, REG_SZ, (const BYTE*)value_value, (wcslen(value_value)+1)*sizeof(WCHAR)) == ERROR_SUCCESS){

		}else{

			throwException( env, "writeStringValue", "RegSetValueEx failed" );
		}

		RegCloseKey(subkey);

	}else{

		throwException( env, "writeStringValue", "RegCreateKeyEx failed" );
	}

}


JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteKeyW(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jboolean	_recursive )
{
	HKEY		key;
	HKEY		subkey;
	WCHAR		subkey_name[1024];

	jstring		result	= NULL;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return;
	}

	if ( !jstringToCharsW( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return;
	}

	if ( RegOpenKey( key, subkey_name, &subkey ) == ERROR_SUCCESS ){


		RegCloseKey(subkey);

		if ( _recursive ){

			if ( SHDeleteKey( key, subkey_name ) != ERROR_SUCCESS ){

				throwException( env, "deleteKey", "SHDeleteKey failed" );
			}
		}else{

			if ( RegDeleteKey( key, subkey_name ) != ERROR_SUCCESS ){

				throwException( env, "deleteKey", "RegDeleteKey failed" );
			}
		}
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteValueW(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	HKEY		key;
	HKEY		subkey;
	WCHAR		subkey_name[1024];
	WCHAR		value_name[1024];

	jstring		result	= NULL;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return;
	}

	if ( !jstringToCharsW( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return;
	}

	if ( !jstringToCharsW( env, _value_name, value_name, sizeof( value_name ))){

		return;
	}

	if ( RegOpenKey( key, subkey_name, &subkey ) == ERROR_SUCCESS ){


		RegCloseKey(subkey);

		if ( SHDeleteValue( key, subkey_name, value_name ) != ERROR_SUCCESS ){

			throwException( env, "deleteValue", "SHDeleteValue failed" );
		}
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_createProcessW(
	JNIEnv*		env,
	jclass		cla, 
	jstring		_command_line, 
	jboolean	_inherit_handles )
{
	WCHAR		command_line[16000];

	STARTUPINFO				start_info;
	PROCESS_INFORMATION		proc_info;

	if ( !jstringToCharsW( env, _command_line, command_line, sizeof( command_line ))){

		return;
	}

	memset( &start_info, 0, sizeof( STARTUPINFO ));

	start_info.cb = sizeof( STARTUPINFO );

	if ( CreateProcess(
			NULL,				// LPCTSTR lpApplicationName,
			command_line,		// LPTSTR lpCommandLine,
			NULL,				// LPSECURITY_ATTRIBUTES lpProcessAttributes,
			NULL,				// LPSECURITY_ATTRIBUTES lpThreadAttributes,
			_inherit_handles,	// BOOL bInheritHandles,
			DETACHED_PROCESS,	// DWORD dwCreationFlags,
			NULL,				// LPVOID lpEnvironment,
			NULL,				// LPCTSTR lpCurrentDirectory,
			&start_info,		// LPSTARTUPINFO lpStartupInfo,
			&proc_info )){		// LPPROCESS_INFORMATION lpProcessInformation


		CloseHandle( proc_info.hThread );
        CloseHandle( proc_info.hProcess );

	}else{

		throwException( env, "createProcess", "CreateProcess failed" );
	}
};



// NON-UNICODE VARIANT FOR WIN95,98,ME




bool
jstringToCharsA(
	JNIEnv		*env,
	jstring		jstr,
	char		*chars,
	int			chars_len )
{
	if ( jstr == NULL ){

		chars[0] = 0;

	}else{

		int	jdata_len = env->GetStringLength(jstr);

		if ( jdata_len >= chars_len ){

			throwException( env, "jstringToChars", "jstring truncation occurred" );

			return( false );
		}

		const jchar *jdata = env->GetStringChars( jstr, NULL );

		for (int i=0;i<jdata_len;i++){

			chars[i] = (char)jdata[i];
		}

		chars[jdata_len]=0;

		env->ReleaseStringChars( jstr, jdata );
	}

	return( true );
}

JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getModuleNameA(
	JNIEnv		*env,
	jclass		cla )
{
	char	buffer[2048];

	if ( !GetModuleFileNameA(application_module, buffer, sizeof( buffer ))){


		throwException( env, "getModuleName", "GetModuleFileName fails" );

		return( NULL );
	}

	return( env->NewStringUTF((char *)buffer));
}



JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getVersionA(
	JNIEnv		*env,
	jclass		cla )
{
	jstring	result = env->NewStringUTF((char *)VERSION);

	return( result );
}


JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readStringValueA(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	HKEY		key;
	HKEY		subkey;
	char		subkey_name[1024];
	char		value_name[1024];

	jstring		result	= NULL;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return( NULL );
	}

	if ( !jstringToCharsA( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return( NULL );
	}

	if ( !jstringToCharsA( env, _value_name, value_name, sizeof( value_name ))){

		return( NULL );
	}

	if ( RegOpenKeyA( key, subkey_name, &subkey ) == ERROR_SUCCESS ){

		BYTE	value[1024];
		DWORD	value_length	= sizeof( value );
		DWORD	type;

		if ( RegQueryValueExA( subkey, value_name, NULL, &type, (unsigned char*)value, &value_length ) == ERROR_SUCCESS){

			if ( type == REG_SZ || type == REG_EXPAND_SZ || type == REG_MULTI_SZ ){

				if ( type == REG_EXPAND_SZ ){

					char	expanded_value[2048];

					ExpandEnvironmentStringsA((const char*)value, expanded_value, sizeof( expanded_value ));
			
					result = env->NewStringUTF((char *)expanded_value);

				}else{


					result = env->NewStringUTF((char *)value);
				}			

			}else{

				throwException( env, "readValue", "type mismach" );
			}
		}else{

			throwException( env, "readStringValue", "RegQueryValueEx failed" );
		}

		RegCloseKey(subkey);

	}else{

		throwException( env, "readStringValue", "RegOpenKey failed" );
	}

	return( result );
}

JNIEXPORT jint JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readWordValueA(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	HKEY		key;
	HKEY		subkey;
	char		subkey_name[1024];
	char		value_name[1024];

	jint		result	= 0;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return( NULL );
	}

	if ( !jstringToCharsA( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return( NULL );
	}

	if ( !jstringToCharsA( env, _value_name, value_name, sizeof( value_name ))){

		return( NULL );
	}

	if ( RegOpenKeyA( key, subkey_name, &subkey ) == ERROR_SUCCESS ){

		BYTE	value[1024];
		DWORD	value_length	= sizeof( value );
		DWORD	type;

		if ( RegQueryValueExA( subkey, value_name, NULL, &type, (unsigned char*)value, &value_length ) == ERROR_SUCCESS){

			if ( type == REG_DWORD ){

				result = (LONG)value[0];

			}else{

				throwException( env, "readValue", "type mismach" );
			}
		}else{

			throwException( env, "readStringValue", "RegQueryValueEx failed" );
		}

		RegCloseKey(subkey);

	}else{

		throwException( env, "readStringValue", "RegOpenKey failed" );
	}

	return(result);
}


JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_writeStringValueA(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name,
	jstring		_value_value )
{
	HKEY		key;
	HKEY		subkey;
	char		subkey_name[1024];
	char		value_name[1024];
	char		value_value[1024];

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return;
	}

	if ( !jstringToCharsA( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return;
	}

	if ( !jstringToCharsA( env, _value_name, value_name, sizeof( value_name ))){

		return;
	}


	if ( !jstringToCharsA( env, _value_value, value_value, sizeof( value_value ))){

		return;
	}


	if ( RegCreateKeyExA( key, subkey_name, 0, REG_NONE, 0, KEY_ALL_ACCESS, NULL, &subkey, NULL ) == ERROR_SUCCESS ){


		if ( RegSetValueExA( subkey, value_name, 0, REG_SZ, (const BYTE*)value_value, strlen(value_value)+1 ) == ERROR_SUCCESS){

		}else{

			throwException( env, "writeStringValue", "RegSetValueEx failed" );
		}

		RegCloseKey(subkey);

	}else{

		throwException( env, "writeStringValue", "RegCreateKeyEx failed" );
	}

}


JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteKeyA(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jboolean	_recursive )
{
	HKEY		key;
	HKEY		subkey;
	char		subkey_name[1024];

	jstring		result	= NULL;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return;
	}

	if ( !jstringToCharsA( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return;
	}

	if ( RegOpenKeyA( key, subkey_name, &subkey ) == ERROR_SUCCESS ){


		RegCloseKey(subkey);

		if ( _recursive ){

			if ( SHDeleteKeyA( key, subkey_name ) != ERROR_SUCCESS ){

				throwException( env, "deleteKey", "SHDeleteKey failed" );
			}
		}else{

			if ( RegDeleteKeyA( key, subkey_name ) != ERROR_SUCCESS ){

				throwException( env, "deleteKey", "RegDeleteKey failed" );
			}
		}
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteValueA(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	HKEY		key;
	HKEY		subkey;
	char		subkey_name[1024];
	char		value_name[1024];

	jstring		result	= NULL;

	key	= mapHKEY( env, _type );

	if ( key == NULL ){

		return;
	}

	if ( !jstringToCharsA( env, _subkey_name, subkey_name, sizeof( subkey_name ))){

		return;
	}

	if ( !jstringToCharsA( env, _value_name, value_name, sizeof( value_name ))){

		return;
	}

	if ( RegOpenKeyA( key, subkey_name, &subkey ) == ERROR_SUCCESS ){


		RegCloseKey(subkey);

		if ( SHDeleteValueA( key, subkey_name, value_name ) != ERROR_SUCCESS ){

			throwException( env, "deleteValue", "SHDeleteValue failed" );
		}
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_createProcessA(
	JNIEnv*		env,
	jclass		cla, 
	jstring		_command_line, 
	jboolean	_inherit_handles )
{
	char		command_line[16000];

	STARTUPINFOA			start_info;
	PROCESS_INFORMATION		proc_info;

	if ( !jstringToCharsA( env, _command_line, command_line, sizeof( command_line ))){

		return;
	}

	memset( &start_info, 0, sizeof( STARTUPINFOA ));

	start_info.cb = sizeof( STARTUPINFOA );

	if ( CreateProcessA(
			NULL,				// LPCTSTR lpApplicationName,
			command_line,		// LPTSTR lpCommandLine,
			NULL,				// LPSECURITY_ATTRIBUTES lpProcessAttributes,
			NULL,				// LPSECURITY_ATTRIBUTES lpThreadAttributes,
			_inherit_handles,	// BOOL bInheritHandles,
			DETACHED_PROCESS,	// DWORD dwCreationFlags,
			NULL,				// LPVOID lpEnvironment,
			NULL,				// LPCTSTR lpCurrentDirectory,
			&start_info,		// LPSTARTUPINFO lpStartupInfo,
			&proc_info )){		// LPPROCESS_INFORMATION lpProcessInformation


		CloseHandle( proc_info.hThread );
        CloseHandle( proc_info.hProcess );

	}else{

		throwException( env, "createProcess", "CreateProcess failed" );
	}
};

// BLAH

JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getModuleName(
	JNIEnv		*env,
	jclass		cla )
{
	if ( non_unicode ){
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getModuleNameA( env, cla ));
	}else{
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getModuleNameW( env, cla ));
	}
}

JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getVersion(
	JNIEnv		*env,
	jclass		cla )
{
	if ( non_unicode ){
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getVersionA( env, cla ));
	}else{
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_getVersionW( env, cla ));
	}
}

JNIEXPORT jstring JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readStringValue(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	if ( non_unicode ){
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readStringValueA( env, cla, _type, _subkey_name, _value_name ));
	}else{
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readStringValueW( env, cla, _type, _subkey_name, _value_name ));
	}
}

JNIEXPORT jint JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readWordValue(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	if ( non_unicode ){
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readWordValueA( env, cla, _type, _subkey_name, _value_name ));
	}else{
		return( Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_readWordValueW( env, cla, _type, _subkey_name, _value_name ));
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_writeStringValue(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name,
	jstring		_value_value )
{
	if ( non_unicode ){
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_writeStringValueA( env, cla, _type, _subkey_name, _value_name, _value_value );
	}else{
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_writeStringValueW( env, cla, _type, _subkey_name, _value_name, _value_value );
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteKey(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jboolean	_recursive )
{
	if ( non_unicode ){
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteKeyA( env, cla, _type, _subkey_name, _recursive );
	}else{
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteKeyW( env, cla, _type, _subkey_name, _recursive );
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteValue(
	JNIEnv		*env,
	jclass		cla,
	jint		_type, 
	jstring		_subkey_name,
	jstring		_value_name )
{
	if ( non_unicode ){
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteValueA( env, cla, _type, _subkey_name, _value_name );
	}else{
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_deleteValueW( env, cla, _type, _subkey_name, _value_name );
	}
}

JNIEXPORT void JNICALL 
Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_createProcess(
	JNIEnv*		env,
	jclass		cla, 
	jstring		_command_line, 
	jboolean	_inherit_handles )
{
	if ( non_unicode ){
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_createProcessA( env, cla, _command_line, _inherit_handles );
	}else{
		Java_org_gudy_azureus2_platform_win32_access_impl_AEWin32AccessInterface_createProcessW( env, cla, _command_line, _inherit_handles );
	}
}
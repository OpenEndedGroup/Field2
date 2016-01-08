#include <com.badlogic.jglfw.Glfw.h>

//@line:273
 
	#include <GLFW/glfw3.h>
	#include <stdio.h>
	
	static jmethodID errorId = 0;
	static jmethodID monitorId = 0;
	static jmethodID windowPosId = 0;
	static jmethodID windowSizeId = 0;
	static jmethodID windowCloseId = 0;
	static jmethodID windowRefreshId = 0;
	static jmethodID windowFocusId = 0;
	static jmethodID windowIconifyId = 0;
	static jmethodID keyId = 0;
	static jmethodID characterId = 0;
	static jmethodID mouseButtonId = 0;
	static jmethodID cursorPosId = 0;
	static jmethodID cursorEnterId = 0;
	static jmethodID scrollId = 0;
	static jmethodID dropId = 0;
	static jobject callback = 0;
	static JavaVM* staticVM = 0;

#ifndef _WIN32
	#include <pthread.h>
	static pthread_key_t envTLS = 0;
	
	void createTLS() {
		pthread_key_create(&envTLS, NULL);
	}

	JNIEnv* getEnv () {
		JNIEnv* env = (JNIEnv*)pthread_getspecific(envTLS);
		if (!env) {
			if (staticVM->GetEnv((void**)&env, JNI_VERSION_1_2) != JNI_OK) {
				printf("Unable to get Env."); fflush(stdout);
				return 0;
			}
			pthread_setspecific(envTLS, env);
		}
		return env;
	}
	
	void destroyEnv() {
		if (envTLS) {
			pthread_key_delete(envTLS);
			envTLS = 0;
		}
	}
#else
	static __thread JNIEnv* envTLS = 0;

	void createTLS() {
	}

	JNIEnv* getEnv () {
		if (!envTLS) {
			if (staticVM->GetEnv((void**)&envTLS, JNI_VERSION_1_2) != JNI_OK) {
				printf("Unable to get Env."); fflush(stdout);
				return 0;
			}
		}
		return envTLS;
	}
	
	void destroyEnv() {
		envTLS = 0;
	}
#endif

	void error(int errorCode, const char* description) {
		if(callback) {
			JNIEnv* env = getEnv();
			env->CallVoidMethod(callback, errorId, (jint)errorCode, env->NewStringUTF(description));
		}
	}
	
	void windowPos(GLFWwindow* window, int x, int y) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, windowPosId, (jlong)window, (jint)x, (jint)y);
		}
	}
	
	void windowSize(GLFWwindow* window, int width, int height) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, windowSizeId, (jlong)window, (jint)width, (jint)height);
		}
	}
	
	void windowClose(GLFWwindow* window) {
		if(callback) {
			getEnv()->CallBooleanMethod(callback, windowCloseId, (jlong)window);
		}
	}
	
	void windowRefresh(GLFWwindow* window) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, windowRefreshId, (jlong)window);
		}
	}
	
	void windowFocus(GLFWwindow* window, int focused) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, windowFocusId, (jlong)window, (jboolean)(GL_TRUE==focused));
		}
	}
	
	void windowIconify(GLFWwindow* window, int iconified) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, windowIconifyId, (jlong)window, (jboolean)(GL_TRUE==iconified));
		}
	}
	
	void mouseButton(GLFWwindow* window, int button, int action, int mods) {
		if(callback) {
		
			getEnv()->CallVoidMethod(callback, mouseButtonId, (jlong)window, (jint)button, (jint)action, (jint)mods);
		}
	}
	
	void cursorPos(GLFWwindow* window, double x, double y) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, cursorPosId, (jlong)window, (jdouble)x, (jdouble)y);
		}
	}
	
	void cursorEnter(GLFWwindow* window, int entered) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, cursorEnterId, (jlong)window, (jboolean)(GL_TRUE==entered));
		}
	}
	
	void scroll(GLFWwindow* window, double xpos, double ypos) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, scrollId, (jlong)window, (jdouble)xpos, (jdouble)ypos);
		}
	}
	
	void key(GLFWwindow* window, int key, int scancode, int action, int mods) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, keyId, (jlong)window, (jint)key, (jint)scancode, (jint)action, (jint)mods);
		}
	}
	
	void character(GLFWwindow* window, unsigned int character) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, characterId, (jlong)window, (jchar)character);
		}
	}
	
	void monitor(GLFWmonitor* monitor, int event) {
		if(callback) {
			getEnv()->CallVoidMethod(callback, monitorId, (jlong)monitor, (jboolean)(GLFW_CONNECTED==event));
		}
	}

	void drop(GLFWwindow* window, int count, const char** names) {
		if(callback) {
			jobjectArray jo = getEnv()->NewObjectArray( count, getEnv()->FindClass("java/lang/String"), NULL);
			for(int i=0;i<count;i++)
			{
				jstring js = getEnv()->NewStringUTF( names[i]);
				getEnv()->SetObjectArrayElement( jo, i, js);
			}
			getEnv()->CallVoidMethod(callback, dropId, (jlong)window, jo);
		}
	}

	JNIEXPORT jboolean JNICALL Java_com_badlogic_jglfw_Glfw_glfwInitJni(JNIEnv* env, jclass clazz) {


//@line:447

		env->GetJavaVM(&staticVM);
		createTLS();

		jclass exception = env->FindClass("java/lang/Exception");
	
		jclass callbackClass = env->FindClass("com/badlogic/jglfw/GlfwCallback");
		if(!callbackClass) {
			env->ThrowNew(exception, "Couldn't find class GlfwCallback");
			return false;
		}
	
		errorId = env->GetMethodID(callbackClass, "error", "(ILjava/lang/String;)V");
		if(!errorId) {
			env->ThrowNew(exception, "Couldn't find error() method");
			return false;
		}
	
		monitorId = env->GetMethodID(callbackClass, "monitor", "(JZ)V");
		if(!monitorId) {
			env->ThrowNew(exception, "Couldn't find monitor() method");
			return false;
		}

		windowPosId = env->GetMethodID(callbackClass, "windowPos", "(JII)V");
		if(!windowPosId) {
			env->ThrowNew(exception, "Couldn't find windowPosId() method");
			return false;
		}
		
		windowSizeId = env->GetMethodID(callbackClass, "windowSize", "(JII)V");
		if(!windowSizeId) {
			env->ThrowNew(exception, "Couldn't find windowSizeId() method");
			return false;
		}

		windowCloseId = env->GetMethodID(callbackClass, "windowClose", "(J)Z");
		if(!windowCloseId) {
			env->ThrowNew(exception, "Couldn't find windowCloseId() method");
			return false;
		}

		windowRefreshId = env->GetMethodID(callbackClass, "windowRefresh", "(J)V");
		if(!windowRefreshId) {
			env->ThrowNew(exception, "Couldn't find windowRefresh() method");
			return false;
		}

		windowFocusId = env->GetMethodID(callbackClass, "windowFocus", "(JZ)V");
		if(!windowFocusId) {
			env->ThrowNew(exception, "Couldn't find windowFocus() method");
			return false;
		}

		windowIconifyId = env->GetMethodID(callbackClass, "windowIconify", "(JZ)V");
		if(!windowIconifyId) {
			env->ThrowNew(exception, "Couldn't find windowIconify() method");
			return false;
		}

		keyId = env->GetMethodID(callbackClass, "key", "(JIIII)V");
		if(!keyId) {
			env->ThrowNew(exception, "Couldn't find key() method");
			return false;
		}

		characterId = env->GetMethodID(callbackClass, "character", "(JC)V");
		if(!characterId) {
			env->ThrowNew(exception, "Couldn't find character() method");
			return false;
		}
		
		mouseButtonId = env->GetMethodID(callbackClass, "mouseButton", "(JIZI)V");
		if(!mouseButtonId) {
			env->ThrowNew(exception, "Couldn't find mouseButton() method, looked for (JIZI)V");
			return false;
		}
		
		cursorPosId = env->GetMethodID(callbackClass, "cursorPos", "(JDD)V");
		if(!cursorPosId) {
			env->ThrowNew(exception, "Couldn't find cursorPos() method");
			return false;
		}
		
		cursorEnterId = env->GetMethodID(callbackClass, "cursorEnter", "(JZ)V");
		if(!cursorEnterId) {
			env->ThrowNew(exception, "Couldn't find cursorEnter() method");
			return false;
		}
		
		scrollId = env->GetMethodID(callbackClass, "scroll", "(JDD)V");
		if(!scrollId) {
			env->ThrowNew(exception, "Couldn't find scroll() method");
			return false;
		}

		dropId = env->GetMethodID(callbackClass, "drop", "(J[Ljava/lang/String;)V");
		if(!dropId) {
			env->ThrowNew(exception, "Couldn't find drop() method");
			return false;
		}

		jboolean result = glfwInit() == GL_TRUE;
		if(result) {
			glfwSetErrorCallback(error);
			glfwSetMonitorCallback(monitor);
			
		}
		return result;
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwTerminate(JNIEnv* env, jclass clazz) {


//@line:558

		if (callback) {
			env->DeleteGlobalRef(callback);
			callback = 0;
		}
		destroyEnv();
		glfwTerminate();
	

}

JNIEXPORT jstring JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetVersionString(JNIEnv* env, jclass clazz) {


//@line:571

		return env->NewStringUTF(glfwGetVersionString());
	

}

static inline jint wrapped_Java_com_badlogic_jglfw_Glfw_glfwGetMonitorsJni
(JNIEnv* env, jclass clazz, jlongArray obj_monitors, long long* monitors) {

//@line:583

		int count = 0;
		GLFWmonitor** mons = glfwGetMonitors(&count);
		if(!mons) return 0;
		
		for(int i = 0; i < count; i++) {
			monitors[i] = (jlong)mons[i];
		}
		return count;
	
}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetMonitorsJni(JNIEnv* env, jclass clazz, jlongArray obj_monitors) {
	long long* monitors = (long long*)env->GetPrimitiveArrayCritical(obj_monitors, 0);

	jint JNI_returnValue = wrapped_Java_com_badlogic_jglfw_Glfw_glfwGetMonitorsJni(env, clazz, obj_monitors, monitors);

	env->ReleasePrimitiveArrayCritical(obj_monitors, monitors, 0);

	return JNI_returnValue;
}

JNIEXPORT jlong JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetPrimaryMonitor(JNIEnv* env, jclass clazz) {


//@line:594

		return (jlong)glfwGetPrimaryMonitor();
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetMonitorX(JNIEnv* env, jclass clazz, jlong monitor) {


//@line:598

		int x = 0;
		int y = 0;
		glfwGetMonitorPos((GLFWmonitor*)monitor, &x, &y);
		return x;
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetMonitorY(JNIEnv* env, jclass clazz, jlong monitor) {


//@line:605

		int x = 0;
		int y = 0;
		glfwGetMonitorPos((GLFWmonitor*)monitor, &x, &y);
		return y;
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetMonitorPhysicalWidth(JNIEnv* env, jclass clazz, jlong monitor) {


//@line:612

		int width = 0;
		int height = 0;
		glfwGetMonitorPhysicalSize((GLFWmonitor*)monitor, &width, &height);
		return width;
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetMonitorPhysicalHeight(JNIEnv* env, jclass clazz, jlong monitor) {


//@line:619

		int width = 0;
		int height = 0;
		glfwGetMonitorPhysicalSize((GLFWmonitor*)monitor, &width, &height);
		return height;
	

}

JNIEXPORT jstring JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetMonitorName(JNIEnv* env, jclass clazz, jlong monitor) {


//@line:626

		return env->NewStringUTF(glfwGetMonitorName((GLFWmonitor*)monitor));
	

}

static inline jint wrapped_Java_com_badlogic_jglfw_Glfw_glfwGetVideoModesJni
(JNIEnv* env, jclass clazz, jlong monitor, jintArray obj_modes, int* modes) {

//@line:646

		int numModes = 0;
		const GLFWvidmode* vidModes = glfwGetVideoModes((GLFWmonitor*)monitor, &numModes);
		for(int i = 0, j = 0; i < numModes; i++) {
			modes[j++] = vidModes[i].width;
			modes[j++] = vidModes[i].height;
			modes[j++] = vidModes[i].redBits;
			modes[j++] = vidModes[i].greenBits;
			modes[j++] = vidModes[i].blueBits;
		}
		return numModes;
	
}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetVideoModesJni(JNIEnv* env, jclass clazz, jlong monitor, jintArray obj_modes) {
	int* modes = (int*)env->GetPrimitiveArrayCritical(obj_modes, 0);

	jint JNI_returnValue = wrapped_Java_com_badlogic_jglfw_Glfw_glfwGetVideoModesJni(env, clazz, monitor, obj_modes, modes);

	env->ReleasePrimitiveArrayCritical(obj_modes, modes, 0);

	return JNI_returnValue;
}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetVideoModeJni(JNIEnv* env, jclass clazz, jlong monitor, jintArray obj_buffer) {
	int* buffer = (int*)env->GetPrimitiveArrayCritical(obj_buffer, 0);


//@line:671

		const GLFWvidmode* mode = glfwGetVideoMode((GLFWmonitor*)monitor);
		buffer[0] = mode->width;
		buffer[1] = mode->height;
		buffer[2] = mode->redBits;
		buffer[3] = mode->greenBits;
		buffer[4] = mode->blueBits;
	
	env->ReleasePrimitiveArrayCritical(obj_buffer, buffer, 0);

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetGamma(JNIEnv* env, jclass clazz, jlong monitor, jfloat gamma) {


//@line:680

		glfwSetGamma((GLFWmonitor*)monitor, gamma);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetGammaRamp(JNIEnv* env, jclass clazz) {


//@line:684

		// FIXME
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetGammaRamp(JNIEnv* env, jclass clazz) {


//@line:688

		// FIXME
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwDefaultWindowHints(JNIEnv* env, jclass clazz) {


//@line:692

		glfwDefaultWindowHints();
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwWindowHint(JNIEnv* env, jclass clazz, jint target, jint hint) {


//@line:696

		glfwWindowHint(target, hint);
	

}

static inline jlong wrapped_Java_com_badlogic_jglfw_Glfw_glfwCreateWindowJni
(JNIEnv* env, jclass clazz, jint width, jint height, jstring obj_title, jlong monitor, jlong share, char* title) {

//@line:704

		GLFWwindow* window = glfwCreateWindow(width, height, title, (GLFWmonitor*)monitor, (GLFWwindow*)share);
		if (window) {
			glfwSetWindowPosCallback(window, windowPos);
			glfwSetWindowSizeCallback(window, windowSize);
			glfwSetWindowCloseCallback(window, windowClose);
			glfwSetWindowRefreshCallback(window, windowRefresh);
			glfwSetWindowFocusCallback(window, windowFocus);
			glfwSetWindowIconifyCallback(window, windowIconify);
			glfwSetKeyCallback(window, key);
			glfwSetCharCallback(window, character);
			glfwSetMouseButtonCallback(window, mouseButton);
			glfwSetCursorPosCallback(window, cursorPos);
			glfwSetCursorEnterCallback(window, cursorEnter);
			glfwSetScrollCallback(window, scroll);
			glfwSetDropCallback(window, drop);
		}
		return (jlong)window;
	
}

JNIEXPORT jlong JNICALL Java_com_badlogic_jglfw_Glfw_glfwCreateWindowJni(JNIEnv* env, jclass clazz, jint width, jint height, jstring obj_title, jlong monitor, jlong share) {
	char* title = (char*)env->GetStringUTFChars(obj_title, 0);

	jlong JNI_returnValue = wrapped_Java_com_badlogic_jglfw_Glfw_glfwCreateWindowJni(env, clazz, width, height, obj_title, monitor, share, title);

	env->ReleaseStringUTFChars(obj_title, title);

	return JNI_returnValue;
}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwDestroyWindow(JNIEnv* env, jclass clazz, jlong window) {


//@line:724

		glfwDestroyWindow((GLFWwindow*)window);
	

}

JNIEXPORT jboolean JNICALL Java_com_badlogic_jglfw_Glfw_glfwWindowShouldClose(JNIEnv* env, jclass clazz, jlong window) {


//@line:728

		return GL_TRUE == glfwWindowShouldClose((GLFWwindow*)window);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetWindowShouldClose(JNIEnv* env, jclass clazz, jlong window, jint value) {


//@line:732

		glfwSetWindowShouldClose((GLFWwindow*)window, value);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetWindowTitle(JNIEnv* env, jclass clazz, jlong window, jstring obj_title) {
	char* title = (char*)env->GetStringUTFChars(obj_title, 0);


//@line:736

		glfwSetWindowTitle((GLFWwindow*)window, title);
	
	env->ReleaseStringUTFChars(obj_title, title);

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetWindowPos(JNIEnv* env, jclass clazz, jlong window, jint x, jint y) {


//@line:740

		glfwSetWindowPos((GLFWwindow*)window, x, y);
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetWindowX(JNIEnv* env, jclass clazz, jlong window) {


//@line:744

		int x = 0;
		int y = 0;
		glfwGetWindowPos((GLFWwindow*)window, &x, &y);
		return x;
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetWindowY(JNIEnv* env, jclass clazz, jlong window) {


//@line:751

		int x = 0;
		int y = 0;
		glfwGetWindowPos((GLFWwindow*)window, &x, &y);
		return y;
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetWindowWidth(JNIEnv* env, jclass clazz, jlong window) {


//@line:758

		int width = 0;
		int height = 0;
		glfwGetWindowSize((GLFWwindow*)window, &width, &height);
		return width;
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetWindowHeight(JNIEnv* env, jclass clazz, jlong window) {


//@line:765

		int width = 0;
		int height = 0;
		glfwGetWindowSize((GLFWwindow*)window, &width, &height);
		return height;
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetWindowSize(JNIEnv* env, jclass clazz, jlong window, jint width, jint height) {


//@line:772

		glfwSetWindowSize((GLFWwindow*)window, width, height);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwIconifyWindow(JNIEnv* env, jclass clazz, jlong window) {


//@line:776

		glfwIconifyWindow((GLFWwindow*)window);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwRestoreWindow(JNIEnv* env, jclass clazz, jlong window) {


//@line:780

		glfwRestoreWindow((GLFWwindow*)window);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwHideWindow(JNIEnv* env, jclass clazz, jlong window) {


//@line:784

		glfwHideWindow((GLFWwindow*)window);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwShowWindow(JNIEnv* env, jclass clazz, jlong window) {


//@line:788

		glfwShowWindow((GLFWwindow*)window);
	

}

JNIEXPORT jlong JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetWindowMonitor(JNIEnv* env, jclass clazz, jlong window) {


//@line:792

		return (jlong)glfwGetWindowMonitor((GLFWwindow*)window);
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetWindowParam(JNIEnv* env, jclass clazz, jlong window, jint param) {


//@line:797

		return glfwGetWindowAttrib((GLFWwindow*)window, param);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetCallbackJni(JNIEnv* env, jclass clazz, jobject javaCallback) {


//@line:811

		if (callback) {
			env->DeleteGlobalRef(callback);
			callback = 0;
		}
		if (javaCallback) callback = env->NewGlobalRef(javaCallback);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwPollEventsJni(JNIEnv* env, jclass clazz, jobject javaCallback) {


//@line:823

		glfwPollEvents();
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwWaitEventsJni(JNIEnv* env, jclass clazz, jobject javaCallback) {


//@line:831

		glfwWaitEvents();
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetInputMode(JNIEnv* env, jclass clazz, jlong window, jint mode) {


//@line:835

		return glfwGetInputMode((GLFWwindow*)window, mode);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetInputMode(JNIEnv* env, jclass clazz, jlong window, jint mode, jint value) {


//@line:839

		glfwSetInputMode((GLFWwindow*)window, mode, value);
	

}

JNIEXPORT jboolean JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetKey(JNIEnv* env, jclass clazz, jlong window, jint key) {


//@line:843

		return glfwGetKey((GLFWwindow*)window, key) == GLFW_PRESS;
	

}

JNIEXPORT jboolean JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetMouseButton(JNIEnv* env, jclass clazz, jlong window, jint button) {


//@line:847

		return glfwGetMouseButton((GLFWwindow*)window, button) == GLFW_PRESS;
	

}

JNIEXPORT jdouble JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetCursorPosX(JNIEnv* env, jclass clazz, jlong window) {


//@line:851

		double x = 0;
		double y = 0;
		glfwGetCursorPos((GLFWwindow*)window, &x, &y);
		return (double)x;
	

}

JNIEXPORT jdouble JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetCursorPosY(JNIEnv* env, jclass clazz, jlong window) {


//@line:858

		double x = 0;
		double y = 0;
		glfwGetCursorPos((GLFWwindow*)window, &x, &y);
		return (double)y;
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetCursorPos(JNIEnv* env, jclass clazz, jlong window, jint x, jint y) {


//@line:865

		glfwSetCursorPos((GLFWwindow*)window, x, y);
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetJoystickParam(JNIEnv* env, jclass clazz, jint joy, jint param)
{
  return -1;
}


JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetJoystickAxes(JNIEnv* env, jclass clazz, jint joy, jfloatArray arr) {

								       int count;
								       const float *ax = glfwGetJoystickAxes(joy, &count);
								       jsize len = env->GetArrayLength(arr);
								       jfloat *body = env->GetFloatArrayElements(arr,0);
								       

								       if (len<count) count = len;
								       for(int i=0;i<count;i++)
								       {
								       body[i] = ax[i];
								       }

								       env->ReleaseFloatArrayElements(arr, body,0);

								       return count;	
}


JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetJoystickButtons(JNIEnv* env, jclass clazz, jint joy, jbyteArray arr) {

                int count;                                                                                 
                const unsigned char *ax = glfwGetJoystickButtons(joy, &count);
                jsize len = env->GetArrayLength(arr);
                jbyte *body = env->GetByteArrayElements(arr, 0);                                 
                                                                                                           
                                                                                                           
                if (len<count) count = len;                                                                
                for(int i=0;i<count;i++)                                                                   
                {                                                                                          
                    body[i] = ax[i];                                                                           
                }                                                                                          
                                                                                                           
                env->ReleaseByteArrayElements(arr, body, 0);                                        
                                                                                                         
                return count;                                                                              

}

JNIEXPORT jstring JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetJoystickName(JNIEnv* env, jclass clazz, jint joy) {


//@line:881

		return env->NewStringUTF(glfwGetJoystickName(joy));
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetClipboardString(JNIEnv* env, jclass clazz, jlong window, jstring obj_string) {
	char* string = (char*)env->GetStringUTFChars(obj_string, 0);


//@line:885

		glfwSetClipboardString((GLFWwindow*)window, string);
	
	env->ReleaseStringUTFChars(obj_string, string);

}

JNIEXPORT jstring JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetClipboardString(JNIEnv* env, jclass clazz, jlong window) {


//@line:889

		return env->NewStringUTF(glfwGetClipboardString((GLFWwindow*)window));
	

}

JNIEXPORT jdouble JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetTime(JNIEnv* env, jclass clazz) {


//@line:893

		return glfwGetTime();
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetTime(JNIEnv* env, jclass clazz, jdouble time) {


//@line:897

		glfwSetTime(time);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwMakeContextCurrentJni(JNIEnv* env, jclass clazz, jlong window) {


//@line:906

		glfwMakeContextCurrent((GLFWwindow*)window);
	

}

JNIEXPORT jlong JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetCurrentContext(JNIEnv* env, jclass clazz) {


//@line:910

		return (jlong)glfwGetCurrentContext();
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSwapBuffers(JNIEnv* env, jclass clazz, jlong window) {


//@line:914

		glfwSwapBuffers((GLFWwindow*)window);
	

}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSwapInterval(JNIEnv* env, jclass clazz, jint interval) {


//@line:918

		glfwSwapInterval(interval);
	

}

static inline jboolean wrapped_Java_com_badlogic_jglfw_Glfw_glfwExtensionSupported
(JNIEnv* env, jclass clazz, jstring obj_extension, char* extension) {

//@line:922

		return glfwExtensionSupported(extension) == GL_TRUE;
	
}

JNIEXPORT jboolean JNICALL Java_com_badlogic_jglfw_Glfw_glfwExtensionSupported(JNIEnv* env, jclass clazz, jstring obj_extension) {
	char* extension = (char*)env->GetStringUTFChars(obj_extension, 0);

	jboolean JNI_returnValue = wrapped_Java_com_badlogic_jglfw_Glfw_glfwExtensionSupported(env, clazz, obj_extension, extension);

	env->ReleaseStringUTFChars(obj_extension, extension);

	return JNI_returnValue;
}

static inline jlong wrapped_Java_com_badlogic_jglfw_Glfw_glfwCreateCursor
(JNIEnv* env, jclass clazz, jobject obj_image, jint width, jint height, jint xhot, jint yhot, char* image) {

//@line:926

	const GLFWimage im = {width, height, (unsigned char*)image};
	return (jlong) glfwCreateCursor(&im, xhot, yhot);
	 
}

JNIEXPORT jlong JNICALL Java_com_badlogic_jglfw_Glfw_glfwCreateCursor(JNIEnv* env, jclass clazz, jobject obj_image, jint width, jint height, jint xhot, jint yhot) {
	char* image = (char*)(obj_image?env->GetDirectBufferAddress(obj_image):0);

	jlong JNI_returnValue = wrapped_Java_com_badlogic_jglfw_Glfw_glfwCreateCursor(env, clazz, obj_image, width, height, xhot, yhot, image);


	return JNI_returnValue;
}

JNIEXPORT void JNICALL Java_com_badlogic_jglfw_Glfw_glfwSetCursor(JNIEnv* env, jclass clazz, jlong window, jlong cursor) {


//@line:931

		glfwSetCursor((GLFWwindow*)window, (GLFWcursor*)cursor);
	

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetFramebufferWidth(JNIEnv* env, jclass clazz, jlong window, jlong cursor) {

                                                                     int width, height;
                                                                     glfwGetFramebufferSize((GLFWwindow*)window, &width, &height);
                                                                     return width;

}

JNIEXPORT jint JNICALL Java_com_badlogic_jglfw_Glfw_glfwGetFramebufferHeight(JNIEnv* env, jclass clazz, jlong window, jlong cursor) {

                                                                     int width, height;
                                                                     glfwGetFramebufferSize((GLFWwindow*)window, &width, &height);
                                                                     return height;
															       }

#include <jni.h>
#include <iostream>
#include "ru_nsu_fit_cpucheker_CPUInfo.h"

JNIEXPORT void JNICALL Java_ru_nsu_fit_cpucheker_CPUInfo_fillInfo(JNIEnv *env, jclass clazz, jobject info) {
    std::cout << "C++ method called" << std::endl;

    jclass infoClass = env->FindClass("ru/nsu/fit/cpucheker/CPUInfo$Info");

    std::cout << "got class" << std::endl;
    std::cout << infoClass << std::endl;

    jfieldID cpuFamilyField = env->GetFieldID(infoClass, "cpuFamily", "Ljava/lang/String;");
    jfieldID coresNumField = env->GetFieldID(infoClass, "coresNum", "I");

    std::cout << "got fields" << std::endl;

    std::string cpuFamily("roblox");

    jstring cpuFamilyString = env->NewStringUTF(cpuFamily.c_str());

    env->SetObjectField(info, cpuFamilyField, cpuFamilyString);
    env->SetIntField(info, coresNumField, 30);
    return;
}


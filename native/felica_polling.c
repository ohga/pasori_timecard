#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <libpafe.h>

#include "jp_ttlv_t_felica_Polling.h"

JNIEXPORT jstring JNICALL Java_jp_ttlv_t_felica_Polling_do_1service(JNIEnv *env, jobject obj)
{
    char buf[17];
    pasori *pa;
    felica *fe;
    int ii, size = 8;
    
    pa = pasori_open();
    if(!pa){
        return NULL;
    }
    pasori_init(pa);

    for(ii = 0; ii < 32; ii++){
        fe = felica_polling(pa, FELICA_POLLING_ANY, 0, 0);
        if(fe) break;
        usleep(128);
    }
    if(!fe){
        pasori_close(pa);
        return NULL;
    }
    
    memset(buf,'\0',sizeof(buf));
    for(ii = 0; ii < size; ii++) {
        char tmp[3];
        memset(tmp, '\0', sizeof(tmp));
        snprintf(tmp, 3, "%02X", fe->IDm[ii]);
        strcat(buf, tmp);
    }
    free(fe);
    pasori_close(pa);

    return (*env)->NewStringUTF(env, buf);
}


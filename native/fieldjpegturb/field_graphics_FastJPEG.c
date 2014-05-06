#include "field_graphics_FastJPEG.h"
#include <stdio.h>
#include <stdlib.h>
#include <jpeglib.h>

JNIEXPORT jlong JNICALL Java_field_graphics_FastJPEG_dimensionsFor
(JNIEnv *env, jobject jthis, jstring filename)
{
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
    FILE * infile;
    cinfo.err = jpeg_std_error(&jerr);
    /* Now we can initialize the JPEG compression object. */
	jboolean isCopy;
    const char *fv = (*env)->GetStringUTFChars(env, filename, &isCopy);

    jpeg_create_decompress(&cinfo);

    if ((infile = fopen(fv, "rb")) == NULL) {
        fprintf(stderr, "can't open %s\n", fv);
        (*env)->ReleaseStringUTFChars(env, filename, fv);
        return 0;
    }
    setvbuf(infile, NULL, _IOFBF, 1024*1024);

    (*env)->ReleaseStringUTFChars(env, filename, fv);
    jpeg_stdio_src(&cinfo, infile);

    (void) jpeg_read_header(&cinfo, TRUE);

    jpeg_calc_output_dimensions(&cinfo);

    fclose(infile);

    return (((long)cinfo.output_width) << 16) | (long)cinfo.output_height;

}
JNIEXPORT void JNICALL Java_field_graphics_FastJPEG_decompress
(JNIEnv *env, jobject jthis, jstring filename, jobject dest, jint width, jint height)
{
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
    FILE * infile;
    int row_stride;		/* physical row width in image buffer */
    cinfo.err = jpeg_std_error(&jerr);
    /* Now we can initialize the JPEG compression object. */
	jboolean isCopy;
    const char *fv = (*env)->GetStringUTFChars(env, filename, &isCopy);
    
    jpeg_create_decompress(&cinfo);
    
    if ((infile = fopen(fv, "rb")) == NULL) {
        fprintf(stderr, "can't open %s\n", fv);
        (*env)->ReleaseStringUTFChars(env, filename, fv);
        return ;
    }
    setvbuf(infile, NULL, _IOFBF, 1024*1024);
    
    (*env)->ReleaseStringUTFChars(env, filename, fv);
    jpeg_stdio_src(&cinfo, infile);
    
    (void) jpeg_read_header(&cinfo, TRUE);
    
    //todo set decomp parameters
    
    (void) jpeg_start_decompress(&cinfo);
    
    row_stride = cinfo.output_width * cinfo.output_components;
    
	unsigned char* destBuf = (unsigned char*)(*env)->GetDirectBufferAddress(env, dest);
    
#define MIN(x,y) ( (x)<(y) ? (x) : (y) )
    
    for(int i=0;i<MIN(height, cinfo.output_height);i++)
    {
        jpeg_read_scanlines(&cinfo, &destBuf, 1);
        destBuf+=row_stride;
    }
    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    fclose(infile);
}
JNIEXPORT void JNICALL Java_field_graphics_FastJPEG_decompressFlipped
(JNIEnv *env, jobject jthis, jstring filename, jobject dest, jint width, jint height)
{
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
    FILE * infile;
    int row_stride;		/* physical row width in image buffer */
    cinfo.err = jpeg_std_error(&jerr);
    /* Now we can initialize the JPEG compression object. */
	jboolean isCopy;
    const char *fv = (*env)->GetStringUTFChars(env, filename, &isCopy);

    jpeg_create_decompress(&cinfo);

    if ((infile = fopen(fv, "rb")) == NULL) {
        fprintf(stderr, "can't open %s\n", fv);
        (*env)->ReleaseStringUTFChars(env, filename, fv);
        return ;
    }
    setvbuf(infile, NULL, _IOFBF, 1024*1024);

    (*env)->ReleaseStringUTFChars(env, filename, fv);
    jpeg_stdio_src(&cinfo, infile);

    (void) jpeg_read_header(&cinfo, TRUE);

    //todo set decomp parameters

    (void) jpeg_start_decompress(&cinfo);

    row_stride = cinfo.output_width * cinfo.output_components;

	unsigned char* destBuf = (unsigned char*)(*env)->GetDirectBufferAddress(env, dest);

#define MIN(x,y) ( (x)<(y) ? (x) : (y) )

	destBuf = destBuf+row_stride*(MIN(height, cinfo.output_height)-1);

    for(int i=0;i<MIN(height, cinfo.output_height);i++)
    {
        jpeg_read_scanlines(&cinfo, &destBuf, 1);
        destBuf-=row_stride;
    }
    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    fclose(infile);
}

JNIEXPORT void JNICALL Java_field_graphics_FastJPEG_decompressG
(JNIEnv *env, jobject jthis, jstring filename, jobject dest, jint width, jint height)
{
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
    FILE * infile;
    int row_stride;		/* physical row width in image buffer */
    cinfo.err = jpeg_std_error(&jerr);
    /* Now we can initialize the JPEG compression object. */
	jboolean isCopy;
    const char *fv = (*env)->GetStringUTFChars(env, filename, &isCopy);
    
    jpeg_create_decompress(&cinfo);
    
    if ((infile = fopen(fv, "rb")) == NULL) {
        fprintf(stderr, "can't open %s\n", fv);
        (*env)->ReleaseStringUTFChars(env, filename, fv);
        return ;
    }
    setvbuf(infile, NULL, _IOFBF, 1024*1024);
    
    (*env)->ReleaseStringUTFChars(env, filename, fv);
    jpeg_stdio_src(&cinfo, infile);
    
    (void) jpeg_read_header(&cinfo, TRUE);
    
    //todo set decomp parameters
    
    (void) jpeg_start_decompress(&cinfo);
    
    row_stride = cinfo.output_width * cinfo.output_components;
    
    unsigned char* t = (unsigned char*)malloc(width*4);
    
	unsigned char* destBuf = (unsigned char*)(*env)->GetDirectBufferAddress(env, dest);
    
#define MIN(x,y) ( (x)<(y) ? (x) : (y) )
    
    for(int i=0;i<MIN(height, cinfo.output_height);i++)
    {
        jpeg_read_scanlines(&cinfo, &t, 1);
        for(int j=0;j<width;j++)
        {
            destBuf[j]=t[j*3+1];
        }
        destBuf+=row_stride/3;
    }
    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    fclose(infile);
}


JNIEXPORT void JNICALL Java_field_graphics_FastJPEG_compress
(JNIEnv *env, jobject jthis, jstring filename, jobject dest, jint width, jint height)
{
    struct jpeg_compress_struct cinfo;
    struct jpeg_error_mgr jerr;
    FILE * outfile;
    int row_stride;		/* physical row width in image buffer */
    cinfo.err = jpeg_std_error(&jerr);
    /* Now we can initialize the JPEG compression object. */
	jboolean isCopy;
    const char *fv = (*env)->GetStringUTFChars(env, filename, &isCopy);
    
    jpeg_create_compress(&cinfo);
    
    if ((outfile = fopen(fv, "w")) == NULL) {
        fprintf(stderr, "can't open %s\n", fv);
        (*env)->ReleaseStringUTFChars(env, filename, fv);
        return ;
    }
    (*env)->ReleaseStringUTFChars(env, filename, fv);
    jpeg_stdio_dest(&cinfo, outfile);
    cinfo.image_width = width;
    cinfo.image_height = height;
    cinfo.input_components = 3;
    cinfo.in_color_space = JCS_RGB;
    jpeg_set_defaults(&cinfo);
    jpeg_set_quality(&cinfo, 95, TRUE);
    jpeg_start_compress(&cinfo, TRUE);
    

    row_stride = cinfo.input_components * cinfo.image_width;
	unsigned char* destBuf = (unsigned char*)(*env)->GetDirectBufferAddress(env, dest);
    
    #define MIN(x,y) ( (x)<(y) ? (x) : (y) )
    
    for(int i=0;i<MIN(height, cinfo.image_height);i++)
    {
        jpeg_write_scanlines(&cinfo, &destBuf, 1);
        destBuf+=row_stride;
    }
    jpeg_finish_compress(&cinfo);
    jpeg_destroy_compress(&cinfo);
    fclose(outfile);
}


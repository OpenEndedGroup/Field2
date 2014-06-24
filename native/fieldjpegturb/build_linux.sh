gcc -I/usr/lib/jvm/jdk1.8.0_05/include/linux/ -I/usr/lib/jvm/jdk1.8.0_05/include/ -L/usr/lib/jvm/jdk1.8.0_20/jre/lib/amd64/ -O3 -Wall -c -fmessage-length=0 -std=c99 -ljpeg8 -fPIC -MMD -MP -MF"field_graphics_FastJPEG2.d" -MT"field_graphics_FastJPEG.d" -o "field_graphics_FastJPEG.o" "field_graphics_FastJPEG.c"

gcc -shared -o libfieldjpegturb.so field_graphics_FastJPEG.o -ljpeg

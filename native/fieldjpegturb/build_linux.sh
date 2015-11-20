gcc -I/usr/include/ -I/usr/lib/jvm/jdk1.9.0/include/linux/ -I/usr/lib/jvm/jdk1.9.0/include/ -L/usr/lib/jvm/jdk1.9.0/lib/amd64/ -O3 -Wall -c -fmessage-length=0 -std=c99 -ljpeg -L/usr/local/x86_64-linux-gnu/ -fPIC -MMD -MP -MF"field_graphics_FastJPEG2.d" -MT"field_graphics_FastJPEG.d" -o "field_graphics_FastJPEG.o" "field_graphics_FastJPEG.c" -L/usr/lib/x86_64-linux-gnu/ -L.

gcc -L/usr/lib/x86_64-linux-gnu/ -shared -o libfieldjpegturb.so field_graphics_FastJPEG.o -L. -ljpeg

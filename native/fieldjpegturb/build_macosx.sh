gcc -I/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home/include/ -I/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home/include/darwin -O3 -Wall -c -fmessage-length=0 -std=c99 -ljpeg -fPIC -MMD -MP -MF"field_graphics_FastJPEG2.d" -MT"field_graphics_FastJPEG.d" -o "field_graphics_FastJPEG.o" "field_graphics_FastJPEG.c"

gcc -shared -ljpeg -o libfieldjpegturb.dylib field_graphics_FastJPEG.o

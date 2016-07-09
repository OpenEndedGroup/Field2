gcc -I/usr/local/Cellar/jpeg/8d/include/ -L/usr/local/Cellar/jpeg/8d/lib/ -I/Library/Java/JavaVirtualMachines/jdk1.9.0.jdk/Contents/Home/include/ -I/Library/Java/JavaVirtualMachines/jdk1.9.0.jdk/Contents/Home/include/darwin -O3 -Wall -c -fmessage-length=0 -std=c99 -ljpeg -fPIC -MMD -MP -MF"field_graphics_FastJPEG2.d" -MT"field_graphics_FastJPEG.d" -o "field_graphics_FastJPEG.o" "field_graphics_FastJPEG.c"

gcc -shared -L/usr/local/Cellar/jpeg/8d/lib/ -ljpeg -o libfieldjpegturb.dylib field_graphics_FastJPEG.o

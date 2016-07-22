
ant -f fieldwork2.xml

cp -r ./lib/helperapp.app ./out/production/fieldwork2/

# IDEA's ant build doesn't copy the executable bit on this app
chmod 777 ./out/production/fieldcore/helperapp.app/Contents/Frameworks/jcef\ Helper.app/Contents/MacOS/jcef\ Helper

# IDEA's ant build messes up the MANIFEST.MF (including the wrong one)
jar vumf out/production/fieldagent/META-INF/MANIFEST.MF out/artifacts/fieldagent_jar/fieldagent.jar 

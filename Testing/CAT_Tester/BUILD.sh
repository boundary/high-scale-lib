# Simple build line
# 
set JAVA_HOME=/usr/local/j2sdk1.5.0_06
javac -classpath $JAVA_HOME/jre/lib/rt.jar:. harness.java org/cliffc/high_scale_lib/*.java ../org/cliffc/high_scale_lib/*.java


echo 
cd "c:\Documents and Settings\Cliff\Desktop\Highly Scalable Java\high-scale-lib\Testing\CAT_Tester"
javac -classpath .;..\.. Harness.java CATCounter.java
cd ..\..
java  -classpath Testing\CAT_Tester;. -Xbootclasspath/p:Testing\CAT_Tester;. Harness 1 3 1 0

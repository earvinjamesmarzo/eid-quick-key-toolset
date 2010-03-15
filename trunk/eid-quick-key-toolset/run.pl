

#script for running maven and launching application
print "Script started \n";

#compile using maven
print "Compile project using maven \n";
system("mvn compile");

#run main class file in eid-toolset
print "Run engine class file \n";
$java = 'C:\Java\jdk1.6.0_07\bin\java';
$classEngine = 'be/cosic/eidtoolset/engine/Engine';
$src = 'eid-toolset/target/classes';
$javajar = '-classpath .'; 

#go to class directory (otherwise java will not find the class)
#after execution of the file, perl will automatically go back to the dir wher the script was executed
chdir("$src") or die "Cant chdir to $pathEngine $!";

#run 
system("$java $javajar $classEngine");
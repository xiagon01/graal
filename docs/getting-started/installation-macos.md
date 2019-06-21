### Installation on macOS Platforms

On the contrary to Oracle JDK or OpenJDK installations performed only on a
systemwide basis, GraalVM environment can be installed for a
single user, hence administrator privileges are not required. However, if
GraalVM is meant to become a default JDK, administrator privileges
are required.

**Note:** in macOS, the JDK installation path is `/Library/Java/JavaVirtualMachines/<graalvm>/Contents/Home`.

GraalVM distribution is comprised to _tar.gz_ file. Unlike Oracle JDK or OpenJDK
distributions for macOS that comes with the _.dmg_ download, GraalVM does not
provide the installation wizard.

* Navigate to [Oracle Technology Network](https://www.oracle.com/technetwork/oracle-labs/program-languages/downloads/index.html) Downloads page and accept the license agreement.
* Select and download **GraalVM based on JDK8, preview for macOS**.
* Extract the archive to your file system. To extract the file to the current
directory from the console, type:
`$ tar -xvf archive.tar.gz`
* There can be multiple JDKs installed on the macOS system and the final step is
to configure the runtime environment.  
  - Add the GraalVM `bin` folder to the `PATH` environment variable:
  `$ export PATH=<path to GraalVM>/Contents/Home/bin:$PATH`

  Verify whether you are using GraalVM with the echo command: `$ echo $PATH`.
  - Set the `JAVA_HOME` environment variable to resolve to the GraalVM installation directory:
  `$ export JAVA_HOME=<path to GraalVM>/Contents/Home`
* You can also specify GraalVM as the JRE or JDK installation in your Java IDE.


**Note on macOS java_home command**

The information property file, _Info.plist_, is in the top level _Contents_
folder. This means that GraalVM participates in the macOS specific
`/usr/libexec/java_home` mechanism. Depending on other JDK 8 installation(s)
available, it is now possible that `/usr/libexec/java_home -v1.8` returns
`/Library/Java/JavaVirtualMachines/graalvm-ee-19.0.2/Contents/Home`.
You can run `/usr/libexec/java_home -v1.8 -V` to see the complete list of 1.8
JVMs available to the `java_home` command. This command appears to sort the JVMs
in decreasing version order and chooses the top one as the default for the
specified version. Within a specific version, the sort order appears to be
stable but is unspecified.

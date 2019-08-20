## LLVM Toolchain

The toolchain is a set of tools and APIs for compiling native programs, such as
C and C++, to bitcode that can be executed with the GraalVM LLVM runtime. To
simplify compiling C/C++ to LLVM bitcode, we provide launchers that invoke the
compiler with special flags to produce results that can be executed by the
GraalVM LLVM runtime. Depending on the execution mode, the launchers location is
`$GRAALVM/jre/languages/llvm/{native|managed}/bin/*` (in the native execution
mode, the path is `$GRAALVM/jre/languages/llvm/native/bin/*`, in the managed
mode -- `$GRAALVM/jre/languages/llvm/managed/bin/*`). They are meant to be
drop-in replacements for the C/C++ compiler when compiling a native program. The
goal is to produce a GraalVM LLVM runtime executable by simply pointing the
build system to those launchers, for example via `CC`/`CXX` environment
variables or by setting `PATH`.

**Warning:** Toolchain support is **experimental**. Experimental features might never be
included in a production version, or might change significantly before being
considered production-ready.

The LLVM toolchain is pre-packaged as a component and can be installed with [GraalVM Updater]({{"/docs/reference-manual/graal-updater/" | relative_url }}) tool:
```
gu install llvm-toolchain
```

The following example shows how the toolchain can be used to compile a `make`-based project.
Let us assume that the `CC` variable is used in the `Makefile` to specify the C
compiler that produces an executable named `myprogram`.
We compile the project as follows:
```
$ make CC=${GRAALVM}/jre/languages/llvm/native/bin/clang myprogram
```
Afterwards, the resulting `myprogram` can be executed by the LLVM runtime:
```
$ ${GRAALVM}/bin/lli myprogram
```

### Use Cases
* **Simplify the compilation to bitcode:** GraalVM users who want to run native projects via the GraalVM LLVM runtime must
first compile these projects to LLVM bitcode. Although it is possible to do this
with the standard LLVM tools (`clang`, `llvm-link`, etc.), there are several
additional considerations, such as optimizations and manual linking. The
toolchain aims to simplify this process, by providing an out-of-the-box drop-in
replacement for the compiler when building native projects targeting the GraalVM
LLVM runtime.

* **Compile native extensions:** GraalVM language implementers often use the GraalVM LLVM runtime to execute
 native extensions, and these extensions are commonly installed by a package
 manager. For example, packages in Python are usually added via `pip install`,
 which means that the Python implementation is required to be able to compile
 these native extensions on demand. The toolchain provides a Java API for
 languages to access the tools bundled with GraalVM.

* **Compile to bitcode at build time:** GraalVM supported languages that integrate with the GraalVM LLVM runtime usually need to build bitcode libraries to integrate with the native pieces of their
implementation. The toolchain can be used as a build-time dependency to achieve
this in a standardized and compatible way.

### File Format
To be compatible with existing build systems, by default, the toolchain will
produce native executables with embedded bitcode (ELF files on Linux, Mach-O
files on macOS).

### Toolchain Identifier
The GraalVM LLVM runtime can be ran in different configurations, which can
differ in how the bitcode is being compiled. Generally, toolchain users do not
need to be concerned, as the GraalVM LLVM runtime knows the mode it is running
and will always provide the right toolchain. However, if a language
implementation wants to store the bitcode compilation for later use, it will
need to be able to identify the toolchain and its configurations used to compile
the bitcode. To do so, each toolchain has an _identifier_. Conventionally, the
identifier denotes the compilation output directory. The internal GraalVM LLVM
runtime library layout follows the same approach.

### Java API Toolchain Service
Language implementations can access the toolchain via the `Toolchain` service. The service provides two methods:

- `TruffleFile getToolPath(String tool)` returns the path to the executable for a
given tool. Every implementation is free to choose its own set of supported
tools. The command line interface of the executable is specific to the tool. If
a tool is not supported or not known, `null` is returned.
- `String getIdentifier()` returns the identifier for the toolchain. It can be
used to distinguish results produced by different toolchains. The identifier can
be used as a path suffix to place results in distinct locations, therefore it
does not contain special characters like slashes or spaces.

The `Toolchain` lives in the `SULONG_API` distribution. The LLVM runtime will
always provide a toolchain that matches its current mode. The service can be
looked-up via the `Env`:
```
LanguageInfo llvmInfo = env.getInternalLanguages().get("llvm");
Toolchain toolchain = env.lookup(llvmInfo, Toolchain.class);
TruffleFile toolPath = toolchain.getToolPath("CC");
String toolchainId = toolchain.getIdentifier();
```

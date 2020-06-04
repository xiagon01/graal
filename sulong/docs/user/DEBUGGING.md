# Debugging on the GraalVM LLVM Runtime

The GraalVM LLVM runtime supports source-level (e.g. C language) debugging with the
[Chrome Developer Tools](https://developers.google.com/web/tools/chrome-devtools/) using GraalVM's
Chrome Inspector. This includes support for single-stepping, breakpoints and inspection of local
and global variables.

To start debugging simply run the following in GraalVM's `bin` directory:

```
$GRAALVM/bin/lli --inspect <bitcode file>
```

When launched, the inspector will suspend execution at the first instruction of the program and print
a link to the console. Pasting this link into Chrome's address bar will open the developer tools for you.

## Breakpoints

Breakpoints can only be set in functions that have already been parsed. GraalVM defaults to parsing
functions in LLVM bitcode files only when they are first being executed. To instead parse functions
eagerly and be able to set breakpoints also in functions not yet executed you can use the option
`lli --llvm.lazyParsing=false`.

## Program-defined breakpoints using `__builtin_debugtrap()`

The `__builtin_debugtrap` function enables you to mark locations in your program at which you explicitly
want GraalVM to halt the program and switch to the debugger. The debugger automatically halts at each call
to this function as if a breakpoint were set on the call. You can use this feature to quickly reach the
code you are actually trying to debug without having to first find and set a breakpoint on it after
launching your application. You can also instruct Chrome Inspector not to suspend your program at the first
source-level statement being executed. When doing so, GraalVM will instead execute your program until it
reaches a call to `__builtin_debugtrap()` before invoking the debugger. To enable this behaviour you need
pass the arguments `lli --inspect.Suspend=false --inspect.WaitAttached=true`.

## FAQ

### I am compiling my bitcode files on another system. Can the source-level debugger find the sources on my system?

In general, debug information in LLVM bitcode files contains absolute search paths to identify the
location of source code. Alternatively, a search path for source files can be specified using
the `--inspect.SourcePath=<path>` option (multiple paths can be specified separated by `:`).

### Can I also debug my program on LLVM-IR level?

GraalVM also contains preliminary support for debugging program on the level of LLVM IR.
This feature is only in the early stages and may be incomplete. To use it, you need to
replace add the option `--experimental-options --llvm.llDebug`.

Also, to debug on LLVM-IR level you need to use `llvm-dis` to disassemble the bitcode
that you want to execute. GraalVM expects a file with the same name as the bitcode module but
with the `.ll` extension in the same directory as the bitcode module it executes.

You can also specify a separate location for the `*.ll` file corresponding to a bitcode
file using the `--llvm.llDebug.Sources` argument. When using this option you need to specify
the path of both the `*.ll` and the bitcode file which it describes. While the option
itself can only be specified once, you can pass it an arbitrary number of path mappings.

```
--llvm.llDebug.Sources=<path to *.bc file>=<path to *.ll file>[:<path to *.bc file>=<path to *.ll file>]*
```

### How can I generate a trace of how GraalVM executes my program?

GraalVM can produce an LLVM IR-level trace of its program execution. You can enable
this feature by passing the `--experimental-options --llvm.traceIR=<...>` option to `lli`.
See `lli --help:languages --help:expert` for more information.

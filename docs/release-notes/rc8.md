## 1.0-RC8
(2018-10-19)
### GraalVM for Java Developers (GraalVM + compiler)
* Added the support for Intel bit manipulation instructions. For more details, see [#666](https://github.com/oracle/graal/pull/666).
* Virtualize `unsafe` compare and swap calls on non-escaping objects. See [#636](https://github.com/oracle/graal/pull/636).

### Maven Artifacts
* The **`com.oracle.truffle`** group ID was renamed to **`org.graalvm.truffle`**.
* The `graal-sdk`, `launcher-common` and `polyglot-tck` artifacts were moved from the **`org.graalvm`** group ID to **`org.graalvm.sdk`**.
* New artifacts that are now available:
  - `org.graalvm.compiler:compiler`
  - `org.graalvm.js:js`
  - `org.graalvm.js:js-launcher`
  - `org.graalvm.js:js-scriptengine`
  - `org.graalvm.regex:regex`
  - `org.graalvm.tools:chromeinspector`
  - `org.graalvm.tools:profiler`
  - `org.graalvm.truffle:truffle-nfi`
  - `com.oracle.substratevm:library-support`
  - `com.oracle.substratevm:objectfile`
  - `com.oracle.substratevm:pointsto`
  - `com.oracle.substratevm:svm-driver`
  - `com.oracle.substratevm:svm`
* The artifacts that are now modular JARs:
  - `truffle-api`
  - `graal-sdk`
  - `js`
  - `js-scriptengine`
  - `compiler`
* The `compiler` artifact provides the **`jdk.internal.vm.compiler`** module and can be used to upgrade that module in JDK 11. Unlike the module present in the JDK, this version contains the optimizing Truffle runtime.

### Native Image Generation

* The native image generator now has automatic support for services loaded using ServiceLoader.
All service implementation classes, listed in the `META-INF` directory, are available automatically as soon as the service interface is used. This eliminates the need to manually register resources and reflection support for such classes. The automatic registration can be disabled with the `-H:-UseServiceLoaderFeature` option.
* Finished the support for isolates (starting multiple independent VM instances at run time) and compressed references (to reduce memory footprint; Enterprise Edition only). More details are in an upcoming blog article.
* A new Maven plugin allows native image generation from within a Maven build.
* All components necessary for native image generation (Substrate VM and the GraalVM compiler) are now available on Maven Central. The dependency for that is:
```
    <dependency>
      <groupId>com.oracle.substratevm</groupId>
      <artifactId>svm</artifactId>
      <version>1.0.0-rc8</version>
      <scope>provided</scope>
    </dependency>
```    

### JavaScript
* Updated Node.js to version 10.9.0.

A complete [changelog](https://github.com/graalvm/graaljs/blob/master/CHANGELOG.md#version-100-rc8) is available on GitHub.

### LLVM Runtime
The project [changelog](https://github.com/oracle/graal/blob/master/sulong/CHANGELOG.md#version-100-rc8) is available on GitHub.

### Ruby
* Ubuntu 18.04 LTS, Fedora 28, and macOS 10.14 (Mojave) now supported.
* `Java.synchronized(object) { }` and `TruffleRuby.synchronized(object) { }` methods have been added.
* Added a `TruffleRuby::AtomicReference` class.
* Performance of setting the last exception on a thread has now been improved.

A complete project [changelog](https://github.com/oracle/truffleruby/blob/master/CHANGELOG.md#10-rc-8-19-october-2018) is available on GitHub.

### Python
* Python now supports the allocation profiler (`--memtracer`) to analyze the heap usage of applications.

The [changelog](https://github.com/graalvm/graalpython/blob/master/CHANGELOG.md#version-100-rc8) is available on GitHub.

### R
GraalVM 1.0-rc8 implementation of R came up with multiple bug fixes which can be found in the project [changelog](https://github.com/oracle/fastr/blob/master/CHANGELOG.md#10-rc-8) on GitHub.

### Tools
**Graal VisualVM**
* Introduced the CPU and Memory Sampler for guest languages.
* Submitted improvements in the R language heapwalker.

### API Changes for GraalVM Integrators (SDK + Truffle)
The major GraalVM SDK and Truffle changes between GraalVM versions are summarized in the project changelogs:
- [GraalVM SDK changelog](https://github.com/oracle/graal/blob/master/sdk/CHANGELOG.md#version-10-rc8)
- [GraalVM Truffle changelog](https://github.com/oracle/graal/blob/master/truffle/CHANGELOG.md#version-100-rc8)

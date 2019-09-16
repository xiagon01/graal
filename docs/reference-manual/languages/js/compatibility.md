## GraalVM JavaScript Compatibility

GraalVM is [ECMAScript 2019](http://www.ecma-international.org/ecma-262/10.0/index.html) compliant and fully compatible with a diverse range of active Node.js (npm) modules.
It will also be compliant to ECMAScript 2020 once this updated specification is published ([draft spec](https://tc39.github.io/ecma262/)).
More than 100,000 npm packages are regularly tested and are compatible with GraalVM, including modules like express, react, async, request, browserify, grunt, mocha, and underscore.
The latest release of GraalVM is based on Node.js version 10.16.3.

### Is GraalVM compatible with the JavaScript language?

_What version of ECMAScript do we support?_

GraalVM is compatible to the ECMAScript 2019 specification.
Some features of ECMAScript 2020 including some proposed features and extensions are available as well, but might not be fully implemented, compliant, or stable, yet.

_How do we know it?_

The compatibility of GraalVM JavaScript is verified by external sources, like the [Kangax ECMAScript compatibility table](https://kangax.github.io/compat-table/es6/).

On our CI system, we test GraalVM JavaScript against a set of test engines, like the official test suite of ECMAScript, [test262](https://github.com/tc39/test262), as well as tests published by V8 and Nashorn, Node.js unit tests, and GraalVM's own unit tests.

For a reference of the JavaScript APIs that GraalVM supports, see [GRAAL.JS-API](https://github.com/graalvm/graaljs/blob/master/docs/user/JavaScriptCompatibility.md).

### Is GraalVM compatible with the original node implementation?

Node.js based on GraalVM is largely compatible with the original Node.js (based on the V8 engine).
This leads to a high number of npm-based modules being compatible with GraalVM (out of the 95k modules we test, more than 90% of them pass all tests).
Several sources of differences have to be considered.

- **Setup:**
GraalVM mostly mimicks the original setup of Node, including the `node` executable, `npm`, and similar. However, not all command-line options are supported (or behave exactly identically), you need to (re-)compile native modules against our v8.h file, etc.

- **Internals:**
GraalVM is implemented on top of a JVM, and thus has a different internal architecture. This implies that some internal mechanisms behave differently and cannot exactly replicate V8 behavior. This will hardly ever affect user code, but might affect modules implemented natively, depending on V8 internals.

- **Performance:**
Due to GraalVM being implemented on top of a JVM, performance characteristics vary from the original native implementation. While GraalVM's peak performance can match V8 on many benchmarks, it will typically take longer to reach the peak (known as _warmup_). Be sure to give the GraalVM compiler some extra time when measuring (peak) performance.

_How do we determine GraalVM's JavaScript compatibility?_

GraalVM is compatible to ECMAScript 2019, guaranteeing compatibility on the language level.
In addition, GraalVM uses the following approaches to check and retain compatibility to Node.js code:

* node-compat-table: GraalVM is compared against other engines using the _node-compat-table_ module, highlighting incompatibilities that might break Node.js code.
* automated mass-testing of modules using _mocha_: in order to test a large set of modules, GraalVM is tested against 95k modules that use the mocha test framework. Using mocha allows automating the process of executing the test and comprehending the test result.
* manual testing of popular modules: a select list of npm modules is tested in a manual test setup. These highly-relevant modules are tested in a more sophisticated manner.

If you want your module to be tested by GraalVM in the future, ensure the module provides some mocha tests (and send us an email so we can ensure it is on the list of tested modules).

_How can one verify GraalVM works on their application?_

If your module ships with tests, execute them with GraalVM.
Of course, this will only test your app, but not its dependencies.
You can use the [compatibility checker]({{"/docs/reference-manual/compatibility/" | relative_url}}) to find whether the module you're interested in is tested on GraalVM, whether the tests pass successfully and so on.
Additionally, you can upload your `package-lock.json` or `package.json` file into that utility and it'll analyze all your dependencies at once.

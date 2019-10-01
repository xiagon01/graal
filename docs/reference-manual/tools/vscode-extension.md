This page explains how to start using **Visual Studio Code support for
GraalVM**, introduced in the 19.2 version. Visual Studio Code (from now on
VS Code) is a source-code editor  that provides embedded Git and GitHub control,
syntax highlighting, code refactoring etc.. To enable a polyglot environment in
VS Code, we created extensions for GraalVM supported languages:  JS, Ruby, R,
Python. This allows a simple registration of GraalVM as a runtime, code editing
and debugging of polyglot applications.

The following extensions are available:
- *graalvm* -- a VS Code extension providing the basic environment for editing and debugging programs running on GraalVM and includes JavaScript and Node.js support by default.
- *graalvm-r* -- a VS Code extension providing the basic support for editing and debugging R programs running on GraalVM.
- *graalvm-ruby* -- a VS Code extension providing the basic support for editing and debugging Ruby programs on GraalVM.
- *graalvm-python* -- a VS Code extension providing the basic support for editing and debugging Python programs running on GraalVM.

### Install Extensions

To start using the extensions in VS Code, take the following steps.
1. Download the **vs-code-extensions-tech-preview.zip**
from [Github](https://github.com/oracle/graal/releases), extract an archive and enter it. Inside the folder you see VSIX packages built for you:
```
graalvm-<version>.vsix
graalvm-r-<version>.vsix
graalvm-ruby-<version>.vsix
graalvm-python-<version>.vsix
```
1.1. As an alternative, you can build VSIX packages yourself.
Clone the extensions source and enter it:
```
git clone https://github.com/oracle/graal.git
cd graal/vs-code
```
1.2. Install `vsce` (short for "Visual Studio Code Extension Manager"), a command-line tool
for packaging, publishing and managing VS Code extensions:
```
npm install -g vsce
```
1.3. Build VSIX packages that could be manually installed later:
```
cd graalvm; npm install; vsce package
cd graalvm-r; npm install; vsce package
cd graalvm-ruby; npm install; vsce package
cd graalvm-python; npm install; vsce package
```

2. Install each package into VS Code with `code --install-extension <extension.vsix>` syntax:
```
code --install-extension graalvm-<version>.vsix
code --install-extension graalvm-r-<version>.vsix
code --install-extension graalvm-ruby-<version>.vsix
code --install-extension graalvm-python-<version>.vsix
```
Alternatively to the manual installation, use the _Install from VSIX..._ action: open Extensions (`Ctrl+Shift+X`) -> More Actions (three dots in the upper right corner) -> Install from VSIX.

![](/docs/img/manual_install.png)

### GraalVM Extension
Upon the *graalvm* extension installation, launch VS Code by
double-clicking on the icon in the launchpad or by typing `code .` from the CLI.
The user is then requested to provide a path to the GraalVM home directory.

![](/docs/img/no-path-to-graalvm.png)

For that purpose, next options can be used (invoke by _Ctrl+Shift+P_ hot keys combination):

* **Select GraalVM Installation** - Provides the UI to select an already installed GraalVM. By default, the following locations are searched for the already installed GraalVM:
  * the extension's global storage
  * `/opt` folder as the default RPM install location
  * `PATH` environment variable content
  * `GRAALVM_HOME` environment variable content
  * `JAVA_HOME` environment variable content
* **Install GraalVM** - Downloads the latest GraalVM release from Github and installs it within the extension's global storage.
* **Install GraalVM Component** - Downloads and installs one of the GraalVM's optional components.

![](/docs/img/component-install.png)

To verify whether the `PATH` environment variable is valid, navigate to Code -> Preferences -> Settings -> Extensions -> GraalVM -> Home:

![](/docs/img/graalvm_path.png)

If the path to GraalVM home directory is provided properly, the following debug
configurations can be used:

* *Attach* - Attaches debugger to a locally running GraalVM.
* *Attach to Remote* - Attaches debugger to the debug port of a remote GraalVM.
* *Launch JavaScript* - Launches  JavaScript using GraalVM in a debug mode.
* *Launch Node.js Application* - Launches a Node.js network application using GraalVM in a debug mode.

![](/docs/img/debug-config.png)

Languages interoperability is one of the defining features of GraalVM, enabled
with [Polyglot APIs](https://www.graalvm.org/sdk/javadoc/). The code completion
invoked inside JavaScript sources provides items for `Polyglot.eval(...)`,
`Polyglot.evalFile(...)` and `Java.type(...)` calls.

![](/docs/img/code-completion-js.png)

For JavaScript sources opened in the editor, all the `Polyglot.eval(...)` calls
are detected and the respective embedded languages are injected to their
locations. For example, having an R code snippet called via the Polyglot API
from inside the JavaScript source, the R language code is embedded inside the
corresponding JavaScript string and all VS Code's editing features (syntax
highlighting, bracket matching, auto closing pairs, code completion, etc.) treat
the content of the string as the R source code.

![](/docs/img/language-embedding-js.png)

### R Extension

Upon the extension installation in VS Code, GraalVM is checked for
presence of the R component and a user is provided with an option of an automatic
installation of the missing component. The _Ctrl+Shift+P_ command from the
Command Palette can be also used to invoke **Install GraalVM
Component** option to install the R component manually.

Once GraalVM contains the R component, the following debug configurations
can be used to debug your R scripts running on GraalVM:

* **Launch R Script** - Launches an R script using GraalVM in a debug mode.
* **Launch R Terminal** - Launches an integrated R terminal running on GraalVM in a debug mode.

![](/docs/img/debug-config-r.png)

Thanks to languages interoperability within GraalVM, the code completion invoked
inside R sources provides items for `eval.polyglot(...)` and `new("<Java type>",
...)` calls. For R sources opened in editor, all the `eval.polyglot(...)` calls are detected and the respective embedded languages are injected to their locations.

![](/docs/img/code-completion-r.png)

Please note, this R extension depends on the basic support for [R language](https://marketplace.visualstudio.com/items?itemName=Ikuyadeu.r) and GraalVM extension in VS Code.

### Ruby Extension

Similar to the above R extension installation, GraalVM is checked for
presence of the Ruby component and a user is provided with an option of an automatic
installation of the missing component. The _Ctrl+Shift+P_ command from the
Command Palette can be also used to invoke **Install GraalVM
Component** option to install the Ruby component manually.

Once GraalVM contains the Ruby component, **Launch Ruby Script** debug
configuration can be used to run your Ruby script in a debug mode.

The code completion invoked inside Ruby sources provides items for `Polyglot.eval(...)`, `Polyglot.eval_file(...)` and `Java.type(...)` calls. As with other languages, all the `Polyglot.eval(...)` calls are detected and the respective embedded languages are injected to their locations. For example, the JavaScript language code is embedded inside the corresponding Ruby string and all VS Code's editing features (syntax highlighting, bracket matching, auto closing pairs, code completion, etc.) treat the content of the string as the JavaScript source code.

![](/docs/img/language-embedding-ruby.png)

This Ruby extension requires a default [Ruby language support](https://marketplace.visualstudio.com/items?itemName=rebornix.Ruby) and GraalVM extension in VS Code.


### Python Extension

GraalVM is checked for presence of the Python component and a user is provided
with an option of an automatic installation of the missing component, similar to
the previous R and Ruby extensions. The _Ctrl+Shift+P_ command from the Command
Palette can be also used to invoke **Install GraalVM Component** option to
install the Ruby component manually.

Once GraalVM contains the Ruby component, **Launch Python Script** debug
configuration can be used to run the Python script in a debug mode.

Python VS Code extension requires a default [Python language support](https://marketplace.visualstudio.com/items?itemName=ms-python.python) and GraalVM extension in VS Code.

# Generating GraalVM Reachability Metadata for HumbleUI/JWM+Skija

This project uses GraalVM Native Image to compile the Smalltalk VM Ahead-Of-Time (AOT). Because the UI layer relies on JWM and Skija—which use the Java Native Interface (JNI) to communicate with a C++ graphics backend—we must explicitly tell the GraalVM compiler which standard JDK classes the C++ code will ask for at runtime.

If these internal JDK callbacks (like `java.lang.Runnable` or standard exceptions) are not registered, GraalVM's static analyzer will strip them from the final binary, causing the native application to crash with a `NullPointerException` or `NoClassDefFoundError` during the boot sequence.

We solve this using a hybrid configuration approach:
1. **Automated Python Script:** Generates broad JNI configurations for the massive surface area of JWM and Skija callbacks.
2. **GraalVM Tracing Agent:** Dynamically records the unpredictable, deeply-nested JDK classes requested by JWM and Skija's C++ engine during startup and saves them to a `reachability-metadata.json` file.

If you upgrade the JWM or Skija dependency versions in the future, you must re-run the Tracing Agent to capture any new JNI boundaries. Follow the steps below.

## How to Update the Reachability Metadata

### Step 1: Enable the Tracing Agent in the JVM
By default, the TruffleSqueak build system excludes the GraalVM Tracing Agent from the standalone JVM to save disk space. Before we can capture data, we must temporarily include it.

1. Open `mx.trufflesqueak/suite.py`.
2. Locate the `TRUFFLESQUEAK_JVM_STANDALONE` distribution block (around line 348).
3. Find the `exclude` array inside the `jvm/` layout and comment out the `native-image-agent` line by adding a `#`:
   ```python
   # "lib/<lib:native-image-agent>",
   ```
4. Rebuild the standalone JVM so the agent library is copied over. Run this in your terminal:
   ```bash
   mx clean
   ts_build
   ```

### Step 2: Run the Agent Capture
Run the standard JVM build with the GraalVM Tracing Agent attached. We will output the results to a temporary folder in the root directory named `ui-agent-config`, using a 5-second periodic writer.

Execute this command from the root of the repository:
```bash
ts_run --vm.agentlib:native-image-agent=config-output-dir=$(pwd)/ui-agent-config,config-write-period-secs=5
```

### Step 3: Exercise the Application
When the Squeak window opens, you must force the C++ backend to trigger its internal JVM callbacks.
* Move the mouse around the Squeak window.
* Click the background to open the World Menu.
* Type a few characters on the keyboard.

### Step 4: The 10-Second Wait
Because the agent uses `config-write-period-secs=5`, it flushes its captured data to disk on a timer.
1. Take your hands off the keyboard and mouse.
2. **Wait at least 10 seconds** to guarantee the timer fires.
3. Quit Squeak normally.

### Step 5: Relocate the Metadata
The agent has now generated a `reachability-metadata.json` file inside your temporary `ui-agent-config` folder. We need to move it to its permanent, version-controlled home.

1. Navigate to the existing `META-INF.native-image` folder in the project root (or inside `src`).
2. Create a new subfolder named `jwm-skija` (e.g., `META-INF.native-image/jwm-skija/`).
3. Move the newly generated `reachability-metadata.json` file directly into this subfolder.

*(Note: GraalVM automatically scans `META-INF.native-image` and all its subdirectories, so isolating it in a subfolder prevents filename collisions with TruffleSqueak's upstream configurations).*

### Step 6: Restore the JVM Exclusions
Now that the metadata is safely in place, you should restore `suite.py` to prevent bloating your local JVM in future builds.

1. Open `suite.py` again.
2. Remove the `#` to restore the exclusion:
   ```python
   "lib/<lib:native-image-agent>",
   ```
3. Delete the now-empty temporary `ui-agent-config` folder from your project root.

### Step 7: Compile the Native Binary
You are now ready to compile the fully-configured, Ahead-Of-Time binary.
```bash
ts_buildNative
```
# SDL3 Bindings

To regenerate the checked-in SDL3 bindings:

1. Download the [SDL3 sources](https://github.com/libsdl-org/SDL/archive/refs/tags/release-3.4.2.zip) and extract them in TruffleSqueak's `src/` directory.
2. Download [jextract](https://jdk.java.net/jextract/) and run it with:

    ```bash
    jextract @./src/de.hpi.swa.trufflesqueak.sdl3/jextract.args
    ```
3. Re-apply [this Windows `size_t` compatibility fix](https://github.com/hpi-swa/trufflesqueak/commit/efea90e2202613d3c6da8f3d54a65f26c86a1ec7).

/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

import static de.hpi.swa.trufflesqueak.sdl3.SDL3Utils.warning;

@SuppressWarnings("restricted")
class MacMenuShortcutDisabler {

    // The inner class delays native initialization until explicitly called.
    // This prevents libobjc lookup crashes on Windows and Linux.
    private static final class ObjC {
        static final Linker LINKER = Linker.nativeLinker();
        static final SymbolLookup LIB = SymbolLookup.libraryLookup("libobjc.A.dylib", Arena.global());

        static final MethodHandle OBJC_GET_CLASS;
        static final MethodHandle SEL_REGISTER_NAME;

        static final MethodHandle MSG_SEND_PTR;
        static final MethodHandle MSG_SEND_PTR_PTR;
        static final MethodHandle MSG_SEND_PTR_LONG;
        static final MethodHandle MSG_SEND_LONG;
        static final MethodHandle MSG_SEND_VOID_PTR;

        static {
            final MemorySegment getClassAddr = LIB.find("objc_getClass").orElseThrow();
            OBJC_GET_CLASS = LINKER.downcallHandle(getClassAddr, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            final MemorySegment selRegNameAddr = LIB.find("sel_registerName").orElseThrow();
            SEL_REGISTER_NAME = LINKER.downcallHandle(selRegNameAddr, FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            final MemorySegment msgSendAddr = LIB.find("objc_msgSend").orElseThrow();

            MSG_SEND_PTR = LINKER.downcallHandle(msgSendAddr,
                            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            MSG_SEND_PTR_PTR = LINKER.downcallHandle(msgSendAddr,
                            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            MSG_SEND_PTR_LONG = LINKER.downcallHandle(msgSendAddr,
                            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

            MSG_SEND_LONG = LINKER.downcallHandle(msgSendAddr,
                            FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

            MSG_SEND_VOID_PTR = LINKER.downcallHandle(msgSendAddr,
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        }
    }

    public static void disableSystemShortcuts() {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nsAppClass = (MemorySegment) ObjC.OBJC_GET_CLASS.invokeExact(arena.allocateFrom("NSApplication"));
            final MemorySegment sharedAppSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("sharedApplication"));
            final MemorySegment app = (MemorySegment) ObjC.MSG_SEND_PTR.invokeExact(nsAppClass, sharedAppSel);

            final MemorySegment mainMenuSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("mainMenu"));
            final MemorySegment mainMenu = (MemorySegment) ObjC.MSG_SEND_PTR.invokeExact(app, mainMenuSel);

            if (mainMenu.equals(MemorySegment.NULL)) {
                return;
            }

            final MemorySegment itemArraySel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("itemArray"));
            final MemorySegment countSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("count"));
            final MemorySegment objectAtIndexSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("objectAtIndex:"));
            final MemorySegment submenuSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("submenu"));
            final MemorySegment keyEquivalentSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("keyEquivalent"));
            final MemorySegment utf8StringSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("UTF8String"));
            final MemorySegment setKeyEquivSel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("setKeyEquivalent:"));

            final MemorySegment nsStringClass = (MemorySegment) ObjC.OBJC_GET_CLASS.invokeExact(arena.allocateFrom("NSString"));
            final MemorySegment utf8Sel = (MemorySegment) ObjC.SEL_REGISTER_NAME.invokeExact(arena.allocateFrom("stringWithUTF8String:"));
            final MemorySegment emptyString = (MemorySegment) ObjC.MSG_SEND_PTR_PTR.invokeExact(nsStringClass, utf8Sel, arena.allocateFrom(""));

            final MemorySegment topLevelItems = (MemorySegment) ObjC.MSG_SEND_PTR.invokeExact(mainMenu, itemArraySel);
            if (topLevelItems.equals(MemorySegment.NULL)) {
                return;
            }

            final long topCount = (long) ObjC.MSG_SEND_LONG.invokeExact(topLevelItems, countSel);

            // Iterate through top-level menus
            for (long i = 0; i < topCount; i++) {
                final MemorySegment topMenuItem = (MemorySegment) ObjC.MSG_SEND_PTR_LONG.invokeExact(topLevelItems, objectAtIndexSel, i);
                final MemorySegment submenu = (MemorySegment) ObjC.MSG_SEND_PTR.invokeExact(topMenuItem, submenuSel);

                if (!submenu.equals(MemorySegment.NULL)) {
                    final MemorySegment subItems = (MemorySegment) ObjC.MSG_SEND_PTR.invokeExact(submenu, itemArraySel);
                    if (!subItems.equals(MemorySegment.NULL)) {
                        final long subCount = (long) ObjC.MSG_SEND_LONG.invokeExact(subItems, countSel);

                        // Iterate through immediate sub-items
                        for (long j = 0; j < subCount; j++) {
                            final MemorySegment subMenuItem = (MemorySegment) ObjC.MSG_SEND_PTR_LONG.invokeExact(subItems, objectAtIndexSel, j);

                            final MemorySegment keyEquivObj = (MemorySegment) ObjC.MSG_SEND_PTR.invokeExact(subMenuItem, keyEquivalentSel);
                            if (!keyEquivObj.equals(MemorySegment.NULL)) {
                                final MemorySegment cStrPtr = (MemorySegment) ObjC.MSG_SEND_PTR.invokeExact(keyEquivObj, utf8StringSel);
                                if (!cStrPtr.equals(MemorySegment.NULL)) {
                                    final String keyEquiv = cStrPtr.reinterpret(Long.MAX_VALUE).getString(0);
                                    if ("q".equalsIgnoreCase(keyEquiv)) {
                                        continue;
                                    }
                                }
                            }

                            // Clear the string shortcut
                            ObjC.MSG_SEND_VOID_PTR.invokeExact(subMenuItem, setKeyEquivSel, emptyString);
                        }
                    }
                }
            }
        } catch (final Throwable t) {
            warning("MacMenuShortcutDisabler: Failed to strip shortcuts: " + t.getMessage());
        }
    }
}

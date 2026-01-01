/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

public final class SqueakLanguageOptions {
    public static final String CODE_FLAG = "--code";
    public static final String CODE_FLAG_SHORT = "-c";
    public static final String CODE_HELP = "Smalltalk code to be executed without display";
    public static final String CONTEXT_STACK_DEPTH = "context-stack-depth";
    public static final String CONTEXT_STACK_DEPTH_FLAG = "--" + CONTEXT_STACK_DEPTH;
    public static final String CONTEXT_STACK_DEPTH_HELP = "Flush context stack when it reaches this depth (0 = disabled)";
    public static final String HEADLESS = "headless";
    public static final String HEADLESS_FLAG = "--" + HEADLESS;
    public static final String HEADLESS_HELP = "Run without a display";
    public static final String IMAGE_ARGUMENTS = "image-arguments";
    public static final String IMAGE_ARGUMENTS_FLAG = "--" + IMAGE_ARGUMENTS;
    public static final String IMAGE_ARGUMENTS_HELP = "Comma-separated list of image arguments";
    public static final String IMAGE_PATH = "image-path";
    public static final String IMAGE_PATH_FLAG = "--" + IMAGE_PATH;
    public static final String IMAGE_PATH_HELP = "Path to image";
    public static final String INTERCEPT_MESSAGES = "intercept-messages";
    public static final String INTERCEPT_MESSAGES_HELP = "Comma-separated list of messages to intercept with an instrument";
    public static final String INTERRUPTS = "disable-interrupts";
    public static final String INTERRUPTS_FLAG = "--" + INTERRUPTS;
    public static final String INTERRUPTS_HELP = "Disable interrupt handler";
    public static final String PRINT_IMAGE_PATH_FLAG = "--print-image-path";
    public static final String PRINT_IMAGE_PATH_HELP = "Print the path to default Squeak/Smalltalk image";
    public static final String QUIET = "quiet";
    public static final String QUIET_FLAG = "--" + QUIET;
    public static final String QUIET_HELP = "Operate quietly";
    public static final String RESOURCE_SUMMARY = "resource-summary";
    public static final String RESOURCE_SUMMARY_FLAG = "--" + RESOURCE_SUMMARY;
    public static final String RESOURCE_SUMMARY_HELP = "Print resource summary on context exit";
    public static final String SIGNAL_INPUT_SEMAPHORE = "signal-input-semaphore";
    public static final String SIGNAL_INPUT_SEMAPHORE_HELP = "Signal the input semaphore";
    public static final String STARTUP = "disable-startup";
    public static final String STARTUP_HELP = "Disable image startup routine in headless mode";
    public static final String TESTING = "testing";
    public static final String TESTING_HELP = "For internal testing purposes only";
    public static final String TRANSCRIPT_FORWARDING_FLAG = "--enable-transcript-forwarding";
    public static final String TRANSCRIPT_FORWARDING_HELP = "Forward stdio to Smalltalk transcript";

    private SqueakLanguageOptions() {
    }
}

/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.shared;

public final class SqueakLanguageOptions {
    public static final String CODE_FLAG = "--code";
    public static final String CODE_FLAG_SHORT = "-c";
    public static final String CODE_HELP = "Smalltalk code to be executed without display";
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
    public static final String LOG_HANDLER_FLAG = "--log-handler";
    public static final String LOG_HANDLER_HELP = "Enable log handler (supported modes are 'mapped', 'file', 'err', 'out')";
    public static final String QUIET = "quiet";
    public static final String QUIET_FLAG = "--" + QUIET;
    public static final String QUIET_HELP = "Operate quietly";
    public static final String SIGNAL_INPUT_SEMAPHORE = "signal-input-semaphore";
    public static final String SIGNAL_INPUT_SEMAPHORE_HELP = "Signal the input semaphore";
    public static final String STACK_DEPTH_PROTECTION = "stack-depth-protection";
    public static final String STACK_DEPTH_PROTECTION_FLAG = "--" + STACK_DEPTH_PROTECTION;
    public static final String STACK_DEPTH_PROTECTION_HELP = "Enable stack depth protection";
    public static final String STORAGE_STRATEGIES = "storage-strategies";
    public static final String STORAGE_STRATEGIES_HELP = "Disable storage strategy optimization for arrays";
    public static final String TESTING = "testing";
    public static final String TESTING_HELP = "For internal testing purposes only";
    public static final String TRANSCRIPT_FORWARDING_FLAG = "--enable-transcript-forwarding";
    public static final String TRANSCRIPT_FORWARDING_HELP = "Forward stdio to Smalltalk transcript";

    private SqueakLanguageOptions() {
    }
}

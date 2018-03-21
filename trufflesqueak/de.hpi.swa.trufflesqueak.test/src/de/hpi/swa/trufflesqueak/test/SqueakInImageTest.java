package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.TEST_RESULT;
import de.hpi.swa.trufflesqueak.model.PointersObject;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakInImageTest extends AbstractSqueakTestCase {
    private static final String IMAGE_PATH = getImagesPathName() + File.separator + "test.image";
    private static Object smalltalkDictionary;
    private static Object smalltalkAssociation;
    private static Object evaluateSymbol;
    private static Object compilerSymbol;

    private static final class TEST_TYPE {
        private static final String PASSING = "Passing"; // should pass
        private static final String FAILING = "Failing"; // some/all test selectors fail/error
        private static final String NOT_TERMINATING = "Not Terminating"; // does not terminate
        private static final String BROKEN_IN_SQUEAK = "Broken in Squeak"; // not working in Squeak
        private static final String REQUIRES_STARTUP = "Requires Startup"; // requires the image to be entirely started (e.g. initialize display, ...)
        private static final String FLAKY = "Flaky"; // flaky tests
        private static final String IGNORE = "Ignored"; // unable to run (e.g. OOM, ...)
    }

    private static final Object[] squeakTests = new Object[]{"AddPrefixNamePolicyTest", TEST_TYPE.PASSING,
                    "AliasTest", TEST_TYPE.PASSING,
                    "AllNamePolicyTest", TEST_TYPE.PASSING,
                    "AllocationTest", TEST_TYPE.IGNORE,
                    "ArbitraryObjectSocketTestCase", TEST_TYPE.IGNORE,
                    "ArrayLiteralTest", TEST_TYPE.PASSING,
                    "ArrayTest", TEST_TYPE.PASSING,
                    "Ascii85ConverterTest", TEST_TYPE.PASSING,
                    "AssociationTest", TEST_TYPE.PASSING,
                    "BagTest", TEST_TYPE.PASSING,
                    "BalloonFontTest", TEST_TYPE.PASSING,
                    "Base64MimeConverterTest", TEST_TYPE.PASSING,
                    "BasicBehaviorClassMetaclassTest", TEST_TYPE.PASSING,
                    "BasicTypeTest", TEST_TYPE.PASSING,
                    "BecomeTest", TEST_TYPE.PASSING,
                    "BehaviorTest", TEST_TYPE.FAILING,
                    "BindingPolicyTest", TEST_TYPE.PASSING,
                    "BitBltClipBugs", TEST_TYPE.PASSING,
                    "BitBltSimulationTest", TEST_TYPE.PASSING,
                    "BitBltTest", TEST_TYPE.NOT_TERMINATING,
                    "BitmapBugz", TEST_TYPE.PASSING,
                    "BitmapStreamTests", TEST_TYPE.IGNORE, // OOM error
                    "BitSetTest", TEST_TYPE.PASSING,
                    "BlockClosureTest", TEST_TYPE.IGNORE, // testRunSimulated does not work headless, testSourceString requires sources
                    "BlockLocalTemporariesRemovalTest", TEST_TYPE.PASSING,
                    "BMPReadWriterTest", TEST_TYPE.FAILING,
                    "BooleanTest", TEST_TYPE.PASSING,
                    "BrowserHierarchicalListTest", TEST_TYPE.PASSING,
                    "BrowserTest", TEST_TYPE.FAILING,
                    "BrowseTest", TEST_TYPE.PASSING,
                    "ByteArrayTest", TEST_TYPE.PASSING,
                    "BytecodeDecodingTests", TEST_TYPE.NOT_TERMINATING,
                    "ByteEncoderTest", TEST_TYPE.PASSING,
                    "CategorizerTest", TEST_TYPE.PASSING,
                    "ChainedSortFunctionTest", TEST_TYPE.PASSING,
                    "ChangeHooksTest", TEST_TYPE.NOT_TERMINATING,
                    "ChangeSetClassChangesTest", TEST_TYPE.NOT_TERMINATING,
                    "CharacterScannerTest", TEST_TYPE.PASSING,
                    "CharacterSetComplementTest", TEST_TYPE.PASSING,
                    "CharacterSetTest", TEST_TYPE.PASSING,
                    "CharacterTest", TEST_TYPE.PASSING,
                    "CircleMorphBugs", TEST_TYPE.PASSING,
                    "CircleMorphTest", TEST_TYPE.PASSING,
                    "ClassAPIHelpBuilderTest", TEST_TYPE.PASSING,
                    "ClassBindingTest", TEST_TYPE.PASSING,
                    "ClassBuilderTest", TEST_TYPE.NOT_TERMINATING,
                    "ClassDescriptionTest", TEST_TYPE.PASSING,
                    "ClassFactoryForTestCaseTest", TEST_TYPE.IGNORE,
                    "ClassRemovalTest", TEST_TYPE.PASSING,
                    "ClassRenameFixTest", TEST_TYPE.FAILING,
                    "ClassTest", TEST_TYPE.NOT_TERMINATING, // fails, but is very slow
                    "ClassTraitTest", TEST_TYPE.PASSING,
                    "ClassVarScopeTest", TEST_TYPE.NOT_TERMINATING, // passes, but is very slow
                    "ClipboardTest", TEST_TYPE.PASSING,
                    "ClosureCompilerTest", TEST_TYPE.NOT_TERMINATING, // requires sources
                    "ClosureTests", TEST_TYPE.PASSING,
                    "CogVMBaseImageTests", TEST_TYPE.FAILING,
                    "CollectionTest", TEST_TYPE.PASSING,
                    "ColorTest", TEST_TYPE.PASSING,
                    "CompiledMethodComparisonTest", TEST_TYPE.NOT_TERMINATING,
                    "CompiledMethodTest", TEST_TYPE.PASSING,
                    "CompiledMethodTrailerTest", TEST_TYPE.FAILING,
                    "CompilerExceptionsTest", TEST_TYPE.PASSING,
                    "CompilerNotifyingTest", TEST_TYPE.FAILING,
                    "CompilerSyntaxErrorNotifyingTest", TEST_TYPE.FAILING,
                    "CompilerTest", TEST_TYPE.PASSING,
                    "ComplexTest", TEST_TYPE.PASSING,
                    "ContextCompilationTest", TEST_TYPE.PASSING,
                    "DataStreamTest", TEST_TYPE.FLAKY,
                    "DateAndTimeEpochTest", TEST_TYPE.PASSING,
                    "DateAndTimeLeapTest", TEST_TYPE.BROKEN_IN_SQUEAK,
                    "DateAndTimeTest", TEST_TYPE.NOT_TERMINATING, // does not terminate sometimes (side effects?)
                    "DateTest", TEST_TYPE.PASSING,
                    "DebuggerExtensionsTest", TEST_TYPE.FAILING,
                    "DebuggerUnwindBug", TEST_TYPE.FAILING,
                    "DecompilerTests", TEST_TYPE.NOT_TERMINATING,
                    "DelayTest", TEST_TYPE.FAILING,
                    "DependencyBrowserTest", TEST_TYPE.IGNORE,
                    "DependentsArrayTest", TEST_TYPE.PASSING,
                    "DictionaryTest", TEST_TYPE.FAILING,
                    "DosFileDirectoryTests", TEST_TYPE.PASSING,
                    "DoubleByteArrayTest", TEST_TYPE.FLAKY, // passes sometimes, one failure in Squeak (BROKEN_IN_SQUEAK)
                    "DoubleWordArrayTest", TEST_TYPE.FLAKY, // two errors in Squeak (BROKEN_IN_SQUEAK)
                    "DurationTest", TEST_TYPE.PASSING,
                    "EnvironmentTest", TEST_TYPE.FAILING,
                    "EPSCanvasTest", TEST_TYPE.NOT_TERMINATING,
                    "EtoysStringExtensionTest", TEST_TYPE.PASSING,
                    "EventManagerTest", TEST_TYPE.PASSING,
                    "ExceptionTests", TEST_TYPE.FAILING,
                    "ExpandedSourceFileArrayTest", TEST_TYPE.PASSING,
                    "ExplicitNamePolicyTest", TEST_TYPE.PASSING,
                    "ExtendedNumberParserTest", TEST_TYPE.PASSING,
                    "FalseTest", TEST_TYPE.PASSING,
                    "FileContentsBrowserTest", TEST_TYPE.NOT_TERMINATING,
                    "FileDirectoryTest", TEST_TYPE.PASSING,
                    "FileList2ModalDialogsTest", TEST_TYPE.FAILING,
                    "FileListTest", TEST_TYPE.IGNORE,
                    "FileStreamTest", TEST_TYPE.PASSING,
                    "FileUrlTest", TEST_TYPE.PASSING,
                    "FlapTabTests", TEST_TYPE.FAILING,
                    "FloatArrayTest", TEST_TYPE.PASSING,
                    "FloatCollectionTest", TEST_TYPE.PASSING,
                    "FloatTest", TEST_TYPE.FAILING,
                    "FontTest", TEST_TYPE.FAILING,
                    "FormCanvasTest", TEST_TYPE.FAILING,
                    "FormTest", TEST_TYPE.PASSING,
                    "FractionTest", TEST_TYPE.PASSING,
                    "GeneratorTest", TEST_TYPE.PASSING,
                    "GenericUrlTest", TEST_TYPE.PASSING,
                    "GlobalTest", TEST_TYPE.PASSING,
                    "GradientFillStyleTest", TEST_TYPE.PASSING,
                    "HandBugs", TEST_TYPE.PASSING,
                    "HashAndEqualsTestCase", TEST_TYPE.PASSING,
                    "HashedCollectionTest", TEST_TYPE.PASSING,
                    "HashTesterTest", TEST_TYPE.PASSING,
                    "HeapTest", TEST_TYPE.PASSING,
                    "HelpBrowserTest", TEST_TYPE.PASSING,
                    "HelpIconsTest", TEST_TYPE.PASSING,
                    "HelpTopicListItemWrapperTest", TEST_TYPE.PASSING,
                    "HelpTopicTest", TEST_TYPE.PASSING,
                    "HexTest", TEST_TYPE.PASSING,
                    "HierarchicalUrlTest", TEST_TYPE.PASSING,
                    "HierarchyBrowserTest", TEST_TYPE.PASSING,
                    "HtmlReadWriterTest", TEST_TYPE.PASSING,
                    "HttpUrlTest", TEST_TYPE.PASSING,
                    "IdentityBagTest", TEST_TYPE.PASSING,
                    "InstallerTest", TEST_TYPE.NOT_TERMINATING,
                    "InstallerUrlTest", TEST_TYPE.PASSING,
                    "InstructionClientTest", TEST_TYPE.PASSING,
                    "InstructionPrinterTest", TEST_TYPE.PASSING,
                    "InstVarRefLocatorTest", TEST_TYPE.PASSING,
                    "IntegerArrayTest", TEST_TYPE.PASSING,
                    "IntegerDigitLogicTest", TEST_TYPE.PASSING,
                    "IntegerTest", TEST_TYPE.PASSING,
                    "IntervalTest", TEST_TYPE.PASSING,
                    "IslandVMTweaksTestCase", TEST_TYPE.FAILING,
                    "JPEGReadWriter2Test", TEST_TYPE.FAILING,
                    "KeyedSetTest", TEST_TYPE.PASSING,
                    "LangEnvBugs", TEST_TYPE.FAILING,
                    "LargeNegativeIntegerTest", TEST_TYPE.PASSING,
                    "LargePositiveIntegerTest", TEST_TYPE.PASSING,
                    "LayoutFrameTest", TEST_TYPE.PASSING,
                    "LinkedListTest", TEST_TYPE.PASSING,
                    "LocaleTest", TEST_TYPE.FAILING,
                    "LongTestCaseTest", TEST_TYPE.PASSING,
                    "LongTestCaseTestUnderTest", TEST_TYPE.PASSING,
                    "MacFileDirectoryTest", TEST_TYPE.PASSING,
                    "MailAddressParserTest", TEST_TYPE.FAILING,
                    "MailDateAndTimeTest", TEST_TYPE.PASSING,
                    "MailMessageTest", TEST_TYPE.FAILING,
                    "MatrixTest", TEST_TYPE.PASSING,
                    "MCAncestryTest", TEST_TYPE.NOT_TERMINATING,
                    "MCChangeNotificationTest", TEST_TYPE.NOT_TERMINATING,
                    "MCClassDefinitionTest", TEST_TYPE.NOT_TERMINATING,
                    "MCDependencySorterTest", TEST_TYPE.PASSING,
                    "MCDictionaryRepositoryTest", TEST_TYPE.NOT_TERMINATING,
                    "MCDirectoryRepositoryTest", TEST_TYPE.NOT_TERMINATING,
                    "MCEnvironmentLoadTest", TEST_TYPE.NOT_TERMINATING,
                    "MCFileInTest", TEST_TYPE.NOT_TERMINATING,
                    "MCInitializationTest", TEST_TYPE.NOT_TERMINATING,
                    "MCMcmUpdaterTest", TEST_TYPE.PASSING,
                    "MCMczInstallerTest", TEST_TYPE.NOT_TERMINATING,
                    "MCMergingTest", TEST_TYPE.IGNORE,
                    "MCMethodDefinitionTest", TEST_TYPE.IGNORE,
                    "MCOrganizationTest", TEST_TYPE.IGNORE,
                    "MCPackageTest", TEST_TYPE.IGNORE,
                    "MCPatchTest", TEST_TYPE.IGNORE,
                    "MCPTest", TEST_TYPE.PASSING,
                    "MCScannerTest", TEST_TYPE.IGNORE,
                    "MCSerializationTest", TEST_TYPE.IGNORE,
                    "MCSnapshotBrowserTest", TEST_TYPE.IGNORE,
                    "MCSnapshotTest", TEST_TYPE.IGNORE,
                    "MCSortingTest", TEST_TYPE.PASSING,
                    "MCStReaderTest", TEST_TYPE.IGNORE,
                    "MCStWriterTest", TEST_TYPE.IGNORE,
                    "MCVersionNameTest", TEST_TYPE.IGNORE,
                    "MCVersionTest", TEST_TYPE.IGNORE,
                    "MCWorkingCopyRenameTest", TEST_TYPE.IGNORE,
                    "MCWorkingCopyTest", TEST_TYPE.IGNORE,
                    "MessageNamesTest", TEST_TYPE.FAILING,
                    "MessageSendTest", TEST_TYPE.PASSING,
                    "MessageSetTest", TEST_TYPE.FAILING,
                    "MessageTraceTest", TEST_TYPE.FAILING,
                    "MethodContextTest", TEST_TYPE.FAILING, // testRestart uses #should:notTakeMoreThan:
                    "MethodHighlightingTests", TEST_TYPE.PASSING,
                    "MethodPragmaTest", TEST_TYPE.FAILING,
                    "MethodPropertiesTest", TEST_TYPE.PASSING,
                    "MethodReferenceTest", TEST_TYPE.PASSING,
                    "MIMEDocumentTest", TEST_TYPE.FLAKY,
                    "MirrorPrimitiveTests", TEST_TYPE.FAILING,
                    "MiscPrimitivePluginTest", TEST_TYPE.FAILING, // failing in Squeak
                    "MonitorTest", TEST_TYPE.FAILING,
                    "MonthTest", TEST_TYPE.PASSING,
                    "MorphBugs", TEST_TYPE.PASSING,
                    "MorphicEventDispatcherTests", TEST_TYPE.FAILING,
                    "MorphicEventFilterTests", TEST_TYPE.FAILING,
                    "MorphicEventTests", TEST_TYPE.FAILING,
                    "MorphicExtrasSymbolExtensionsTest", TEST_TYPE.PASSING,
                    "MorphicToolBuilderTests", TEST_TYPE.FAILING,
                    "MorphicUIManagerTest", TEST_TYPE.FAILING,
                    "MorphTest", TEST_TYPE.PASSING,
                    "MultiByteFileStreamTest", TEST_TYPE.IGNORE,
                    "MVCToolBuilderTests", TEST_TYPE.NOT_TERMINATING,
                    "NamePolicyTest", TEST_TYPE.PASSING,
                    "NumberParsingTest", TEST_TYPE.PASSING,
                    "NumberTest", TEST_TYPE.PASSING,
                    "ObjectFinalizerTests", TEST_TYPE.FAILING,
                    "ObjectTest", TEST_TYPE.FAILING,
                    "OrderedCollectionInspectorTest", TEST_TYPE.PASSING,
                    "OrderedCollectionTest", TEST_TYPE.PASSING,
                    "OrderedDictionaryTest", TEST_TYPE.PASSING,
                    "PackageDependencyTest", TEST_TYPE.NOT_TERMINATING,
                    "PackagePaneBrowserTest", TEST_TYPE.PASSING,
                    "ParserEditingTest", TEST_TYPE.PASSING,
                    "PasteUpMorphTest", TEST_TYPE.PASSING,
                    "PCCByCompilationTest", TEST_TYPE.IGNORE,
                    "PCCByLiteralsTest", TEST_TYPE.IGNORE,
                    "PluggableMenuItemSpecTests", TEST_TYPE.PASSING,
                    "PluggableTextMorphTest", TEST_TYPE.PASSING,
                    "PNGReadWriterTest", TEST_TYPE.NOT_TERMINATING,
                    "PointTest", TEST_TYPE.PASSING,
                    "PolygonMorphTest", TEST_TYPE.PASSING,
                    "PreferencesTest", TEST_TYPE.REQUIRES_STARTUP,
                    "ProcessSpecificTest", TEST_TYPE.FAILING,
                    "ProcessTerminateBug", TEST_TYPE.FAILING,
                    "ProcessTest", TEST_TYPE.FAILING,
                    "PromiseTest", TEST_TYPE.FAILING,
                    "ProtoObjectTest", TEST_TYPE.PASSING,
                    "PureBehaviorTest", TEST_TYPE.NOT_TERMINATING,
                    "RandomTest", TEST_TYPE.NOT_TERMINATING,
                    "ReadStreamTest", TEST_TYPE.PASSING,
                    "ReadWriteStreamTest", TEST_TYPE.PASSING,
                    "RecentMessagesTest", TEST_TYPE.PASSING,
                    "RectangleTest", TEST_TYPE.PASSING,
                    "ReferenceStreamTest", TEST_TYPE.FLAKY,
                    "ReleaseTest", TEST_TYPE.NOT_TERMINATING,
                    "RemoteStringTest", TEST_TYPE.PASSING,
                    "RemovePrefixNamePolicyTest", TEST_TYPE.PASSING,
                    "RenderBugz", TEST_TYPE.FAILING,
                    "ResumableTestFailureTestCase", TEST_TYPE.PASSING,
                    "RunArrayTest", TEST_TYPE.PASSING,
                    "RWBinaryOrTextStreamTest", TEST_TYPE.FAILING,
                    "RxMatcherTest", TEST_TYPE.PASSING,
                    "RxParserTest", TEST_TYPE.PASSING,
                    "ScaledDecimalTest", TEST_TYPE.PASSING,
                    "ScannerTest", TEST_TYPE.PASSING,
                    "ScheduleTest", TEST_TYPE.PASSING,
                    "ScrollBarTest", TEST_TYPE.FAILING,
                    "ScrollPaneLeftBarTest", TEST_TYPE.PASSING,
                    "ScrollPaneRetractableBarsTest", TEST_TYPE.PASSING,
                    "ScrollPaneTest", TEST_TYPE.PASSING,
                    "SecureHashAlgorithmTest", TEST_TYPE.PASSING,
                    "SemaphoreTest", TEST_TYPE.FAILING,
                    "SequenceableCollectionTest", TEST_TYPE.PASSING,
                    "SetTest", TEST_TYPE.PASSING,
                    "SetWithNilTest", TEST_TYPE.FAILING,
                    "SharedQueue2Test", TEST_TYPE.FAILING,
                    "SHParserST80Test", TEST_TYPE.BROKEN_IN_SQUEAK,
                    "SimpleSwitchMorphTest", TEST_TYPE.PASSING,
                    "SimpleTestResourceTestCase", TEST_TYPE.PASSING,
                    "SliderTest", TEST_TYPE.PASSING,
                    "SmallIntegerTest", TEST_TYPE.PASSING,
                    "SmalltalkImageTest", TEST_TYPE.PASSING,
                    "SmartRefStreamTest", TEST_TYPE.NOT_TERMINATING,
                    "SMDependencyTest", TEST_TYPE.PASSING,
                    "SMTPClientTest", TEST_TYPE.IGNORE,
                    "SocketStreamTest", TEST_TYPE.FAILING,
                    "SocketTest", TEST_TYPE.FAILING,
                    "SortedCollectionTest", TEST_TYPE.PASSING,
                    "SortFunctionTest", TEST_TYPE.PASSING,
                    "SqNumberParserTest", TEST_TYPE.PASSING,
                    "SqueakSSLTest", TEST_TYPE.FAILING,
                    "ST80MenusTest", TEST_TYPE.PASSING,
                    "ST80PackageDependencyTest", TEST_TYPE.BROKEN_IN_SQUEAK,
                    "StackTest", TEST_TYPE.PASSING,
                    "StandardSourceFileArrayTest", TEST_TYPE.PASSING,
                    "StickynessBugz", TEST_TYPE.PASSING,
                    "StopwatchTest", TEST_TYPE.FAILING,
                    "StringSocketTestCase", TEST_TYPE.FAILING,
                    "StringTest", TEST_TYPE.FAILING,
                    "SumBugs", TEST_TYPE.PASSING,
                    "SUnitExtensionsTest", TEST_TYPE.FAILING,
                    "SUnitTest", TEST_TYPE.FAILING,
                    "SUnitToolBuilderTests", TEST_TYPE.NOT_TERMINATING,
                    "SymbolTest", TEST_TYPE.PASSING,
                    "SystemChangeErrorHandling", TEST_TYPE.FLAKY,
                    "SystemChangeFileTest", TEST_TYPE.IGNORE,
                    "SystemChangeNotifierTest", TEST_TYPE.FLAKY,
                    "SystemChangeTestRoot", TEST_TYPE.PASSING,
                    "SystemDictionaryTest", TEST_TYPE.PASSING,
                    "SystemNavigationTest", TEST_TYPE.PASSING,
                    "SystemOrganizerTest", TEST_TYPE.PASSING,
                    "SystemVersionTest", TEST_TYPE.PASSING,
                    "TestIndenting", TEST_TYPE.PASSING,
                    "TestNewParagraphFix", TEST_TYPE.PASSING,
                    "TestObjectsAsMethods", TEST_TYPE.PASSING,
                    "TestParagraphFix", TEST_TYPE.PASSING,
                    "TestSpaceshipOperator", TEST_TYPE.PASSING,
                    "TestURI", TEST_TYPE.PASSING,
                    "TestValueWithinFix", TEST_TYPE.NOT_TERMINATING,
                    "TestVMStatistics", TEST_TYPE.PASSING,
                    "TextAlignmentTest", TEST_TYPE.PASSING,
                    "TextAnchorTest", TEST_TYPE.PASSING,
                    "TextAndTextStreamTest", TEST_TYPE.PASSING,
                    "TextAttributesScanningTest", TEST_TYPE.FAILING,
                    "TextDiffBuilderTest", TEST_TYPE.PASSING,
                    "TextEditorTest", TEST_TYPE.PASSING,
                    "TextEmphasisTest", TEST_TYPE.PASSING,
                    "TextFontChangeTest", TEST_TYPE.PASSING,
                    "TextFontReferenceTest", TEST_TYPE.PASSING,
                    "TextKernTest", TEST_TYPE.PASSING,
                    "TextLineEndingsTest", TEST_TYPE.PASSING,
                    "TextLineTest", TEST_TYPE.PASSING,
                    "TextMorphTest", TEST_TYPE.PASSING,
                    "TextStyleTest", TEST_TYPE.PASSING,
                    "TextTest", TEST_TYPE.PASSING,
                    "ThirtyTwoBitRegisterTest", TEST_TYPE.NOT_TERMINATING,
                    "TileMorphTest", TEST_TYPE.PASSING,
                    "TimespanDoSpanAYearTest", TEST_TYPE.PASSING,
                    "TimespanDoTest", TEST_TYPE.PASSING,
                    "TimespanTest", TEST_TYPE.PASSING,
                    "TimeStampTest", TEST_TYPE.PASSING,
                    "TimeTest", TEST_TYPE.PASSING,
                    "TraitCompositionTest", TEST_TYPE.NOT_TERMINATING,
                    "TraitFileOutTest", TEST_TYPE.NOT_TERMINATING,
                    "TraitMethodDescriptionTest", TEST_TYPE.NOT_TERMINATING,
                    "TraitsTestCase", TEST_TYPE.PASSING,
                    "TraitSystemTest", TEST_TYPE.PASSING,
                    "TraitTest", TEST_TYPE.PASSING,
                    "TrueTest", TEST_TYPE.PASSING,
                    "UndefinedObjectTest", TEST_TYPE.PASSING,
                    "UnderscoreSelectorsTest", TEST_TYPE.PASSING,
                    "UnimplementedCallBugz", TEST_TYPE.PASSING,
                    "UnixFileDirectoryTests", TEST_TYPE.PASSING,
                    "UrlTest", TEST_TYPE.PASSING,
                    "UserInterfaceThemeTest", TEST_TYPE.NOT_TERMINATING,
                    "UTF16TextConverterTest", TEST_TYPE.BROKEN_IN_SQUEAK,
                    "UTF32TextConverterTest", TEST_TYPE.FAILING,
                    "UTF8TextConverterTest", TEST_TYPE.FAILING,
                    "UTF8EdgeCaseTest", TEST_TYPE.FAILING, // failing in Squeak
                    "UUIDPrimitivesTest", TEST_TYPE.PASSING,
                    "UUIDTest", TEST_TYPE.PASSING,
                    "VersionNumberTest", TEST_TYPE.PASSING,
                    "WeakFinalizersTest", TEST_TYPE.PASSING,
                    "WeakIdentityKeyDictionaryTest", TEST_TYPE.PASSING,
                    "WeakMessageSendTest", TEST_TYPE.FAILING,
                    "WeakRegistryTest", TEST_TYPE.FAILING,
                    "WeakSetInspectorTest", TEST_TYPE.PASSING,
                    "WeakSetTest", TEST_TYPE.FAILING,
                    "WebClientServerTest", TEST_TYPE.FAILING,
                    "WeekTest", TEST_TYPE.PASSING,
                    "WideCharacterSetTest", TEST_TYPE.FAILING,
                    "WideStringTest", TEST_TYPE.NOT_TERMINATING,
                    "Win32VMTest", TEST_TYPE.PASSING,
                    "WordArrayTest", TEST_TYPE.PASSING,
                    "WorldStateTest", TEST_TYPE.NOT_TERMINATING,
                    "WriteStreamTest", TEST_TYPE.FAILING,
                    "XMLParserTest", TEST_TYPE.PASSING,
                    "YearMonthWeekTest", TEST_TYPE.PASSING,
                    "YearTest", TEST_TYPE.PASSING,};

    @Test
    public void test1AsSymbol() {
        assertEquals(image.asSymbol, asSymbol("asSymbol"));
    }

    @Test
    public void test2Numerical() {
        // Evaluate a few simple expressions to ensure that methodDictionaries grow correctly.
        for (long i = 0; i < 10; i++) {
            assertEquals(i + 1, evaluate(i + " + 1"));
        }
        assertEquals(4L, evaluate("-1 \\\\ 5"));
        assertEquals(LargeIntegerObject.SMALLINTEGER32_MIN, evaluate("SmallInteger minVal"));
        assertEquals(LargeIntegerObject.SMALLINTEGER32_MAX, evaluate("SmallInteger maxVal"));
        assertEquals("9223372036854775808", evaluate("-9223372036854775808 / -1").toString()); // Long.MIN_VALUE / -1
    }

    @Test
    public void test3ThisContext() {
        assertEquals(42L, evaluate("thisContext return: 42"));
    }

    @Test
    public void test4Ensure() {
        assertEquals(21L, evaluate("[21] ensure: [42]"));
        assertEquals(42L, evaluate("[21] ensure: [^42]"));
        assertEquals(21L, evaluate("[^21] ensure: [42]"));
        assertEquals(42L, evaluate("[^21] ensure: [^42]"));
    }

    @Test
    public void test5OnError() {
        Object result = evaluate("[self error: 'foobar'] on: Error do: [:err| ^ err messageText]");
        assertEquals("foobar", result.toString());
        assertEquals("foobar", evaluate("[[self error: 'foobar'] value] on: Error do: [:err| ^ err messageText]").toString());
        assertEquals(image.sqTrue, evaluate("[[self error: 'foobar'] on: ZeroDivide do: [:e|]] on: Error do: [:err| ^ true]"));
        assertEquals(image.sqTrue, evaluate("[self error: 'foobar'. false] on: Error do: [:err| ^ err return: true]"));
    }

    @Test
    public void test6Value() {
        assertEquals(42L, evaluate("[42] value"));
        assertEquals(21L, evaluate("[[21] value] value"));
    }

    @Test
    public void test7SUnitTest() {
        assertEquals(image.sqTrue, evaluate("(TestCase new should: [1/0] raise: ZeroDivide) isKindOf: TestCase"));
    }

    @Test
    public void test8MethodContextRestart() {
        // MethodContextTest>>testRestart uses #should:notTakeMoreThan: which requires process switching
        assertEquals(image.sqTrue, evaluate("[MethodContextTest new privRestartTest. true] value"));
    }

    @Test
    public void test9TinyBenchmarks() {
        String resultString = evaluate("1 tinyBenchmarks").toString();
        assertTrue(resultString.contains("bytecodes/sec"));
        assertTrue(resultString.contains("sends/sec"));
    }

    @Ignore
    @Test
    public void testInspectSqueakTest() {
        runTestCase("ByteArrayTest");
    }

    @Ignore
    @Test
    public void testInspectSqueakTestSelector() {
        image.getOutput().println(evaluate("(WordArrayTest run: #testCannotPutNegativeValue) asString"));
    }

    @Test
    public void testWPassingSqueakTests() {
        List<String> failing = new ArrayList<>();
        String[] testClasses = getSqueakTests(TEST_TYPE.PASSING);
        printHeader(TEST_TYPE.PASSING, testClasses);
        for (int i = 0; i < testClasses.length; i++) {
            String result = runTestCase(testClasses[i]);
            if (!result.contains("passed")) {
                failing.add(result);
            }
        }
        failIfNotEmpty(failing);
    }

    @Test
    public void testXFlakySqueakTests() {
        String[] testClasses = getSqueakTests(TEST_TYPE.FLAKY);
        printHeader(TEST_TYPE.FLAKY, testClasses);
        for (int i = 0; i < testClasses.length; i++) {
            runTestCase(testClasses[i]);
        }
    }

    @Test
    public void testYFailingSqueakTests() {
        testAndFailOnPassing(TEST_TYPE.FAILING);
    }

    @Ignore
    @Test
    public void testZNotTerminatingSqueakTests() {
        int timeoutSeconds = 15;
        List<String> passing = new ArrayList<>();
        String[] testClasses = getSqueakTests(TEST_TYPE.NOT_TERMINATING);
        printHeader(TEST_TYPE.NOT_TERMINATING, testClasses);
        for (int i = 0; i < testClasses.length; i++) {
            String testClass = testClasses[i];
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    passing.add(runTestCase(testClass));
                }
            });
            thread.start();
            long endTimeMillis = System.currentTimeMillis() + timeoutSeconds * 1000;
            while (thread.isAlive()) {
                if (System.currentTimeMillis() > endTimeMillis) {
                    image.getOutput().println("did not terminate in time");
                    thread.interrupt();
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException t) {
                }
            }

        }
        failIfNotEmpty(passing);
    }

    @BeforeClass
    public static void loadTestImage() {
        image = new SqueakImageContext(IMAGE_PATH);
        image.getOutput().println();
        image.getOutput().println("Loading test image at " + IMAGE_PATH + "...");
        try {
            image.fillInFrom(new FileInputStream(IMAGE_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        patchImageForTesting();
    }

    private static void patchImageForTesting() {
        /*
         * Set author initials and disable timeout logic by patching TestCase>>#timeout:after: (uses
         * processes -> incompatible to running headless).
         */
        System.out.println("Modifying StartUpList for testing...");
        evaluate("{EventSensor. Project} do: [:ea | Smalltalk removeFromStartUpList: ea]");
        System.out.println("Processing StartUpList...");
        evaluate("Smalltalk processStartUpList: true");
        System.out.println("Setting author initials...");
        evaluate("Utilities setAuthorInitials: 'TruffleSqueak'"); // needs to happen after `FileDirectory startUp`.
        System.out.println("Patching timeout methods...");
        Object patchResult = evaluate(
                        "TestCase addSelectorSilently: #timeout:after: withMethod: (TestCase compile: 'timeout: aBlock after: seconds ^ aBlock value' notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method");
        assertNotEquals(image.nil, patchResult);
        patchResult = evaluate(
                        "BlockClosure addSelectorSilently: #valueWithin:onTimeout: withMethod: (BlockClosure compile: 'valueWithin: aDuration onTimeout: timeoutBlock ^ self value' notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method");
        assertNotEquals(image.nil, patchResult);
    }

    private static String getImagesPathName() {
        return System.getenv("TRUFFLESQUEAK_ROOT") + File.separator + "images";
    }

    private static Object getSmalltalkDictionary() {
        if (smalltalkDictionary == null) {
            smalltalkDictionary = image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SmalltalkDictionary);
        }
        return smalltalkDictionary;
    }

    private static Object getSmalltalkAssociation() {
        if (smalltalkAssociation == null) {
            smalltalkAssociation = new PointersObject(image, image.schedulerAssociation.getSqClass(), new Object[]{image.newSymbol("Smalltalk"), getSmalltalkDictionary()});
        }
        return smalltalkAssociation;
    }

    private static Object getEvaluateSymbol() {
        if (evaluateSymbol == null) {
            evaluateSymbol = asSymbol("evaluate:");
        }
        return evaluateSymbol;
    }

    private static Object getCompilerSymbol() {
        if (compilerSymbol == null) {
            compilerSymbol = asSymbol("Compiler");
        }
        return compilerSymbol;
    }

    private static Object asSymbol(String value) {
        String fakeMethodName = "fakeAsSymbol" + value.hashCode();
        CompiledCodeObject method = makeMethod(
                        new Object[]{4L, image.asSymbol, image.wrap(value), image.newSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x21, 0xD0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    private static Object evaluate(String expression) {
        // ^ (Smalltalk at: #Compiler) evaluate: '{expression}'
        String fakeMethodName = "fakeEvaluate" + expression.hashCode();
        CompiledCodeObject method = makeMethod(
                        new Object[]{6L, getEvaluateSymbol(), getSmalltalkAssociation(), getCompilerSymbol(), image.wrap(expression), asSymbol(fakeMethodName), getSmalltalkAssociation()},
                        new int[]{0x41, 0x22, 0xC0, 0x23, 0xE0, 0x7C});
        return runMethod(method, getSmalltalkDictionary());
    }

    private static String[] getSqueakTests(String type) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < squeakTests.length; i += 2) {
            if (squeakTests[i + 1].equals(type)) {
                result.add((String) squeakTests[i]);
            }
        }
        return result.toArray(new String[0]);
    }

    private static String runTestCase(String testClassName) {
        image.getOutput().print(testClassName + ": ");
        image.getOutput().flush();
        String result;
        try {
            result = extractFailuresAndErrorsFromTestResult(evaluate(testClassName + " buildSuite run"));
        } catch (Exception e) {
            result = "failed with an error: " + e.toString();
        }
        image.getOutput().println(result);
        return testClassName + ": " + result;
    }

    private static void testAndFailOnPassing(String type) {
        List<String> passing = new ArrayList<>();
        String[] testClasses = getSqueakTests(type);
        printHeader(type, testClasses);
        for (int i = 0; i < testClasses.length; i++) {
            String result = runTestCase(testClasses[i]);
            if (result.contains("passed")) {
                passing.add(result);
            }
        }
        failIfNotEmpty(passing);
    }

    private static String extractFailuresAndErrorsFromTestResult(Object result) {
        if (!(result instanceof BaseSqueakObject) || !result.toString().equals("a TestResult")) {
            return "did not return a TestResult, got " + result.toString();
        }
        BaseSqueakObject testResult = (BaseSqueakObject) result;
        List<String> output = new ArrayList<>();
        BaseSqueakObject failureArray = (BaseSqueakObject) ((BaseSqueakObject) testResult.at0(TEST_RESULT.FAILURES)).at0(1);
        for (int i = 0; i < failureArray.size(); i++) {
            BaseSqueakObject value = (BaseSqueakObject) failureArray.at0(i);
            if (value != image.nil) {
                output.add(value.at0(0) + " (E)");
            }
        }
        BaseSqueakObject errorArray = (BaseSqueakObject) ((BaseSqueakObject) testResult.at0(TEST_RESULT.ERRORS)).at0(0);
        for (int i = 0; i < errorArray.size(); i++) {
            BaseSqueakObject value = (BaseSqueakObject) errorArray.at0(i);
            if (value != image.nil) {
                output.add(value.at0(0) + " (F)");
            }
        }
        if (output.size() == 0) {
            return "passed";
        }
        return String.join(", ", output);
    }

    private static void failIfNotEmpty(List<String> list) {
        if (!list.isEmpty()) {
            fail(String.join("\n", list));
        }
    }

    private static void printHeader(String type, String[] testClasses) {
        image.getOutput().println();
        image.getOutput().println(String.format("== %s %s Squeak Tests ====================", testClasses.length, type));
    }
}

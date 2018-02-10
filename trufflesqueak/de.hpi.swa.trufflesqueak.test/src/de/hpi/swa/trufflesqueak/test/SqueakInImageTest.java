package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertNotEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SqueakInImageTest extends AbstractSqueakTestCase {
    private static final String IMAGE_PATH = System.getenv("TRAVIS_BUILD_DIR") + "/images/test.image";
    private static Object smalltalkDictionary;
    private static Object smalltalkAssociation;
    private static Object evaluateSymbol;
    private static Object compilerSymbol;

    private static final Pattern runPattern = Pattern.compile("([0-9]+)\\ run");
    private static final Pattern passesPattern = Pattern.compile("([0-9]+)\\ passes");

    @Test
    public void testAAsSymbol() {
        assertEquals(image.asSymbol, asSymbol("asSymbol"));
    }

    @Test
    public void testBNumerical() {
        // Evaluate a few simple expressions to ensure that methodDictionaries grow correctly.
        for (long i = 0; i < 10; i++) {
            assertEquals(i + 1, evaluate(i + " + 1"));
        }
        assertEquals(4L, evaluate("-1 \\\\ 5"));
    }

    @Test
    public void testCThisContext() {
        assertEquals(42L, evaluate("thisContext return: 42"));
    }

    @Test
    public void testDEnsure() {
        assertEquals(21L, evaluate("[21] ensure: [42]"));
        assertEquals(42L, evaluate("[21] ensure: [^42]"));
        assertEquals(21L, evaluate("[^21] ensure: [42]"));
        assertEquals(42L, evaluate("[^21] ensure: [^42]"));
    }

    @Test
    public void testEOnError() {
        Object result = evaluate("[self error: 'foobar'] on: Error do: [:err| ^ err messageText]");
        assertEquals("foobar", result.toString());
        assertEquals("foobar", evaluate("[[self error: 'foobar'] value] on: Error do: [:err| ^ err messageText]").toString());
        assertEquals(image.sqTrue, evaluate("[[self error: 'foobar'] on: ZeroDivide do: [:e|]] on: Error do: [:err| ^ true]"));
        assertEquals(image.sqTrue, evaluate("[self error: 'foobar'. false] on: Error do: [:err| ^ err return: true]"));
    }

    @Test
    public void testFValue() {
        assertEquals(42L, evaluate("[42] value"));
        assertEquals(21L, evaluate("[[21] value] value"));
    }

    @Test
    public void testGSUnitTest() {
        assertEquals(image.sqTrue, evaluate("(TestCase new should: [1/0] raise: ZeroDivide) isKindOf: TestCase"));
    }

    @Test
    public void testWTinyBenchmarks() {
        String resultString = evaluate("1 tinyBenchmarks").toString();
        assertTrue(resultString.contains("bytecodes/sec"));
        assertTrue(resultString.contains("sends/sec"));
    }

    @Test
    public void testXPassingSqueakTests() {
        String[] testClasses = {"AddPrefixNamePolicyTest", "AliasTest", "AllNamePolicyTest", "AssociationTest", "BagTest", "BalloonFontTest", "BasicBehaviorClassMetaclassTest", "BasicTypeTest",
                        "BindingPolicyTest", "BitmapBugz", "BitSetTest", "BooleanTest", "ByteEncoderTest", "ChainedSortFunctionTest", "ClassBindingTest", "CollectionTest", "CompilerExceptionsTest",
                        "CompilerTest", "ContextCompilationTest", "DosFileDirectoryTests", "EventManagerTest", "ExpandedSourceFileArrayTest", "ExplicitNamePolicyTest", "GenericUrlTest", "GlobalTest",
                        "HandBugs", "HashAndEqualsTestCase", "HashedCollectionTest", "HashTesterTest", "HelpTopicListItemWrapperTest", "HelpTopicTest", "HttpUrlTest", "IdentityBagTest",
                        "InstructionClientTest", "InstVarRefLocatorTest", "IntegerArrayTest", "KeyedSetTest", "LongTestCaseTest", "LongTestCaseTestUnderTest", "MacFileDirectoryTest", "MatrixTest",
                        "MCDependencySorterTest", "MessageSendTest", "MonthTest", "MorphicExtrasSymbolExtensionsTest", "NamePolicyTest", "OrderedCollectionTest", "OrderedDictionaryTest",
                        "PackagePaneBrowserTest", "PluggableMenuItemSpecTests", "ProtoObjectTest", "RectangleTest", "RemovePrefixNamePolicyTest", "ScheduleTest", "SetTest", "SMDependencyTest",
                        "SortFunctionTest", "StackTest", "StandardSourceFileArrayTest", "SystemChangeErrorHandlingTest", "SystemChangeNotifierTest", "SystemChangeTestRoot", "SystemDictionaryTest",
                        "SystemOrganizerTest", "SystemVersionTest", "TestNewParagraphFix", "TestSpaceshipOperator", "TextAlignmentTest", "TextEmphasisTest", "TextFontChangeTest",
                        "TextFontReferenceTest", "TextKernTest", "TextLineTest", "TextStyleTest", "TextTest", "TimespanDoSpanAYearTest", "TimespanDoTest", "TraitsTestCase", "UndefinedObjectTest",
                        "UnimplementedCallBugz", "UnixFileDirectoryTests", "UUIDTest", "WeakFinalizersTest", "WeakSetInspectorTest", "WeekTest", "Win32VMTest", "YearMonthWeekTest", "YearTest"};
        image.getOutput().println();
        for (int i = 0; i < testClasses.length; i++) {
            String testClass = testClasses[i];
            if (evaluate(testClass + " buildSuite run hasPassed").equals(image.sqTrue)) {
                // Generate some output for TravisCI
                image.getOutput().print(".");
                image.getOutput().flush();
            } else {
                fail(testClass + " failed");
            }
        }
    }

    @Test
    public void testYFailingSqueakTests() {
        String[] testClasses = {"ArbitraryObjectSocketTestCase", "ArrayTest", "Ascii85ConverterTest", "Base64MimeConverterTest", "BecomeTest", "BehaviorTest", "BlockLocalTemporariesRemovalTest",
                        "BrowserHierarchicalListTest", "BrowserTest", "CharacterScannerTest", "CharacterSetComplementTest", "CharacterSetTest", "CharacterTest", "ClassAPIHelpBuilderTest",
                        "ClassDescriptionTest", "ClosureCompilerTest", "CogVMBaseImageTests", "CompiledMethodTrailerTest", "CompilerNotifyingTest", "CompilerSyntaxErrorNotifyingTest",
                        "DataStreamTest", "DateAndTimeEpochTest", "DateAndTimeLeapTest", "DateTest", "DebuggerExtensionsTest", "DependentsArrayTest", "DictionaryTest", "DoubleByteArrayTest",
                        "DoubleWordArrayTest", "DurationTest", "EPSCanvasTest", "EtoysStringExtensionTest", "FalseTest", "FileContentsBrowserTest", "FileDirectoryTest", "FileList2ModalDialogsTest",
                        "FileListTest", "FileUrlTest", "FloatArrayTest", "FloatCollectionTest", "FontTest", "FormTest", "GradientFillStyleTest", "HierarchicalUrlTest", "HierarchyBrowserTest",
                        "HtmlReadWriterTest", "InstallerUrlTest", "InstructionPrinterTest", "IntegerDigitLogicTest", "IslandVMTweaksTestCase", "JPEGReadWriter2Test", "LargeNegativeIntegerTest",
                        "LargePositiveIntegerTest", "LayoutFrameTest", "LinkedListTest", "MailAddressParserTest", "MailDateAndTimeTest", "MailMessageTest", "MCMcmUpdaterTest", "MCSortingTest",
                        "MethodHighlightingTests", "MethodPragmaTest", "MethodPropertiesTest", "MIMEDocumentTest", "MorphicEventDispatcherTests", "MorphicEventFilterTests", "MultiByteFileStreamTest",
                        "ObjectFinalizerTests", "ObjectTest", "ParserEditingTest", "ReadWriteStreamTest", "ReferenceStreamTest", "RunArrayTest", "RWBinaryOrTextStreamTest", "ScannerTest",
                        "SetWithNilTest", "SHParserST80Test", "SmallIntegerTest", "SmalltalkImageTest", "SmartRefStreamTest", "SMTPClientTest", "SocketStreamTest", "SocketTest", "SqueakSSLTest",
                        "StringTest", "SumBugs", "SymbolTest", "SystemChangeFileTest", "TestIndenting", "TestParagraphFix", "TestURI", "TextAnchorTest", "TextAttributesScanningTest",
                        "TextDiffBuilderTest", "TextLineEndingsTest", "TimespanTest", "TimeStampTest", "TimeTest", "UnderscoreSelectorsTest", "UrlTest", "UTF16TextConverterTest",
                        "UTF32TextConverterTest", "UTF8TextConverterTest", "UTFTextConverterWithByteOrderTest", "UUIDPrimitivesTest", "WeakIdentityKeyDictionaryTest", "WeakMessageSendTest",
                        "WeakSetTest", "WebClientServerTest", "WideCharacterSetTest", "WideStringTest", "WordArrayTest", "WriteStreamTest", "XMLParserTest"};
        image.getOutput().println();
        image.getOutput().println("== Failing Squeak Tests ===================");
        for (int i = 0; i < testClasses.length; i++) {
            String testClass = testClasses[i];
            image.getOutput().print(testClass + ": ");
            String resultString = evaluate(testClass + " buildSuite run asString").toString();
            image.getOutput().println(resultString);
            Matcher runMatcher = runPattern.matcher(resultString);
            Matcher passesMatcher = passesPattern.matcher(resultString);
            if (runMatcher.find() && passesMatcher.find()) {
                if (runMatcher.group(1).equals(passesMatcher.group(1))) {
                    fail(testClass + " was failing but appears to be passing now");
                }
            } else {
                fail("Unable to find number of runs or/and number of passes");
            }
        }
        image.getOutput().print("==================================== ");
        image.getOutput().flush();
    }

    @BeforeClass
    public static void setUpSqueakImageContext() {
        PrintWriter out = new PrintWriter(System.out, true);
        PrintWriter err = new PrintWriter(System.err, true);
        image = new SqueakImageContext(null, null, out, err);
        try {
            image.fillInFrom(new FileInputStream(IMAGE_PATH));
        } catch (IOException e) {
            e.printStackTrace();
        }
        patchTestCaseTimeoutAfter();
    }

    private static void patchTestCaseTimeoutAfter() {
        /*
         * Disable timeout logic by patching TestCase>>#timeout:after: (uses processes -> incompatible to
         * running headless).
         */
        Object patchResult = evaluate(
                        "TestCase addSelectorSilently: #timeout:after: withMethod: (TestCase compile: 'timeout: aBlock after: seconds ^ aBlock value' notifying: nil trailer: (CompiledMethodTrailer empty) ifFail: [^ nil]) method");
        assertNotEquals(image.nil, patchResult);
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
}

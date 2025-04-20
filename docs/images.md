# Images for TruffleSqueak

## TruffleSqueak Image Creation

Run the following in a workspace, then save and quit the image:

```smalltalk
Metacello new
    baseline: 'TruffleSqueak';
    repository: 'github://hpi-swa/trufflesqueak:image/src';
    load: #('tests').
Metacello new
    baseline: 'AWFYBenchmarks';
    repository: 'github://hpi-swa/trufflesqueak:image-awfy/src';
    load.
(Smalltalk at: #TruffleSqueakUtilities) setUpImage.
```

## TruffleSqueak Test Image Creation

Run the following in a workspace, then save and quit the image:

```smalltalk
Metacello new
    baseline: 'TruffleSqueak';
    repository: 'github://hpi-swa/trufflesqueak:image/src';
    load: #('tests').
(Smalltalk at: #TruffleSqueakUtilities) setUpTestImage.
```

## Cuis Test Image Creation

Run the following in a [Cuis-Smalltalk-Dev](https://github.com/Cuis-Smalltalk/Cuis-Smalltalk-Dev) checkout:

```bash
./CuisVM.app/Contents/MacOS/Squeak CuisImage/Cuis?.?-????.image -d "\
  Utilities classPool at: #AuthorName put: 'TruffleSqueak'.
  Utilities classPool at: #AuthorInitials put: 'TS'.
  ChangeSet installNewUpdates.
  CodePackageFile installPackage: DirectoryEntry currentDirectory // 'Packages/BaseImageTests.pck.st'.
  ChangeSet fileIn: DirectoryEntry currentDirectory // '.ContinuousIntegrationScripts/TestResultConsolePrinter.st'.
  Smalltalk saveAndQuit.
"
```

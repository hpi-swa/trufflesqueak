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

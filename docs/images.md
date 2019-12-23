# Images for GraalSqueak

## GraalSqueak Image Creation

Run the following in a workspace, then save and quit the image:

```smalltalk
Metacello new
    baseline: 'GraalSqueak';
    repository: 'filetree://{path/to/image-branch}/src';
    load.
(Smalltalk at: #GraalSqueakUtilities) setupImage.
```

## GraalSqueak Test Image Creation

Run the following in a workspace, then save and quit the image:

```smalltalk
Metacello new
    baseline: 'GraalSqueak';
    repository: 'filetree://{path/to/image-branch}/src';
    load.
(Smalltalk at: #GraalSqueakUtilities) setupTestImage.
```

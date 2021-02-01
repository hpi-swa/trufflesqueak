statistics
getAllLayouts
	^ (((Class allSubInstances select: [ :ea | ea instSpec = 1]) "All PointersObjects"
		collect: [ :ea | (self primitiveGetVMObject: ea) ]) select: [ :ea | ea hasLayout ] )
		collect: [ :ea | ea getLayout ].
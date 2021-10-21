primitives
formToBufferedImage: aForm
	| cm sm ints db raster |
	cm := (Java type: 'java.awt.image.DirectColorModel') new: 32
		and: 16r00ff0000 and: 16r0000ff00 and: 16r000000ff and: "16rff000000" -16777216.
	sm := cm createCompatibleSampleModel: aForm width and: aForm height.
	ints := aForm bits vmObject storage.
	db := (Java type: 'java.awt.image.DataBufferInt') new: (
		TruffleSqueakUtilities primitiveVMObjectToHostObject: ints) and: ints size and: 0.
	raster := (Java type: 'java.awt.image.Raster') createWritableRaster: sm and: db and: nil.
	^ (Java type: 'java.awt.image.BufferedImage') new: cm and: raster and: true and: nil.

helpers
closeAllWindowsAndMorphs
	ReleaseBuilder deleteAllWindows.
	"Delete all logos and buttons"
	{ImageMorph. SimpleButtonMorph} do: [ :ea |
		(World submorphsSatisfying: [ :m |	(m isKindOf: ea) ]) do: [ :m | m delete ] ].
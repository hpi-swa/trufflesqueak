private
createAnnotationMorph: aString

	^ Text
		string: (String value: 1) 
		attribute: (TextAnchor new anchoredMorph: (CircleMorph new
			changeTableLayout;
			hResizing: #rigid;
			vResizing: #rigid;
			extent: 10@10;
			color: Color red;
			balloonText: aString))
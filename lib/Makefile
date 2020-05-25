#
# Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
#
# Licensed under the MIT License.
#

TARGET = SqueakFFIPrims

ifeq ($(OS),Windows_NT)
	LIBFLAGS = -shared
	EXTENSION = dll
else
	UNAME_S := $(shell uname -s)
	ifeq ($(UNAME_S),Linux)
		LIBFLAGS = -shared
		EXTENSION = so
	else ifeq ($(UNAME_S),Darwin)
		LIBFLAGS = -dynamiclib
		EXTENSION = dylib
	endif
endif

$(TARGET).$(EXTENSION): $(TARGET).c
	$(CC) -fPIC $(LIBFLAGS) $(TARGET).c -o $(TARGET).$(EXTENSION)

.PHONY: clean
clean:
	rm -f $(TARGET).$(EXTENSION)

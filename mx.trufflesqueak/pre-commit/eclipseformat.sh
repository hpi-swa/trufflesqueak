#!/usr/bin/env bash
#
# Copyright (c) 2026-2026 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2026-2026 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

mx eclipseformat --no-backup --primary --filelist <(echo "${@}" | tr ' ' '\n')

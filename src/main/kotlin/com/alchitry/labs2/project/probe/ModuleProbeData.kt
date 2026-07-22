package com.alchitry.labs2.project.probe

import com.alchitry.labs2.parsers.hdl.types.Signal

data class ModuleProbeData(
    val signals: List<Signal>,
    val subModules: Collection<ModuleProbeData>
)
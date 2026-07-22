package com.alchitry.labs2.project.probe

import com.alchitry.labs2.parsers.hdl.types.ModuleInstance
import com.alchitry.labs2.parsers.hdl.types.Signal

sealed interface SignalTreeNode {
    val parent: SignalTree?

    fun hasParent(parent: SignalTree): Boolean {
        var current: SignalTreeNode = this
        while (true) {
            if (current.parent == parent) {
                return true
            }
            current = current.parent ?: return false
        }
    }
}

class SignalTree(
    val module: ModuleInstance,
    override val parent: SignalTree?,
    childrenBuilder: (SignalTree) -> List<SignalTreeNode>
) : SignalTreeNode {
    val children: List<SignalTreeNode> = childrenBuilder(this)
    override fun toString(): String {
        return "SignalTree(module=$module, children=$children)"
    }
}

class SignalNode(
    val signal: Signal,
    override val parent: SignalTree
) : SignalTreeNode {
    override fun toString(): String {
        return "SignalNode(signal=$signal)"
    }
}
package com.quadient.wfdxml.utils

class Util {
    static String emptyWorkflow() {
        return wrapWithWorkflow("")
    }

    static def wrapWithWorkflow = { it ->
        """\
        <?xml version="1.0" encoding="UTF-8"?>
        <WorkFlow><Property><Name>WorkflowLock</Name><Value>false</Value></Property>$it</WorkFlow>""".stripIndent().trim()
    }
}
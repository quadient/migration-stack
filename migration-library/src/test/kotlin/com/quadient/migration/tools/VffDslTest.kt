package com.quadient.migration.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VffDslTest {

    @Test
    fun `simple case`() {
        val flow = flow {
            type = Flow.Type.SelectByCondition

            condition {
                inline = "outer"

                flow {
                    type = Flow.Type.Section

                    p {
                        +"some text"
                    }

                    condition {
                        inline = "inner"

                        p {
                            +"other text"
                        }
                    }
                    default {}
                }
            }
            default {}

        }

        val result = flow.toString()

        assertEquals(
            """<flow type="selectbycondition"><condition inline="outer"><flow type="section"><p>some text</p><condition inline="inner"><p>other text</p></condition><default></default></flow></condition><default></default></flow>""",
            result
        )
    }
}
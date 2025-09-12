package utils

import com.quadient.migration.example.common.util.Csv
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class CsvTest {


    @TestFactory
    Collection<DynamicTest> test() {
        return ([Tuple.tuple(null, []),
                 Tuple.tuple("", [""]),
                 Tuple.tuple("a,b,c", ["a", "b", "c"]),
                 Tuple.tuple("a,b,", ["a", "b", ""]),
                 Tuple.tuple(",", ["", ""]),
                 Tuple.tuple(",,", ["", "", ""]),
                 Tuple.tuple("\"a\",\"b\",\"c\"", ["a", "b", "c"]),
                 Tuple.tuple("\"a,with,commas\",b", ["a,with,commas", "b"]),
                 Tuple.tuple("\"a,with,commas\"\"inside,quotes\"\"\",b", ["a,with,commas\"inside,quotes\"", "b"]),
                 Tuple.tuple("\"a \"\"quoted\"\" word\",x", ["a \"quoted\" word", "x"]),
                 Tuple.tuple("\"embedded \"\"quotes\"\" and, commas\",end", ["embedded \"quotes\" and, commas", "end"]),
                 Tuple.tuple(" spaced , values ", [" spaced ", " values "]),
                 Tuple.tuple("\"unclosed,field", ["unclosed,field"]),
                 Tuple.tuple("\"\"", [""]),
                 Tuple.tuple("\"\",", ["", ""]),
                 Tuple.tuple("\"\",x", ["", "x"])] as List<Tuple2<String, List<String>>>)
            .collect { input ->
                DynamicTest.dynamicTest(input.v1 ?: "null") {
                    def split = Csv.split(input.v1)
                    Assertions.assertEquals(input.v2, split)
                }
            }
    }
}

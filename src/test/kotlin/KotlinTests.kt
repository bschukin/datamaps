import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.f
import org.testng.annotations.Test

/**
 * Created by Щукин on 27.10.2017.
 */
class KotlinTests {

    @Test
    fun workWithMaps() {

        val map = mutableMapOf<String, String>()
        map["xxx"] = "zzz";

        println(map["xxx"])

        var myJson = """
                  user(id: 1) {
                    name
                    age
                    friends {
                      name
                    }
                  }
                }
        """.trimIndent()

        println(myJson)

    }

    @Test
    fun testInfixFunctions() {

        //val e1 =

        val dp = DataProjection("dasd")
                .filter({
                    val e = f("hw") gt 10 or f("xxx")
                    e and f("bnn")
                })
    }


}




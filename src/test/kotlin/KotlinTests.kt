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
}



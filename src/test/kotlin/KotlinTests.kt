
import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.f
import org.apache.commons.lang.text.StrSubstitutor
import org.testng.annotations.Test
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

/**
 * Created by Щукин on 27.10.2017.
 */
class KotlinTests {

    @Test
    fun workWithMaps() {

        val map = mutableMapOf<String, String>()
        map["xxx"] = "zzz";

        println(map["xxx"])

        val myJson = """
                  user(id: 1) {
                    n
                    age
                    friends {
                      n
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
                    val e = f("hw") gt 10 or ((
                            f("xxx")
                            ))
                    e and f("bnn")
                })
    }

    @Test
    fun testStrSubstitor() {



        var myStr = "hello {{world}}"
        val map = mutableMapOf<String, String>()
        map.put("world", "hell")
        var s = StrSubstitutor(map, "{{", "}}")



        println(s.replace(myStr))

    }

    var someVar= 0
    @Test
    fun testKolinReflection() {

        voidd(A::b)
        voidd(A::z)
        void(A::class)
    }

    private fun void(kClass: KClass<*>) {

    }


    public class DProjection(kClass: KClass<*>)
    {

    }

    fun voidd(objectt: KCallable<*>)
    {
        println(objectt.name)
    }


    class A
    {
        var b:B? = null
        var z:B? = null
    }

    class B {
        var name2 = ""
    }
}



